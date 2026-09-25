// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.IMinecartRail;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class BlockRailStraight extends BaseRailBlock implements IMinecartRail {

    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;

    private final MapCodec<? extends BlockRailStraight> codec;
    private final float maxSpeed;

    public BlockRailStraight(BlockBehaviour.Properties properties, float maxSpeed) {
        super(true, properties);
        this.maxSpeed = maxSpeed;
        this.codec = simpleCodec(props -> new BlockRailStraight(props, maxSpeed));
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(SHAPE, RailShape.NORTH_SOUTH)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    public float railMaxSpeed() {
        return maxSpeed;
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return codec;
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }
}
