// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.lib.Library;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record HbmNarrowedIngredient(Holder<Item> item, Ingredient within)
        implements CustomIngredient {

    public static final Identifier ID = Library.id("narrowed");

    private static final MapCodec<HbmNarrowedIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Item.CODEC
                                                    .fieldOf("item")
                                                    .forGetter(HbmNarrowedIngredient::item),
                                            Ingredient.CODEC
                                                    .fieldOf("within")
                                                    .forGetter(HbmNarrowedIngredient::within))
                                    .apply(i, HbmNarrowedIngredient::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, HbmNarrowedIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    Item.STREAM_CODEC,
                    HbmNarrowedIngredient::item,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    HbmNarrowedIngredient::within,
                    HbmNarrowedIngredient::new);

    public static Ingredient of(Holder<Item> item, Ingredient within) {
        return new HbmNarrowedIngredient(item, within).toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(item) && within.test(stack);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(item);
    }

    @Override
    public SlotDisplay display() {
        List<SlotDisplay> shown =
                CountIngredient.displayStacks(within, 1).stream()
                        .filter(stack -> stack.is(item))
                        .<SlotDisplay>map(
                                stack ->
                                        new SlotDisplay.ItemStackSlotDisplay(
                                                ItemStackTemplate.fromNonEmptyStack(stack)))
                        .toList();
        return new SlotDisplay.Composite(shown);
    }

    public static final CustomIngredientSerializer<HbmNarrowedIngredient> SERIALIZER =
            new Serializer();

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer
            implements CustomIngredientSerializer<HbmNarrowedIngredient> {

        @Override
        public Identifier getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<HbmNarrowedIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmNarrowedIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    }
}
