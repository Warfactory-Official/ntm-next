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

package com.hbm.world.feature;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class LibraryDungeonFeature extends PlaceTemplateFeature {
    private static final List<BlockPos> CORNERS =
            List.of(
                    new BlockPos(0, 0, 0), new BlockPos(8, 0, 0),
                    new BlockPos(8, 0, 10), new BlockPos(0, 0, 10));

    public LibraryDungeonFeature() {
        super("library_dungeon");
    }

    @Override
    protected List<BlockPos> guardOffsets() {
        return CORNERS;
    }

    @Override
    protected boolean isValidSpawn(WorldGenLevel level, BlockPos pos) {
        if (pos.getY() <= 5) return false;
        BlockState above = level.getBlockState(pos.above(8));
        BlockState below = level.getBlockState(pos.below());
        return isSolid(above) && isSolid(below);
    }
}
