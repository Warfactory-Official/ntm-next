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
