// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.BlockEntityFoundryOutlet;
import com.hbm.util.I18nUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityFoundryOutlet.class, calling = "refreshRedstone")
public class FoundryOutlet extends Block
        implements IToolable, ILookOverlay, ITickingBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_N = Shapes.box(0.3125D, 0D, 0.625D, 0.6875D, 0.5D, 1D);
    private static final VoxelShape SHAPE_S = Shapes.box(0.3125D, 0D, 0D, 0.6875D, 0.5D, 0.375D);
    private static final VoxelShape SHAPE_W = Shapes.box(0.625D, 0D, 0.3125D, 1D, 0.5D, 0.6875D);
    private static final VoxelShape SHAPE_E = Shapes.box(0D, 0D, 0.3125D, 0.375D, 0.5D, 0.6875D);

    public FoundryOutlet(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(WATERLOGGED, false));
    }

    private static @Nullable BlockEntityFoundryOutlet outlet(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityFoundryOutlet be ? be : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_S;
            case WEST -> SHAPE_W;
            case EAST -> SHAPE_E;
            default -> SHAPE_N;
        };
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
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntityFoundryOutlet be = outlet(level, pos);
        if (be == null) return InteractionResult.PASS;

        MaterialStack mat = ItemScraps.getMats(held);
        if (mat != null) {
            be.filter = mat.material;
        } else {
            be.invertRedstone = !be.invertRedstone;
        }
        be.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntityFoundryOutlet be = outlet(level, pos);
        if (be == null) return InteractionResult.PASS;
        be.invertRedstone = !be.invertRedstone;
        be.setChanged();
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
        BlockEntityFoundryOutlet be = outlet(level, pos);
        if (be == null) return false;

        if (tool == ToolType.SCREWDRIVER) {
            if (!level.isClientSide()) {
                be.filter = null;
                be.invertFilter = false;
                be.setChanged();
            }
            return true;
        }

        if (tool == ToolType.HAND_DRILL) {
            if (!level.isClientSide()) {
                be.invertFilter = !be.invertFilter;
                be.setChanged();
            }
            return true;
        }

        return false;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        BlockEntityFoundryOutlet be = outlet(level, pos);
        if (be == null) return;

        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFF4000, 0x401000);

        if (be.filter != null) {
            info.line(
                    I18nUtil.resolveKey("foundry.filter", be.filter.getLocalizedName()), 0xFFFF55);
        }
        if (be.invertFilter) {
            info.line(I18nUtil.resolveKey("foundry.invertFilter"), 0xFFFF55);
        }
        if (be.invertRedstone) {
            info.line(I18nUtil.resolveKey("foundry.inverted"), 0xAA0000);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFoundryOutlet(pos, state);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(
                state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
