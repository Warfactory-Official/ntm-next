// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.CablePaintableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityCablePaintable extends BlockEntityPipePaintable {

    public BlockEntityCablePaintable(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_PAINTABLE.get(), pos, state);
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (!nbt.contains("paintblock")) return;
        setCamo(Block.stateById(nbt.getIntOr("paintblock", 0)));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CablePaintableBlock
                && !state.getValue(CablePaintableBlock.PAINTED)) {
            level.setBlock(
                    pos, state.setValue(CablePaintableBlock.PAINTED, true), Block.UPDATE_ALL);
        }
    }
}
