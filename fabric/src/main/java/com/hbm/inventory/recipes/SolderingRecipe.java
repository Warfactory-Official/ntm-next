// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class SolderingRecipe extends GenericRecipe {

    private static final CountIngredient[] EMPTY_GROUP = new CountIngredient[0];
    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            CountIngredient.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("pcb", List.of())
                                                    .forGetter(Extras::pcb),
                                            CountIngredient.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("solder", List.of())
                                                    .forGetter(Extras::solder))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    CountIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    Extras::pcb,
                    CountIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    Extras::solder,
                    Extras::new);
    private static final BiConsumer<SolderingRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.pcb = extras.pcb().toArray(CountIngredient[]::new);
                recipe.solder = extras.solder().toArray(CountIngredient[]::new);
            };
    private static final Function<SolderingRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(Arrays.asList(recipe.pcb), Arrays.asList(recipe.solder));
    public static final MapCodec<SolderingRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    SolderingRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, SolderingRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    SolderingRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<SolderingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public CountIngredient[] pcb = EMPTY_GROUP;
    public CountIngredient[] solder = EMPTY_GROUP;

    public SolderingRecipe(String name) {
        super(name);
    }

    public SolderingRecipe setPcb(CountIngredient... pcb) {
        this.pcb = pcb;
        return this;
    }

    public SolderingRecipe setSolder(CountIngredient... solder) {
        this.solder = solder;
        return this;
    }

    public CountIngredient[] toppings() {
        return inputItem == null ? EMPTY_GROUP : inputItem;
    }

    public CountIngredient[] pcb() {
        return pcb;
    }

    public CountIngredient[] solder() {
        return solder;
    }

    public int duration() {
        return duration;
    }

    public long consumption() {
        return power;
    }

    public @Nullable FluidStackNTM fluid() {
        return inputFluid.length == 0 ? null : inputFluid[0];
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    public Item previewItem() {
        return outputItems()[0].unwrap().getFirst().value().getItem();
    }

    @Override
    protected void input(List<String> list) {
        super.input(list);
        for (CountIngredient[] group : new CountIngredient[][] {pcb, solder}) {
            for (CountIngredient ingredient : group) {
                ItemStack display = ingredient.extractForCyclingDisplay(20);
                if (display.isEmpty()) continue;
                list.add(
                        "  "
                                + ChatFormatting.GRAY
                                + display.getCount()
                                + "x "
                                + display.getHoverName().getString());
            }
        }
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return SolderingRecipes.INSTANCE;
    }

    private record Extras(List<CountIngredient> pcb, List<CountIngredient> solder) {}
}
