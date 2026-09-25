// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.tileentity.network.BlockEntityCranePartitioner;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CranePartitioner extends Block
        implements ITickingBlock, IConveyorBelt, IEnterableBlock, ICapabilityBlock {

    public static final MapCodec<CranePartitioner> CODEC = simpleCodec(CranePartitioner::new);

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Shapes.box(0D, 0D, 0D, 1D, 0.75D, 1D);

    public CranePartitioner(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CRANE_PARTITIONER).items();
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
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCranePartitioner(pos, state);
    }

    public Direction getTravelDirection(Level level, BlockPos pos, @Nullable Vec3 itemPos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return getTravelDirection(level, pos, null) == dir;
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return false;
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {}

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCranePartitioner partitioner)) return;

        ItemStack stack = entity.getItemStack().copy();
        int slots = BlockEntityCranePartitioner.SLOT_COUNT;
        boolean queued = CrystallizerRecipes.INSTANCE.getAmount(stack) > 0;

        ItemStack left =
                queued
                        ? InventoryUtil.tryAddItemToInventory(
                                partitioner.inventory, 0, slots - 1, stack)
                        : InventoryUtil.tryAddItemToInventory(
                                partitioner.inventory, slots, slots * 2 - 1, stack);
        partitioner.setChanged();

        if (!left.isEmpty()) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            left.copy()));
        }
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        Vec3 snap = snap(pos, clamped, dir);

        Vec3 dest =
                snap.subtract(
                        dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(clamped);
        double len = motion.length();
        return clamped.add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(pos, clamp(pos, itemPos), getTravelDirection(level, pos, itemPos));
    }

    private static Vec3 snap(BlockPos pos, Vec3 clamped, Direction dir) {
        double posX = dir.getStepX() != 0 ? clamped.x : pos.getX() + 0.5;
        double posZ = dir.getStepZ() != 0 ? clamped.z : pos.getZ() + 0.5;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    private static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(
                Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D),
                itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }
}
