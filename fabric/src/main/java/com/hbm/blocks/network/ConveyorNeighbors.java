// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmContracts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class ConveyorNeighbors {
    private ConveyorNeighbors() {}

    public static boolean beltAt(BlockGetter view, BlockPos pos) {
        return NtmContracts.CONVEYOR_BELT.type().isInstance(blockAt(view, pos));
    }

    public static boolean receiverAt(BlockGetter view, BlockPos pos) {
        return isReceiver(blockAt(view, pos));
    }

    public static boolean isReceiver(@Nullable Block block) {
        return NtmContracts.CONVEYOR_BELT.type().isInstance(block)
                || NtmContracts.ENTERABLE.type().isInstance(block);
    }

    public static void refreshAround(ServerLevel level, BlockPos cell) {
        var chunk = BlockMultiblockCore.readableChunk(level, cell.getX(), cell.getZ());
        if (chunk == null) return;
        BlockState changed = chunk.getBlockState(cell);
        for (int dy = -1; dy <= 1; dy += 2) {
            BlockPos pos = cell.above(dy);
            BlockState before = chunk.getBlockState(pos);
            if (!(before.getBlock() instanceof ConveyorLiftBlock
                    || before.getBlock() instanceof ConveyorChuteBlock)) continue;
            BlockState after =
                    before.updateShape(
                            level,
                            level,
                            pos,
                            dy < 0 ? Direction.UP : Direction.DOWN,
                            cell,
                            changed,
                            level.getRandom());
            if (after != before) level.setBlock(pos, after, Block.UPDATE_ALL);
        }
    }

    private static @Nullable Block blockAt(BlockGetter view, BlockPos pos) {
        BlockState state = view.getBlockState(pos);
        if (!MultiblockSurface.isFoldedCell(state)) return state.getBlock();
        BlockPos core = MultiblockSurface.coreOfAny(view, pos, state);
        return core == null ? null : view.getBlockState(core).getBlock();
    }
}
