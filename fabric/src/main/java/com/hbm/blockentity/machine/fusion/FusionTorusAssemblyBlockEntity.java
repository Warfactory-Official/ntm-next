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

package com.hbm.blockentity.machine.fusion;

import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ReactorBlockData;
import com.hbm.blocks.multiblock.AssemblyPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FusionTorusAssemblyBlockEntity extends StructureAssemblyBlockEntity {

    public FusionTorusAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_TORUS_STRUCT.get(), pos, state);
    }

    private static Block componentFor(int cell) {
        return switch (cell) {
            case 1 -> ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get();
            case 2 -> ModBlocks.FUSION_COMPONENT_BLANKET.get();
            default -> ModBlocks.FUSION_COMPONENT_MOTOR.get();
        };
    }

    public static void forEachRequirement(AssemblyPattern.CellConsumer out) {
        int[][][] layout = ReactorBlockData.TORUS_LAYOUT;
        int span = layout[0].length;
        int half = span / 2;

        for (int y = 0; y < 5; y++) {
            int[][] layer = layout[y > 2 ? 4 - y : y];
            for (int x = 0; x < span; x++) {
                for (int z = 0; z < layout[0][0].length; z++) {
                    int cell = layer[x][z];
                    if (cell == 0) continue;
                    out.accept(componentFor(cell), x - half, y, z - half);
                }
            }
        }
    }

    private static final class Pattern {
        private static final AssemblyPattern VALUE = build();

        private static AssemblyPattern build() {
            var builder = new AssemblyPattern.Builder(1, false);
            forEachRequirement((block, x, y, z) -> builder.add(block, x, y, z));
            return builder.build();
        }
    }

    @Override
    public AssemblyPattern getPattern() {
        return Pattern.VALUE;
    }

    @Override
    public AABB getPreviewBounds() {
        return new AABB(-8, -1, -8, 9, 6, 9);
    }

    @Override
    protected void assemble(int variant) {
        ModBlocks.FUSION_TORUS.get().placeAssembled(level, worldPosition, Direction.NORTH);
    }
}
