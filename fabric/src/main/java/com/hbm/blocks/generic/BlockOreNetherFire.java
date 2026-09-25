// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
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

public class BlockOreNetherFire extends Block {

    public BlockOreNetherFire(BlockBehaviour.Properties props) {
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

        RandomSource random = params.getLevel().getRandom();
        int fortune =
                tool == null
                        ? 0
                        : EnchantmentHelper.getItemEnchantmentLevel(
                                enchantments.getOrThrow(Enchantments.FORTUNE), tool);

        int count = fortune > 0 ? Math.max(0, random.nextInt(fortune + 2) - 1) + 1 : 1;

        Float radius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);

        int fire = 0;
        int phosphorus = 0;
        for (int i = 0; i < count; i++) {
            if (random.nextInt(10) == 0) phosphorus++;
            else fire++;
        }
        if (radius != null) {
            fire = decay(random, fire, radius);
            phosphorus = decay(random, phosphorus, radius);
        }

        List<ItemStack> drops = new ArrayList<>(2);
        if (fire > 0) drops.add(new ItemStack(ModItems.POWDER_FIRE.get(), fire));
        if (phosphorus > 0) drops.add(new ItemStack(ModItems.INGOT_PHOSPHORUS.get(), phosphorus));
        return drops;
    }

    private static int decay(RandomSource random, int count, float radius) {
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (random.nextFloat() <= 1.0F / radius) kept++;
        }
        return kept;
    }
}
