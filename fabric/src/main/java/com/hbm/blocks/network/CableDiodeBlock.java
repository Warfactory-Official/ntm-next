// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.lib.Library;
import com.hbm.tileentity.network.BlockEntityCableDiode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CableDiodeBlock extends Block
        implements ITickingBlock, IToolable, ILookOverlay, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static Consumer<BlockEntityCableDiode> OPEN_SCREEN = diode -> {};

    public CableDiodeBlock(BlockBehaviour.Properties props) {
        super(props);
        BlockState def = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (Direction dir : Direction.VALUES) def = def.setValue(connectionFor(dir), false);
        registerDefaultState(def);
    }

    public static BooleanProperty connectionFor(Direction dir) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                FACING,
                BlockStateProperties.NORTH,
                BlockStateProperties.EAST,
                BlockStateProperties.SOUTH,
                BlockStateProperties.WEST,
                BlockStateProperties.UP,
                BlockStateProperties.DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state =
                defaultBlockState()
                        .setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
        for (Direction dir : Direction.VALUES) {
            BlockPos npos = ctx.getClickedPos().relative(dir);
            state =
                    state.setValue(
                            connectionFor(dir),
                            Library.canConnect(ctx.getLevel(), npos, dir.getOpposite()));
        }
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        for (Direction dir : Direction.VALUES) {
            result =
                    result.setValue(
                            connectionFor(rotation.rotate(dir)),
                            state.getValue(connectionFor(dir)));
        }
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction dir : Direction.VALUES) {
            result =
                    result.setValue(
                            connectionFor(mirror.mirror(dir)), state.getValue(connectionFor(dir)));
        }
        return result;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        boolean joins = Library.canConnect(level, pos.relative(dir), dir.getOpposite());
        if (level.isClientSide())
            return Library.predictArm(state, connectionFor(dir), neighbourState, joins);
        return state.setValue(connectionFor(dir), joins);
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
        if (level instanceof ServerLevel sl) {
            LevelNodeGraph.watchForeignNeighbours(sl, pos);
            Library.redrawArms(state, sl, pos);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCableDiode(pos, state);
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
        IToolable.ToolType tool = IToolable.ToolType.getType(stack);
        if (tool == ToolType.SCREWDRIVER
                || tool == ToolType.HAND_DRILL
                || tool == ToolType.DEFUSER) {
            return InteractionResult.PASS;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityCableDiode diode) {
            OPEN_SCREEN.accept(diode);
        }
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
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCableDiode diode)) return false;
        if (level.isClientSide())
            return tool == ToolType.SCREWDRIVER
                    || tool == ToolType.HAND_DRILL
                    || tool == ToolType.DEFUSER;

        switch (tool) {
            case SCREWDRIVER -> {
                diode.stepLimitUp();
            }
            case HAND_DRILL -> {
                diode.stepLimitDown();
            }
            case DEFUSER -> {
                int p = diode.priority.ordinal() + 1;
                if (p > 4) p = 0;
                diode.priority = IEnergyHandlerMK2.ConnectionPriority.VALUES[p];
            }
            default -> {
                return false;
            }
        }
        diode.settingsChanged();
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCableDiode diode)) return;
        info.title(getName().getString(), 0xffff00, 0x404000)
                .line("Max.: " + BobMathUtil.getShortNumber(diode.getMaxPower()) + "HE/t")
                .line("Priority: " + diode.priority.name());
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CABLE_DIODE)
                .powerIn()
                .faces(BlockEntityCableDiode.class, BlockEntityCableDiode::acceptsFace);
    }
}
