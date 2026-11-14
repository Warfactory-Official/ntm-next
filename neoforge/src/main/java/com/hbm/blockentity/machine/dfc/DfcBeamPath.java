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

package com.hbm.blockentity.machine.dfc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

final class DfcBeamPath {
    private DfcBeamPath() {}

    static @Nullable DfcChamberBlockEntity chamber(
            Level level,
            BlockPos origin,
            Direction facing,
            BlockPos.MutableBlockPos cursor,
            int range) {
        cursor.set(origin);
        for (int distance = 1; distance <= range; distance++) {
            cursor.move(facing);
            if (level.getBlockEntity(cursor) instanceof DfcChamberBlockEntity chamber)
                return chamber;
            if (!level.getBlockState(cursor).isAir()) return null;
        }
        return null;
    }
}
