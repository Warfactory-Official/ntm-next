// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import java.util.Objects;

public final class SyncArrays {
    private SyncArrays() {}

    public static void copy(
            SyncSource owner,
            int mask,
            int[] source,
            int sourceIndex,
            int[] target,
            int targetIndex,
            int length) {
        copy(owner, mask, 0, source, sourceIndex, target, targetIndex, length);
    }

    public static void copy(
            SyncSource owner,
            int mask,
            long units,
            int[] source,
            int sourceIndex,
            int[] target,
            int targetIndex,
            int length) {
        Objects.checkFromIndexSize(sourceIndex, length, source.length);
        Objects.checkFromIndexSize(targetIndex, length, target.length);
        boolean changed = false;
        int first = source == target && targetIndex > sourceIndex ? length - 1 : 0;
        int step = first == 0 ? 1 : -1;
        for (int n = 0, i = first; n < length; n++, i += step) {
            int value = source[sourceIndex + i];
            changed |= target[targetIndex + i] != value;
            target[targetIndex + i] = value;
        }
        if (changed) owner.syncArrayChanged(target, -1, mask, units);
    }

    public static void copy(
            SyncSource owner,
            int mask,
            long[] source,
            int sourceIndex,
            long[] target,
            int targetIndex,
            int length) {
        copy(owner, mask, 0, source, sourceIndex, target, targetIndex, length);
    }

    public static void copy(
            SyncSource owner,
            int mask,
            long units,
            long[] source,
            int sourceIndex,
            long[] target,
            int targetIndex,
            int length) {
        Objects.checkFromIndexSize(sourceIndex, length, source.length);
        Objects.checkFromIndexSize(targetIndex, length, target.length);
        boolean changed = false;
        int first = source == target && targetIndex > sourceIndex ? length - 1 : 0;
        int step = first == 0 ? 1 : -1;
        for (int n = 0, i = first; n < length; n++, i += step) {
            long value = source[sourceIndex + i];
            changed |= target[targetIndex + i] != value;
            target[targetIndex + i] = value;
        }
        if (changed) owner.syncArrayChanged(target, -1, mask, units);
    }
}
