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

package com.hbm.explosion;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

public final class SphereTraversal {

    private SphereTraversal() {}

    public static void forEach(
            BlockPos center, int radius, Consumer<BlockPos.MutableBlockPos> action) {
        int budget = radius * radius / 2;
        var cell = new BlockPos.MutableBlockPos();
        int cx = center.getX(), cy = center.getY(), cz = center.getZ();

        for (int dx = -reach(budget), xEnd = -dx; dx <= xEnd; dx++) {
            int afterX = budget - dx * dx;
            for (int dy = -reach(afterX), yEnd = -dy; dy <= yEnd; dy++) {
                int afterY = afterX - dy * dy;
                for (int dz = -reach(afterY), zEnd = -dz; dz <= zEnd; dz++) {
                    action.accept(cell.set(cx + dx, cy + dy, cz + dz));
                }
            }
        }
    }

    private static int reach(int budget) {
        return budget > 0 ? (int) Math.sqrt(budget - 1) : -1;
    }
}
