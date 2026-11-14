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

package com.hbm.blockentity.network;

import com.hbm.blockentity.GhostFilters;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.inventory.container.RadioTorchCounterMenu;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RadioCounterBlockEntity extends RadioTorchBlockEntity
        implements WorldlyContainer, GhostFilters {
    public static final int FILTER_COUNT = 3;

    private static final int COUNT_UNIT = 4;
    private static final int MATCHER_UNIT = 7;
    private static final int CHANNEL_UNIT = 8;
    private static final long COUNT_MASK = 0x70;
    private static final long CHANNEL_MASK = 0x700;
    private static final int[] NO_SLOTS = new int[0];
    private static final Window FILTER_WINDOW = new Window(0, FILTER_COUNT);

    private final NonNullList<ItemStack> filters =
            NonNullList.withSize(FILTER_COUNT, ItemStack.EMPTY);
    private final boolean[] force = new boolean[FILTER_COUNT];
    private BlockPos inventoryPosition;

    public final int[] lastCounts = new int[FILTER_COUNT];
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_COUNT);
    public final String[] channels = new String[FILTER_COUNT];

    public RadioCounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_COUNTER.get(), pos, state, InventoryUtil::anchorAt);
        inventoryPosition = pos.relative(supportDirection());
        Arrays.fill(channels, "");
    }

    @Override
    public void refreshAttached() {
        super.refreshAttached();
        inventoryPosition = worldPosition.relative(supportDirection());
    }

    @Override
    public void tickServer() {
        var anchor = attached(Container.class);
        var inventory = InventoryUtil.whole(level, inventoryPosition, anchor);
        if (inventory != null) {
            for (int row = 0; row < FILTER_COUNT; row++) {
                var filter = filters.get(row);
                if (channels[row].isEmpty() || filter.isEmpty()) continue;

                int count = 0;
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    var stack = inventory.getItem(slot);
                    if (!stack.isEmpty() && matcher.isValidForFilter(filter, row, stack)) {
                        count += stack.getCount();
                    }
                }

                if (polling || force[row] || count != lastCounts[row]) {
                    RadioSystem.send(level, channels[row], count);
                    force[row] = false;
                }
                if (count != lastCounts[row]) {
                    hbm$syncArraySet$RadioCounterBlockEntity$lastCounts(lastCounts, row, count);
                    setChanged();
                }
            }
        }
        networkPackNT(15);
    }

    public void setChannel(int row, String value) {
        if (channels[row].equals(value)) return;
        hbm$syncArraySet$RadioCounterBlockEntity$channels(channels, row, value);
        force[row] = true;
        setChanged();
    }

    public void togglePolling() {
        setPolling(!polling);
    }

    @Override
    public Window filterWindow() {
        return FILTER_WINDOW;
    }

    @Override
    public void advanceFilterMode(int slot) {
        matcher.nextMode(level, filters.get(slot), slot);
        force[slot] = true;
        setChanged();
    }

    @Override
    public int getContainerSize() {
        return FILTER_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (var filter : filters) {
            if (!filter.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return filters.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        var removed = ContainerHelper.removeItem(filters, slot, count);
        if (!removed.isEmpty()) {
            force[slot] = true;
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(filters, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        var template = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        filters.set(slot, template);
        force[slot] = true;
        setChanged();
    }

    @Override
    public void clearContent() {
        filters.clear();
        Arrays.fill(force, true);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RadioTorchCounterMenu(containerId, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyCounter");
    }

    @Override
    protected void loadRadio(ValueInput input) {
        hbm$syncSet$RadioTorchBlockEntity$polling(input.getBooleanOr("p", false));
        ContainerHelper.loadAllItems(input.childOrEmpty("inventory"), filters);
        for (int row = 0; row < FILTER_COUNT; row++) {
            hbm$syncArraySet$RadioCounterBlockEntity$channels(
                    channels, row, input.getStringOr("c" + row, ""));
            hbm$syncArraySet$RadioCounterBlockEntity$lastCounts(
                    lastCounts, row, input.getIntOr("l" + row, 0));
        }
        matcher.load(input);
        Arrays.fill(force, false);
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        ContainerHelper.saveAllItems(output.child("inventory"), filters);
        for (int row = 0; row < FILTER_COUNT; row++) {
            output.putString("c" + row, channels[row]);
            output.putInt("l" + row, lastCounts[row]);
        }
        matcher.save(output);
    }

    @Override
    public long syncUnitMask() {
        return 0x7f1;
    }

    @Override
    public void syncArrayChanged(Object value, int index, int mask, long units) {
        if (value == lastCounts) {
            long changedUnits = index < 0 ? COUNT_MASK : 1L << (COUNT_UNIT + index);
            syncUnitsChanged(mask, changedUnits);
        } else if (value == channels) {
            long changedUnits = index < 0 ? CHANNEL_MASK : 1L << (CHANNEL_UNIT + index);
            syncUnitsChanged(mask, changedUnits);
        } else {
            super.syncArrayChanged(value, index, mask, units);
        }
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(polling);
            case 4, 5, 6 -> output.writeInt(lastCounts[unit - COUNT_UNIT]);
            case MATCHER_UNIT -> matcher.serialize(output);
            case 8, 9, 10 -> writeString(output, channels[unit - CHANNEL_UNIT]);
            default -> throw new IllegalArgumentException("Radio counter unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$RadioTorchBlockEntity$polling(input.readBoolean());
            case 4, 5, 6 ->
                    hbm$syncArraySet$RadioCounterBlockEntity$lastCounts(
                            lastCounts, unit - COUNT_UNIT, input.readInt());
            case MATCHER_UNIT -> matcher.deserialize(input);
            case 8, 9, 10 ->
                    hbm$syncArraySet$RadioCounterBlockEntity$channels(
                            channels, unit - CHANNEL_UNIT, readString(input));
            default -> throw new IllegalArgumentException("Radio counter unit " + unit);
        }
    }

    public final int hbm$syncArraySet$RadioCounterBlockEntity$lastCounts(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.lastCounts) {
                syncArrayChanged(array, index, 3, COUNT_MASK);
            }
        }
        return next;
    }

    public final String hbm$syncArraySet$RadioCounterBlockEntity$channels(
            String[] array, int index, String next) {
        if (array[index] != next) {
            boolean changed = !java.util.Objects.equals(array[index], next);
            array[index] = next;
            if (array == this.channels && changed) {
                syncArrayChanged(array, index, 3, CHANNEL_MASK);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(
                RadioCounterBlockEntity.class, "lastCounts", 3, COUNT_MASK);
        com.hbm.packet.SyncBindings.field(
                RadioCounterBlockEntity.class, "matcher", 3, 1L << MATCHER_UNIT);
        com.hbm.packet.SyncBindings.field(
                RadioCounterBlockEntity.class, "channels", 3, CHANNEL_MASK);
    }
}
