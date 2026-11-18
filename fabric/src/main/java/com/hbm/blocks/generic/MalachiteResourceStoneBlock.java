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

import com.hbm.items.ModItems;
import com.hbm.items.variant.OreChunkType;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class MalachiteResourceStoneBlock extends Block {
    public MalachiteResourceStoneBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemInstance tool = params.getOptionalParameter(LootContextParams.TOOL);
        HolderLookup.RegistryLookup<Enchantment> enchantments =
                params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (tool != null
                && EnchantmentHelper.getItemEnchantmentLevel(
                                enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool)
                        > 0) {
            return List.of(new ItemStack(asItem()));
        }

        int fortune =
                tool == null
                        ? 0
                        : EnchantmentHelper.getItemEnchantmentLevel(
                                enchantments.getOrThrow(Enchantments.FORTUNE), tool);
        int count = 3 + fortune + params.getLevel().getRandom().nextInt(fortune + 2);
        Float radius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (radius != null && params.getLevel().getRandom().nextFloat() > 1.0F / radius)
            return List.of();
        return List.of(ModItems.CHUNK_ORE.stack(OreChunkType.MALACHITE, count));
    }
}
