// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.util.I18nUtil;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CrucibleRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            MaterialStack.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("input_materials", List.of())
                                                    .forGetter(Extras::input),
                                            MaterialStack.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("output_materials", List.of())
                                                    .forGetter(Extras::output),
                                            ExtraCodecs.POSITIVE_INT
                                                    .optionalFieldOf("frequency", 1)
                                                    .forGetter(Extras::frequency))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    MaterialStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    Extras::input,
                    MaterialStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    Extras::output,
                    ByteBufCodecs.VAR_INT,
                    Extras::frequency,
                    Extras::new);
    private static final BiConsumer<CrucibleRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.input = extras.input().toArray(MaterialStack[]::new);
                recipe.output = extras.output().toArray(MaterialStack[]::new);
                recipe.frequency = extras.frequency();
            };
    private static final Function<CrucibleRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(nullSafe(recipe.input), nullSafe(recipe.output), recipe.frequency);
    public static final MapCodec<CrucibleRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    CrucibleRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    CrucibleRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<CrucibleRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public MaterialStack[] input;
    public MaterialStack[] output;
    public int frequency = 1;

    public CrucibleRecipe(String name) {
        super(name);
    }

    private static List<MaterialStack> nullSafe(MaterialStack[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    public CrucibleRecipe setup(int frequency, ItemStackTemplate icon) {
        this.frequency = frequency;
        this.setIcon(icon);
        return this;
    }

    public CrucibleRecipe inputs(MaterialStack... input) {
        this.input = input;
        return this;
    }

    public CrucibleRecipe outputs(MaterialStack... output) {
        this.output = output;
        return this;
    }

    public int getInputAmount() {
        int content = 0;
        for (MaterialStack stack : input) content += stack.amount;
        return content;
    }

    @Override
    public List<String> print() {
        List<String> list = new ArrayList<>();
        header(list);
        input(list);
        output(list);
        return list;
    }

    @Override
    protected void input(List<String> list) {
        list.add(ChatFormatting.BOLD + I18nUtil.resolveKey("gui.recipe.input") + ":");
        boolean shift = ModifierKeys.leftShiftHeld();
        for (MaterialStack stack : input) {
            list.add(
                    stack.material.getLocalizedName()
                            + ": "
                            + Mats.formatAmount(stack.amount, shift));
        }
    }

    @Override
    protected void output(List<String> list) {
        list.add(ChatFormatting.BOLD + I18nUtil.resolveKey("gui.recipe.output") + ":");
        boolean shift = ModifierKeys.leftShiftHeld();
        for (MaterialStack stack : output) {
            list.add(
                    stack.material.getLocalizedName()
                            + ": "
                            + Mats.formatAmount(stack.amount, shift));
        }
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CrucibleRecipes.INSTANCE;
    }

    private record Extras(List<MaterialStack> input, List<MaterialStack> output, int frequency) {}
}
