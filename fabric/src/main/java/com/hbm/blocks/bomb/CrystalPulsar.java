// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionChaos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class CrystalPulsar extends Block {

    public CrystalPulsar(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {

        if (!level.isClientSide()
                || level.getBlockState(pos.east()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.west()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.above()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.below()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.south()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.north()).is(ModBlocks.CRYSTAL_VIRUS.get())) {
            ExplosionChaos.hardenVirus(level, pos, 10);
        }
    }
}
