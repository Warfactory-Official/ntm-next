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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.saveddata.SoyuzRenderSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LaunchpadSoyuzBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock {

    public LaunchpadSoyuzBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState cellStateOfKind(int kind, Direction facing, int shapeId) {
        if (kind == 0) return super.cellStateOfKind(kind, facing, shapeId);
        return ModBlocks.LAUNCHPAD_SOYUZ_ANCHOR
                .get()
                .defaultBlockState()
                .setValue(MultiblockCoreBlock.FACING, facing)
                .setValue(LaunchpadSoyuzAnchorCellBlock.SEGMENT, kind);
    }

    public static int anchorX(int segment, Direction facing) {
        if (segment == 0) return 0;
        return MultiblockMaskTable.worldX(segment * 16 + 4, 0, 2, facing);
    }

    public static int anchorZ(int segment, Direction facing) {
        if (segment == 0) return 0;
        return MultiblockMaskTable.worldZ(segment * 16 + 4, 0, 2, facing);
    }

    public static int sectionOwner(BlockPos core, Direction facing, int sectionX, int sectionZ) {
        int selected = -1;
        int nearest = Integer.MAX_VALUE;
        for (int segment = 0; segment <= 3; segment++) {
            int anchorSectionX =
                    SectionPos.blockToSectionCoord(core.getX() + anchorX(segment, facing));
            int anchorSectionZ =
                    SectionPos.blockToSectionCoord(core.getZ() + anchorZ(segment, facing));
            int distance =
                    Math.max(
                            Math.abs(anchorSectionX - sectionX),
                            Math.abs(anchorSectionZ - sectionZ));
            if (distance < nearest) {
                nearest = distance;
                selected = segment;
            }
        }
        if (nearest > 1) {
            throw new IllegalStateException();
        }
        return selected;
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        SoyuzRenderSavedData existing = SoyuzRenderSavedData.getExisting(level);
        if (existing != null) existing.remove(pos.asLong());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchpadSoyuzBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCHPAD_SOYUZ).powerIn().fluidIn().items().fe();
    }
}
