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

package com.hbm.world.bulk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

@FunctionalInterface
public interface BulkTransform {
    BulkTransform IDENTITY = pos -> pos;

    static BulkTransform of(Mirror first, Mirror second, Rotation rotation) {
        boolean mirrorX = (first == Mirror.FRONT_BACK) != (second == Mirror.FRONT_BACK);
        boolean mirrorZ = (first == Mirror.LEFT_RIGHT) != (second == Mirror.LEFT_RIGHT);
        return offset -> {
            int x = mirrorX ? -offset.getX() : offset.getX();
            int z = mirrorZ ? -offset.getZ() : offset.getZ();
            return switch (rotation) {
                case NONE -> new BlockPos(x, offset.getY(), z);
                case CLOCKWISE_90 -> new BlockPos(-z, offset.getY(), x);
                case CLOCKWISE_180 -> new BlockPos(-x, offset.getY(), -z);
                case COUNTERCLOCKWISE_90 -> new BlockPos(z, offset.getY(), -x);
            };
        };
    }

    BlockPos apply(BlockPos offset);

    default Direction direction(Direction direction) {
        BlockPos transformed =
                apply(
                        new BlockPos(
                                direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        for (Direction candidate : Direction.VALUES) {
            if (transformed.getX() == candidate.getStepX()
                    && transformed.getY() == candidate.getStepY()
                    && transformed.getZ() == candidate.getStepZ()) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Bulk direction transform must preserve a grid axis");
    }
}
