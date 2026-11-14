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
import com.hbm.blocks.machine.StructBlock;
import com.hbm.blocks.multiblock.AssemblyPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class LauncherAssemblyBlockEntity extends StructureAssemblyBlockEntity {

    public static final int COMPACT_RADIUS = 1;
    public static final int TABLE_RADIUS = 4;
    public static final int SCAFFOLD_OFFSET = 3;
    public static final int SCAFFOLD_HEIGHT = 12;
    private static final Direction[] SCAFFOLD_SIDES = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    public LauncherAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTI_CORE.get(), pos, state);
    }

    public static void forEachPadCell(int radius, PadVisitor out) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx != 0 || dz != 0) out.accept(dx, dz);
            }
        }
    }

    public static boolean isLarge(BlockState state) {
        return state.getBlock() instanceof StructBlock struct && struct.large;
    }

    @Override
    public AssemblyPattern getPattern() {
        return isLarge(getBlockState()) ? Patterns.LARGE : Patterns.COMPACT;
    }

    private static final class Patterns {
        private static final AssemblyPattern COMPACT = build(false);
        private static final AssemblyPattern LARGE = build(true);

        private static AssemblyPattern build(boolean large) {
            var builder = new AssemblyPattern.Builder(large ? 4 : 1, false);
            forEachPadCell(
                    large ? TABLE_RADIUS : COMPACT_RADIUS,
                    (x, z) -> builder.add(ModBlocks.STRUCT_LAUNCHER.get(), x, 0, z));
            if (large) {
                for (int side = 0; side < SCAFFOLD_SIDES.length; side++) {
                    var direction = SCAFFOLD_SIDES[side];
                    for (int y = 1; y < SCAFFOLD_HEIGHT; y++) {
                        builder.add(
                                ModBlocks.STRUCT_SCAFFOLD.get(),
                                null,
                                1 << side,
                                direction.getStepX() * SCAFFOLD_OFFSET,
                                y,
                                direction.getStepZ() * SCAFFOLD_OFFSET);
                    }
                }
            }
            return builder.build();
        }
    }

    @Override
    protected void assemble(int variant) {
        if (isLarge(getBlockState())) buildTable(SCAFFOLD_SIDES[variant]);
        else buildCompact();
    }

    @Override
    public int getPreviewVariant(long milliseconds) {
        if (!isLarge(getBlockState())) return 0;
        return switch ((int) (milliseconds % 4000 / 1000)) {
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 3;
            default -> 0;
        };
    }

    @Override
    public boolean usesSouthPreviewNormal() {
        return true;
    }

    @Override
    public AABB getPreviewBounds() {
        int radius = isLarge(getBlockState()) ? TABLE_RADIUS : COMPACT_RADIUS;
        int height = isLarge(getBlockState()) ? SCAFFOLD_HEIGHT : 1;
        return new AABB(-radius, 0, -radius, radius + 1, height, radius + 1);
    }

    @Override
    public AABB getVisualBounds() {
        return getPreviewBounds().inflate(1);
    }

    private void buildCompact() {
        ModBlocks.COMPACT_LAUNCHER.get().placeAssembled(level, worldPosition, Direction.NORTH);
    }

    private void buildTable(Direction scaffold) {
        BlockPos foot = worldPosition.relative(scaffold, SCAFFOLD_OFFSET);
        for (int i = 1; i < SCAFFOLD_HEIGHT; i++) level.removeBlock(foot.above(i), false);
        ModBlocks.LAUNCH_TABLE
                .get()
                .placeAssembled(level, worldPosition, scaffold.getCounterClockWise());
    }

    public interface PadVisitor {
        void accept(int dx, int dz);
    }
}
