// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.interfaces.IToolable;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.network.BlockEntityPipePaintable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FluidDuctPaintableBlock extends FluidDuctBlockBase
        implements EntityBlock, IToolable, PaintableCamoBlock {

    public static final BooleanProperty PAINTED = BooleanProperty.create("painted");
    public static final BooleanProperty OVERLAY = BooleanProperty.create("overlay");

    public FluidDuctPaintableBlock(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(PAINTED, false).setValue(OVERLAY, true));
    }

    public static boolean allowedPaint(Block paint, Block that) {
        if (paint == Blocks.GRASS_BLOCK) return false;
        return paint.defaultBlockState().isSolidRender() && paint != that;
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

        if (stack.getItem() instanceof FluidIdentifierItem) return InteractionResult.PASS;
        if (stack.getItem() instanceof BlockItem blockItem
                && !state.getValue(PAINTED)
                && allowedPaint(blockItem.getBlock(), this)) {
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof BlockEntityPipePaintable pipe) {
                pipe.setCamo(blockItem.getBlock().defaultBlockState());
                level.setBlock(pos, state.setValue(PAINTED, true), Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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
        if (tool == ToolType.SCREWDRIVER) {
            if (state.getValue(PAINTED)
                    && level.getBlockEntity(pos) instanceof BlockEntityPipePaintable pipe
                    && pipe.getCamo() != null) {
                if (!level.isClientSide()) {
                    pipe.setCamo(null);
                    level.setBlock(pos, state.setValue(PAINTED, false), Block.UPDATE_ALL);
                }
                return true;
            }
            return false;
        }
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPipePaintable(pos, state);
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }
}
