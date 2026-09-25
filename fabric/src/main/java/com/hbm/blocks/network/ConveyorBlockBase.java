// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.interfaces.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class ConveyorBlockBase extends Block implements IConveyorBelt, IToolable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SLAB = Shapes.box(0D, 0D, 0D, 1D, 0.25D, 1D);

    protected ConveyorBlockBase(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SLAB;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SLAB;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    public Direction getInputDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    public Direction getOutputDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING);
    }

    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Snap snap = snap(level, pos, itemPos);

        Vec3 dest =
                snap.snap()
                        .subtract(
                                dir.getStepX() * speed,
                                dir.getStepY() * speed,
                                dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(snap.item());
        double len = motion.length();
        return snap.item()
                .add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(level, pos, itemPos).snap();
    }

    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) posX = clamped.x;
        if (dir.getStepZ() != 0) posZ = clamped.z;

        return new Snap(new Vec3(posX, pos.getY() + 0.25, posZ), clamped);
    }

    protected static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(
                Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D),
                itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level.isClientSide()) return;
        if (!(entity instanceof ItemEntity item) || entity.tickCount <= 10 || entity.isRemoved())
            return;

        EntityMovingItem moving = new EntityMovingItem(level);
        moving.setItemStack(item.getItem().copy());
        Vec3 snap = getClosestSnappingPosition(level, pos, entity.position());
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(moving);

        entity.discard();
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

        BlockState state = level.getBlockState(pos);

        if (!player.isShiftKeyDown()) {
            level.setBlock(
                    pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), UPDATE_ALL);
        } else {
            onSneakScrew(level, pos, state);
        }

        return true;
    }

    protected abstract void onSneakScrew(Level level, BlockPos pos, BlockState state);

    protected record Snap(Vec3 snap, Vec3 item) {}
}
