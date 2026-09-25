// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public final class TagResultRecipe extends NormalCraftingRecipe {

    public static final MapCodec<TagResultRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Recipe.CommonInfo.MAP_CODEC.forGetter(
                                                    r -> r.commonInfo),
                                            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(
                                                    r -> r.bookInfo),
                                            Result.CODEC.fieldOf("result").forGetter(r -> r.result),
                                            Ingredient.CODEC
                                                    .listOf(1, 9)
                                                    .fieldOf("ingredients")
                                                    .forGetter(r -> r.ingredients))
                                    .apply(i, TagResultRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TagResultRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC,
                    r -> r.commonInfo,
                    CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
                    r -> r.bookInfo,
                    Result.STREAM_CODEC,
                    r -> r.result,
                    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    r -> r.ingredients,
                    TagResultRecipe::new);
    public static final RecipeSerializer<TagResultRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Result result;
    private final List<Ingredient> ingredients;

    public TagResultRecipe(
            Recipe.CommonInfo commonInfo,
            CraftingRecipe.CraftingBookInfo bookInfo,
            Result result,
            List<Ingredient> ingredients) {
        super(commonInfo, bookInfo);
        this.result = result;
        this.ingredients = ingredients;
    }

    public Result result() {
        return result;
    }

    public Optional<Holder<Item>> resolve() {
        Iterator<Holder<Item>> members =
                BuiltInRegistries.ITEM.getTagOrEmpty(result.tag()).iterator();
        return members.hasNext() ? Optional.of(members.next()) : Optional.empty();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size() || resolve().isEmpty()) return false;

        return input.size() == 1 && ingredients.size() == 1
                ? ingredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return resolve().map(item -> new ItemStack(item, result.count())).orElse(ItemStack.EMPTY);
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(ingredients);
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay output =
                resolve()
                        .<SlotDisplay>map(
                                item ->
                                        new SlotDisplay.ItemStackSlotDisplay(
                                                new ItemStackTemplate(
                                                        item,
                                                        result.count(),
                                                        DataComponentPatch.EMPTY)))
                        .orElse(SlotDisplay.Empty.INSTANCE);
        return List.of(
                new ShapelessCraftingRecipeDisplay(
                        ingredients.stream().map(Ingredient::display).toList(),
                        output,
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<TagResultRecipe> getSerializer() {
        return SERIALIZER;
    }

    public record Result(TagKey<Item> tag, int count) {
        static final Codec<Result> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                TagKey.codec(Registries.ITEM)
                                                        .fieldOf("tag")
                                                        .forGetter(Result::tag),
                                                ExtraCodecs.POSITIVE_INT
                                                        .optionalFieldOf("count", 1)
                                                        .forGetter(Result::count))
                                        .apply(i, Result::new));
        static final StreamCodec<ByteBuf, Result> STREAM_CODEC =
                StreamCodec.composite(
                        TagKey.streamCodec(Registries.ITEM),
                        Result::tag,
                        ByteBufCodecs.VAR_INT,
                        Result::count,
                        Result::new);
    }
}
