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

import com.hbm.blocks.ModBlocks;
import com.hbm.potion.HbmPotion;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

public class SellafieldBlock extends Block {

    public static final int LEVELS = 6;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, LEVELS - 1);
    public static final MapCodec<SellafieldBlock> CODEC = simpleCodec(SellafieldBlock::new);

    public SellafieldBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(5.0F)
                .sound(SoundType.STONE)
                .randomTicks()
                .noLootTable()
                .requiresCorrectToolForDrops();
    }

    public static int tint(int level) {
        return switch (level) {
            case 0 -> 0x4C7939;
            case 1 -> 0x418223;
            case 2 -> 0x338C0E;
            case 3 -> 0x1C9E00;
            case 4 -> 0x02B200;
            case 5 -> 0x00D300;
            default -> throw new IllegalArgumentException();
        };
    }

    public ItemStack stackFor(int level) {
        var stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LEVEL, level));
        return stack;
    }

    public static int level(ItemStack stack) {
        var properties =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        var value = properties.get(LEVEL);
        return value == null ? 0 : value;
    }

    @Override
    protected MapCodec<SellafieldBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var stage = state.getValue(LEVEL);
        var bound = stage == 0 ? 25 : 15;
        if (random.nextInt(bound) != 0) {
            return;
        }

        if (stage == 0) {
            level.setBlockAndUpdate(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
        } else {
            level.setBlock(pos, state.setValue(LEVEL, stage - 1), UPDATE_CLIENTS);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel && entity instanceof LivingEntity living) {
            var stage = state.getValue(LEVEL);
            var amplifier = stage == LEVELS - 1 ? stage * 2 : stage;
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 600, amplifier));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return stackFor(state.getValue(LEVEL));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackFor(state.getValue(LEVEL)));
    }

    public static class SellafieldBlockItem extends BlockItem {

        public SellafieldBlockItem(SellafieldBlock block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            var stage = SellafieldBlock.level(stack);
            var descriptionId = getDescriptionId();
            if (stage != 0) {
                descriptionId += "." + stage;
            }
            return Component.translatable(descriptionId);
        }
    }
}
