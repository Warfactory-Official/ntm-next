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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssemblyPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class WatzAssemblyBlockEntity extends StructureAssemblyBlockEntity {
    private static final class Pattern {
        private static final AssemblyPattern VALUE = build();

        private static AssemblyPattern build() {
            var builder = new AssemblyPattern.Builder(1, false);
            var element = ModBlocks.WATZ_ELEMENT.get();
            var cooler = ModBlocks.WATZ_COOLER.get();
            var boltedRim = ModBlocks.WATZ_END_BOLTED.get();
            int[] layerOffsets = {
                1, 0,
                2, 0,
                0, 1,
                0, 2,
                -1, 0,
                -2, 0,
                0, -1,
                0, -2,
                1, 1,
                1, -1,
                -1, 1,
                -1, -1,
                2, 1,
                2, -1,
                1, 2,
                -1, 2,
                -2, 1,
                -2, -1,
                1, -2,
                -1, -2,
                3, -1,
                -1, 3,
                -3, -1,
                -1, -3,
                3, 0,
                0, 3,
                -3, 0,
                0, -3,
                3, 1,
                1, 3,
                -3, 1,
                1, -3,
                2, 2,
                2, -2,
                -2, 2,
                -2, -2
            };

            builder.add(cooler, 0, 1, 0);
            builder.add(cooler, 0, 2, 0);
            for (int y = 0; y < 3; y++) {
                for (int cell = 0; cell < layerOffsets.length / 2; cell++) {
                    Block required;
                    if (cell < 12) {
                        required = element;
                    } else if (cell < 20) {
                        required = cooler;
                    } else {
                        required = boltedRim;
                    }
                    builder.add(required, layerOffsets[cell * 2], y, layerOffsets[cell * 2 + 1]);
                }
            }
            return builder.build();
        }
    }

    public static void visitCells(AssemblyPattern.CellConsumer visitor) {
        var pattern = Pattern.VALUE;
        for (int cell = 0; cell < pattern.size(); cell++) {
            long offset = pattern.getOffset(cell);
            visitor.accept(
                    pattern.getBlock(cell),
                    BlockPos.getX(offset),
                    BlockPos.getY(offset),
                    BlockPos.getZ(offset));
        }
    }

    public WatzAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ_STRUCT.get(), pos, state);
    }

    @Override
    public AssemblyPattern getPattern() {
        return Pattern.VALUE;
    }

    @Override
    public AABB getPreviewBounds() {
        return new AABB(-3, 0, -3, 4, 3, 4);
    }

    @Override
    public AABB getVisualBounds() {
        return new AABB(-3, -1, -3, 4, 4, 4);
    }

    @Override
    protected void assemble(int variant) {
        ModBlocks.WATZ.get().placeAssembled(level, worldPosition, Direction.NORTH);
    }
}
