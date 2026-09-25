// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.tileentity.BlockEntitySkeletonHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockSkeletonHolder extends Block implements EntityBlock {

    public static final IntegerProperty FACING = IntegerProperty.create("facing", 2, 5);

    public BlockSkeletonHolder(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, 3));
    }

    private static int fromPlacerDirection(Direction placerFacing) {
        return switch (placerFacing) {
            case SOUTH -> 5;
            case WEST -> 3;
            case NORTH -> 4;
            default -> 2;
        };
    }

    private static Direction toRenderDirection(int facing) {
        return switch (facing) {
            case 3 -> Direction.SOUTH;
            case 5 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private static int fromRenderDirection(Direction dir) {
        return switch (dir) {
            case SOUTH -> 3;
            case WEST -> 5;
            case NORTH -> 2;
            default -> 4;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySkeletonHolder(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, fromPlacerDirection(ctx.getHorizontalDirection()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                FACING,
                fromRenderDirection(rotation.rotate(toRenderDirection(state.getValue(FACING)))));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(toRenderDirection(state.getValue(FACING))));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySkeletonHolder pedestal))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        if (pedestal.item.isEmpty() && !held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            pedestal.item = held.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            pedestal.setChanged();
            pedestal.networkPackNTTracking();
            return InteractionResult.SUCCESS;
        } else if (!pedestal.item.isEmpty() && held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.setItemInHand(InteractionHand.MAIN_HAND, pedestal.item.copy());
            pedestal.item = ItemStack.EMPTY;
            pedestal.setChanged();
            pedestal.networkPackNTTracking();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
