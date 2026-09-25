// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.foundry.FoundryChannelGraph;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.BlockEntityFoundryChannel;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.AutoRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@AutoRotate
public class FoundryChannel extends Block implements ITickingBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    private static final VoxelShape CORE = Shapes.box(0.3125D, 0D, 0.3125D, 0.6875D, 0.5D, 0.6875D);
    private static final VoxelShape ARM_N = Shapes.box(0.3125D, 0D, 0D, 0.6875D, 0.5D, 0.3125D);
    private static final VoxelShape ARM_S = Shapes.box(0.3125D, 0D, 0.6875D, 0.6875D, 0.5D, 1D);
    private static final VoxelShape ARM_W = Shapes.box(0D, 0D, 0.3125D, 0.3125D, 0.5D, 0.6875D);
    private static final VoxelShape ARM_E = Shapes.box(0.6875D, 0D, 0.3125D, 1D, 0.5D, 0.6875D);

    public FoundryChannel(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(EAST, false)
                        .setValue(WATERLOGGED, false));
    }

    private static final BooleanProperty[] BY_DIRECTION = byDirection();

    private static BooleanProperty[] byDirection() {
        BooleanProperty[] out = new BooleanProperty[Direction.values().length];
        out[Direction.NORTH.ordinal()] = NORTH;
        out[Direction.EAST.ordinal()] = EAST;
        out[Direction.SOUTH.ordinal()] = SOUTH;
        out[Direction.WEST.ordinal()] = WEST;
        return out;
    }

    public static BooleanProperty property(Direction dir) {
        return BY_DIRECTION[dir.ordinal()];
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        if (!dir.getAxis().isHorizontal()) return false;

        BlockPos target = pos.relative(dir);
        BlockState state = level.getBlockState(target);
        Block b = state.getBlock();

        if (b instanceof FoundryOutlet && state.getValue(FoundryOutlet.FACING) == dir) return true;

        return b == ModBlocks.FOUNDRY_CHANNEL.get() || b == ModBlocks.FOUNDRY_MOLD.get();
    }

    private static BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(NORTH, canConnectTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, canConnectTo(level, pos, Direction.SOUTH))
                .setValue(WEST, canConnectTo(level, pos, Direction.WEST))
                .setValue(EAST, canConnectTo(level, pos, Direction.EAST));
    }

    private static @Nullable BlockEntityFoundryChannel channel(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityFoundryChannel be ? be : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeState(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos())
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (!dir.getAxis().isHorizontal()) return state;
        return state.setValue(property(dir), canConnectTo(level, pos, dir));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(
                state.getValue(WEST) ? 0D : 0.3125D,
                0D,
                state.getValue(NORTH) ? 0D : 0.3125D,
                state.getValue(EAST) ? 1D : 0.6875D,
                0.5D,
                state.getValue(SOUTH) ? 1D : 0.6875D);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_N);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_S);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_W);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_E);
        return shape;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            FoundryChannelGraph.get(sl)
                    .addNode(pos.asLong(), null, FoundryChannelGraph.OPEN_HORIZONTALS);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        FoundryChannelGraph.get(level).removeNode(pos.asLong());
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ItemTags.SHOVELS)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFoundryChannel channel))
            return InteractionResult.PASS;

        if (channel.amount > 0 && channel.type != null) {
            ItemStack scrap = ItemScraps.create(new MaterialStack(channel.type, channel.amount));
            player.getInventory().placeItemBackInInventory(scrap);
            channel.amount = 0;
            channel.type = null;
            channel.setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFoundryChannel(pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
