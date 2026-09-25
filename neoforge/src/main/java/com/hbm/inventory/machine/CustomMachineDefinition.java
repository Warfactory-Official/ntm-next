// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.machine;

import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public record CustomMachineDefinition(
        String recipeKey,
        Component name,
        int fluidInCount,
        int fluidInCap,
        int itemInCount,
        int fluidOutCount,
        int fluidOutCap,
        int itemOutCount,
        boolean generatorMode,
        int maxPollutionCap,
        boolean fluxMode,
        double recipeSpeedMult,
        double recipeConsumptionMult,
        long maxPower,
        int maxHeat,
        List<Cell> cells) {

    public static final int MAX_FLUID_SLOTS = 3;
    public static final int MAX_ITEM_SLOTS = 6;

    public static final ResourceKey<Registry<CustomMachineDefinition>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("custom_machine"));

    public record Cell(HolderSet<Block> blocks, int x, int y, int z) {

        public static final Codec<Cell> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                RegistryCodecs.homogeneousList(Registries.BLOCK)
                                                        .fieldOf("blocks")
                                                        .forGetter(Cell::blocks),
                                                Codec.INT.fieldOf("x").forGetter(Cell::x),
                                                Codec.INT.fieldOf("y").forGetter(Cell::y),
                                                Codec.INT.fieldOf("z").forGetter(Cell::z))
                                        .apply(i, Cell::new));
    }

    public static final Codec<CustomMachineDefinition> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .fieldOf("recipe_key")
                                                    .forGetter(CustomMachineDefinition::recipeKey),
                                            ComponentSerialization.CODEC
                                                    .fieldOf("name")
                                                    .forGetter(CustomMachineDefinition::name),
                                            Codec.intRange(0, MAX_FLUID_SLOTS)
                                                    .optionalFieldOf("fluid_in_count", 0)
                                                    .forGetter(
                                                            CustomMachineDefinition::fluidInCount),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .optionalFieldOf("fluid_in_cap", 0)
                                                    .forGetter(CustomMachineDefinition::fluidInCap),
                                            Codec.intRange(0, MAX_ITEM_SLOTS)
                                                    .optionalFieldOf("item_in_count", 0)
                                                    .forGetter(
                                                            CustomMachineDefinition::itemInCount),
                                            Codec.intRange(0, MAX_FLUID_SLOTS)
                                                    .optionalFieldOf("fluid_out_count", 0)
                                                    .forGetter(
                                                            CustomMachineDefinition::fluidOutCount),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .optionalFieldOf("fluid_out_cap", 0)
                                                    .forGetter(
                                                            CustomMachineDefinition::fluidOutCap),
                                            Codec.intRange(0, MAX_ITEM_SLOTS)
                                                    .optionalFieldOf("item_out_count", 0)
                                                    .forGetter(
                                                            CustomMachineDefinition::itemOutCount),
                                            Codec.BOOL
                                                    .optionalFieldOf("generator_mode", false)
                                                    .forGetter(
                                                            CustomMachineDefinition::generatorMode),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .optionalFieldOf("max_pollution_cap", 0)
                                                    .forGetter(
                                                            CustomMachineDefinition
                                                                    ::maxPollutionCap),
                                            Codec.BOOL
                                                    .optionalFieldOf("flux_mode", false)
                                                    .forGetter(CustomMachineDefinition::fluxMode),
                                            Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE)
                                                    .optionalFieldOf("recipe_speed_mult", 1.0D)
                                                    .forGetter(
                                                            CustomMachineDefinition
                                                                    ::recipeSpeedMult),
                                            Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE)
                                                    .optionalFieldOf(
                                                            "recipe_consumption_mult", 1.0D)
                                                    .forGetter(
                                                            CustomMachineDefinition
                                                                    ::recipeConsumptionMult),
                                            Codec.LONG
                                                    .optionalFieldOf("max_power", 0L)
                                                    .forGetter(CustomMachineDefinition::maxPower),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .optionalFieldOf("max_heat", 0)
                                                    .forGetter(CustomMachineDefinition::maxHeat),
                                            Cell.CODEC
                                                    .listOf()
                                                    .fieldOf("cells")
                                                    .forGetter(CustomMachineDefinition::cells))
                                    .apply(i, CustomMachineDefinition::new));
}
