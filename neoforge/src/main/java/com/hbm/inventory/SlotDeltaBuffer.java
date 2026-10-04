// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class SlotDeltaBuffer {
    private final Long2IntOpenHashMap indices = new Long2IntOpenHashMap();
    private Object[] sources = new Object[16];
    private int[] slots = new int[16];
    private int[] next = new int[16];
    private ItemStack[] stacks = new ItemStack[16];
    private int size;

    public SlotDeltaBuffer() {
        indices.defaultReturnValue(-1);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void record(Object source, int slot, @Nullable ItemStack snapshot) {
        long key = key(source, slot);
        int head = indices.get(key);
        for (int index = head; index >= 0; index = next[index]) {
            if (sources[index] == source && slots[index] == slot) {
                stacks[index] = snapshot;
                return;
            }
        }
        if (size == sources.length) {
            int capacity = size * 2;
            sources = Arrays.copyOf(sources, capacity);
            slots = Arrays.copyOf(slots, capacity);
            next = Arrays.copyOf(next, capacity);
            stacks = Arrays.copyOf(stacks, capacity);
        }
        sources[size] = source;
        slots[size] = slot;
        stacks[size] = snapshot;
        next[size] = head;
        indices.put(key, size++);
    }

    public Batch take() {
        Batch batch =
                new Batch(
                        Arrays.copyOf(sources, size),
                        Arrays.copyOf(slots, size),
                        Arrays.copyOf(stacks, size));
        for (int i = 0; i < size; i++) indices.remove(key(sources[i], slots[i]));
        Arrays.fill(sources, 0, size, null);
        Arrays.fill(stacks, 0, size, null);
        size = 0;
        return batch;
    }

    private static long key(Object source, int slot) {
        return (long) System.identityHashCode(source) << 32 | slot & 0xffffffffL;
    }

    public record Batch(Object[] sources, int[] slots, ItemStack[] stacks) {
        public int size() {
            return sources.length;
        }
    }
}
