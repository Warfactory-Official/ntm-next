// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.EnumChunkType;
import com.hbm.items.ModItems;
import com.mojang.serialization.MapCodec;
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

public class BlockResourceStoneMalachite extends Block {

    public static final MapCodec<BlockResourceStoneMalachite> CODEC =
            simpleCodec(BlockResourceStoneMalachite::new);

    public BlockResourceStoneMalachite(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<BlockResourceStoneMalachite> codec() {
        return CODEC;
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
        return List.of(ModItems.CHUNK_ORE.stack(EnumChunkType.MALACHITE, count));
    }
}
