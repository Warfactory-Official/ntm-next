// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
