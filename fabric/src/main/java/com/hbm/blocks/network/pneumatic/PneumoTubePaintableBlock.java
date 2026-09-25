// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network.pneumatic;

import com.hbm.api.ntl.IPneumaticConnector;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.FluidDuctPaintableBlock;
import com.hbm.blocks.network.PaintableCamoBlock;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTubePaintable;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PneumoTubePaintableBlock extends Block
        implements ITickingBlock,
                IToolable,
                ICapabilityBlock,
                PaintableCamoBlock,
                IPneumaticConnector {

    public static final BooleanProperty PAINTED = BooleanProperty.create("painted");
    public static final BooleanProperty OVERLAY = BooleanProperty.create("overlay");

    public PneumoTubePaintableBlock(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(PAINTED, false).setValue(OVERLAY, true));
    }

    @Override
    public BooleanProperty paintedProperty() {
        return PAINTED;
    }

    @Override
    public BooleanProperty overlayProperty() {
        return OVERLAY;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PAINTED, OVERLAY);
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
        if (stack.getItem() instanceof BlockItem blockItem
                && !state.getValue(PAINTED)
                && FluidDuctPaintableBlock.allowedPaint(blockItem.getBlock(), this)) {
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof BlockEntityPneumoTubePaintable tube) {
                tube.setCamo(blockItem.getBlock().defaultBlockState());
                level.setBlock(pos, state.setValue(PAINTED, true), Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube))
            return InteractionResult.PASS;
        if (!tube.isCompressor() && !tube.isEndpoint()) return InteractionResult.PASS;
        if (!level.isClientSide()) tube.openMenu(player, pos);
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
        BlockState state = level.getBlockState(pos);

        if (tool == ToolType.HAND_DRILL) {
            if (!state.getValue(PAINTED)) return false;
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTubePaintable tube)
                    tube.setCamo(null);
                level.setBlock(pos, state.setValue(PAINTED, false), Block.UPDATE_ALL);
            }
            return true;
        }

        if (tool == ToolType.SCREWDRIVER)
            return PneumoTubeBlock.screwDirections(level, player, pos);

        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.setBlock(
                        pos, state.setValue(OVERLAY, !state.getValue(OVERLAY)), Block.UPDATE_ALL);
            }
            return true;
        }

        return false;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this))
            PneumaticNetwork.addNode(sl, pos);
        if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTube be) be.refreshRedstone();
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PneumaticNetwork.removeNode(level, pos);
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
        if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTube be) be.refreshRedstone();
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPneumoTubePaintable(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PNEUMATIC_TUBE_PAINTABLE)
                .fluidIn()
                .fluidFaces(
                        BlockEntityPneumoTube.class,
                        (be, face) ->
                                face.fluid() == null || be.acceptsFluid(face.fluid(), face.side()));
    }
}
