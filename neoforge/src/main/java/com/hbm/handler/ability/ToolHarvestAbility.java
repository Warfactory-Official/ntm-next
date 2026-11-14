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

package com.hbm.handler.ability;

import com.hbm.data.DataGroups;
import com.hbm.data.ItemData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ShredderRecipe;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ToolAbilityItem;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class ToolHarvestAbility extends BaseAbility {

    private static final int BASE_SORT_ORDER = 100;
    private static final int[] FORTUNE_POWERS = {1, 2, 3, 4, 5, 9};

    private static final LootTool HELD = (dig, _) -> dig.held();
    private static final Outcome ORDINARY =
            (dig, pos, _) -> ToolAbilityItem.harvest(dig, pos, false);

    public static final ToolHarvestAbility NONE =
            new ToolHarvestAbility(100, "none", "", oneLevel(), null, HELD, ORDINARY);
    public static final ToolHarvestAbility SILK_TOUCH =
            new ToolHarvestAbility(
                    101,
                    "silk",
                    "tool.ability.silktouch",
                    oneLevel(),
                    ItemData.TOOL_ABILITY_SILK,
                    (dig, _) -> enchanted(dig, Enchantments.SILK_TOUCH, 1),
                    ORDINARY);
    public static final ToolHarvestAbility FORTUNE =
            new ToolHarvestAbility(
                    102,
                    "luck",
                    "tool.ability.luck",
                    levelText(FORTUNE_POWERS),
                    ItemData.TOOL_ABILITY_LUCK,
                    (dig, level) -> enchanted(dig, Enchantments.FORTUNE, FORTUNE_POWERS[level]),
                    ORDINARY);
    public static final ToolHarvestAbility AUTO_SMELTER =
            new ToolHarvestAbility(
                    103,
                    "smelter",
                    "tool.ability.smelter",
                    oneLevel(),
                    ItemData.TOOL_ABILITY_FURNACE,
                    HELD,
                    ToolHarvestAbility::smelt);
    public static final ToolHarvestAbility AUTO_SHREDDER =
            new ToolHarvestAbility(
                    104,
                    "shredder",
                    "tool.ability.shredder",
                    oneLevel(),
                    ItemData.TOOL_ABILITY_SHREDDER,
                    HELD,
                    ToolHarvestAbility::shred);
    public static final ToolHarvestAbility AUTO_CENTRIFUGE =
            new ToolHarvestAbility(
                    105,
                    "centrifuge",
                    "tool.ability.centrifuge",
                    oneLevel(),
                    ItemData.TOOL_ABILITY_CENTRIFUGE,
                    HELD,
                    ToolHarvestAbility::centrifuge);
    public static final ToolHarvestAbility AUTO_CRYSTALLIZER =
            new ToolHarvestAbility(
                    106,
                    "crystallizer",
                    "tool.ability.crystallizer",
                    oneLevel(),
                    ItemData.TOOL_ABILITY_CRYSTALLIZER,
                    HELD,
                    ToolHarvestAbility::crystallize);
    public static final ToolHarvestAbility MERCURY_TOUCH =
            new ToolHarvestAbility(
                    107,
                    "mercury",
                    "tool.ability.mercury",
                    oneLevel(),
                    ItemData.TOOL_ABILITY_MERCURY,
                    HELD,
                    ToolHarvestAbility::mercury);

    private static final ToolHarvestAbility[] MEMBERS = {
        NONE,
        SILK_TOUCH,
        FORTUNE,
        AUTO_SMELTER,
        AUTO_SHREDDER,
        AUTO_CENTRIFUGE,
        AUTO_CRYSTALLIZER,
        MERCURY_TOUCH
    };

    public static final Codec<ToolHarvestAbility> CODEC =
            AbilityCodecs.byId(MEMBERS, ToolHarvestAbility::id);
    public static final StreamCodec<ByteBuf, ToolHarvestAbility> STREAM_CODEC =
            AbilityCodecs.byIndex(MEMBERS);

    private final String id;
    private final LootTool loot;
    private final Outcome outcome;

    private ToolHarvestAbility(
            int sortOrder,
            String id,
            String translationKey,
            String[] levelText,
            DataGroups.@Nullable Param<Boolean> gate,
            LootTool loot,
            Outcome outcome) {
        super(translationKey, sortOrder, levelText, gate);
        this.id = id;
        this.loot = loot;
        this.outcome = outcome;
    }

    public String id() {
        return id;
    }

    @Override
    int index() {
        return sortOrder() - BASE_SORT_ORDER;
    }

    public ItemStack lootTool(ToolDig dig, int level) {
        return loot.choose(dig, level);
    }

    public void harvestBlock(ToolDig dig, BlockPos pos, BlockState state) {
        outcome.apply(dig, pos, state);
    }

    private static ItemStack enchanted(ToolDig dig, ResourceKey<Enchantment> key, int power) {
        Holder<Enchantment> enchantment =
                dig.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemStack held = dig.held();
        int applied =
                held.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .getLevel(enchantment);
        ItemStack tool = held.copy();
        tool.enchant(enchantment, applied == power ? power + 1 : Math.max(applied, power));
        return tool;
    }

    private static void convert(ToolDig dig, BlockPos pos, ItemStack replacement) {
        if (ToolAbilityItem.harvest(dig, pos, true))
            ToolAbilityItem.dropAtReference(dig, replacement);
    }

    private static ItemStack probe(BlockState state) {
        return new ItemStack(state.getBlock());
    }

    private static void smelt(ToolDig dig, BlockPos pos, BlockState state) {
        ServerLevel level = dig.level();
        List<ItemStack> drops =
                Block.getDrops(
                        state,
                        level,
                        pos,
                        level.getBlockEntity(pos),
                        dig.player(),
                        dig.harvestTool());
        List<ItemStack> smelted = null;
        for (int i = 0, n = drops.size(); i < n; i++) {
            ItemStack drop = drops.get(i);
            var input = new SingleRecipeInput(drop);
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) continue;
            ItemStack result = recipe.get().value().assemble(input);
            result.setCount(result.getCount() * drop.getCount());
            if (smelted == null) smelted = new ArrayList<>(drops);
            smelted.set(i, result);
        }
        if (smelted == null) {
            ToolAbilityItem.harvest(dig, pos, false);
            return;
        }
        if (!ToolAbilityItem.harvest(dig, pos, true)) return;
        for (int i = 0, n = smelted.size(); i < n; i++)
            ToolAbilityItem.dropAtReference(dig, smelted.get(i));
    }

    private static void shred(ToolDig dig, BlockPos pos, BlockState state) {
        ShredderRecipe recipe = ShredderRecipes.getRecipe(probe(state));
        ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.output();
        if (output.isEmpty() || output.is(ModItems.SCRAP.get())) {
            ToolAbilityItem.harvest(dig, pos, false);
            return;
        }
        convert(dig, pos, output.copy());
    }

    private static void centrifuge(ToolDig dig, BlockPos pos, BlockState state) {
        ItemStack[] outputs = CentrifugeRecipes.INSTANCE.getOutputs(probe(state), dig.level());
        if (outputs == null) {
            ToolAbilityItem.harvest(dig, pos, false);
            return;
        }
        if (!ToolAbilityItem.harvest(dig, pos, true)) return;
        for (ItemStack output : outputs) ToolAbilityItem.dropAtReference(dig, output);
    }

    private static void crystallize(ToolDig dig, BlockPos pos, BlockState state) {
        CrystallizerRecipe recipe =
                CrystallizerRecipes.INSTANCE.getOutput(probe(state), NTMFluids.PEROXIDE);
        if (recipe == null) {
            ToolAbilityItem.harvest(dig, pos, false);
            return;
        }
        convert(dig, pos, recipe.output().copy());
    }

    private static void mercury(ToolDig dig, BlockPos pos, BlockState state) {
        RandomSource random = dig.player().getRandom();
        int nuggets =
                state.is(Blocks.REDSTONE_ORE)
                        ? 4 + random.nextInt(5)
                        : state.is(Blocks.REDSTONE_BLOCK) ? 8 + random.nextInt(7) : 0;
        if (nuggets == 0) {
            ToolAbilityItem.harvest(dig, pos, false);
            return;
        }
        convert(dig, pos, new ItemStack(ModItems.NUGGET_MERCURY.get(), nuggets));
    }

    @FunctionalInterface
    private interface LootTool {

        ItemStack choose(ToolDig dig, int level);
    }

    @FunctionalInterface
    private interface Outcome {

        void apply(ToolDig dig, BlockPos pos, BlockState state);
    }
}
