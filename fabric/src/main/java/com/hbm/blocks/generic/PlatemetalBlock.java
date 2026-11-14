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

import com.hbm.items.block.BlastInfoBlockItem;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class PlatemetalBlock extends Block {
    public static final EnumProperty<Variant> VARIANT =
            EnumProperty.create("variant", Variant.class);
    public static final MapCodec<PlatemetalBlock> CODEC = simpleCodec(PlatemetalBlock::new);

    public PlatemetalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(VARIANT, Variant.BASE));
    }

    public ItemStack stackFor(Variant variant) {
        var stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, stateFor(variant));
        return stack;
    }

    public static BlockItemStateProperties stateFor(Variant variant) {
        return BlockItemStateProperties.EMPTY.with(VARIANT, variant);
    }

    public static Variant variant(ItemStack stack) {
        var properties =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        var variant = properties.get(VARIANT);
        return variant == null ? Variant.BASE : variant;
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return stackFor(state.getValue(VARIANT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected MapCodec<PlatemetalBlock> codec() {
        return CODEC;
    }

    public enum Variant implements StringRepresentable {
        BASE,
        BLACK,
        WHITE,
        RED,
        GREEN,
        LIGHT_GRAY,
        BLUE,
        PURPLE,
        CYAN,
        PINK,
        LIME,
        YELLOW,
        LIGHT_BLUE,
        MAGENTA,
        ORANGE;

        public static final Variant[] VALUES = values();

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class PlatemetalItem extends BlastInfoBlockItem {
        public PlatemetalItem(PlatemetalBlock block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            var variant = variant(stack);
            String key = getDescriptionId();
            if (variant != Variant.BASE) {
                key += "." + variant.getSerializedName();
            }
            return Component.translatable(key);
        }
    }
}
