// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.blocks.multiblock.MultiblockFootprint;
import java.util.Arrays;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class MultiblockBreakSnapshot implements BiConsumer<BlockPos, BlockState> {
    private long[] positions = new long[32];
    private BlockState[] states = new BlockState[32];
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private int count;

    public void capture(Level level, BlockPos core, BlockState state) {
        count = 0;
        MultiblockFootprint.visitPresent(level, core, state, this);
    }

    @Override
    public void accept(BlockPos pos, BlockState state) {
        if (count == positions.length) {
            positions = Arrays.copyOf(positions, count * 2);
            states = Arrays.copyOf(states, count * 2);
        }
        positions[count] = pos.asLong();
        states[count++] = state;
    }

    public void remove(Level level, BlockPos hit) {
        ChunkAccess chunk = null;
        int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
        for (int i = count - 1; i >= 0; i--) {
            pos.set(positions[i]);
            if (pos.equals(hit)) continue;
            int x = pos.getX() >> 4, z = pos.getZ() >> 4;
            if (x != chunkX || z != chunkZ) {
                chunk = level.getChunk(x, z, ChunkStatus.FULL, false);
                chunkX = x;
                chunkZ = z;
            }
            if (chunk == null || chunk.getBlockState(pos) != states[i]) continue;
            level.setBlock(
                    pos, states[i].getFluidState().createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
}
