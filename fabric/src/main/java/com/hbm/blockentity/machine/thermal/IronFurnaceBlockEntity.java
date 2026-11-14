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

package com.hbm.blockentity.machine.thermal;

import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.FurnaceIronMenu;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class IronFurnaceBlockEntity extends SmeltingFurnaceBlockEntity
        implements MenuProvider, MachineSoundProvider, UpgradeInfoProvider, SyncUnitSchema {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL_A = 1;
    public static final int SLOT_FUEL_B = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_UPGRADE = 4;
    public static final int SLOT_COUNT = 5;
    private static final int[] EXPOSED = {SLOT_INPUT, SLOT_FUEL_A, SLOT_FUEL_B, SLOT_OUTPUT};
    private static final UpgradeLimits UPGRADES = UpgradeLimits.of(UpgradeType.SPEED, 3);
    public final ModuleBurnTime burnModule =
            new ModuleBurnTime()
                    .setLigniteTimeMod(1.25)
                    .setCoalTimeMod(1.25)
                    .setCokeTimeMod(1.5)
                    .setSolidTimeMod(2)
                    .setRocketTimeMod(2)
                    .setBalefireTimeMod(2);
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final SmeltingRecipeCache smelting = new SmeltingRecipeCache();

    public int maxBurnTime;
    public int burnTime;
    public int progress;
    public int processingTime;

    {
        hbm$syncSet$IronFurnaceBlockEntity$processingTime(160);
    }

    public boolean wasOn;
    public boolean smeltable;

    public IronFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_IRON.get(), pos, state, SLOT_COUNT, SLOT_OUTPUT);
    }

    @Override
    public void tickServer() {
        int previousBurn = burnTime;
        int previousProgress = progress;
        upgrades.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        hbm$syncSet$IronFurnaceBlockEntity$processingTime(
                160 - 80 * upgrades.getLevel(UpgradeType.SPEED) / 3);
        if (burnTime <= 0) {
            for (int slot = SLOT_FUEL_A; slot <= SLOT_FUEL_B; slot++) {
                int duration = fuelTime(getItem(slot));
                if (duration <= 0) continue;
                hbm$syncSet$IronFurnaceBlockEntity$burnTime(duration);
                hbm$syncSet$IronFurnaceBlockEntity$maxBurnTime(duration);
                SmeltingRecipeCache.consumeFuel(inventory, slot);
                setChanged();
                break;
            }
        }
        var input = getItem(SLOT_INPUT);
        var result = smelting.result((ServerLevel) level, input);
        hbm$syncSet$IronFurnaceBlockEntity$smeltable(
                burnTime > 0
                        && SmeltingRecipeCache.fits(
                                getItem(SLOT_OUTPUT), result, Integer.MAX_VALUE));
        hbm$syncSet$IronFurnaceBlockEntity$wasOn(
                smeltable && breatheAir(TickPhase.every(this, 5) ? 1 : 0));
        if (wasOn) {
            hbm$syncSet$IronFurnaceBlockEntity$progress((int) (progress + 1));
            hbm$syncSet$IronFurnaceBlockEntity$burnTime((int) (burnTime - 1));
            if (progress % 15 == 0 && !isMuffled()) {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS,
                        1F,
                        0.5F + 0.5F * level.getRandom().nextFloat());
            }
            if (progress >= processingTime) {
                SmeltingRecipeCache.insert(inventory, SLOT_OUTPUT, result);
                input.shrink(1);
                recipesUsed.record(0, smelting.recipe());
                hbm$syncSet$IronFurnaceBlockEntity$progress(0);
                setChanged();
            }
            if (TickPhase.every(this, 20))
                PollutionHandler.add(
                        level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
        } else hbm$syncSet$IronFurnaceBlockEntity$progress(0);
        if (previousBurn != burnTime || previousProgress != progress) setChanged();
        networkPackNT(50);
    }

    private int fuelTime(ItemStack stack) {
        return burnModule.getBurnTime(this, stack);
    }

    public boolean canSmelt() {
        return level.isClientSide()
                ? smeltable
                : burnTime > 0
                        && SmeltingRecipeCache.fits(
                                getItem(SLOT_OUTPUT),
                                smelting.result((ServerLevel) level, getItem(SLOT_INPUT)),
                                Integer.MAX_VALUE);
    }

    @Override
    public void tickClient() {
        if (progress <= 0) return;
        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        var across = facing.getClockWise();
        double distance = progress % 2 == 0 ? 1 : 0.5;
        double x = worldPosition.getX() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        level.addParticle(
                ParticleTypes.SMOKE,
                x - facing.getStepX() * distance - across.getStepX() * 0.1875,
                worldPosition.getY() + 2,
                z - facing.getStepZ() * distance - across.getStepZ() * 0.1875,
                0,
                0.01,
                0);
        if (progress % 5 == 0) {
            double spread = level.getRandom().nextFloat();
            double height = level.getRandom().nextFloat();
            level.addParticle(
                    ParticleTypes.FLAME,
                    x + facing.getStepX() * 0.25 + across.getStepX() * spread,
                    worldPosition.getY() + 0.25 + 0.25 * height,
                    z + facing.getStepZ() * 0.25 + across.getStepZ() * spread,
                    0,
                    0,
                    0);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return EXPOSED;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT ->
                    level instanceof ServerLevel server && smelting.accepts(server, stack);
            case SLOT_FUEL_A, SLOT_FUEL_B -> burnModule.isFuel(this, stack);
            default -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED)
            UpgradeInfo.percent(tooltip, "upgrade.delay", -50 * level / 3, true);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceIron");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FurnaceIronMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$IronFurnaceBlockEntity$maxBurnTime(input.getIntOr("maxBurnTime", maxBurnTime));
        hbm$syncSet$IronFurnaceBlockEntity$burnTime(input.getIntOr("burnTime", burnTime));
        hbm$syncSet$IronFurnaceBlockEntity$progress(input.getIntOr("progress", progress));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("burnTime", burnTime);
        output.putInt("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 63L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(maxBurnTime);
            case 1 -> output.writeInt(burnTime);
            case 2 -> output.writeInt(progress);
            case 3 -> output.writeInt(processingTime);
            case 4 -> output.writeBoolean(wasOn);
            case 5 -> output.writeBoolean(smeltable);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$IronFurnaceBlockEntity$maxBurnTime(input.readInt());
            case 1 -> hbm$syncSet$IronFurnaceBlockEntity$burnTime(input.readInt());
            case 2 -> hbm$syncSet$IronFurnaceBlockEntity$progress(input.readInt());
            case 3 -> hbm$syncSet$IronFurnaceBlockEntity$processingTime(input.readInt());
            case 4 -> hbm$syncSet$IronFurnaceBlockEntity$wasOn(input.readBoolean());
            case 5 -> hbm$syncSet$IronFurnaceBlockEntity$smeltable(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$IronFurnaceBlockEntity$maxBurnTime(int next) {
        if (this.maxBurnTime != next) {
            this.maxBurnTime = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$IronFurnaceBlockEntity$burnTime(int next) {
        if (this.burnTime != next) {
            this.burnTime = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$IronFurnaceBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$IronFurnaceBlockEntity$processingTime(int next) {
        if (this.processingTime != next) {
            this.processingTime = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    public final void hbm$syncSet$IronFurnaceBlockEntity$wasOn(boolean next) {
        if (this.wasOn != next) {
            this.wasOn = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    public final void hbm$syncSet$IronFurnaceBlockEntity$smeltable(boolean next) {
        if (this.smeltable != next) {
            this.smeltable = next;
            if ((32L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32L);
        }
    }
}
