/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.generator;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RadGenMenu;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class RadGenBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema {

    public static final int CHANNELS = 12;
    public static final int SLOT_COUNT = 24;
    public static final long MAX_POWER = 1_000_000;
    private static final int[] SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    public final int[] progress = new int[CHANNELS];
    public final int[] maxProgress = new int[CHANNELS];
    public final int[] production = new int[CHANNELS];
    public long power;
    public boolean isOn;
    private final NonNullList<ItemStack> burning = NonNullList.withSize(CHANNELS, ItemStack.EMPTY);

    public RadGenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADGEN.get(), pos, state, SLOT_COUNT);
    }

    public static @Nullable WasteFuel fuelFor(ItemStack stack) {
        return WasteFuel.forItem(stack);
    }

    public ItemStack processing(int channel) {
        return burning.get(channel);
    }

    @Override
    public void tickServer() {
        for (int channel = 0; channel < CHANNELS; channel++) {
            if (!burning.get(channel).isEmpty()) continue;
            var feed = inventory.get(channel);
            var fuel = fuelFor(feed);
            if (feed.isEmpty() || fuel == null || !fuel.fits(inventory.get(channel + CHANNELS)))
                continue;
            burning.set(channel, feed.split(1));
            hbm$syncArraySet$RadGenBlockEntity$progress(progress, channel, 0);
            hbm$syncArraySet$RadGenBlockEntity$maxProgress(maxProgress, channel, fuel.duration());
            hbm$syncArraySet$RadGenBlockEntity$production(production, channel, fuel.rate());
        }

        boolean active = false;
        long generated = power;
        for (int channel = 0; channel < CHANNELS; channel++) {
            var item = burning.get(channel);
            if (item.isEmpty()) continue;
            active = true;
            generated += production[channel];
            hbm$syncArraySet$RadGenBlockEntity$progress(
                    progress, channel, (int) (progress[channel] + (1)));
            if (progress[channel] < maxProgress[channel]) continue;
            hbm$syncArraySet$RadGenBlockEntity$progress(progress, channel, 0);
            int productSlot = channel + CHANNELS;
            inventory.set(productSlot, fuelFor(item).finish(inventory.get(productSlot)));
            burning.set(channel, ItemStack.EMPTY);
        }
        long capped = Math.min(MAX_POWER, generated);
        boolean changed = active || power != capped || isOn != active;
        hbm$syncSet$RadGenBlockEntity$power(capped);
        hbm$syncSet$RadGenBlockEntity$isOn(active);
        if (changed) setChanged();
        networkPackNT(50);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$RadGenBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack candidate) {
        if (slot >= CHANNELS || fuelFor(candidate) == null) return false;
        var target = inventory.get(slot);
        if (target.isEmpty()) return true;
        int count = target.getCount();
        for (int channel = 0; channel < CHANNELS; channel++) {
            var feed = inventory.get(channel);
            if (feed.isEmpty()
                    || ItemStack.isSameItemSameComponents(feed, candidate)
                            && feed.getCount() < count) return false;
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= CHANNELS;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radGen");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RadGenMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadChannels(input, "progress", progress);
        loadChannels(input, "maxProgress", maxProgress);
        loadChannels(input, "production", production);
        hbm$syncSet$RadGenBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$RadGenBlockEntity$isOn(input.getBooleanOr("isOn", isOn));
        input.child("processing").ifPresent(child -> ContainerHelper.loadAllItems(child, burning));
    }

    private static void loadChannels(ValueInput input, String key, int[] channels) {
        input.getIntArray(key)
                .ifPresent(
                        values -> {
                            if (values.length != CHANNELS) throw new IllegalArgumentException(key);
                            System.arraycopy(values, 0, channels, 0, CHANNELS);
                        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        output.putIntArray("production", production);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        ContainerHelper.saveAllItems(output.child("processing"), burning);
    }

    @Override
    public long syncUnitMask() {
        return 31;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeChannels(output, progress);
            case 1 -> writeChannels(output, maxProgress);
            case 2 -> writeChannels(output, production);
            case 3 -> output.writeLong(power);
            case 4 -> output.writeBoolean(isOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readChannels(input, progress);
            case 1 -> readChannels(input, maxProgress);
            case 2 -> readChannels(input, production);
            case 3 -> hbm$syncSet$RadGenBlockEntity$power(input.readLong());
            case 4 -> hbm$syncSet$RadGenBlockEntity$isOn(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    private static void writeChannels(ByteBuf output, int[] values) {
        for (int channel = 0; channel < CHANNELS; channel++) output.writeInt(values[channel]);
    }

    private static void readChannels(ByteBuf input, int[] values) {
        for (int channel = 0; channel < CHANNELS; channel++) values[channel] = input.readInt();
    }

    public final int hbm$syncArraySet$RadGenBlockEntity$progress(int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.progress) {
                syncArrayChanged(array, index, 3, 1);
            }
        }
        return next;
    }

    public final int hbm$syncArraySet$RadGenBlockEntity$maxProgress(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.maxProgress) {
                syncArrayChanged(array, index, 3, 2);
            }
        }
        return next;
    }

    public final int hbm$syncArraySet$RadGenBlockEntity$production(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.production) {
                syncArrayChanged(array, index, 3, 4);
            }
        }
        return next;
    }

    public final void hbm$syncSet$RadGenBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$RadGenBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RadGenBlockEntity.class, "progress", 3, 1);
        com.hbm.packet.SyncBindings.field(RadGenBlockEntity.class, "maxProgress", 3, 2);
        com.hbm.packet.SyncBindings.field(RadGenBlockEntity.class, "production", 3, 4);
    }
}
