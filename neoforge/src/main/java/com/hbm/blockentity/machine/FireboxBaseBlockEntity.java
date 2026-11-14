/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.tile.HeatHandler;
import com.hbm.blockentity.HatchSwing;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blockentity.machine.AshpitBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.items.variant.BriquetteType;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.hbm.inventory.material.MaterialTagRegistry.ANY_COKE;
import static com.hbm.inventory.material.MaterialTagRegistry.COAL;
import static com.hbm.inventory.material.MaterialTagRegistry.COALCOKE;
import static com.hbm.inventory.material.MaterialTagRegistry.KEY_STICK;
import static com.hbm.inventory.material.MaterialTagRegistry.LIGCOKE;
import static com.hbm.inventory.material.MaterialTagRegistry.LIGNITE;
import static com.hbm.inventory.material.MaterialTagRegistry.PETCOKE;
import static com.hbm.inventory.material.MaterialTagRegistry.WOOD;

public abstract class FireboxBaseBlockEntity extends PollutingMachineBlockEntity
        implements MachineSoundProvider, HeatHandler, FluidTankHandler, SyncUnitSchema {

    private static final int[] ACCESSIBLE_SLOTS = {0, 1};

    private static final List<TagKey<Item>> COAL_ASH =
            List.of(
                    COAL.gem(),
                    COAL.dustTiny(),
                    COAL.dust(),
                    COAL.ore(),
                    COAL.block(),
                    COAL.fragment(),
                    LIGNITE.gem(),
                    LIGNITE.dust(),
                    LIGNITE.ore(),
                    LIGNITE.fragment(),
                    COALCOKE.gem(),
                    COALCOKE.block(),
                    PETCOKE.gem(),
                    PETCOKE.block(),
                    LIGCOKE.gem(),
                    LIGCOKE.block(),
                    ANY_COKE.gem(),
                    ANY_COKE.block());
    private static final List<TagKey<Item>> WOOD_ASH =
            List.of(
                    ItemTags.LOGS,
                    ItemTags.PLANKS,
                    ItemTags.WOODEN_SLABS,
                    ItemTags.WOODEN_STAIRS,
                    KEY_STICK,
                    ItemTags.SAPLINGS,
                    WOOD.stock(),
                    WOOD.grip());

    public int maxBurnTime;
    public int burnTime;
    public int burnHeat;
    public boolean wasOn;

    {
        hbm$syncSet$FireboxBaseBlockEntity$wasOn(false);
    }

    public float doorAngle = 0;
    public float prevDoorAngle = 0;
    public int heatEnergy;
    private int playersUsing;

    {
        hbm$syncSet$FireboxBaseBlockEntity$playersUsing(0);
    }

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    protected FireboxBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, 50);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!level.isClientSide())
            this.hbm$syncSet$FireboxBaseBlockEntity$playersUsing((int) (this.playersUsing + 1));
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!level.isClientSide())
            this.hbm$syncSet$FireboxBaseBlockEntity$playersUsing((int) (this.playersUsing - 1));
    }

    @Override
    public void tickServer() {
        hbm$syncSet$FireboxBaseBlockEntity$wasOn(false);
        boolean canOperate;

        if (burnTime <= 0) {
            canOperate = breatheAir(0);

            for (int i = 0; i < 2; i++) {
                ItemStack stack = inventory.get(i);
                if (stack.isEmpty()) continue;

                int baseTime = getModule().getBurnTime(level, stack);
                if (baseTime <= 0) continue;

                this.hbm$syncSet$FireboxBaseBlockEntity$burnTime((int) (baseTime * getTimeMult()));
                this.hbm$syncSet$FireboxBaseBlockEntity$maxBurnTime(this.burnTime);
                this.hbm$syncSet$FireboxBaseBlockEntity$burnHeat(
                        getModule().getBurnHeat(getBaseHeat(), stack));

                if (level.getBlockEntity(worldPosition.below())
                        instanceof AshpitBlockEntity ashpit) {
                    ashpit.addAsh(getAshFromFuel(stack), baseTime);
                    ashpit.markChanged();
                }

                ItemStackTemplate remainder = stack.getCraftingRemainder();
                stack.shrink(1);
                if (stack.isEmpty() && remainder != null) inventory.set(i, remainder.create());
                markChanged();

                this.hbm$syncSet$FireboxBaseBlockEntity$wasOn(true);
                break;
            }
        } else {

            if (this.heatEnergy < getMaxHeat()) {
                canOperate = breatheAir(TickPhase.every(this, 500 / getBaseHeat()) ? 1 : 0);
                if (canOperate) {
                    hbm$syncSet$FireboxBaseBlockEntity$burnTime((int) (burnTime - 1));
                    markChanged();
                    if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                        this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
                    }
                }
            } else {
                canOperate = breatheAir(0);
            }

            if (canOperate) {
                this.hbm$syncSet$FireboxBaseBlockEntity$wasOn(true);

                if (level.getRandom().nextInt(15) == 0 && !isMuffled()) {
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition,
                            SoundEvents.FIRE_AMBIENT,
                            SoundSource.BLOCKS,
                            1.0F,
                            0.5F + level.getRandom().nextFloat() * 0.5F);
                }
            }
        }

        int prevHeat = this.heatEnergy;
        int prevBurnHeat = this.burnHeat;
        if (wasOn) {
            this.hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(
                    Math.min(this.heatEnergy + this.burnHeat, getMaxHeat()));
        } else {
            this.hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(
                    Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0));
            if (canOperate) this.hbm$syncSet$FireboxBaseBlockEntity$burnHeat(0);
        }
        if (this.heatEnergy != prevHeat || this.burnHeat != prevBurnHeat) markChanged();

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        prevDoorAngle = doorAngle;
        doorAngle = HatchSwing.next(doorAngle, playersUsing > 0);

        if (wasOn && TickPhase.every(this, 5)) {
            Direction dir = MultiblockCoreBlock.coreFacing(getBlockState());
            double x = worldPosition.getX() + 0.5 + dir.getStepX();
            double y = worldPosition.getY() + 0.25;
            double z = worldPosition.getZ() + 0.5 + dir.getStepZ();
            level.addParticle(
                    ParticleTypes.FLAME,
                    x + level.getRandom().nextDouble() * 0.5 - 0.25,
                    y + level.getRandom().nextDouble() * 0.25,
                    z + level.getRandom().nextDouble() * 0.5 - 0.25,
                    0,
                    0,
                    0);
        }
    }

    public static AshType getAshFromFuel(ItemStack stack) {
        BriquetteType briquette = ModItems.BRIQUETTE.typeOf(stack);
        if (briquette != null) return briquette == BriquetteType.WOOD ? AshType.WOOD : AshType.COAL;
        for (TagKey<Item> tag : COAL_ASH) if (stack.is(tag)) return AshType.COAL;
        for (TagKey<Item> tag : WOOD_ASH) if (stack.is(tag)) return AshType.WOOD;
        return AshType.MISC;
    }

    public abstract ModuleBurnTime getModule();

    public abstract int getBaseHeat();

    public abstract double getTimeMult();

    public abstract int getMaxHeat();

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return getModule().getBurnTime(level, stack) > 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.hbm$syncSet$FireboxBaseBlockEntity$maxBurnTime(input.getIntOr("maxBurnTime", 0));
        this.hbm$syncSet$FireboxBaseBlockEntity$burnTime(input.getIntOr("burnTime", 0));
        this.hbm$syncSet$FireboxBaseBlockEntity$burnHeat(input.getIntOr("burnHeat", 0));
        this.hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(input.getIntOr("heatEnergy", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("burnTime", burnTime);
        output.putInt("burnHeat", burnHeat);
        output.putInt("heatEnergy", heatEnergy);
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void setHeatStored(int remaining) {
        hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(remaining);
        markChanged();
    }

    @Override
    public int tankCount() {
        return 0;
    }

    @Override
    public FluidTankNTM tank(int index) {
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.maxBurnTime);
            case 1 -> output.writeInt(this.burnTime);
            case 2 -> output.writeInt(this.burnHeat);
            case 3 -> output.writeInt(this.heatEnergy);
            case 4 -> output.writeInt(this.playersUsing);
            case 5 -> output.writeBoolean(this.wasOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$FireboxBaseBlockEntity$maxBurnTime(input.readInt());
            case 1 -> this.hbm$syncSet$FireboxBaseBlockEntity$burnTime(input.readInt());
            case 2 -> this.hbm$syncSet$FireboxBaseBlockEntity$burnHeat(input.readInt());
            case 3 -> this.hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(input.readInt());
            case 4 -> this.hbm$syncSet$FireboxBaseBlockEntity$playersUsing(input.readInt());
            case 5 -> this.hbm$syncSet$FireboxBaseBlockEntity$wasOn(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$FireboxBaseBlockEntity$maxBurnTime(int next) {
        if (this.maxBurnTime != next) {
            this.maxBurnTime = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$FireboxBaseBlockEntity$burnTime(int next) {
        if (this.burnTime != next) {
            this.burnTime = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$FireboxBaseBlockEntity$burnHeat(int next) {
        if (this.burnHeat != next) {
            this.burnHeat = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$FireboxBaseBlockEntity$wasOn(boolean next) {
        if (this.wasOn != next) {
            this.wasOn = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$FireboxBaseBlockEntity$heatEnergy(int next) {
        if (this.heatEnergy != next) {
            this.heatEnergy = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$FireboxBaseBlockEntity$playersUsing(int next) {
        if (this.playersUsing != next) {
            this.playersUsing = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }
}
