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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.container.RBMKAutoloaderMenu;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.SoundHandle;
import com.hbm.util.SyncInterpolation;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class RBMKAutoloaderBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                SettingsHandler,
                MachineSoundProvider,
                MenuProvider,
                SyncUnitSchema {

    public static final double SPEED = 0.005D;
    public static final int SLOTS = 18;
    public static final int OUTPUT_START = 9;
    private static final int DELAY = 40;
    private static final int[] ALL_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
    };
    private static final int[] NO_SLOTS = {};

    public static Consumer<RBMKAutoloaderBlockEntity> CLIENT_SOUND = be -> {};
    public static Consumer<RBMKAutoloaderBlockEntity> CLIENT_TOWER = be -> {};

    public double piston;
    public double renderPiston;
    public double lastPiston;
    public @Nullable SoundHandle audioLift;
    public int cycle;

    {
        hbm$syncSet$RBMKAutoloaderBlockEntity$cycle(50);
    }

    public boolean isRetracting = true;
    private double syncPiston;
    private final SyncInterpolation ease = new SyncInterpolation();
    private int delay = 0;

    public RBMKAutoloaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_AUTOLOADER.get(), pos, state, SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkAutoloader");
    }

    @Override
    public void tickServer() {
        if (delay > 0) {
            delay--;
            markChanged();
        }

        if (delay <= 0 && isRetracting && piston > 0D) {
            hbm$syncSet$RBMKAutoloaderBlockEntity$piston((double) (piston - (SPEED)));
            markChanged();
            if (piston <= 0) {
                hbm$syncSet$RBMKAutoloaderBlockEntity$piston(0);
                delay = DELAY;
            }
        }

        if (isRetracting && level.getGameTime() % 20 == 0 && hasFuel() && hasSpace()) {
            RBMKRodBlockEntity rod = channel();
            if (rod != null && rod.coldEnoughForAutoloader()) {
                ItemStack loaded = rod.getItem(0);
                if (loaded.isEmpty()
                        || (loaded.getItem() instanceof RBMKRodItem
                                && RBMKRodItem.getEnrichment(loaded) * 100 < cycle)) {
                    isRetracting = false;
                    markChanged();
                }
            }
        }

        if (delay <= 0 && !isRetracting && piston < 1D) {
            hbm$syncSet$RBMKAutoloaderBlockEntity$piston((double) (piston + (SPEED)));
            markChanged();
            if (piston >= 1) {
                hbm$syncSet$RBMKAutoloaderBlockEntity$piston(1);
                delay = DELAY;
            }
        }

        if (!isRetracting && piston >= 1D) {
            hbm$syncSet$RBMKAutoloaderBlockEntity$piston(1D);

            RBMKRodBlockEntity rod = channel();
            if (rod != null) {
                ItemStack loaded = rod.getItem(0);
                if (!loaded.isEmpty() && hasSpace()) {
                    for (int i = OUTPUT_START; i < SLOTS; i++) {
                        if (getItem(i).isEmpty()) {
                            setItem(i, loaded.copy());
                            rod.setItem(0, ItemStack.EMPTY);
                            break;
                        }
                    }
                }
                if (rod.getItem(0).isEmpty()) {
                    for (int i = 0; i < OUTPUT_START; i++) {
                        ItemStack stack = getItem(i);
                        if (!stack.isEmpty()
                                && stack.getItem() instanceof RBMKRodItem
                                && RBMKRodItem.getEnrichment(stack) * 100 >= cycle) {
                            rod.setItem(0, stack.copy());
                            setItem(i, ItemStack.EMPTY);
                            break;
                        }
                    }
                }

                isRetracting = true;
                delay = DELAY;
                markChanged();
            }
        }

        networkPackNT(100);
    }

    private @Nullable RBMKRodBlockEntity channel() {
        BlockPos core = MultiblockSurface.coreOfAny(level, worldPosition.below());
        if (core == null) return null;
        return level.getBlockEntity(core) instanceof RBMKRodBlockEntity rod ? rod : null;
    }

    @Override
    public void tickClient() {
        lastPiston = renderPiston;
        renderPiston = ease.step(renderPiston, syncPiston);

        CLIENT_SOUND.accept(this);
        if (renderPiston > 0.99) CLIENT_TOWER.accept(this);
    }

    public boolean hasFuel() {
        for (int i = 0; i < OUTPUT_START; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()
                    && stack.getItem() instanceof RBMKRodItem
                    && RBMKRodItem.getEnrichment(stack) * 100 >= cycle) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpace() {
        for (int i = OUTPUT_START; i < SLOTS; i++) if (getItem(i).isEmpty()) return true;
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof RBMKRodItem
                && RBMKRodItem.getEnrichment(stack) * 100 >= cycle
                && slot < OUTPUT_START;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return piston <= 0 ? ALL_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RBMKAutoloaderBlockEntity$piston(input.getDoubleOr("piston", 0D));
        isRetracting = input.getBooleanOr("ret", false);
        delay = input.getIntOr("delay", 0);
        hbm$syncSet$RBMKAutoloaderBlockEntity$cycle(input.getIntOr("cycle", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("piston", piston);
        output.putBoolean("ret", isRetracting);
        output.putInt("delay", delay);
        output.putInt("cycle", cycle);
    }

    public void adjustCycle(boolean increase) {
        if (increase && cycle < 95)
            hbm$syncSet$RBMKAutoloaderBlockEntity$cycle((int) (cycle + (5)));
        if (!increase && cycle > 5)
            hbm$syncSet$RBMKAutoloaderBlockEntity$cycle((int) (cycle - (5)));
        hbm$syncSet$RBMKAutoloaderBlockEntity$cycle(Mth.clamp(cycle, 5, 95));
        markChanged();
    }

    @Override
    public @Nullable CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putInt("cycle", cycle);
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        nbt.getInt("cycle")
                .ifPresent(
                        value ->
                                hbm$syncSet$RBMKAutoloaderBlockEntity$cycle(
                                        Mth.clamp(value, 5, 95)));
        markChanged();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RBMKAutoloaderMenu(id, inv, this);
    }

    private void readPiston(ByteBuf input) {
        syncPiston = input.readDouble();
    }

    @Override
    public void afterSyncUnits(long units) {
        ease.restart(2);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(this.piston);
            case 1 -> output.writeInt(this.cycle);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPiston(input);
            case 1 -> this.hbm$syncSet$RBMKAutoloaderBlockEntity$cycle(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        return piston > 0.01 && piston < 0.99;
    }

    public final void hbm$syncSet$RBMKAutoloaderBlockEntity$piston(double next) {
        if (Double.doubleToRawLongBits(this.piston) != Double.doubleToRawLongBits(next)) {
            this.piston = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$RBMKAutoloaderBlockEntity$cycle(int next) {
        if (this.cycle != next) {
            this.cycle = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
