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

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.pollution.PollutionType;
import com.hbm.hazard.Exposure;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.*;
import com.hbm.util.RegistryUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidTraitCodecs {

    private static final Map<String, MapCodec<? extends FluidTrait>> BY_NAME =
            new LinkedHashMap<>();
    private static final Map<String, FluidTrait> FLAGS = new LinkedHashMap<>();
    private static final Map<String, FluidTrait> PHASES = new LinkedHashMap<>();
    private static final Map<String, MapCodec<? extends FluidTrait>> CONTAINERS =
            new LinkedHashMap<>();
    private static final Codec<Integer> HEX_RGB =
            Codec.STRING.comapFlatMap(
                    hex -> {
                        if (hex.length() != 7 || hex.charAt(0) != '#') {
                            return DataResult.error(() -> "colour '" + hex + "' is not #RRGGBB");
                        }
                        try {
                            return DataResult.success(HexFormat.fromHexDigits(hex, 1, 7));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "colour '" + hex + "' is not #RRGGBB");
                        }
                    },
                    rgb -> "#" + HexFormat.of().toHexDigits(rgb, 6));
    public static final Codec<Integer> COLOR =
            Codec.either(HEX_RGB, Codec.INT)
                    .xmap(
                            either -> either.map(rgb -> rgb, raw -> raw),
                            color ->
                                    (color & 0xFF000000) == 0
                                            ? Either.left(color)
                                            : Either.right(color));

    static {
        phase("liquid", FT_Liquid.INSTANCE);
        phase("gaseous", FT_Gaseous.INSTANCE);
        phase("plasma", FT_Plasma.INSTANCE);
        marker("gaseous_art", FT_Gaseous_ART.INSTANCE);
        marker("viscous", FT_Viscous.INSTANCE);
        marker("amat", FT_Amat.INSTANCE);
        marker("leadcontainer", FT_LeadContainer.INSTANCE);
        marker("delicious", FT_Delicious.INSTANCE);
        marker("noid", FT_NoID.INSTANCE);
        marker("nocontainer", FT_NoContainer.INSTANCE);
        marker("unsiphonable", FT_Unsiphonable.INSTANCE);

        container(
                "canister",
                RecordCodecBuilder.<CD_Canister>mapCodec(
                        i ->
                                i.group(
                                                COLOR.fieldOf("color")
                                                        .forGetter((CD_Canister t) -> t.color))
                                        .apply(i, CD_Canister::new)));

        container(
                "gastank",
                RecordCodecBuilder.<CD_Gastank>mapCodec(
                        i ->
                                i.group(
                                                COLOR.fieldOf("bottle_color")
                                                        .forGetter((CD_Gastank t) -> t.bottleColor),
                                                COLOR.fieldOf("label_color")
                                                        .forGetter((CD_Gastank t) -> t.labelColor))
                                        .apply(i, CD_Gastank::new)));

        put(
                "corrosive",
                RecordCodecBuilder.<CorrosiveTrait>mapCodec(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .fieldOf("rating")
                                                        .forGetter(CorrosiveTrait::rating))
                                        .apply(i, CorrosiveTrait::new)));

        put(
                "flammable",
                RecordCodecBuilder.<FlammableTrait>mapCodec(
                        i ->
                                i.group(
                                                Codec.LONG
                                                        .fieldOf("energy")
                                                        .forGetter(FlammableTrait::heatPerBucket))
                                        .apply(i, FlammableTrait::new)));

        put(
                "combustible",
                RecordCodecBuilder.<CombustibleTrait>mapCodec(
                        i ->
                                i.group(
                                                Codec.STRING
                                                        .xmap(
                                                                CombustibleTrait.Grade::valueOf,
                                                                Enum::name)
                                                        .fieldOf("grade")
                                                        .forGetter(CombustibleTrait::grade),
                                                Codec.LONG
                                                        .fieldOf("energy")
                                                        .forGetter(
                                                                CombustibleTrait::energyPerBucket))
                                        .apply(i, CombustibleTrait::new)));

        put(
                "poison",
                RecordCodecBuilder.<PoisonTrait>mapCodec(
                        i ->
                                i.group(
                                                Codec.BOOL
                                                        .fieldOf("withering")
                                                        .forGetter(PoisonTrait::withering),
                                                Codec.INT
                                                        .fieldOf("level")
                                                        .forGetter(PoisonTrait::level))
                                        .apply(i, PoisonTrait::new)));

        put(
                "pwrmoderator",
                RecordCodecBuilder.<ReactorModeratorTrait>mapCodec(
                        i ->
                                i.group(
                                                Codec.DOUBLE
                                                        .fieldOf("multiplier")
                                                        .forGetter(
                                                                ReactorModeratorTrait::multiplier))
                                        .apply(i, ReactorModeratorTrait::new)));

        put(
                "pheromone",
                RecordCodecBuilder.<PheromoneTrait>mapCodec(
                        i ->
                                i.group(Codec.INT.fieldOf("type").forGetter(PheromoneTrait::kind))
                                        .apply(i, PheromoneTrait::new)));

        put(
                "ventradiation",
                RecordCodecBuilder.<RadiationReleaseTrait>mapCodec(
                        i ->
                                i.group(
                                                Codec.FLOAT
                                                        .fieldOf("rad_per_mb")
                                                        .forGetter(RadiationReleaseTrait::perMb))
                                        .apply(i, RadiationReleaseTrait::new)));

        Codec<Supplier<Fluid>> fluidRef =
                Identifier.CODEC.comapFlatMap(
                        id -> {
                            try {
                                Fluid resolved = RegistryUtil.fluid(id);
                                return DataResult.success((Supplier<Fluid>) () -> resolved);
                            } catch (IllegalStateException missing) {
                                return DataResult.error(missing::getMessage);
                            }
                        },
                        supplier -> BuiltInRegistries.FLUID.getKey(supplier.get()));

        Codec<HeatableTrait.Step> heatingStep =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.intRange(1, Integer.MAX_VALUE)
                                                        .fieldOf("amount_req")
                                                        .forGetter(HeatableTrait.Step::consumed),
                                                Codec.intRange(1, Integer.MAX_VALUE)
                                                        .fieldOf("heat_req")
                                                        .forGetter(HeatableTrait.Step::heat),
                                                fluidRef.fieldOf("produces")
                                                        .forGetter(step -> step::product),
                                                Codec.intRange(1, Integer.MAX_VALUE)
                                                        .fieldOf("amount_produced")
                                                        .forGetter(HeatableTrait.Step::produced))
                                        .apply(
                                                i,
                                                (consumed, heat, product, produced) ->
                                                        new HeatableTrait.Step(
                                                                heat, consumed, product,
                                                                produced)));

        Codec<Map<HeatableTrait.Mode, Double>> heatEff =
                Codec.unboundedMap(
                        Codec.STRING.xmap(HeatableTrait.Mode::valueOf, Enum::name), Codec.DOUBLE);
        put(
                "heatable",
                RecordCodecBuilder.<HeatableTrait>mapCodec(
                        i ->
                                i.group(
                                                heatingStep
                                                        .listOf()
                                                        .validate(
                                                                steps ->
                                                                        steps.isEmpty()
                                                                                ? DataResult.error(
                                                                                        () ->
                                                                                                "a heatable trait declares no step")
                                                                                : DataResult
                                                                                        .success(
                                                                                                steps))
                                                        .fieldOf("steps")
                                                        .forGetter(HeatableTrait::steps),
                                                heatEff.fieldOf("efficiency")
                                                        .forGetter(
                                                                FluidTraitCodecs::heatEfficiency))
                                        .apply(i, HeatableTrait::new)));

        Codec<Map<CoolableTrait.Mode, Double>> coolEff =
                Codec.unboundedMap(
                        Codec.STRING.xmap(CoolableTrait.Mode::valueOf, Enum::name), Codec.DOUBLE);
        put(
                "coolable",
                RecordCodecBuilder.<CoolableTrait>mapCodec(
                        i ->
                                i.group(
                                                fluidRef.fieldOf("cools_to")
                                                        .forGetter(t -> t::product),
                                                Codec.INT
                                                        .fieldOf("amount_req")
                                                        .forGetter(CoolableTrait::consumed),
                                                Codec.INT
                                                        .fieldOf("amount_produced")
                                                        .forGetter(CoolableTrait::produced),
                                                Codec.INT
                                                        .fieldOf("heat_energy")
                                                        .forGetter(CoolableTrait::heat),
                                                coolEff.fieldOf("efficiency")
                                                        .forGetter(
                                                                FluidTraitCodecs::coolEfficiency))
                                        .apply(i, CoolableTrait::new)));

        MapCodec<ToxinTrait.Harm> directDamage =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                ResourceKey.codec(Registries.DAMAGE_TYPE)
                                                        .fieldOf("damage")
                                                        .forGetter(ToxinTrait.Harm::damage),
                                                Codec.FLOAT
                                                        .fieldOf("amount")
                                                        .forGetter(ToxinTrait.Harm::amount),
                                                Codec.INT
                                                        .fieldOf("delay")
                                                        .forGetter(ToxinTrait.Harm::delay),
                                                Exposure.CODEC
                                                        .fieldOf("hazard")
                                                        .forGetter(ToxinTrait.Harm::mask),
                                                Codec.BOOL
                                                        .fieldOf("full_body")
                                                        .forGetter(ToxinTrait.Harm::fullBody))
                                        .apply(i, ToxinTrait.Harm::new));

        MapCodec<ToxinTrait.Afflict> effects =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                MobEffectInstance.CODEC
                                                        .listOf()
                                                        .fieldOf("effects")
                                                        .forGetter(ToxinTrait.Afflict::effects),
                                                Exposure.CODEC
                                                        .fieldOf("hazard")
                                                        .forGetter(ToxinTrait.Afflict::mask),
                                                Codec.BOOL
                                                        .fieldOf("full_body")
                                                        .forGetter(ToxinTrait.Afflict::fullBody))
                                        .apply(
                                                i,
                                                (effs, mask, fullBody) ->
                                                        new ToxinTrait.Afflict(
                                                                mask, fullBody, effs)));

        Codec<String> toxinKind =
                Codec.STRING.validate(
                        kind ->
                                switch (kind) {
                                    case "direct_damage", "effects" -> DataResult.success(kind);
                                    default ->
                                            DataResult.error(
                                                    () ->
                                                            "unknown toxin kind '"
                                                                    + kind
                                                                    + "'; expected direct_damage or effects");
                                });
        Codec<ToxinTrait.Dose> toxinEntry =
                toxinKind.dispatch(
                        "kind",
                        dose -> dose instanceof ToxinTrait.Harm ? "direct_damage" : "effects",
                        kind ->
                                switch (kind) {
                                    case "direct_damage" -> directDamage;
                                    case "effects" -> effects;
                                    default -> throw new IllegalArgumentException(kind);
                                });

        put(
                "toxin",
                RecordCodecBuilder.<ToxinTrait>mapCodec(
                        i ->
                                i.group(
                                                toxinEntry
                                                        .listOf()
                                                        .fieldOf("entries")
                                                        .forGetter(ToxinTrait::doses))
                                        .apply(i, ToxinTrait::new)));

        Codec<Map<PollutionType, Float>> pollution =
                Codec.unboundedMap(PollutionType.CODEC, Codec.FLOAT);
        put(
                "polluting",
                RecordCodecBuilder.<PollutingTrait>mapCodec(
                        i ->
                                i.group(
                                                pollution
                                                        .optionalFieldOf("release", Map.of())
                                                        .forGetter(PollutingTrait::spilled),
                                                pollution
                                                        .optionalFieldOf("burn", Map.of())
                                                        .forGetter(PollutingTrait::burned))
                                        .apply(i, PollutingTrait::new)));
    }

    private FluidTraitCodecs() {}

    private static Map<HeatableTrait.Mode, Double> heatEfficiency(HeatableTrait trait) {
        Map<HeatableTrait.Mode, Double> out = new LinkedHashMap<>();
        for (var mode : HeatableTrait.Mode.values()) {
            double eff = trait.efficiency(mode);
            if (eff != 0.0D) out.put(mode, eff);
        }
        return out;
    }

    private static Map<CoolableTrait.Mode, Double> coolEfficiency(CoolableTrait trait) {
        Map<CoolableTrait.Mode, Double> out = new LinkedHashMap<>();
        for (var mode : CoolableTrait.Mode.values()) {
            double eff = trait.efficiency(mode);
            if (eff != 0.0D) out.put(mode, eff);
        }
        return out;
    }

    private static <T extends FluidTrait> void marker(String name, T instance) {
        put(name, MapCodec.unit(instance));
        FLAGS.put(name, instance);
    }

    private static <T extends FluidTrait> void phase(String name, T instance) {
        put(name, MapCodec.unit(instance));
        PHASES.put(name, instance);
    }

    private static <T extends FluidTrait> void container(String name, MapCodec<T> codec) {
        put(name, codec);
        CONTAINERS.put(name, codec);
    }

    public static <T extends FluidTrait> void register(String name, MapCodec<T> codec) {
        if (BY_NAME.containsKey(name)) {
            throw new IllegalStateException("trait codec " + name + " is already registered");
        }
        put(name, codec);
    }

    public static <T extends FluidTrait> void registerFlag(String name, T instance) {
        if (BY_NAME.containsKey(name)) {
            throw new IllegalStateException("trait codec " + name + " is already registered");
        }
        marker(name, instance);
    }

    private static <T extends FluidTrait> void put(String name, MapCodec<T> codec) {
        if (FluidTraitRegistry.kind(name) == null) {
            throw new IllegalStateException(
                    "no trait is registered under '"
                            + name
                            + "', so this codec "
                            + "dispatches on a name nothing can produce");
        }
        BY_NAME.put(name, codec);
    }

    public static @Nullable MapCodec<? extends FluidTrait> byName(String name) {
        return BY_NAME.get(name);
    }

    public static String coverage() {
        return BY_NAME.size() + "/" + FluidTraitRegistry.names().size();
    }

    public static Map<String, MapCodec<? extends FluidTrait>> all() {
        return Map.copyOf(BY_NAME);
    }

    public static Map<String, FluidTrait> flags() {
        return Map.copyOf(FLAGS);
    }

    public static Map<String, FluidTrait> phases() {
        return Map.copyOf(PHASES);
    }

    public static Map<String, MapCodec<? extends FluidTrait>> containers() {
        return Map.copyOf(CONTAINERS);
    }

    public static Map<String, MapCodec<? extends FluidTrait>> parameterized() {
        Map<String, MapCodec<? extends FluidTrait>> out = new LinkedHashMap<>(BY_NAME);
        out.keySet().removeAll(FLAGS.keySet());
        out.keySet().removeAll(PHASES.keySet());
        out.keySet().removeAll(CONTAINERS.keySet());
        return out;
    }

    public static @Nullable String nameOf(FluidTrait trait) {
        return FluidTraitRegistry.nameOf(trait.getClass());
    }
}
