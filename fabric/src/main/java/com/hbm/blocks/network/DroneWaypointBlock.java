// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModItems;
import com.hbm.tileentity.network.BlockEntityDroneWaypoint;
import com.hbm.tileentity.network.BlockEntityDroneWaypointRequest;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DroneWaypointBlock extends Block implements ITickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public static final MapCodec<DroneWaypointBlock> CODEC =
            simpleCodec(props -> new DroneWaypointBlock(props, Kind.TRANSPORT));

    private final Kind kind;

    public DroneWaypointBlock(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case TRANSPORT -> new BlockEntityDroneWaypoint(pos, state);
            case REQUEST -> new BlockEntityDroneWaypointRequest(pos, state);
        };
    }

    private static boolean canAttach(Level level, BlockPos support, Direction face) {
        BlockState state = level.getBlockState(support);
        return state.isFaceSturdy(level, support, face)
                || state.isCollisionShapeFullBlock(level, support);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockPos support = context.getClickedPos().relative(face.getOpposite());
        return canAttach(context.getLevel(), support, face)
                ? defaultBlockState().setValue(FACING, face)
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
        Direction face = state.getValue(FACING);
        if (!canAttach(level, pos.relative(face.getOpposite()), face))
            level.destroyBlock(pos, true);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double min = 6D;
        double max = 10D;
        return switch (state.getValue(FACING)) {
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
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (kind == Kind.TRANSPORT && stack.is(ModItems.DRONE_LINKER.get()))
            return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind != Kind.TRANSPORT) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityDroneWaypoint waypoint))
            return InteractionResult.PASS;
        if (!level.isClientSide()) waypoint.addHeight(player.isSecondaryUseActive() ? -1 : 1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (kind != Kind.TRANSPORT
                || !(level.getBlockEntity(pos) instanceof BlockEntityDroneWaypoint waypoint)) {
            return;
        }

        info.title(getName().getString(), 0xffff00, 0x404000);
        info.line("Waypoint distance: " + waypoint.height);
        if (waypoint.next != null) {
            info.line(
                    "Next waypoint: "
                            + waypoint.next.getX()
                            + " / "
                            + waypoint.next.getY()
                            + " / "
                            + waypoint.next.getZ());
        }
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
        TRANSPORT,
        REQUEST
    }
}
