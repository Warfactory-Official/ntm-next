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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.inventory.container.OreSlopperMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.special.BedrockOreBaseItem;
import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.BedrockOreNewItem.BedrockOreGrade;
import com.hbm.items.special.BedrockOreNewItem.BedrockOreType;
import com.hbm.items.special.BedrockOreNewItem;
import com.hbm.items.special.BedrockOreSample;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.EntitySounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class OreSlopperBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                UpgradeInfoProvider,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT_START = 3;
    public static final int SLOT_OUTPUT_END = 8;
    public static final int SLOT_UPGRADE_START = 9;
    public static final int SLOT_UPGRADE_END = 10;
    public static final int SLOT_COUNT = 11;

    public static final long maxPower = 100_000L;
    public static final int waterUsed = 1_000;
    public static final long consumptionBase = 200L;
    public static final int TANK_CAPACITY = 16_000;

    private static final int[] ACCESSIBLE_SLOTS = {2, 3, 4, 5, 6, 7, 8};
    private static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3).and(UpgradeType.EFFECT, 3);
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    public final double[] ores = new double[BedrockOreType.KINDS.values().size()];
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    public long power;
    public long consumption;

    {
        hbm$syncSet$OreSlopperBlockEntity$consumption(consumptionBase);
    }

    public float progress;
    public boolean processing;
    public SlopperAnimation animation = SlopperAnimation.LOWERING;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    public float slider, prevSlider;
    public float bucket, prevBucket;
    public float blades, prevBlades;
    public float fan, prevFan;
    public int delay;

    public OreSlopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_SLOPPER.get(), pos, state, SLOT_COUNT);
        hbm$syncArraySet$OreSlopperBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.WATER, TANK_CAPACITY));
        hbm$syncArraySet$OreSlopperBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.SLOP, TANK_CAPACITY));
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    private static @Nullable Fluid getFluidOutput(@Nullable Fluid input) {
        return input == NTMFluids.WATER ? NTMFluids.SLOP : null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineOreSlopper");
    }

    @Override
    public void tickServer() {
        hbm$syncSet$OreSlopperBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, maxPower - power, false))));

        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) markChanged();
        Fluid conversion = getFluidOutput(tanks[0].getTankType());
        if (conversion != null) tanks[1].setTankType(conversion);

        hbm$syncSet$OreSlopperBlockEntity$processing(false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int efficiency = Math.min(upgradeManager.getLevel(UpgradeType.EFFECT), 3);

        hbm$syncSet$OreSlopperBlockEntity$consumption(
                consumptionBase + (consumptionBase * speed) / 2 + (consumptionBase * efficiency));

        if (canSlop()) {
            hbm$syncSet$OreSlopperBlockEntity$power((long) (power - (consumption)));
            hbm$syncSet$OreSlopperBlockEntity$progress(
                    (float) (progress + (1F / (600 - speed * 150))));
            hbm$syncSet$OreSlopperBlockEntity$processing(true);
            markChanged();

            while (progress >= 1F && canSlop()) {
                hbm$syncSet$OreSlopperBlockEntity$progress((float) (progress - (1F)));

                BedrockOreSample sample = BedrockOreBaseItem.sample(inventory.get(SLOT_INPUT));
                double multiplier = 1D + efficiency * 0.1D;
                for (int i = 0; i < sample.amounts().length; i++) {
                    ores[BedrockOreType.KINDS.index(sample.set().kinds().get(i))] +=
                            sample.amounts()[i] * multiplier;
                }

                inventory.get(SLOT_INPUT).shrink(1);
                tanks[0].setFill(tanks[0].getFill() - waterUsed);
                tanks[1].setFill(tanks[1].getFill() + waterUsed);
                setChanged();
            }

            shred();
        } else {
            if (progress != 0) markChanged();
            hbm$syncSet$OreSlopperBlockEntity$progress(0);
        }

        List<BedrockOreKind> kinds = BedrockOreType.KINDS.values();
        for (int index = 0; index < kinds.size(); index++) {
            if (ores[index] < 1) continue;
            ItemStack output = BedrockOreNewItem.make(BedrockOreGrade.BASE, kinds.get(index));
            outer:
            while (ores[index] >= 1) {
                for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
                    ItemStack slot = inventory.get(i);
                    if (!slot.isEmpty()
                            && ItemStack.isSameItemSameComponents(slot, output)
                            && slot.getCount() < slot.getMaxStackSize()) {
                        slot.grow(1);
                        ores[index] -= 1F;
                        markChanged();
                        continue outer;
                    }
                }
                for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
                    if (inventory.get(i).isEmpty()) {
                        inventory.set(i, output.copy());
                        ores[index] -= 1F;
                        markChanged();
                        continue outer;
                    }
                }
                break;
            }
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    private void shred() {
        ServerLevel server = (ServerLevel) level;
        Direction dir = MultiblockCoreBlock.coreFacing(getBlockState());
        AABB box =
                new AABB(
                                worldPosition.getX() - 0.5D,
                                worldPosition.getY() + 1,
                                worldPosition.getZ() - 0.5D,
                                worldPosition.getX() + 1.5D,
                                worldPosition.getY() + 3,
                                worldPosition.getZ() + 1.5D)
                        .move(dir.getStepX(), 0, dir.getStepZ());

        for (Entity e : server.getEntities((Entity) null, box)) {
            e.hurtServer(server, server.damageSources().source(ModDamageTypes.BLENDER), 1000F);

            if (!e.isAlive() && e instanceof LivingEntity) {
                ParticleCreators.giblets(server, e, GibletType.MEAT, 5);
                EntitySounds.play(
                        e,
                        e.getX(),
                        e.getY(),
                        e.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2.0F,
                        0.95F + server.getRandom().nextFloat() * 0.2F);
            }
        }
    }

    @Override
    public void tickClient() {
        prevSlider = slider;
        prevBucket = bucket;
        prevBlades = blades;
        prevFan = fan;

        if (!processing) return;

        blades += 15F;
        fan += 35F;

        if (blades >= 360) {
            blades -= 360;
            prevBlades -= 360;
        }
        if (fan >= 360) {
            fan -= 360;
            prevFan -= 360;
        }

        Player viewer = ClientPlayerAccess.player();
        if (animation == SlopperAnimation.DUMPING
                && viewer != null
                && viewer.getEyePosition()
                                .distanceToSqr(
                                        worldPosition.getX() + 0.5,
                                        worldPosition.getY() + 4,
                                        worldPosition.getZ() + 0.5)
                        <= 2500) {
            Direction dir = MultiblockCoreBlock.coreFacing(getBlockState());
            RandomSource rand = level.getRandom();
            ClientEffects.spawnExtBlockDust(
                    worldPosition.getX() + 0.5 + dir.getStepX() + rand.nextGaussian() * 0.25,
                    worldPosition.getY() + 4.25,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() + rand.nextGaussian() * 0.25,
                    Blocks.IRON_BLOCK.defaultBlockState());
        }

        if (delay > 0) {
            delay--;
            return;
        }

        switch (animation) {
            case LOWERING -> {
                bucket += 1F / 40F;
                if (bucket >= 1F) {
                    bucket = 1F;
                    animation = SlopperAnimation.LIFTING;
                    delay = 20;
                }
            }
            case LIFTING -> {
                bucket -= 1F / 40F;
                if (bucket <= 0) {
                    bucket = 0F;
                    animation = SlopperAnimation.MOVE_SHREDDER;
                    delay = 10;
                }
            }
            case MOVE_SHREDDER -> {
                slider += 1F / 50F;
                if (slider >= 1F) {
                    slider = 1F;
                    animation = SlopperAnimation.DUMPING;
                    delay = 60;
                }
            }
            case DUMPING -> animation = SlopperAnimation.MOVE_BUCKET;
            case MOVE_BUCKET -> {
                slider -= 1F / 50F;
                if (slider <= 0F) {
                    animation = SlopperAnimation.LOWERING;
                    delay = 10;
                }
            }
        }
    }

    public boolean canSlop() {
        if (getFluidOutput(tanks[0].getTankType()) == null) return false;
        if (tanks[0].getFill() < waterUsed) return false;
        if (tanks[1].getFill() + waterUsed > tanks[1].getMaxFill()) return false;
        if (power < consumption) return false;
        ItemStack in = inventory.get(SLOT_INPUT);
        return !in.isEmpty() && in.getItem() == ModItems.BEDROCK_ORE_BASE.get();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        hbm$syncSet$OreSlopperBlockEntity$power(Math.max(0L, Math.min(p, maxPower)));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tanks[0];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> stack.getItem() == ModItems.BEDROCK_ORE_BASE.get();
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> MachineUpgradeItem.isUpgrade(stack);
            default -> slot < SLOT_OUTPUT_START || slot > SLOT_OUTPUT_END;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) UpgradeInfo.speedAndPower(tooltip, -level * 25, level * 50);
        else {
            UpgradeInfo.percent(tooltip, "upgrade.efficiency", level * 10, true);
            UpgradeInfo.percent(tooltip, "upgrade.consumption", level * 100, false);
        }
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new OreSlopperMenu(containerId, playerInventory, this);
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$OreSlopperBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$OreSlopperBlockEntity$progress(input.getFloatOr("progress", 0F));
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("slop").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("slop"));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(consumption);
            case 1 -> output.writeLong(power);
            case 2 -> output.writeBoolean(processing);
            case 3 -> output.writeFloat(progress);
            case 4 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$OreSlopperBlockEntity$consumption(input.readLong());
            case 1 -> hbm$syncSet$OreSlopperBlockEntity$power(input.readLong());
            case 2 -> hbm$syncSet$OreSlopperBlockEntity$processing(input.readBoolean());
            case 3 -> hbm$syncSet$OreSlopperBlockEntity$progress(input.readFloat());
            case 4 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public enum SlopperAnimation {
        LOWERING,
        LIFTING,
        MOVE_SHREDDER,
        DUMPING,
        MOVE_BUCKET
    }

    public final FluidTankNTM hbm$syncArraySet$OreSlopperBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 4);
            }
        }
        return next;
    }

    public final void hbm$syncSet$OreSlopperBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$OreSlopperBlockEntity$consumption(long next) {
        if (this.consumption != next) {
            this.consumption = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$OreSlopperBlockEntity$progress(float next) {
        if (Float.floatToRawIntBits(this.progress) != Float.floatToRawIntBits(next)) {
            this.progress = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$OreSlopperBlockEntity$processing(boolean next) {
        if (this.processing != next) {
            this.processing = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(OreSlopperBlockEntity.class, "tanks", 3, 1L << 4);
    }
}
