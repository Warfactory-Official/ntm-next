// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class BlockRedBrick extends Block {
    public static final IntegerProperty FACE = IntegerProperty.create("face", 0, 6);

    public BlockRedBrick(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACE, context.getNearestLookingDirection().getOpposite().get3DDataValue());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        int face = state.getValue(FACE);
        return face == 6
                ? state
                : state.setValue(
                        FACE, rotation.rotate(Direction.from3DDataValue(face)).get3DDataValue());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        int face = state.getValue(FACE);
        return face == 6
                ? state
                : state.rotate(mirror.getRotation(Direction.from3DDataValue(face)));
    }
}
