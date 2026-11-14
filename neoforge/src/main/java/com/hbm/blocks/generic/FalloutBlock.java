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

package com.hbm.blocks.generic;

import com.hbm.extprop.ContaminationEffect;
import com.hbm.extprop.LivingHazards;
import com.hbm.items.ModItems;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FalloutBlock extends Block {
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;

    private static final VoxelShape[] LAYER_SHAPES =
            Block.boxes(
                    SnowLayerBlock.MAX_HEIGHT,
                    layers -> Block.column(16.0, 0.0, 16.0 * layers / SnowLayerBlock.MAX_HEIGHT));

    public static final MapCodec<FalloutBlock> CODEC = simpleCodec(FalloutBlock::new);

    @Override
    protected MapCodec<? extends FalloutBlock> codec() {
        return CODEC;
    }

    public FalloutBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(0.1F)
                .sound(SoundType.GRAVEL)
                .noOcclusion()
                .replaceable()
                .noLootTable();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LAYER_SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LAYER_SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getVisualShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LAYER_SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return LAYER_SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    public static boolean supports(
            Block layer, BlockState below, BlockGetter level, BlockPos belowPos) {
        if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) {
            return false;
        }
        if (below.is(BlockTags.LEAVES)) {
            return true;
        }
        if (below.is(layer)) {
            return below.getValue(LAYERS) == SnowLayerBlock.MAX_HEIGHT;
        }
        return Block.isFaceFull(below.getCollisionShape(level, belowPos), Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var belowPos = pos.below();
        return supports(this, level.getBlockState(belowPos), level, belowPos);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(
                state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            var layers = Math.min(existing.getValue(LAYERS) + 1, SnowLayerBlock.MAX_HEIGHT);
            return existing.setValue(LAYERS, layers);
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        var layers = state.getValue(LAYERS);
        if (layers < SnowLayerBlock.MAX_HEIGHT && context.getItemInHand().is(asItem())) {
            if (context.replacingClickedOnBlock()) {
                return context.getClickedFace() == Direction.UP;
            }
            return true;
        }
        return layers == 1;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()
                && entity instanceof LivingEntity living
                && !(living instanceof Player player && player.isCreative())) {
            living.addEffect(new UncurableEffectInstance(HbmPotion.radiation(), 12000, 0));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            LivingHazards.addDose(player, new ContaminationEffect(1.0F, 200, false));
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        if (!state.canSurvive(params.getLevel(), pos)) {
            return List.of();
        }
        return List.of(new ItemStack(ModItems.FALLOUTITEM.get()));
    }
}
