// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.IGUIProvider;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.items.tool.ItemTooling;
import com.hbm.util.Facing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class CraneBlockBase extends Block implements ITickingBlock, IToolable {

    public static final EnumProperty<Direction> INPUT =
            EnumProperty.create("input", Direction.class);
    public static final EnumProperty<Direction> OUTPUT =
            EnumProperty.create("output", Direction.class);

    protected CraneBlockBase(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(INPUT, Direction.NORTH)
                        .setValue(OUTPUT, Direction.SOUTH));
    }

    public static Direction inputSide(BlockState state) {
        return state.getValue(INPUT);
    }

    public static Direction outputSide(BlockState state) {
        return state.getValue(OUTPUT);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INPUT, OUTPUT);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(INPUT, rotation.rotate(state.getValue(INPUT)))
                .setValue(OUTPUT, rotation.rotate(state.getValue(OUTPUT)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(INPUT, mirror.mirror(state.getValue(INPUT)))
                .setValue(OUTPUT, mirror.mirror(state.getValue(OUTPUT)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction input = ctx.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(INPUT, input).setValue(OUTPUT, input.getOpposite());
    }

    public Direction getInputSide(Level level, BlockPos pos) {
        return inputSide(level.getBlockState(pos));
    }

    public Direction getOutputSide(Level level, BlockPos pos) {
        return outputSide(level.getBlockState(pos));
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof ItemTooling || stack.getItem() instanceof ItemConveyorWand) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof IGUIProvider provider))
            return InteractionResult.PASS;
        if (!level.isClientSide()) provider.openMenu(player, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (side == null) return false;

        BlockState state = level.getBlockState(pos);
        Direction input = inputSide(state);
        Direction output = outputSide(state);
        Direction aimed = side;

        if (player.isSecondaryUseActive()) {
            if (aimed == output) aimed = aimed.getOpposite();
            level.setBlockAndUpdate(
                    pos,
                    aimed == input
                            ? state.setValue(INPUT, output).setValue(OUTPUT, input)
                            : state.setValue(OUTPUT, aimed));
        } else {
            if (aimed == input) aimed = aimed.getOpposite();
            level.setBlockAndUpdate(
                    pos,
                    aimed == output
                            ? state.setValue(INPUT, output).setValue(OUTPUT, input)
                            : state.setValue(INPUT, aimed));
        }

        return true;
    }

    public int topRotation(BlockState state) {
        return switch (inputSide(state)) {
            case NORTH -> 3;
            case SOUTH -> 0;
            case WEST -> 1;
            case EAST -> 2;
            default -> 0;
        };
    }

    protected static int turnedTopRotation(BlockState state) {
        Direction input = inputSide(state);
        if (input.getAxis() == Direction.Axis.Y) return 0;

        Direction leftHand = Facing.rotate(outputSide(state), input);

        if (leftHand == Direction.UP) {
            return switch (input) {
                case NORTH -> 2;
                case SOUTH -> 1;
                case WEST -> 3;
                default -> 0;
            };
        }

        if (leftHand == Direction.DOWN) {
            return switch (input) {
                case NORTH -> 1;
                case SOUTH -> 2;
                case EAST -> 3;
                default -> 0;
            };
        }

        return switch (input) {
            case SOUTH -> 3;
            case WEST -> 2;
            case EAST -> 1;
            default -> 0;
        };
    }
}
