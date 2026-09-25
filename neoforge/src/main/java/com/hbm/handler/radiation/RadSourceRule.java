// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.hazard.HazardSystem;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record RadSourceRule(
        HolderSet<Block> targets, Optional<StatePropertiesPredicate> state, Rate rate) {

    public static final ResourceKey<Registry<RadSourceRule>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("rad_source"));

    public static final Codec<RadSourceRule> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            RegistryCodecs.homogeneousList(Registries.BLOCK)
                                                    .fieldOf("targets")
                                                    .forGetter(RadSourceRule::targets),
                                            StatePropertiesPredicate.CODEC
                                                    .optionalFieldOf("state")
                                                    .forGetter(RadSourceRule::state),
                                            Rate.CODEC
                                                    .fieldOf("rate")
                                                    .forGetter(RadSourceRule::rate))
                                    .apply(instance, RadSourceRule::new));

    public boolean matches(BlockState candidate) {
        return state.map(p -> p.matches(candidate)).orElse(true);
    }

    public sealed interface Rate {

        Map<String, MapCodec<? extends Rate>> TYPES =
                Map.of(
                        "fixed", Fixed.MAP_CODEC,
                        "item_hazard", ItemHazard.MAP_CODEC);

        Codec<Rate> CODEC =
                Codec.STRING
                        .comapFlatMap(
                                name ->
                                        TYPES.containsKey(name)
                                                ? DataResult.success(name)
                                                : DataResult.error(
                                                        () ->
                                                                "unknown rad source rate "
                                                                        + name
                                                                        + ", known: "
                                                                        + TYPES.keySet()),
                                name -> name)
                        .dispatch("type", Rate::typeName, TYPES::get);

        String typeName();

        double emission(Block block);

        double saturation(Block block);

        record Fixed(double emission, double saturation) implements Rate {

            static final MapCodec<Fixed> MAP_CODEC =
                    RecordCodecBuilder.mapCodec(
                            instance ->
                                    instance.group(
                                                    Codec.DOUBLE
                                                            .fieldOf("emission")
                                                            .forGetter(Fixed::emission),
                                                    Codec.DOUBLE
                                                            .optionalFieldOf("saturation", 0.0D)
                                                            .forGetter(Fixed::saturation))
                                            .apply(instance, Fixed::new));

            @Override
            public String typeName() {
                return "fixed";
            }

            @Override
            public double emission(Block block) {
                return emission;
            }

            @Override
            public double saturation(Block block) {
                return saturation;
            }
        }

        record ItemHazard() implements Rate {

            static final MapCodec<ItemHazard> MAP_CODEC = MapCodec.unit(ItemHazard::new);

            @Override
            public String typeName() {
                return "item_hazard";
            }

            @Override
            public double emission(Block block) {
                return rawRads(block) * 0.1F / 20.0D;
            }

            @Override
            public double saturation(Block block) {
                return rawRads(block);
            }

            private static float rawRads(Block block) {
                return (float) HazardSystem.getRawRadsFromBlock(block);
            }
        }
    }
}
