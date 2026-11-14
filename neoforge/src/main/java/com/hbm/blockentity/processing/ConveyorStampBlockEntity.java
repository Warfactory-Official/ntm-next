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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.inventory.recipes.PressRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.StampItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.SyncInterpolation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class ConveyorStampBlockEntity extends MachineBlockEntity
        implements EnergyHandler, SyncUnitSchema {
    public static final int SLOT_STAMP = 0;
    public static final long MAX_POWER = 50_000;

    public long power;
    public double press;
    public double renderPress;
    public double lastPress;
    public ItemStack syncStack = ItemStack.EMPTY;
    private boolean retracting;
    private int pause;
    private final AABB workVolume;
    private final SyncInterpolation strokeEase = new SyncInterpolation();

    public ConveyorStampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR_PRESS.get(), pos, state, 1);
        workVolume =
                new AABB(
                        pos.getX(),
                        pos.getY() + 1,
                        pos.getZ(),
                        pos.getX() + 1,
                        pos.getY() + 1.5,
                        pos.getZ() + 1);
    }

    private @Nullable PressRecipe recipeFor(
            MovingItemEntity rider, StampItem.@Nullable StampType stamp) {
        var stack = rider.getItemStack();
        return stack.getCount() == 1 ? PressRecipes.INSTANCE.getRecipe(stack, stamp) : null;
    }

    private boolean admit() {
        var stamp = StampItem.typeOf(inventory.get(SLOT_STAMP));
        if (stamp == null) return false;
        var riders = level.getEntitiesOfClass(MovingItemEntity.class, workVolume);
        for (var rider : riders) {
            if (recipeFor(rider, stamp) == null) continue;
            double x = rider.getX() - worldPosition.getX();
            double z = rider.getZ() - worldPosition.getZ();
            if (x > 0.35 && x < 0.65 && z > 0.35 && z < 0.65) {
                rider.setPos(worldPosition.getX() + 0.5, rider.getY(), worldPosition.getZ() + 0.5);
            }
            return true;
        }
        return false;
    }

    private void stampRiders() {
        var stamp = StampItem.typeOf(inventory.get(SLOT_STAMP));
        var riders = level.getEntitiesOfClass(MovingItemEntity.class, workVolume);
        for (var rider : riders) {
            var row = recipeFor(rider, stamp);
            if (row == null) continue;
            var product = row.output().copy();
            var replacement = new MovingItemEntity(level);
            replacement.setPos(rider.getX(), rider.getY(), rider.getZ());
            replacement.setItemStack(product);
            rider.discard();
            level.addFreshEntity(replacement);
        }
        StampingMachineBlockEntity.wearStamp(inventory, SLOT_STAMP);
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.PRESS_OPERATE.get(),
                SoundSource.BLOCKS,
                isMuffled() ? 0.15F : 1.5F,
                1F);
    }

    @Override
    public void tickServer() {
        if (pause > 0) {
            pause--;
        } else if (power >= 100 && (retracting || press > 0 || admit())) {
            hbm$syncSet$ConveyorStampBlockEntity$power((long) (power - (100)));
            if (retracting) {
                hbm$syncSet$ConveyorStampBlockEntity$press(Math.max(0, press - 0.125));
                if (press == 0) {
                    retracting = false;
                    pause = 5;
                }
            } else {
                hbm$syncSet$ConveyorStampBlockEntity$press(Math.min(1, press + 0.125));
                if (press == 1) {
                    stampRiders();
                    retracting = true;
                    pause = 5;
                }
            }
            setChanged();
        }
        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        lastPress = renderPress;
        renderPress = strokeEase.step(renderPress, press);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$ConveyorStampBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ConveyorStampBlockEntity$power(input.getLongOr("power", 0));
        hbm$syncSet$ConveyorStampBlockEntity$press(input.getDoubleOr("press", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putDouble("press", press);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeDouble(press);
            case 2 -> {
                var stamp = inventory.get(SLOT_STAMP);
                output.writeInt(stamp.isEmpty() ? -1 : Item.getId(stamp.getItem()));
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ConveyorStampBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$ConveyorStampBlockEntity$press(input.readDouble());
            case 2 -> {
                int id = input.readInt();
                syncStack = id < 0 ? ItemStack.EMPTY : new ItemStack(Item.byId(id));
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        strokeEase.restart(2);
    }

    public final void hbm$syncSet$ConveyorStampBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ConveyorStampBlockEntity$press(double next) {
        if (Double.doubleToRawLongBits(this.press) != Double.doubleToRawLongBits(next)) {
            this.press = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                ConveyorStampBlockEntity.class, new int[] {0}, 4, false, false);
    }
}
