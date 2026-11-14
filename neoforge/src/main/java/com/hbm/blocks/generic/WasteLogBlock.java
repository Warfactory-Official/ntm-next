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

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class WasteLogBlock extends RotatedPillarBlock {
    private final Supplier<Item> barkDrop;

    public WasteLogBlock(BlockBehaviour.Properties props, Supplier<Item> barkDrop) {
        super(props);
        this.barkDrop = barkDrop;
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(5.0F, 1.5F)
                .sound(SoundType.WOOD)
                .noLootTable()
                .ignitedByLava();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var tool = params.getParameter(LootContextParams.TOOL);
        var enchantments = params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (EnchantmentHelper.getItemEnchantmentLevel(
                        enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool)
                > 0) {
            return List.of(new ItemStack(asItem()));
        }

        RandomSource random = params.getLevel().getRandom();
        if (random.nextInt(1000) == 0) {
            return List.of(new ItemStack(barkDrop.get()));
        }
        return List.of(new ItemStack(Items.CHARCOAL, 2 + random.nextInt(3)));
    }
}
