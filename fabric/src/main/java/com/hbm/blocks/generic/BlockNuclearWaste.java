// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockNuclearWaste extends BlockHazard {

    public static final MapCodec<BlockNuclearWaste> CODEC = simpleCodec(BlockNuclearWaste::new);

    public BlockNuclearWaste(BlockBehaviour.Properties props) {
        super(props);
        setDisplayEffect(ExtDisplayEffect.RADFOG);
    }

    @Override
    protected MapCodec<? extends BlockNuclearWaste> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos off = pos.relative(Direction.getRandom(random));
        if (random.nextInt(2) == 0 && level.getBlockState(off).isAir()) {
            level.setBlockAndUpdate(off, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }
        super.tick(state, level, pos, random);
    }
}
