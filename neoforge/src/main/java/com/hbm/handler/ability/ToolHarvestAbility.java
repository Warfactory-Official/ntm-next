// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.ability;

import com.hbm.data.ItemData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum ToolHarvestAbility implements BaseAbility, StringRepresentable {
    NONE("", 0),
    SILK("tool.ability.silktouch", 1) {
        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_SILK.get();
        }

        @Override
        public ItemStack harvestTool(ToolDig dig, int abilityLevel) {
            return enchanted(dig, Enchantments.SILK_TOUCH, 1);
        }
    },
    LUCK("tool.ability.luck", 2) {
        private static final int[] POWER = {1, 2, 3, 4, 5, 9};

        @Override
        public int levels() {
            return POWER.length;
        }

        @Override
        public String extension(int level) {
            return " (" + POWER[level] + ")";
        }

        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_LUCK.get();
        }

        @Override
        public ItemStack harvestTool(ToolDig dig, int abilityLevel) {
            return enchanted(dig, Enchantments.FORTUNE, POWER[abilityLevel]);
        }
    },
    SMELTER("tool.ability.smelter", 3) {
        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_FURNACE.get();
        }

        @Override
        public void onHarvestBlock(ToolDig dig, int abilityLevel, BlockPos pos, BlockState state) {
            ServerLevel level = dig.level();
            List<ItemStack> drops =
                    new ArrayList<>(
                            Block.getDrops(
                                    state,
                                    level,
                                    pos,
                                    level.getBlockEntity(pos),
                                    dig.player(),
                                    dig.harvestTool()));

            boolean smelts = false;
            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);
                SingleRecipeInput input = new SingleRecipeInput(drop);
                ItemStack result =
                        level.recipeAccess()
                                .getRecipeFor(RecipeType.SMELTING, input, level)
                                .map(holder -> holder.value().assemble(input))
                                .orElse(ItemStack.EMPTY);
                if (result.isEmpty()) continue;

                result = result.copy();
                result.setCount(result.getCount() * drop.getCount());
                drops.set(i, result);
                smelts = true;
            }

            ItemToolAbility.harvest(dig, pos, smelts);
            if (!smelts) return;

            for (ItemStack drop : drops) ItemToolAbility.dropAtReference(dig, drop.copy());
        }
    },
    SHREDDER("tool.ability.shredder", 4) {
        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_SHREDDER.get();
        }

        @Override
        public void onHarvestBlock(ToolDig dig, int abilityLevel, BlockPos pos, BlockState state) {
            ItemStack result = ShredderRecipes.getShredderResult(new ItemStack(state.getBlock()));
            boolean shreds = !result.isEmpty() && !result.is(ModItems.SCRAP.get());

            ItemToolAbility.harvest(dig, pos, shreds);
            if (shreds) ItemToolAbility.dropAtReference(dig, result.copy());
        }
    },
    CENTRIFUGE("tool.ability.centrifuge", 5) {
        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_CENTRIFUGE.get();
        }

        @Override
        public void onHarvestBlock(ToolDig dig, int abilityLevel, BlockPos pos, BlockState state) {
            ItemStack[] results =
                    CentrifugeRecipes.INSTANCE.getOutputs(
                            new ItemStack(state.getBlock()), dig.level());

            ItemToolAbility.harvest(dig, pos, results != null);
            if (results == null) return;

            for (ItemStack result : results) {
                if (result != null && !result.isEmpty())
                    ItemToolAbility.dropAtReference(dig, result.copy());
            }
        }
    },
    CRYSTALLIZER("tool.ability.crystallizer", 6) {
        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_CRYSTALLIZER.get();
        }

        @Override
        public void onHarvestBlock(ToolDig dig, int abilityLevel, BlockPos pos, BlockState state) {
            CrystallizerRecipe recipe =
                    CrystallizerRecipes.INSTANCE.getOutput(
                            new ItemStack(state.getBlock()), NTMFluids.PEROXIDE);

            ItemToolAbility.harvest(dig, pos, recipe != null);
            if (recipe != null) ItemToolAbility.dropAtReference(dig, recipe.output().copy());
        }
    },
    MERCURY("tool.ability.mercury", 7) {
        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_MERCURY.get();
        }

        @Override
        public void onHarvestBlock(ToolDig dig, int abilityLevel, BlockPos pos, BlockState state) {
            int mercury = 0;
            if (state.is(Blocks.REDSTONE_ORE)) mercury = dig.player().getRandom().nextInt(5) + 4;
            if (state.is(Blocks.REDSTONE_BLOCK)) mercury = dig.player().getRandom().nextInt(7) + 8;

            ItemToolAbility.harvest(dig, pos, mercury > 0);

            if (mercury > 0) {
                ItemToolAbility.dropAtReference(
                        dig, new ItemStack(ModItems.NUGGET_MERCURY.get(), mercury));
            }
        }
    };

    public static final ToolHarvestAbility[] VALUES = values();
    public static final Codec<ToolHarvestAbility> CODEC =
            StringRepresentable.fromEnum(ToolHarvestAbility::values);
    public static final StreamCodec<ByteBuf, ToolHarvestAbility> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> VALUES[id], Enum::ordinal);

    private static final int SORT_ORDER_BASE = 100;

    private final String translationKey;
    private final int sortOrder;

    ToolHarvestAbility(String translationKey, int order) {
        this.translationKey = translationKey;
        this.sortOrder = SORT_ORDER_BASE + order;
    }

    private static ItemStack enchanted(
            ToolDig dig, ResourceKey<Enchantment> enchantment, int power) {
        ItemStack tool = dig.held().copy();
        tool.update(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY,
                existing -> {
                    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
                    mutable.set(
                            dig.level()
                                    .registryAccess()
                                    .lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(enchantment),
                            power);
                    return mutable.toImmutable();
                });
        return tool;
    }

    public ItemStack harvestTool(ToolDig dig, int abilityLevel) {
        return dig.held();
    }

    public void onHarvestBlock(ToolDig dig, int abilityLevel, BlockPos pos, BlockState state) {
        ItemToolAbility.harvest(dig, pos, false);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String translationKey() {
        return translationKey;
    }

    @Override
    public int sortOrder() {
        return sortOrder;
    }
}
