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

import com.hbm.blockentity.PlushieBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.registration.OpenRegistry;
import com.hbm.sound.ModSounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class PlushieBlock extends Block implements TickingEntityBlock {

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final OpenRegistry<Plushie> TYPES = new OpenRegistry<>("plushie", Plushie::id);

    static {
        for (var type : PlushieType.values()) {
            TYPES.add(type);
        }
    }

    public PlushieBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlushieBlockEntity(pos, state);
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
        if (level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            var type = PlushieBlockEntity.typeOf(stack);
            if (plushie.type != type) {
                plushie.hbm$syncSet$PlushieBlockEntity$type(type);
                plushie.setChanged();
            }
            if (!level.isClientSide()) {
                plushie.networkPackNTTracking();
            }
        }
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            return PlushieBlockEntity.stackOf(plushie.type);
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            var item =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            PlushieBlockEntity.stackOf(plushie.type),
                            0,
                            0,
                            0);
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
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
        if (level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            return plushie.type.useItemOn(stack, plushie, player);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            if (level.isClientSide()) {
                plushie.squish();
            } else {
                plushie.type.squeak(level, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public interface Plushie {

        String name();

        String label();

        @Nullable String inscription();

        ItemStack stack();

        void squeak(Level level, BlockPos pos);

        default InteractionResult useItemOn(
                ItemStack stack, PlushieBlockEntity plushie, Player player) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        default String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum PlushieType implements Plushie {
        NONE("NONE", null),
        YOMI("Yomi", "Hi! Can I be your rabbit friend?"),
        NUMBERNINE("Number Nine", "None of y'all deserve coal."),
        HUNDUN("Hundun", "混沌"),
        DERG("Dragon", "Squeeze him.");

        public final String label;
        public final @Nullable String inscription;

        PlushieType(String label, @Nullable String inscription) {
            this.label = label;
            this.inscription = inscription;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public @Nullable String inscription() {
            return inscription;
        }

        @Override
        public ItemStack stack() {
            return this == NONE ? new ItemStack(ModBlocks.PLUSHIE) : ModBlocks.PLUSHIES.stack(this);
        }

        @Override
        public void squeak(Level level, BlockPos pos) {
            if (this == HUNDUN) {
                level.playSound(
                        null,
                        pos,
                        ModSounds.BLOCK_HUNDUNS_MAGNIFICENT_HOWL.get(),
                        SoundSource.BLOCKS,
                        100F,
                        1F);
            } else {
                level.playSound(
                        null, pos, ModSounds.BLOCK_PLUSHY.get(), SoundSource.BLOCKS, 0.25F, 1F);
            }
        }
    }
}
