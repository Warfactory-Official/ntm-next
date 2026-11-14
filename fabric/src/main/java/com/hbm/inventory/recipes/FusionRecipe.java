/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.util.NumberFormatter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class FusionRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("ignition_temp", 0L)
                                                    .forGetter(Extras::ignitionTemp),
                                            Codec.LONG
                                                    .optionalFieldOf("output_temp", 0L)
                                                    .forGetter(Extras::outputTemp),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("neutron_flux", 0D)
                                                    .forGetter(Extras::neutronFlux),
                                            Codec.FLOAT
                                                    .optionalFieldOf("red", 1F)
                                                    .forGetter(Extras::r),
                                            Codec.FLOAT
                                                    .optionalFieldOf("green", 0.2F)
                                                    .forGetter(Extras::g),
                                            Codec.FLOAT
                                                    .optionalFieldOf("blue", 0.6F)
                                                    .forGetter(Extras::b))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.of(
                    (buf, extras) -> {
                        buf.writeVarLong(extras.ignitionTemp());
                        buf.writeVarLong(extras.outputTemp());
                        buf.writeDouble(extras.neutronFlux());
                        buf.writeFloat(extras.r());
                        buf.writeFloat(extras.g());
                        buf.writeFloat(extras.b());
                    },
                    buf ->
                            new Extras(
                                    buf.readVarLong(),
                                    buf.readVarLong(),
                                    buf.readDouble(),
                                    buf.readFloat(),
                                    buf.readFloat(),
                                    buf.readFloat()));
    private static final BiConsumer<FusionRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.ignitionTemp = extras.ignitionTemp();
                recipe.outputTemp = extras.outputTemp();
                recipe.neutronFlux = extras.neutronFlux();
                recipe.r = extras.r();
                recipe.g = extras.g();
                recipe.b = extras.b();
            };
    private static final Function<FusionRecipe, Extras> EXTRACT_EXTRAS =
            recipe ->
                    new Extras(
                            recipe.ignitionTemp,
                            recipe.outputTemp,
                            recipe.neutronFlux,
                            recipe.r,
                            recipe.g,
                            recipe.b);
    public static final MapCodec<FusionRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    FusionRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    FusionRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<FusionRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public long ignitionTemp;
    public long outputTemp;
    public double neutronFlux;
    public float r = 1F;
    public float g = 0.2F;
    public float b = 0.6F;

    public FusionRecipe(String name) {
        super(name);
    }

    public FusionRecipe setInputEnergy(long ignitionTemp) {
        this.ignitionTemp = ignitionTemp;
        return this;
    }

    public FusionRecipe setOutputEnergy(long outputTemp) {
        this.outputTemp = outputTemp;
        return this;
    }

    public FusionRecipe setOutputFlux(double neutronFlux) {
        this.neutronFlux = neutronFlux;
        return this;
    }

    public FusionRecipe setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    @Override
    public List<Component> print() {
        List<Component> list = new ArrayList<>();
        list.add(getLocalizedName().copy().withStyle(ChatFormatting.YELLOW));

        duration(list);
        power(list);
        list.add(
                Component.translatable("gui.recipe.fusionIn")
                        .append(": ")
                        .append(
                                Component.translatable(
                                        "unit.ignition_per_tick",
                                        NumberFormatter.formatCompact(ignitionTemp)))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        list.add(
                Component.translatable("gui.recipe.fusionOut")
                        .append(": ")
                        .append(
                                Component.translatable(
                                        "unit.heat_per_tick",
                                        NumberFormatter.formatCompact(outputTemp)))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        list.add(
                Component.translatable("gui.recipe.fusionFlux")
                        .append(": ")
                        .append(
                                Component.translatable(
                                        "unit.flux_per_tick", ((int) (neutronFlux * 10)) / 10D))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        input(list);
        output(list);

        return list;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return FusionRecipes.INSTANCE;
    }

    private record Extras(
            long ignitionTemp, long outputTemp, double neutronFlux, float r, float g, float b) {}
}
