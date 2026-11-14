/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.EPressMenu;
import com.hbm.items.machine.StampItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ElectricPressBlockEntity extends StampingMachineBlockEntity
        implements EnergyHandler, UpgradeInfoProvider {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE = 4;
    public static final int SLOT_COUNT = 5;
    public static final long MAX_POWER = 50_000;
    private static final int[] AUTOMATION = {1, 2, 3};
    private static final int[] EXTENSION = {56, 67, 78, 90};
    private static final int[] RETRACTION = {25, 30, 35, 40};
    private static final UpgradeLimits UPGRADES = UpgradeLimits.of(UpgradeType.SPEED, 3);

    public long power;

    public ElectricPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_PRESS.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickClient() {
        super.tickClient();
    }

    @Override
    public void tickServer() {
        long charge = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        hbm$syncSet$ElectricPressBlockEntity$power((long) (power + (charge)));
        boolean changed = charge != 0;
        boolean ready = canProcess();
        if (power >= 100 && (ready || isRetracting || delay > 0)) {
            hbm$syncSet$ElectricPressBlockEntity$power((long) (power - (100)));
            changed = true;
            int tier =
                    Math.min(
                            3,
                            MachineUpgradeItem.getLevel(
                                    inventory.get(SLOT_UPGRADE), UpgradeType.SPEED));
            if (delay > 0) {
                delay--;
            } else if (isRetracting) {
                hbm$syncSet$StampingMachineBlockEntity$progress(
                        (int) (progress - (RETRACTION[tier])));
                if (progress <= 0) {
                    isRetracting = false;
                    delay = STROKE_PAUSE_TICKS - tier;
                }
            } else if (ready) {
                hbm$syncSet$StampingMachineBlockEntity$progress(
                        (int) (progress + (EXTENSION[tier])));
                if (progress >= MAX_PROGRESS) {
                    completeStamp();
                    isRetracting = true;
                    delay = STROKE_PAUSE_TICKS - tier;
                }
            }
        }
        if (changed) setChanged();
        networkPackNT(50);
    }

    @Override
    protected void syncPress() {
        WorldSounds.machine(
                level,
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                ModSounds.PRESS_OPERATE.get(),
                SoundSource.BLOCKS,
                isMuffled() ? 0.15F : 1.5F,
                1F);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$ElectricPressBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) {
            tooltip.add(
                    Component.translatable("tooltip.hbm.upgrade.press_stroke", 1.25 + 0.25 * tier)
                            .withStyle(ChatFormatting.GREEN));
            tooltip.add(
                    Component.translatable(
                                    "tooltip.hbm.upgrade.press_pause", STROKE_PAUSE_TICKS - tier)
                            .withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_STAMP -> stack.getItem() instanceof StampItem;
            case SLOT_INPUT -> !(stack.getItem() instanceof StampItem);
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.epress");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new EPressMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ElectricPressBlockEntity$power(input.getLongOr("power", 0));
        hbm$syncSet$StampingMachineBlockEntity$progress(input.getIntOr("progress", progress));
        isRetracting = input.getBooleanOr("isRetracting", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putBoolean("isRetracting", isRetracting);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(progress);
            case 2 -> writePreview(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ElectricPressBlockEntity$power(input.readLong());
            case 1 -> readStroke(input);
            case 2 -> readPreview(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ElectricPressBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                ElectricPressBlockEntity.class, new int[] {1, 2, 3}, 4, false, true);
    }
}
