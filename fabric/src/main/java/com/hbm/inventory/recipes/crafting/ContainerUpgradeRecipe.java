// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class ContainerUpgradeRecipe extends NormalCraftingRecipe {

    public static final MapCodec<ContainerUpgradeRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Recipe.CommonInfo.MAP_CODEC.forGetter(
                                                    o -> o.commonInfo),
                                            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(
                                                    o -> o.bookInfo),
                                            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                                            ItemStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(o -> o.result),
                                            Ingredient.CODEC
                                                    .fieldOf("carrier")
                                                    .forGetter(o -> o.carrier))
                                    .apply(i, ContainerUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC,
                    o -> o.commonInfo,
                    CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
                    o -> o.bookInfo,
                    ShapedRecipePattern.STREAM_CODEC,
                    o -> o.pattern,
                    ItemStackTemplate.STREAM_CODEC,
                    o -> o.result,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    o -> o.carrier,
                    ContainerUpgradeRecipe::new);
    public static final RecipeSerializer<ContainerUpgradeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;
    private final Ingredient carrier;

    public ContainerUpgradeRecipe(
            Recipe.CommonInfo commonInfo,
            CraftingRecipe.CraftingBookInfo bookInfo,
            ShapedRecipePattern pattern,
            ItemStackTemplate result,
            Ingredient carrier) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
        this.carrier = carrier;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && carrier.test(stack)) {
                return result.apply(result.count(), stack.getComponentsPatch());
            }
        }
        assert false : "carrier ingredient absent from a matched grid";
        return result.create();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(pattern.ingredients());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
                new ShapedCraftingRecipeDisplay(
                        pattern.width(),
                        pattern.height(),
                        pattern.ingredients().stream()
                                .map(
                                        e ->
                                                e.map(Ingredient::display)
                                                        .orElse(SlotDisplay.Empty.INSTANCE))
                                .toList(),
                        new SlotDisplay.ItemStackSlotDisplay(result),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<ContainerUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
