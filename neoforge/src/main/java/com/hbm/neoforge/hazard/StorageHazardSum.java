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

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardSystem;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

final class StorageHazardSum {
    private static final ThreadLocal<Path> paths = ThreadLocal.withInitial(Path::new);

    private StorageHazardSum() {}

    static float sum(UUID id, boolean backpack, List<ItemStack> contents) {
        Path path = paths.get();
        int depth = path.depth;
        int offset = depth * 3;
        long most = id.getMostSignificantBits();
        long least = id.getLeastSignificantBits();
        long kind = backpack ? 1L : 0L;
        for (int i = 0; i < offset; i += 3) {
            if (path.keys[i] == most && path.keys[i + 1] == least && path.keys[i + 2] == kind)
                return 0F;
        }
        if (offset == path.keys.length) path.keys = Arrays.copyOf(path.keys, path.keys.length * 2);
        path.keys[offset] = most;
        path.keys[offset + 1] = least;
        path.keys[offset + 2] = kind;
        path.depth = depth + 1;
        try {
            float radiation = 0F;
            for (ItemStack held : contents) {
                if (!held.isEmpty()) {
                    radiation +=
                            (float) HazardSystem.getContainedRadsFromStack(held) * held.getCount();
                }
            }
            return radiation;
        } finally {
            path.depth = depth;
        }
    }

    private static final class Path {
        long[] keys = new long[24];
        int depth;
    }
}
