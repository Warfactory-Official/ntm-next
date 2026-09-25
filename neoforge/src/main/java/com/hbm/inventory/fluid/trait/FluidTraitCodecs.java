// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.pollution.PollutionType;
import com.hbm.hazard.HazardClass;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.*;
import com.hbm.util.RegistryUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                RecordCodecBuilder.<FT_Corrosive>mapCodec(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .fieldOf("rating")
                                                        .forGetter(FT_Corrosive::getRating))
                                        .apply(i, FT_Corrosive::new)));

        put(
                "flammable",
                RecordCodecBuilder.<FT_Flammable>mapCodec(
                        i ->
                                i.group(
                                                Codec.LONG
                                                        .fieldOf("energy")
                                                        .forGetter(FT_Flammable::getHeatEnergy))
                                        .apply(i, FT_Flammable::new)));

        put(
                "combustible",
                RecordCodecBuilder.<FT_Combustible>mapCodec(
                        i ->
                                i.group(
                                                Codec.STRING
                                                        .xmap(
                                                                FT_Combustible.FuelGrade::valueOf,
                                                                Enum::name)
                                                        .fieldOf("grade")
                                                        .forGetter(FT_Combustible::gradeOrThrow),
                                                Codec.LONG
                                                        .fieldOf("energy")
                                                        .forGetter(
                                                                FT_Combustible
                                                                        ::getCombustionEnergy))
                                        .apply(i, FT_Combustible::new)));

        put(
                "poison",
                RecordCodecBuilder.<FT_Poison>mapCodec(
                        i ->
                                i.group(
                                                Codec.BOOL
                                                        .fieldOf("withering")
                                                        .forGetter(FT_Poison::isWithering),
                                                Codec.INT
                                                        .fieldOf("level")
                                                        .forGetter(FT_Poison::getLevel))
                                        .apply(i, FT_Poison::new)));

        put(
                "pwrmoderator",
                RecordCodecBuilder.<FT_PWRModerator>mapCodec(
                        i ->
                                i.group(
                                                Codec.DOUBLE
                                                        .fieldOf("multiplier")
                                                        .forGetter(FT_PWRModerator::getMultiplier))
                                        .apply(i, FT_PWRModerator::new)));

        put(
                "pheromone",
                RecordCodecBuilder.<FT_Pheromone>mapCodec(
                        i ->
                                i.group(Codec.INT.fieldOf("type").forGetter(FT_Pheromone::getType))
                                        .apply(i, FT_Pheromone::new)));

        put(
                "ventradiation",
                RecordCodecBuilder.<FT_VentRadiation>mapCodec(
                        i ->
                                i.group(
                                                Codec.FLOAT
                                                        .fieldOf("rad_per_mb")
                                                        .forGetter(FT_VentRadiation::getRadPerMB))
                                        .apply(i, FT_VentRadiation::new)));

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

        Codec<FT_Heatable.HeatingStep> heatingStep =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .fieldOf("amount_req")
                                                        .forGetter(step -> step.amountReq),
                                                Codec.INT
                                                        .fieldOf("heat_req")
                                                        .forGetter(step -> step.heatReq),
                                                fluidRef.fieldOf("produces")
                                                        .forGetter(step -> step::typeProduced),
                                                Codec.INT
                                                        .fieldOf("amount_produced")
                                                        .forGetter(step -> step.amountProduced))
                                        .apply(i, FT_Heatable.HeatingStep::new));

        Codec<Map<FT_Heatable.HeatingType, Double>> heatEff =
                Codec.unboundedMap(
                        Codec.STRING.xmap(FT_Heatable.HeatingType::valueOf, Enum::name),
                        Codec.DOUBLE);
        put(
                "heatable",
                RecordCodecBuilder.<FT_Heatable>mapCodec(
                        i ->
                                i.group(
                                                heatingStep
                                                        .listOf()
                                                        .fieldOf("steps")
                                                        .forGetter(FT_Heatable::steps),
                                                heatEff.fieldOf("efficiency")
                                                        .forGetter(
                                                                FluidTraitCodecs::heatEfficiency))
                                        .apply(i, FluidTraitCodecs::heatable)));

        Codec<Map<FT_Coolable.CoolingType, Double>> coolEff =
                Codec.unboundedMap(
                        Codec.STRING.xmap(FT_Coolable.CoolingType::valueOf, Enum::name),
                        Codec.DOUBLE);
        put(
                "coolable",
                RecordCodecBuilder.<FT_Coolable>mapCodec(
                        i ->
                                i.group(
                                                fluidRef.fieldOf("cools_to")
                                                        .forGetter(t -> t::coolsTo),
                                                Codec.INT
                                                        .fieldOf("amount_req")
                                                        .forGetter(t -> t.amountReq),
                                                Codec.INT
                                                        .fieldOf("amount_produced")
                                                        .forGetter(t -> t.amountProduced),
                                                Codec.INT
                                                        .fieldOf("heat_energy")
                                                        .forGetter(t -> t.heatEnergy),
                                                coolEff.fieldOf("efficiency")
                                                        .forGetter(
                                                                FluidTraitCodecs::coolEfficiency))
                                        .apply(i, FluidTraitCodecs::coolable)));

        Codec<HazardClass> hazard = Codec.STRING.xmap(HazardClass::valueOf, Enum::name);
        MapCodec<FT_Toxin.ToxinDirectDamage> directDamage =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                ResourceKey.codec(Registries.DAMAGE_TYPE)
                                                        .fieldOf("damage")
                                                        .forGetter(e -> e.damage),
                                                Codec.FLOAT
                                                        .fieldOf("amount")
                                                        .forGetter(e -> e.amount),
                                                Codec.INT.fieldOf("delay").forGetter(e -> e.delay),
                                                hazard.optionalFieldOf("hazard")
                                                        .forGetter(
                                                                e -> Optional.ofNullable(e.clazz)),
                                                Codec.BOOL
                                                        .fieldOf("full_body")
                                                        .forGetter(e -> e.fullBody))
                                        .apply(
                                                i,
                                                (damage, amount, delay, clazz, fullBody) ->
                                                        new FT_Toxin.ToxinDirectDamage(
                                                                damage,
                                                                amount,
                                                                delay,
                                                                clazz.orElse(null),
                                                                fullBody)));

        MapCodec<FT_Toxin.ToxinEffects> effects =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                MobEffectInstance.CODEC
                                                        .listOf()
                                                        .fieldOf("effects")
                                                        .forGetter(e -> e.effects),
                                                hazard.optionalFieldOf("hazard")
                                                        .forGetter(
                                                                e -> Optional.ofNullable(e.clazz)),
                                                Codec.BOOL
                                                        .fieldOf("full_body")
                                                        .forGetter(e -> e.fullBody))
                                        .apply(
                                                i,
                                                (effs, clazz, fullBody) -> {
                                                    FT_Toxin.ToxinEffects out =
                                                            new FT_Toxin.ToxinEffects(
                                                                    clazz.orElse(null), fullBody);
                                                    out.effects.addAll(effs);
                                                    return out;
                                                }));

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
        Codec<FT_Toxin.ToxinEntry> toxinEntry =
                toxinKind.dispatch(
                        "kind",
                        entry ->
                                entry instanceof FT_Toxin.ToxinDirectDamage
                                        ? "direct_damage"
                                        : "effects",
                        kind ->
                                switch (kind) {
                                    case "direct_damage" -> directDamage;
                                    case "effects" -> effects;
                                    default -> throw new IllegalArgumentException(kind);
                                });

        put(
                "toxin",
                RecordCodecBuilder.<FT_Toxin>mapCodec(
                        i ->
                                i.group(
                                                toxinEntry
                                                        .listOf()
                                                        .fieldOf("entries")
                                                        .forGetter(t -> t.entries))
                                        .apply(i, FluidTraitCodecs::toxin)));

        Codec<Map<PollutionType, Float>> pollution =
                Codec.unboundedMap(
                        Codec.STRING.xmap(PollutionType::valueOf, Enum::name), Codec.FLOAT);

        put(
                "polluting",
                RecordCodecBuilder.<FT_Polluting>mapCodec(
                        i ->
                                i.group(
                                                pollution
                                                        .optionalFieldOf("release", Map.of())
                                                        .forGetter(FT_Polluting::getReleaseMap),
                                                pollution
                                                        .optionalFieldOf("burn", Map.of())
                                                        .forGetter(FT_Polluting::getBurnMap))
                                        .apply(i, FluidTraitCodecs::polluting)));
    }

    private FluidTraitCodecs() {}

    private static Map<FT_Heatable.HeatingType, Double> heatEfficiency(FT_Heatable trait) {
        Map<FT_Heatable.HeatingType, Double> out = new LinkedHashMap<>();
        for (FT_Heatable.HeatingType type : FT_Heatable.HeatingType.values()) {
            double eff = trait.getEfficiency(type);
            if (eff != 0.0D) out.put(type, eff);
        }
        return out;
    }

    private static FT_Heatable heatable(
            List<FT_Heatable.HeatingStep> steps, Map<FT_Heatable.HeatingType, Double> efficiency) {
        FT_Heatable out = new FT_Heatable();
        for (FT_Heatable.HeatingStep step : steps) {
            out.addStep(step.heatReq, step.amountReq, step::typeProduced, step.amountProduced);
        }
        efficiency.forEach(out::setEff);
        return out;
    }

    private static Map<FT_Coolable.CoolingType, Double> coolEfficiency(FT_Coolable trait) {
        Map<FT_Coolable.CoolingType, Double> out = new LinkedHashMap<>();
        for (FT_Coolable.CoolingType type : FT_Coolable.CoolingType.values()) {
            double eff = trait.getEfficiency(type);
            if (eff != 0.0D) out.put(type, eff);
        }
        return out;
    }

    private static FT_Coolable coolable(
            Supplier<Fluid> coolsTo,
            int amountReq,
            int amountProduced,
            int heatEnergy,
            Map<FT_Coolable.CoolingType, Double> efficiency) {
        FT_Coolable out = new FT_Coolable(coolsTo, amountReq, amountProduced, heatEnergy);
        efficiency.forEach(out::setEff);
        return out;
    }

    private static FT_Toxin toxin(List<FT_Toxin.ToxinEntry> entries) {
        FT_Toxin out = new FT_Toxin();
        entries.forEach(out::addEntry);
        return out;
    }

    private static FT_Polluting polluting(
            Map<PollutionType, Float> release, Map<PollutionType, Float> burn) {
        FT_Polluting out = new FT_Polluting();
        release.forEach(out::release);
        burn.forEach(out::burn);
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

    private static <T extends FluidTrait> void put(String name, MapCodec<T> codec) {
        if (!FluidTrait.traitNameMap.containsKey(name)) {
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
        return BY_NAME.size() + "/" + FluidTrait.traitNameMap.size();
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
        for (Map.Entry<String, Class<? extends FluidTrait>> e :
                FluidTrait.traitNameMap.entrySet()) {
            if (e.getValue() == trait.getClass()) return e.getKey();
        }
        return null;
    }
}
