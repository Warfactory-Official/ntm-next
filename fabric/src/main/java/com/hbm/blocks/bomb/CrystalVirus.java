// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.WorldData;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class CrystalVirus extends Block {

    public CrystalVirus(Properties props) {
        super(props);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!WorldData.ENABLE_VIRUS.get()) return;

        spreadTo(level, pos.east());
        spreadTo(level, pos.above());
        spreadTo(level, pos.south());
        spreadTo(level, pos.west());
        spreadTo(level, pos.below());
        spreadTo(level, pos.north());
        level.setBlockAndUpdate(pos, ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState());
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
                && isCrystalOrAir(level.getBlockState(pos.east()))
                && isCrystalOrAir(level.getBlockState(pos.west()))
                && isCrystalOrAir(level.getBlockState(pos.above()))
                && isCrystalOrAir(level.getBlockState(pos.below()))
                && isCrystalOrAir(level.getBlockState(pos.south()))
                && isCrystalOrAir(level.getBlockState(pos.north()))) {
            level.setBlockAndUpdate(pos, ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState());
        }
    }

    private void spreadTo(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isCrystalOrAir(state)) {
            level.setBlockAndUpdate(pos, defaultBlockState());
        }
    }

    private static boolean isCrystalOrAir(BlockState state) {
        return state.isAir()
                || state.is(ModBlocks.CRYSTAL_VIRUS.get())
                || state.is(ModBlocks.CRYSTAL_HARDENED.get())
                || state.is(ModBlocks.CRYSTAL_PULSAR.get());
    }
}
