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

package com.hbm.api.energymk2;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class MixedEnergyPlan {
    final Object[] foreign;
    final int natives;
    final int[] nativeOf;
    final int[] foreignOf;
    final long[] initial;
    final long[] remaining;
    final long[] localReserve;
    final long[] reserveExports;
    private final Long2ObjectOpenHashMap<int[]> ports = new Long2ObjectOpenHashMap<>();
    public @Nullable Object loaderStaging;
    boolean active = true;
    private int[] undoSlot = new int[16];
    private long[] undoAmount = new long[16];
    private int undoSize;

    MixedEnergyPlan(int natives, Object[] foreign, int[] nativeOf, int[] foreignOf) {
        this.natives = natives;
        this.foreign = foreign;
        this.nativeOf = nativeOf;
        this.foreignOf = foreignOf;
        initial = new long[nativeOf.length * 2];
        remaining = new long[nativeOf.length * 2];
        localReserve = new long[natives];
        reserveExports = new long[natives];
    }

    static int slot(int edge, boolean toForeign) {
        return edge * 2 + (toForeign ? 0 : 1);
    }

    static long toHeCeil(long numerator) {
        long divisor = EnergyConversion.feRatio();
        return numerator / divisor + (numerator % divisor == 0 ? 0 : 1);
    }

    private static long cost(long fe, long maximum) {
        long ratio = EnergyConversion.heRatio();
        return fe > maximum / ratio ? maximum : fe * ratio;
    }

    void port(long posKey, Direction side, int edge) {
        int[] sides = ports.get(posKey);
        if (sides == null) {
            ports.put(posKey, sides = new int[6]);
            Arrays.fill(sides, -1);
        }
        sides[side.ordinal()] = edge;
    }

    void quota(int slot, long numerator) {
        initial[slot] = remaining[slot] = numerator;
    }

    public int slot(long posKey, Direction side, boolean toForeign) {
        if (!active) return -1;
        int[] sides = ports.get(posKey);
        int edge = sides == null ? -1 : sides[side.ordinal()];
        return edge < 0 ? -1 : slot(edge, toForeign);
    }

    public long availableFe(int slot) {
        if (slot < 0) return 0;
        long numerator = remaining[slot];
        long divisor = EnergyConversion.heRatio();
        return numerator / divisor + (numerator % divisor == 0 ? 0 : 1);
    }

    public long reserveFe(int slot, long fe) {
        long before = remaining[slot];
        record(slot, before);
        long amount = cost(fe, before);
        remaining[slot] -= amount;
        return amount;
    }

    public void refund(int slot, long reserved, long acceptedFe) {
        long spent = cost(acceptedFe, reserved);
        if (spent == reserved) return;
        record(slot, remaining[slot]);
        remaining[slot] += reserved - spent;
    }

    void take(int slot, long fe) {
        if (slot >= 0 && fe > 0) remaining[slot] -= cost(fe, remaining[slot]);
    }

    private void record(int slot, long before) {
        if (undoSize == undoSlot.length) {
            undoSlot = Arrays.copyOf(undoSlot, undoSize * 2);
            undoAmount = Arrays.copyOf(undoAmount, undoSize * 2);
        }
        undoSlot[undoSize] = slot;
        undoAmount[undoSize++] = before;
    }

    public int checkpoint() {
        return undoSize;
    }

    public void rollback(int checkpoint) {
        while (undoSize > checkpoint) {
            undoSize--;
            remaining[undoSlot[undoSize]] = undoAmount[undoSize];
        }
    }

    public void committed() {
        undoSize = 0;
    }

    long incoming(int node) {
        long total = 0;
        for (int edge = 0; edge < nativeOf.length; edge++) {
            if (nativeOf[edge] != node) continue;
            long amount = toHeCeil(remaining[slot(edge, false)]);
            total = Long.MAX_VALUE - total < amount ? Long.MAX_VALUE : total + amount;
        }
        return total;
    }

    long consumedFe(int j, boolean toForeign) {
        long numerator = 0;
        for (int edge = 0; edge < foreignOf.length; edge++) {
            if (foreignOf[edge] != j) continue;
            int slot = slot(edge, toForeign);
            long spent = initial[slot] - remaining[slot];
            numerator = Long.MAX_VALUE - numerator < spent ? Long.MAX_VALUE : numerator + spent;
        }
        return numerator / EnergyConversion.heRatio();
    }
}
