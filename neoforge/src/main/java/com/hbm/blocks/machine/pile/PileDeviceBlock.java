/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.machine.pile;

import com.hbm.blockentity.machine.pile.PileControlBlockEntity;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.blockentity.machine.pile.PileVentBlockEntity;
import com.hbm.blocks.HorizontalBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PileDeviceBlock extends HorizontalBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                ToolInteractionHandler,
                LookOverlayProvider {

    private static final VoxelShape NORTH_SUPPORT = Block.box(0D, 0D, 0D, 16D, 16D, 1D);
    private static final VoxelShape SOUTH_SUPPORT = Block.box(0D, 0D, 15D, 16D, 16D, 16D);
    private static final VoxelShape WEST_SUPPORT = Block.box(0D, 0D, 0D, 1D, 16D, 16D);
    private static final VoxelShape EAST_SUPPORT = Block.box(15D, 0D, 0D, 16D, 16D, 16D);

    private final Kind kind;

    public PileDeviceBlock(BlockBehaviour.Properties props, Kind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case LOADER -> new PileLoaderBlockEntity(pos, state);
            case VENT -> new PileVentBlockEntity(pos, state);
            case CONTROL -> new PileControlBlockEntity(pos, state);
        };
    }

    @Override
    public MachineCaps caps() {
        return switch (kind) {
            case LOADER -> MachineCaps.of(ModBlockEntities.PILE_LOADER).items();
            case VENT ->
                    MachineCaps.of(ModBlockEntities.PILE_VENT)
                            .fluidIn()
                            .fluidFaces(
                                    PileVentBlockEntity.class,
                                    (be, face) ->
                                            face.side() == be.orientation()
                                                    && (face.fluid() == null
                                                            || face.fluid() == NTMFluids.AIR));
            case CONTROL -> MachineCaps.NONE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (kind == Kind.VENT) return Shapes.empty();
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SUPPORT;
            case SOUTH -> SOUTH_SUPPORT;
            case WEST -> WEST_SUPPORT;
            case EAST -> EAST_SUPPORT;
            default -> throw new IllegalStateException();
        };
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
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind == Kind.CONTROL)
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) face = Direction.NORTH;
        return defaultBlockState().setValue(FACING, face);
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
        if (kind != Kind.LOADER || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var loader = (PileLoaderBlockEntity) level.getBlockEntity(pos);
        if (loader.travel <= 0D && !loader.extending) {
            if (PileLoaderBlockEntity.canLoadRod(stack) && loader.getItem(0).isEmpty()) {
                loader.setItem(0, stack.copyWithCount(1));
                stack.consume(1, player);
                level.playSound(
                        null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1F, 1F);
            } else loader.startLoading();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind != Kind.LOADER || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide())
            ((PileLoaderBlockEntity) level.getBlockEntity(pos)).startLoading();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        BlockPos entry =
                kind == Kind.CONTROL
                        ? pos.below()
                        : pos.relative(level.getBlockState(pos).getValue(FACING).getOpposite());
        Direction surface =
                kind == Kind.CONTROL ? Direction.UP : level.getBlockState(pos).getValue(FACING);
        BlockState state = level.getBlockState(entry);
        return state.is(ModBlocks.PILE_BLOCK.get())
                && ((PileBlock) state.getBlock())
                        .onToolUse(level, player, entry, surface, hit, tool);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (kind == Kind.LOADER
                && level.getBlockEntity(pos) instanceof PileLoaderBlockEntity loader) {
            info.title(getName())
                    .row(
                            Component.translatable(
                                    "overlay.hbm.pile.loader.temp",
                                    Math.round(loader.observedHeat),
                                    PileCoreBlockEntity.MAX_HEAT));
            if (!loader.getItem(0).isEmpty())
                info.row(
                        Component.empty()
                                .append(
                                        Component.translatable(
                                                "overlay.hbm.pile.loader.loading",
                                                loader.getItem(0).getHoverName())));
            if (!loader.observedRod.isEmpty()) {
                info.row(
                        Component.empty()
                                .append(
                                        Component.translatable(
                                                "overlay.hbm.pile.loader.last_rod",
                                                loader.observedRod.getHoverName())));
                if (loader.observedDepletionPercent > 0D)
                    info.row(
                            Component.empty()
                                    .append(
                                            Component.translatable(
                                                    "overlay.hbm.pile.loader.depletion",
                                                    Math.round(loader.observedDepletionPercent))));
            }
        } else if (kind == Kind.CONTROL
                && level.getBlockEntity(pos) instanceof PileControlBlockEntity control) {
            info.title(getName())
                    .row(
                            Component.translatable(
                                    "overlay.hbm.pile.control.extension",
                                    (int) (control.travel * 100D)));
        }
    }

    public enum Kind {
        LOADER("pile_loader"),
        VENT("pile_vent"),
        CONTROL("pile_control");

        private final String modelName;

        Kind(String modelName) {
            this.modelName = modelName;
        }

        public String modelName() {
            return modelName;
        }
    }
}
