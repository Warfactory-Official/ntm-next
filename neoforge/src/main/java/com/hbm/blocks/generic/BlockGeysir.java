// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.BlockEntityGeysir;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class BlockGeysir extends Block implements ITickingBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final MapCodec<BlockGeysir> CODEC = simpleCodec(BlockGeysir::new);

    public BlockGeysir(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityGeysir(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {

        if (this != ModBlocks.GEYSIR_NETHER.get()) return;
        level.addParticle(
                ParticleTypes.FLAME,
                pos.getX() + 0.5D,
                pos.getY() + 1.0625D,
                pos.getZ() + 0.5D,
                0.0D,
                0.0D,
                0.0D);
    }
}
