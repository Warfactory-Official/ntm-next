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

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.util.ChunkUtil;
import com.hbm.world.bulk.BulkAction;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SealFrameBlock extends Block {

    public SealFrameBlock(Properties props) {
        super(props);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (BulkAction.active()) return;
        LongOpenHashSet owners = new LongOpenHashSet();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockState lid =
                        ChunkUtil.blockStateIfLoaded(level, at.setWithOffset(pos, dx, 0, dz));
                if (lid == null || !lid.is(ModBlocks.SEAL_HATCH.get())) continue;
                BlockPos owner = AssembledMembers.owner(level, at);
                if (owner != null && owners.add(owner.asLong())) SealBlock.revalidate(level, owner);
            }
        }
    }
}
