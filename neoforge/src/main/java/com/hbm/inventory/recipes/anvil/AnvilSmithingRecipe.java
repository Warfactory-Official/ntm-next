// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.food.FoodAdditives;
import com.hbm.items.special.ItemHot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AnvilSmithingRecipe extends GenericRecipe {

    private static final Codec<Kind> KIND_CODEC =
            Codec.STRING.xmap(
                    name -> Kind.valueOf(name.toUpperCase(Locale.ROOT)),
                    kind -> kind.name().toLowerCase(Locale.ROOT));
    private static final StreamCodec<ByteBuf, Kind> KIND_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> Kind.values()[id], Enum::ordinal);
    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .optionalFieldOf("tier", 1)
                                                    .forGetter(Extras::tier),
                                            KIND_CODEC
                                                    .optionalFieldOf("kind", Kind.PLAIN)
                                                    .forGetter(Extras::kind),
                                            Codec.BOOL
                                                    .optionalFieldOf("shapeless", false)
                                                    .forGetter(Extras::shapeless),
                                            Codec.STRING
                                                    .optionalFieldOf("shape")
                                                    .forGetter(Extras::shapeTag))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    Extras::tier,
                    KIND_STREAM_CODEC,
                    Extras::kind,
                    ByteBufCodecs.BOOL,
                    Extras::shapeless,
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
                    Extras::shapeTag,
                    Extras::new);
    private static final BiConsumer<AnvilSmithingRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.tier = extras.tier();
                recipe.kind = extras.kind();
                recipe.shapeless = extras.shapeless();
                recipe.shapeTag = extras.shapeTag().orElse(null);
            };
    private static final Function<AnvilSmithingRecipe, Extras> EXTRACT_EXTRAS =
            recipe ->
                    new Extras(
                            recipe.tier,
                            recipe.kind,
                            recipe.shapeless,
                            Optional.ofNullable(recipe.shapeTag));
    public static final MapCodec<AnvilSmithingRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    AnvilSmithingRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilSmithingRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    AnvilSmithingRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<AnvilSmithingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final Style RENAMED =
            Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE);
    public int tier;
    public Kind kind = Kind.PLAIN;
    public boolean shapeless;

    public String shapeTag;

    public AnvilSmithingRecipe(String name) {
        super(name);
    }

    public AnvilSmithingRecipe setTier(int tier) {
        this.tier = tier;
        return this;
    }

    public AnvilSmithingRecipe makeShapeless() {
        this.shapeless = true;
        return this;
    }

    public AnvilSmithingRecipe setKind(Kind kind) {
        this.kind = kind;
        return this;
    }

    public AnvilSmithingRecipe setShape(String shapeTag) {
        this.shapeTag = shapeTag;
        this.kind = Kind.MOLD;
        return this;
    }

    public CountIngredient left() {
        return inputItem[0];
    }

    public CountIngredient right() {
        return inputItem[1];
    }

    public boolean matches(ItemStack left, ItemStack right, long gameTime) {
        return matchesInt(left, right, gameTime) != -1;
    }

    public int matchesInt(ItemStack left, ItemStack right, long gameTime) {
        if (kind == Kind.POISON) {
            return !left.isEmpty()
                            && FoodAdditives.isFood(left)
                            && doesStackMatch(right, right(), gameTime)
                    ? 0
                    : -1;
        }
        if (kind == Kind.RENAME) {
            return doesStackMatch(right, right(), gameTime) && right.has(DataComponents.CUSTOM_NAME)
                    ? 0
                    : -1;
        }
        if (kind == Kind.MOLD) {
            if (!doesStackMatch(right, right(), gameTime)) return -1;

            if (left.getCount() != left().count()) return -1;
            return (shapeTag != null ? matchesShape(left) : left().matchesItem(left)) ? 0 : -1;
        }
        if (doesStackMatch(left, left(), gameTime) && doesStackMatch(right, right(), gameTime))
            return 0;
        if (shapeless) {
            return doesStackMatch(right, left(), gameTime)
                            && doesStackMatch(left, right(), gameTime)
                    ? 1
                    : -1;
        }
        return -1;
    }

    public boolean doesStackMatch(ItemStack input, CountIngredient recipe, long gameTime) {
        if (kind == Kind.HOT
                && input.getItem() instanceof ItemHot
                && ItemHot.getHeat(input, gameTime) < 0.5D) return false;
        return recipe.test(input);
    }

    public long expiresAt(ItemStack left, ItemStack right) {
        return kind == Kind.HOT ? Math.min(heatExpiry(left), heatExpiry(right)) : Long.MAX_VALUE;
    }

    private static long heatExpiry(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemHot)) return Long.MAX_VALUE;
        Long until = stack.get(ModDataComponents.HOT_UNTIL.get());
        return until == null ? Long.MAX_VALUE : until - (ItemHot.getMaxHeat(stack) + 1L) / 2 + 1;
    }

    private boolean matchesShape(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String prefix = shapeTag + "/";
        var tags = stack.getItem().builtInRegistryHolder().tags().iterator();
        while (tags.hasNext()) {
            Identifier id = tags.next().location();
            if (!id.getPath().startsWith(prefix)) continue;
            if ("c".equals(id.getNamespace()) || "hbm".equals(id.getNamespace())) return true;
        }
        return false;
    }

    public ItemStack getSimpleOutput() {
        if (outputItems() == null || outputItems().length == 0) return ItemStack.EMPTY;
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY).copy();
    }

    public ItemStack getOutput(ItemStack left, ItemStack right, long gameTime) {
        if (kind == Kind.POISON) {
            ItemStack out = left.copyWithCount(1);
            int additive =
                    right.is(ModItems.PILL_RED.get())
                            ? FoodAdditives.RED_PILL
                            : FoodAdditives.CYANIDE;
            out.set(
                    ModDataComponents.FOOD_ADDITIVES.get(),
                    out.getOrDefault(ModDataComponents.FOOD_ADDITIVES.get(), 0) | additive);
            return out;
        }
        if (kind == Kind.RENAME) {
            ItemStack out = left.copyWithCount(1);
            String name =
                    right.get(DataComponents.CUSTOM_NAME).getString().replace("\\&", "\u00a7");
            out.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(RENAMED));
            return out;
        }
        ItemStack out = getSimpleOutput();
        updateOutputHeat(out, left, right, gameTime);
        return out;
    }

    public void updateOutputHeat(ItemStack out, ItemStack left, ItemStack right, long gameTime) {
        if (kind == Kind.HOT
                && left.getItem() instanceof ItemHot
                && right.getItem() instanceof ItemHot
                && out.getItem() instanceof ItemHot) {
            ItemHot.heatUp(
                    out,
                    gameTime,
                    (ItemHot.getHeat(left, gameTime) + ItemHot.getHeat(right, gameTime)) / 2.0D);
        }
    }

    public int amountConsumed(int index, boolean mirrored) {
        if (kind == Kind.MOLD) return index == 1 ? right().count() : 0;

        if (kind == Kind.RENAME) return index == 0 ? left().count() : 0;
        if (index == 0) return mirrored ? right().count() : left().count();
        if (index == 1) return mirrored ? left().count() : right().count();
        return 0;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return AnvilSmithingRecipes.INSTANCE;
    }

    public enum Kind {
        PLAIN,

        MOLD,

        HOT,
        POISON,

        RENAME
    }

    private record Extras(int tier, Kind kind, boolean shapeless, Optional<String> shapeTag) {}
}
