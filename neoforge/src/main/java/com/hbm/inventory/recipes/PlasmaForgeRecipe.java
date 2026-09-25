// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PlasmaForgeRecipe extends GenericRecipe {

    private static final MapCodec<Long> IGNITION_MAP_CODEC =
            Codec.LONG.optionalFieldOf("ignition_temp", 0L);
    private static final StreamCodec<RegistryFriendlyByteBuf, Long> IGNITION_STREAM_CODEC =
            ByteBufCodecs.VAR_LONG.cast();
    private static final BiConsumer<PlasmaForgeRecipe, Long> APPLY_IGNITION =
            (recipe, ignitionTemp) -> recipe.ignitionTemp = ignitionTemp;
    private static final Function<PlasmaForgeRecipe, Long> EXTRACT_IGNITION =
            recipe -> recipe.ignitionTemp;
    public static final MapCodec<PlasmaForgeRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    PlasmaForgeRecipe::new, IGNITION_MAP_CODEC, APPLY_IGNITION, EXTRACT_IGNITION);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlasmaForgeRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    PlasmaForgeRecipe::new,
                    IGNITION_STREAM_CODEC,
                    APPLY_IGNITION,
                    EXTRACT_IGNITION);
    public static final RecipeSerializer<PlasmaForgeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public long ignitionTemp;

    public PlasmaForgeRecipe(String name) {
        super(name);
    }

    public PlasmaForgeRecipe setInputEnergy(long ignitionTemp) {
        this.ignitionTemp = ignitionTemp;
        return this;
    }

    @Override
    public List<String> print() {
        List<String> list = new ArrayList<>();

        header(list);
        autoSwitch(list);
        duration(list);
        power(list);
        list.add(
                ChatFormatting.LIGHT_PURPLE
                        + I18nUtil.resolveKey("gui.recipe.plasmaIn")
                        + ": "
                        + BobMathUtil.getShortNumber(ignitionTemp)
                        + "TU/t");
        input(list);
        output(list);

        return list;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PlasmaForgeRecipes.INSTANCE;
    }
}
