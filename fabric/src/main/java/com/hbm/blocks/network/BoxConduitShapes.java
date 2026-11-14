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

package com.hbm.blocks.network;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BoxConduitShapes {

    public static final int DUCT_HUB = 1;
    public static final int CABLE_HUB = 2;

    private BoxConduitShapes() {}

    public static int maskOf(BlockState state) {
        int mask = 0;
        if (state.getValue(BlockStateProperties.NORTH)) mask |= 1;
        if (state.getValue(BlockStateProperties.SOUTH)) mask |= 2;
        if (state.getValue(BlockStateProperties.DOWN)) mask |= 4;
        if (state.getValue(BlockStateProperties.UP)) mask |= 8;
        if (state.getValue(BlockStateProperties.WEST)) mask |= 16;
        if (state.getValue(BlockStateProperties.EAST)) mask |= 32;
        return mask;
    }

    public static boolean maskHas(int mask, Direction direction) {
        int bit =
                switch (direction) {
                    case NORTH -> 1;
                    case SOUTH -> 2;
                    case DOWN -> 4;
                    case UP -> 8;
                    case WEST -> 16;
                    case EAST -> 32;
                };
        return (mask & bit) != 0;
    }

    public static VoxelShape[] selection(int size, int hub) {
        return TransportBlockData.BOX_SELECTION[hub - 1][size];
    }

    public static VoxelShape[] collision(int size, int hub) {
        return TransportBlockData.BOX_COLLISION[hub - 1][size];
    }
}
