// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

public class BlockSellafieldOre extends BlockSellafieldSlaked {

    public static final MapCodec<BlockSellafieldOre> CODEC =
            simpleCodec(p -> new BlockSellafieldOre(p, null, 0, 0));

    private final @Nullable Supplier<Item> drop;
    private final int xpMin;
    private final int xpMax;

    public BlockSellafieldOre(
            BlockBehaviour.Properties props, @Nullable Supplier<Item> drop, int xpMin, int xpMax) {
        super(props);
        this.drop = drop;
        this.xpMin = xpMin;
        this.xpMax = xpMax;
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(5.0F)
                .sound(SoundType.STONE)
                .noLootTable()
                .requiresCorrectToolForDrops();
    }

    @Override
    protected MapCodec<BlockSellafieldOre> codec() {
        return CODEC;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (drop == null) return List.of(new ItemStack(asItem()));

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
        int count =
                fortune > 0
                        ? Math.max(0, params.getLevel().getRandom().nextInt(fortune + 2) - 1) + 1
                        : 1;
        return List.of(new ItemStack(drop.get(), count));
    }

    @Override
    public int getExpDrop(
            BlockState state,
            LevelAccessor level,
            BlockPos pos,
            @Nullable BlockEntity blockEntity,
            @Nullable Entity breaker,
            ItemStack tool) {
        return xpMax > 0 ? UniformInt.of(xpMin, xpMax).sample(level.getRandom()) : 0;
    }
}
