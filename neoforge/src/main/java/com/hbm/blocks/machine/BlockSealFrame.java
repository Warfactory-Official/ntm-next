// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSealFrame extends Block {

    public BlockSealFrame(Properties props) {
        super(props);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        LongOpenHashSet owners = new LongOpenHashSet();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockState lid =
                        ChunkUtil.blockStateIfLoaded(level, at.setWithOffset(pos, dx, 0, dz));
                if (lid == null || !lid.is(ModBlocks.SEAL_HATCH.get())) continue;
                BlockPos owner = AssembledMembers.owner(level, at);
                if (owner != null && owners.add(owner.asLong())) BlockSeal.revalidate(level, owner);
            }
        }
    }
}
