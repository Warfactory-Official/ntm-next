/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.generic;

import com.hbm.blockentity.SnowglobeBlockEntity;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SnowglobeBlock extends Block implements EntityBlock {
    private static final VoxelShape SNOWGLOBE_SHAPE = Block.box(4, 0, 4, 12, 5, 12);

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static Consumer<SnowglobeBlockEntity> OPEN_GUI;

    public SnowglobeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SNOWGLOBE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SNOWGLOBE_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SnowglobeBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = RotationSegment.convertToSegment(context.getRotation() + 180F);
        return defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof SnowglobeBlockEntity snowglobe) {
            var type = SnowglobeBlockEntity.typeOf(stack);
            if (snowglobe.type != type) {
                snowglobe.hbm$syncSet$SnowglobeBlockEntity$type(type);
                snowglobe.setChanged();
            }
            if (!level.isClientSide()) {
                snowglobe.networkPackNTTracking();
            }
        }
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof SnowglobeBlockEntity snowglobe) {
            return SnowglobeBlockEntity.stackOf(snowglobe.type);
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof SnowglobeBlockEntity snowglobe) {
            var item =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            SnowglobeBlockEntity.stackOf(snowglobe.type),
                            0,
                            0,
                            0);
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()
                && level.getBlockEntity(pos) instanceof SnowglobeBlockEntity snowglobe) {
            OPEN_GUI.accept(snowglobe);
        }
        return InteractionResult.SUCCESS;
    }

    public enum SnowglobeType {
        NONE("NONE", null),
        RIVETCITY("Rivet City", "Welcome to Rivet City. Please wait while the bridge extends."),
        TENPENNYTOWER(
                "Tenpenny Tower",
                "Tenpenny Tower is the brainchild of Allistair Tenpenny, a British refugee who came to the Capital Wasteland seeking his fortune."),
        LUCKY38(
                "Lucky 38",
                "My guess? Leads to a big cashout at some casino - and if the \"38\" on it is any indication... well... Lucky 38 it is."),
        SIERRAMADRE(
                "Sierra Madre",
                "It's the moment you've been waiting for, the reason we're all here - the Gala Event, the Grand Opening of the Sierra Madre Casino."),
        PRYDWEN(
                "The Prydwen",
                "People of the Commonwealth. Do not interfere. Our intentions are peaceful. We are the Brotherhood of Steel.");

        public static final SnowglobeType[] VALUES = values();

        public final String label;
        public final @Nullable String inscription;

        SnowglobeType(String label, @Nullable String inscription) {
            this.label = label;
            this.inscription = inscription;
        }

        public static int count() {
            return VALUES.length;
        }
    }
}
