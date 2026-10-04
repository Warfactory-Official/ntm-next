// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.inventory.InventorySlotAccess;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

public final class FuseSlots {
    private final Long2IntOpenHashMap indices = new Long2IntOpenHashMap();
    private long[] keys = new long[4];
    private long[] deadlines = new long[4];
    private int size;
    private Object[] sources = new Object[4];
    private int[] counts = new int[4];
    private int sourceCount;
    private long[] due = new long[4];

    public FuseSlots() {
        indices.defaultReturnValue(-1);
    }

    public void update(Object source, int slot, long deadline) {
        int index = -1;
        int vacant = -1;
        for (int i = 0; i < sourceCount; i++) {
            if (sources[i] == source) index = i;
            else if (sources[i] == null && vacant < 0) vacant = i;
        }
        if (index < 0) {
            if (deadline == Long.MAX_VALUE) return;
            if (vacant >= 0) index = vacant;
            else {
                if (sourceCount == sources.length) {
                    sources = Arrays.copyOf(sources, sourceCount * 2);
                    counts = Arrays.copyOf(counts, sourceCount * 2);
                }
                index = sourceCount++;
            }
            sources[index] = source;
        }
        long key = (long) index << 32 | slot & 0xffffffffL;
        int entry = indices.get(key);
        if (deadline == Long.MAX_VALUE) {
            if (entry < 0) return;
            indices.remove(key);
            int last = --size;
            if (entry != last) {
                keys[entry] = keys[last];
                deadlines[entry] = deadlines[last];
                indices.put(keys[entry], entry);
            }
            if (--counts[index] == 0) sources[index] = null;
            while (sourceCount > 0 && sources[sourceCount - 1] == null) sourceCount--;
        } else {
            if (entry < 0) {
                if (size == keys.length) {
                    keys = Arrays.copyOf(keys, size * 2);
                    deadlines = Arrays.copyOf(deadlines, size * 2);
                }
                entry = size++;
                keys[entry] = key;
                indices.put(key, entry);
                counts[index]++;
            }
            deadlines[entry] = deadline;
        }
    }

    public long minimum() {
        long minimum = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) minimum = Math.min(minimum, deadlines[i]);
        return minimum;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void removeSource(Object source) {
        int index = -1;
        for (int i = 0; i < sourceCount; i++)
            if (sources[i] == source) {
                index = i;
                break;
            }
        if (index < 0) return;
        for (int i = size - 1; i >= 0; i--)
            if ((int) (keys[i] >>> 32) == index) {
                int slot = (int) keys[i];
                update(source, slot, Long.MAX_VALUE);
            }
    }

    public int due(long now) {
        if (due.length < size) due = new long[Math.max(size, due.length * 2)];
        int count = 0;
        for (int i = 0; i < size; i++) if (deadlines[i] <= now) due[count++] = keys[i];
        return count;
    }

    public Object source(int index) {
        return sources[(int) (due[index] >>> 32)];
    }

    public int slot(int index) {
        return (int) due[index];
    }

    public static ItemStack read(Object source, int slot) {
        return InventorySlotAccess.of(source).read(source, slot);
    }
}
