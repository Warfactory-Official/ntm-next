/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.config;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.SellafieldSlakedBlock;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public record FalloutConfigJSON(List<FalloutEntry> rules) {

    public static final ResourceKey<Registry<FalloutConfigJSON>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("fallout_table"));
    public static final ResourceKey<FalloutConfigJSON> DEFAULT =
            ResourceKey.create(REGISTRY, RegistryUtil.id("default"));

    public static final Codec<FalloutConfigJSON> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            FalloutEntry.CODEC
                                                    .listOf()
                                                    .fieldOf("rules")
                                                    .forGetter(FalloutConfigJSON::rules))
                                    .apply(instance, FalloutConfigJSON::new));

    public static List<FalloutEntry> entries(RegistryAccess registries) {
        return registries.lookupOrThrow(REGISTRY).getValueOrThrow(DEFAULT).rules();
    }

    public record WeightedState(BlockState state, int weight) {

        public static final Codec<WeightedState> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                BlockState.CODEC
                                                        .fieldOf("state")
                                                        .forGetter(WeightedState::state),
                                                ExtraCodecs.POSITIVE_INT
                                                        .fieldOf("weight")
                                                        .forGetter(WeightedState::weight))
                                        .apply(instance, WeightedState::new));
    }

    public record FalloutEntry(
            Optional<HolderSet<Block>> targets,
            Optional<StatePropertiesPredicate> state,
            boolean mustBeOpaque,
            boolean restrictDepth,
            List<WeightedState> primary,
            List<WeightedState> secondary,
            double chance,
            double minDist,
            double maxDist,
            double falloffStart,
            List<String> preserve) {

        public static final Codec<FalloutEntry> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                RegistryCodecs.<Block>holderSet(
                                                                (ResourceKey<
                                                                                ? extends
                                                                                        Registry<
                                                                                                Block>>)
                                                                        Registries.BLOCK)
                                                        .optionalFieldOf("targets")
                                                        .forGetter(FalloutEntry::targets),
                                                StatePropertiesPredicate.CODEC
                                                        .optionalFieldOf("state")
                                                        .forGetter(FalloutEntry::state),
                                                Codec.BOOL
                                                        .optionalFieldOf("must_be_opaque", false)
                                                        .forGetter(FalloutEntry::mustBeOpaque),
                                                Codec.BOOL
                                                        .optionalFieldOf("restrict_depth", false)
                                                        .forGetter(FalloutEntry::restrictDepth),
                                                WeightedState.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("primary", List.of())
                                                        .forGetter(FalloutEntry::primary),
                                                WeightedState.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("secondary", List.of())
                                                        .forGetter(FalloutEntry::secondary),
                                                Codec.DOUBLE
                                                        .optionalFieldOf("chance", 1.0D)
                                                        .forGetter(FalloutEntry::chance),
                                                Codec.DOUBLE
                                                        .optionalFieldOf(
                                                                "min_distance_percent", 0.0D)
                                                        .forGetter(FalloutEntry::minDist),
                                                Codec.DOUBLE
                                                        .optionalFieldOf(
                                                                "max_distance_percent", 100.0D)
                                                        .forGetter(FalloutEntry::maxDist),
                                                Codec.DOUBLE
                                                        .optionalFieldOf("falloff_start", 0.9D)
                                                        .forGetter(FalloutEntry::falloffStart),
                                                Codec.STRING
                                                        .listOf()
                                                        .optionalFieldOf("preserve", List.of())
                                                        .forGetter(FalloutEntry::preserve))
                                        .apply(instance, FalloutEntry::new));

        public @Nullable BlockState eval(
                ServerLevel level,
                BlockPos pos,
                BlockState current,
                double dist,
                RandomSource random) {
            if (dist > maxDist || dist < minDist) return null;
            if (targets.isPresent() && !current.is(targets.get())) return null;
            if (state.isPresent() && !state.get().matches(current)) return null;
            if (mustBeOpaque && !current.canOcclude()) return null;
            if (dist > maxDist * falloffStart
                    && Math.abs(random.nextGaussian())
                            < Math.pow(
                                            (dist - maxDist * falloffStart)
                                                    / (maxDist - maxDist * falloffStart),
                                            2D)
                                    * 3D) {
                return null;
            }

            WeightedState conversion =
                    chooseRandomOutcome(
                            chance == 1D || random.nextDouble() < chance ? primary : secondary,
                            random);
            if (conversion == null) return null;

            BlockState target = conversion.state();
            Block slaked = ModBlocks.SELLAFIELD_SLAKED.get();
            Block bedrock = ModBlocks.SELLAFIELD_BEDROCK.get();
            if (target.is(slaked) && current.is(slaked) && shade(target) <= shade(current))
                return null;
            if (target.is(bedrock) && current.is(bedrock) && shade(target) <= shade(current))
                return null;
            if (current.is(bedrock) && !target.is(bedrock)) return null;
            if (pos.getY() == level.getMinY() && !target.is(bedrock)) return null;

            for (String name : preserve) {
                Property<?> property = current.getBlock().getStateDefinition().getProperty(name);
                if (property != null && target.hasProperty(property))
                    target = copy(current, target, property);
            }
            return target;
        }

        private static int shade(BlockState state) {
            return state.hasProperty(SellafieldSlakedBlock.SHADE)
                    ? state.getValue(SellafieldSlakedBlock.SHADE)
                    : 0;
        }

        private static <T extends Comparable<T>> BlockState copy(
                BlockState from, BlockState to, Property<T> p) {
            return to.setValue(p, from.getValue(p));
        }

        private static @Nullable WeightedState chooseRandomOutcome(
                List<WeightedState> blocks, RandomSource random) {
            if (blocks.isEmpty()) return null;
            int weight = 0;
            for (WeightedState choice : blocks) weight = Math.addExact(weight, choice.weight());
            int r = random.nextInt(weight);
            for (WeightedState choice : blocks) {
                r -= choice.weight();
                if (r < 0) return choice;
            }
            throw new IllegalStateException();
        }
    }
}
