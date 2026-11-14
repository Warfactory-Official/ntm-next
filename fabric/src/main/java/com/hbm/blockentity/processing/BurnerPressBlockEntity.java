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

package com.hbm.blockentity.processing;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.container.PressMenu;
import com.hbm.items.machine.StampItem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
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

public final class BurnerPressBlockEntity extends StampingMachineBlockEntity {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_STORAGE_START = 4;
    public static final int SLOT_STORAGE_COUNT = 9;
    public static final int SLOT_COUNT = 13;
    public static final int MAX_SPEED = 400;
    public static final int FUEL_PER_OP = 200;
    private static final int[] AUTOMATION = {0, 1, 2, 3};
    private static final Direction[] FACES = Direction.values();

    public int burnTime;
    public int speed;
    private boolean preheated;

    public BurnerPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESS.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickClient() {
        super.tickClient();
    }

    @Override
    public void tickServer() {
        boolean ready = canProcess();
        int nextSpeed =
                burnTime >= FUEL_PER_OP && (ready || isRetracting)
                        ? Math.min(MAX_SPEED, speed + (preheated ? 4 : 1))
                        : Math.max(0, speed - 1);
        boolean changed = nextSpeed != speed;
        hbm$syncSet$BurnerPressBlockEntity$speed(nextSpeed);
        if (delay > 0) {
            delay--;
            changed = true;
        } else {
            int step = speed * 25 / MAX_SPEED;
            if (isRetracting || progress > 0 && !ready) {
                if (!isRetracting) {
                    isRetracting = true;
                    changed = true;
                }
                if (step > 0) {
                    hbm$syncSet$StampingMachineBlockEntity$progress((int) (progress - (step)));
                    changed = true;
                    if (progress <= 0) {
                        isRetracting = false;
                        delay = STROKE_PAUSE_TICKS;
                    }
                }
            } else if (ready && burnTime >= FUEL_PER_OP && step > 0) {
                hbm$syncSet$StampingMachineBlockEntity$progress((int) (progress + (step)));
                changed = true;
                if (progress >= 200) {
                    completeStamp();
                    hbm$syncSet$BurnerPressBlockEntity$burnTime((int) (burnTime - (FUEL_PER_OP)));
                    isRetracting = true;
                    delay = STROKE_PAUSE_TICKS;
                }
            }
        }
        if (burnTime < FUEL_PER_OP) {
            var fuel = inventory.get(SLOT_FUEL);
            int credit = fuelDuration(fuel);
            if (credit > 0) {
                var remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty())
                    inventory.set(
                            SLOT_FUEL, remainder == null ? ItemStack.EMPTY : remainder.create());
                hbm$syncSet$BurnerPressBlockEntity$burnTime((int) (burnTime + (credit)));
                changed = true;
            }
        }
        if (changed) setChanged();
        networkPackNT(50);
    }

    public void refreshPreheater() {
        var cursor = new BlockPos.MutableBlockPos();
        boolean heated = false;
        for (var face : FACES) {
            cursor.setWithOffset(worldPosition, face);
            if (level.getBlockState(cursor).is(ModBlocks.PRESS_PREHEATER.get())) heated = true;
        }
        if (preheated != heated) {
            preheated = heated;
            setChanged();
        }
    }

    private int fuelDuration(ItemStack stack) {
        return FuelHandler.getBurnTime(this, stack);
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
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> fuelDuration(stack) > 0;
            case SLOT_STAMP -> stack.getItem() instanceof StampItem;
            case SLOT_INPUT -> !(stack.getItem() instanceof StampItem);
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.press");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PressMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$BurnerPressBlockEntity$burnTime(input.getIntOr("burnTime", burnTime));
        hbm$syncSet$StampingMachineBlockEntity$progress(input.getIntOr("progress", progress));
        hbm$syncSet$BurnerPressBlockEntity$speed(input.getIntOr("speed", speed));
        isRetracting = input.getBooleanOr("isRetracting", false);
        delay = input.getIntOr("delay", 0);
        preheated = input.getBooleanOr("preheated", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burnTime", burnTime);
        output.putInt("progress", progress);
        output.putInt("speed", speed);
        output.putBoolean("isRetracting", isRetracting);
        output.putInt("delay", delay);
        output.putBoolean("preheated", preheated);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xF;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(progress);
            case 1 -> output.writeInt(speed);
            case 2 -> output.writeInt(burnTime);
            case 3 -> writePreview(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readStroke(input);
            case 1 -> hbm$syncSet$BurnerPressBlockEntity$speed(input.readInt());
            case 2 -> hbm$syncSet$BurnerPressBlockEntity$burnTime(input.readInt());
            case 3 -> readPreview(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$BurnerPressBlockEntity$burnTime(int next) {
        if (this.burnTime != next) {
            this.burnTime = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$BurnerPressBlockEntity$speed(int next) {
        if (this.speed != next) {
            this.speed = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                BurnerPressBlockEntity.class, new int[] {1, 2, 3}, 8, false, true);
    }
}
