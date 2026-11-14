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

package com.hbm.blockentity;

import com.hbm.interfaces.SettingsHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface GhostFilters extends SettingsHandler, Container {
    record Window(int start, int end) {

        public Window {
            if (start < 0 || end < start)
                throw new IllegalArgumentException("filter window [" + start + ", " + end + ")");
        }

        public boolean contains(int slot) {
            return slot >= start && slot < end;
        }

        public int size() {
            return end - start;
        }
    }

    Window filterWindow();

    void advanceFilterMode(int slot);

    default boolean admitsFilter(ItemStack stack) {
        return true;
    }

    default void putFilter(int slot, ItemStack stack) {
        if (!filterWindow().contains(slot) || !admitsFilter(stack)) return;
        setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        advanceFilterMode(slot);
        setChanged();
    }

    @Override
    default CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag settings = new CompoundTag();
        settings.put("items", save(this));
        return settings;
    }

    @Override
    default void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        ListTag items = settings.getListOrEmpty("items");
        if (option < items.size()) load(this, items);
    }

    @Override
    default String[] getSettingsOptions(Level level, BlockPos pos) {
        return new String[] {"copytool.filter"};
    }

    static CompoundTag entry(int offset, ItemStack stack) {
        CompoundTag entry = new CompoundTag();
        entry.putByte("slot", (byte) offset);
        entry.put("stack", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
        return entry;
    }

    static @Nullable ItemStack entryStack(CompoundTag entry) {
        Tag stack = entry.get("stack");
        return stack == null
                ? null
                : ItemStack.CODEC.parse(NbtOps.INSTANCE, stack).result().orElse(null);
    }

    static ListTag save(GhostFilters filters) {
        Window window = filters.filterWindow();
        ListTag items = new ListTag();
        for (int offset = 0; offset < window.size(); offset++) {
            ItemStack stack = filters.getItem(window.start() + offset);
            if (!stack.isEmpty()) items.add(entry(offset, stack));
        }
        return items;
    }

    static void load(GhostFilters filters, ListTag items) {
        Window window = filters.filterWindow();
        load(filters, items, window.start(), window.end());
    }

    static boolean load(GhostFilters filters, ListTag items, int start, int end) {
        Window window = filters.filterWindow();
        int count = Math.min(items.size(), window.size());
        byte[] offsets = new byte[count];
        ItemStack[] stacks = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            CompoundTag entry = items.getCompoundOrEmpty(i);
            var offset = entry.getByte("slot");
            ItemStack stack = entryStack(entry);
            if (offset.isEmpty() || stack == null) return false;
            offsets[i] = offset.get();
            stacks[i] = stack;
        }
        for (int i = 0; i < count; i++) {
            int offset = offsets[i];
            if (offset < 0 || offset >= window.size() || !filters.admitsFilter(stacks[i])) continue;
            int slot = window.start() + offset;
            if (slot < start || slot >= end) continue;
            filters.setItem(slot, stacks[i]);
            filters.advanceFilterMode(slot);
            filters.setChanged();
        }
        return true;
    }
}
