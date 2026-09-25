// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ITickingBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.tileentity.network.BlockEntityRadioTorch;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class RadioTorchBlock extends Block implements ITickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static Consumer<BlockEntity> OPEN_SCREEN = blockEntity -> {};
    private final Kind kind;

    public RadioTorchBlock(BlockBehaviour.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.UP).setValue(LIT, false));
    }

    private boolean canAttach(Level level, BlockPos support, Direction face) {
        BlockState state = level.getBlockState(support);
        return switch (kind) {
            case READER -> NtmContracts.ROR_VALUE_PROVIDER.at(level, support, state) != null;
            case CONTROLLER -> NtmContracts.ROR_INTERACTIVE.at(level, support, state) != null;
            case COUNTER ->
                    state.isFaceSturdy(level, support, face)
                            || NtmContracts.INVENTORY.at(level, support, state) != null;
            default ->
                    state.isFaceSturdy(level, support, face)
                            || state.hasAnalogOutputSignal()
                            || state.isSignalSource();
        };
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        BlockPos support = context.getClickedPos().relative(direction.getOpposite());
        return canAttach(context.getLevel(), support, direction)
                ? defaultBlockState().setValue(FACING, direction)
                : null;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block source,
            @Nullable Orientation orientation,
            boolean moving) {
        super.neighborChanged(state, level, pos, source, orientation, moving);
        Direction facing = state.getValue(FACING);

        if (!canAttach(level, pos.relative(facing.getOpposite()), facing)) {
            if (!level.isClientSide()) level.destroyBlock(pos, true);
        } else if (level.getBlockEntity(pos) instanceof BlockEntityRadioTorch torch)
            torch.refreshAttached();
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        double min = 6D;
        double max = 10D;
        return switch (direction) {
            case DOWN -> Block.box(min, min, min, max, 16D, max);
            case UP -> Block.box(min, 0D, min, max, max, max);
            case NORTH -> Block.box(min, min, min, max, max, 16D);
            case SOUTH -> Block.box(min, min, 0D, max, max, max);
            case WEST -> Block.box(min, min, min, 16D, max, max);
            case EAST -> Block.box(0D, min, min, max, max, max);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRadioTorch.create(kind, pos, state);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind == Kind.RECEIVER || kind == Kind.LOGIC;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof BlockEntityRadioTorch radio
                ? radio.lastState
                : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (kind == Kind.COUNTER) {
            if (!level.isClientSide() && blockEntity instanceof MenuProvider provider)
                player.openMenu(provider);
        } else if (level.isClientSide() && blockEntity != null) {
            OPEN_SCREEN.accept(blockEntity);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public enum Kind {
        SENDER,
        RECEIVER,
        COUNTER,
        LOGIC,
        READER,
        CONTROLLER
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level.getBlockEntity(pos) instanceof BlockEntityRadioTorch torch)) return;
        torch.refreshAttached();

        if ((kind == Kind.RECEIVER || kind == Kind.LOGIC) && !oldState.is(this)) {
            torch.lastUpdate = level.getGameTime();
        }
    }
}
