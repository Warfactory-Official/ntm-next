// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitCodecs;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class NTMFluidPropertyCodec {

    private static final Codec<EnumSymbol> SYMBOL =
            Codec.STRING.xmap(EnumSymbol::valueOf, Enum::name);
    private static final MapCodec<Nfpa> NFPA =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.INT.fieldOf("health").forGetter(Nfpa::health),
                                            Codec.INT.fieldOf("flame").forGetter(Nfpa::flame),
                                            Codec.INT.fieldOf("react").forGetter(Nfpa::react))
                                    .apply(i, Nfpa::new));
    private static final Codec<String> PHASE =
            Codec.STRING.validate(
                    name ->
                            FluidTraitCodecs.phases().containsKey(name)
                                    ? DataResult.success(name)
                                    : DataResult.error(
                                            () ->
                                                    "unknown fluid phase '"
                                                            + name
                                                            + "'; expected one of "
                                                            + FluidTraitCodecs.phases().keySet()));
    private static final Codec<Map<String, FluidTrait>> TRAITS =
            Codec.dispatchedMap(
                    Codec.STRING,
                    name -> {
                        MapCodec<? extends FluidTrait> codec =
                                FluidTraitCodecs.parameterized().get(name);
                        return codec != null
                                ? codec.codec()
                                : wrongBucket(
                                        name,
                                        "a stateless one belongs in " + "\"flags\"",
                                        "parameterized trait");
                    });
    private static final Codec<Map<String, FluidTrait>> CONTAINERS =
            Codec.dispatchedMap(
                    Codec.STRING,
                    name -> {
                        MapCodec<? extends FluidTrait> codec =
                                FluidTraitCodecs.containers().get(name);
                        return codec != null
                                ? codec.codec()
                                : wrongBucket(
                                        name,
                                        "only canister and gastank are " + "container definitions",
                                        "container definition");
                    });

    public static final Codec<NTMFluidProperty> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            PHASE.fieldOf("type")
                                                    .forGetter(NTMFluidPropertyCodec::phaseName),
                                            FluidTraitCodecs.COLOR
                                                    .fieldOf("color")
                                                    .forGetter(NTMFluidProperty::color),
                                            SYMBOL.fieldOf("symbol")
                                                    .forGetter(NTMFluidProperty::symbol),
                                            Codec.INT
                                                    .fieldOf("temperature")
                                                    .forGetter(NTMFluidProperty::temperature),
                                            NFPA.fieldOf("nfpa").forGetter(Nfpa::of),
                                            Codec.STRING
                                                    .listOf()
                                                    .optionalFieldOf("flags", List.of())
                                                    .forGetter(NTMFluidPropertyCodec::flagNames),
                                            TRAITS.optionalFieldOf("traits", Map.of())
                                                    .forGetter(p -> group(p, false)),
                                            CONTAINERS
                                                    .optionalFieldOf("containers", Map.of())
                                                    .forGetter(p -> group(p, true)),
                                            DataCodecs.listOrSingle(
                                                            TagKey.hashedCodec(Registries.FLUID))
                                                    .optionalFieldOf("shares", List.of())
                                                    .forGetter(NTMFluidProperty::shares))
                                    .apply(i, NTMFluidPropertyCodec::assemble));

    private NTMFluidPropertyCodec() {}

    private static Codec<FluidTrait> wrongBucket(String name, String hint, String expected) {
        return MapCodec.unit(() -> (FluidTrait) null)
                .codec()
                .validate(
                        ignored ->
                                DataResult.error(
                                        () -> "'" + name + "' is not a " + expected + "; " + hint));
    }

    private static String phaseName(NTMFluidProperty property) {
        for (FluidTrait trait : property.traits()) {
            String name = FluidTraitCodecs.nameOf(trait);
            if (name != null && FluidTraitCodecs.phases().containsKey(name)) return name;
        }
        throw new IllegalStateException(
                "a fluid carries no phase trait, so it has no type to ship; every "
                        + "fluid must declare exactly one of "
                        + FluidTraitCodecs.phases().keySet());
    }

    private static List<String> flagNames(NTMFluidProperty property) {
        List<String> out = new ArrayList<>();
        for (FluidTrait trait : property.traits()) {
            String name = FluidTraitCodecs.nameOf(trait);
            if (name != null && FluidTraitCodecs.flags().containsKey(name)) out.add(name);
        }
        return out;
    }

    private static Map<String, FluidTrait> group(NTMFluidProperty property, boolean containers) {
        Map<String, FluidTrait> out = new LinkedHashMap<>();
        Map<String, ?> bucket =
                containers ? FluidTraitCodecs.containers() : FluidTraitCodecs.parameterized();
        for (FluidTrait trait : property.traits()) {
            String name = FluidTraitCodecs.nameOf(trait);
            if (name != null && bucket.containsKey(name)) out.put(name, trait);
        }
        return out;
    }

    private static NTMFluidProperty assemble(
            String phase,
            int color,
            EnumSymbol symbol,
            int temperature,
            Nfpa nfpa,
            List<String> flags,
            Map<String, FluidTrait> traits,
            Map<String, FluidTrait> containers,
            List<TagKey<Fluid>> shares) {
        List<FluidTrait> all = new ArrayList<>();
        all.add(FluidTraitCodecs.phases().get(phase));
        for (String flag : flags) {
            FluidTrait trait = FluidTraitCodecs.flags().get(flag);
            if (trait == null) {
                throw new IllegalStateException(
                        "'"
                                + flag
                                + "' is not a stateless trait; a parameterized one "
                                + "belongs in \"traits\"");
            }
            all.add(trait);
        }
        all.addAll(traits.values());
        all.addAll(containers.values());
        return new NTMFluidProperty(
                color,
                symbol,
                temperature,
                nfpa.health(),
                nfpa.flame(),
                nfpa.react(),
                all.toArray(new FluidTrait[0]),
                List.copyOf(shares));
    }

    private record Nfpa(int health, int flame, int react) {

        static Nfpa of(NTMFluidProperty property) {
            return new Nfpa(property.nfpaHealth(), property.nfpaFlame(), property.nfpaReact());
        }
    }
}
