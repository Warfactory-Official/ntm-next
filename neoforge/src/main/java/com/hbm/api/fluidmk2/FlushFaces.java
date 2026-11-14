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

package com.hbm.api.fluidmk2;

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmCapabilities.CapRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface FlushFaces {
    @FunctionalInterface
    interface Visitor {
        void contact(BlockPos position, Direction side);
    }

    @FunctionalInterface
    interface FaceFilter {
        boolean test(BlockPos cell, Direction side);
    }

    void collect(ServerLevel level, BlockPos pos, Visitor out);

    static FlushFaces sixFaces() {
        return (level, pos, out) -> {
            for (Direction dir : Direction.VALUES)
                out.contact(pos.relative(dir), dir.getOpposite());
        };
    }

    static FlushFaces activePlane() {
        return activePlane((cell, side) -> true);
    }

    static FlushFaces activePlane(FaceFilter filter) {
        return (level, pos, out) -> {
            BlockState state = level.getBlockState(pos);
            MultiblockCoreBlock core = MultiblockSurface.foldedCore(state);
            if (core == null) return;
            Direction facing = state.getValue(MultiblockCoreBlock.FACING);
            MultiblockSurface.forEachActiveFace(
                    core,
                    pos,
                    facing,
                    CapRole.FLUID_OUT,
                    (cell, side) -> {
                        if (filter.test(cell, side))
                            out.contact(cell.relative(side), side.getOpposite());
                    });
        };
    }

    static FlushFaces own() {
        FlushFaces plane = activePlane();
        return (level, pos, out) -> {
            if (MultiblockSurface.foldedCore(level.getBlockState(pos)) == null) {
                sixFaces().collect(level, pos, out);
            } else {
                plane.collect(level, pos, out);
            }
        };
    }
}
