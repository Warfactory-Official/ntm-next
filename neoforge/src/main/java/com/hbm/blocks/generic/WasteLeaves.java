// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.particle.HbmEffectNT;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WasteLeaves extends Block {

    public static final MapCodec<WasteLeaves> CODEC = simpleCodec(WasteLeaves::new);

    private static final int SHED_CHANCE = 30;
    private static final int LEAF_CHANCE = 7;
    private static final int FALL_TIME = 2;

    public WasteLeaves(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(SHED_CHANCE) == 0) {
            level.removeBlock(pos, false);

            if (level.getBlockState(pos.below()).isAir()) {
                FallingBlockEntity leaves =
                        FallingBlockEntity.fall(
                                level, pos, ModBlocks.LEAVES_LAYER.get().defaultBlockState());
                leaves.time = FALL_TIME;
                leaves.disableDrop();
            }
        }

        super.randomTick(state, level, pos, rand);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        if (rand.nextInt(LEAF_CHANCE) == 0 && level.getBlockState(pos.below()).isAir()) {
            ClientEffects.spawn(
                    HbmEffectNT.DeadLeaf,
                    level,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() - 0.05D,
                    pos.getZ() + rand.nextDouble(),
                    1F);
        }
    }
}
