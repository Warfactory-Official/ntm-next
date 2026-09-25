// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.BlockEntityFissure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class BlockFissure extends Block implements ITickingBlock, ICapabilityBlock {

    public static final BooleanProperty CRATER = BooleanProperty.create("crater");

    public static final MapCodec<BlockFissure> CODEC = simpleCodec(BlockFissure::new);

    public BlockFissure(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CRATER, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFissure(pos, state);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (!level.getBlockState(above).canBeReplaced()) return;
        Block lava =
                state.getValue(CRATER)
                        ? ModBlocks.RAD_LAVA_BLOCK.get()
                        : ModBlocks.VOLCANIC_LAVA_BLOCK.get();
        level.setBlockAndUpdate(above, lava.defaultBlockState());
    }

    @Override
    public MachineCaps caps() {

        return MachineCaps.of(ModBlockEntities.FISSURE)
                .fluidOut()
                .fluidFaces(BlockEntityFissure.class, (be, face) -> face.side() == Direction.DOWN);
    }
}
