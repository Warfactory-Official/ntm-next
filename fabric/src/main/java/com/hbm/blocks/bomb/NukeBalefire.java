// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.BlockEntityNukeBalefire;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class NukeBalefire extends BlockMachineHorizontal implements IBomb {

    public NukeBalefire(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNukeBalefire(pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityNukeBalefire bomb)) {
            return BombReturnCode.UNDEFINED;
        }
        if (!bomb.isLoaded()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        bomb.explode();
        return BombReturnCode.DETONATED;
    }
}
