// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;

public record SuitProtection(
        List<Kind> protections,
        Optional<Armor> armor,
        Optional<Holder<MobEffect>> effect,
        double maxHealth,
        Optional<HolderSet<Item>> claddingIfModified) {
    public static final ResourceKey<Registry<SuitProtection>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("suit_protection"));
    public static final Codec<SuitProtection> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<SuitProtection>mapCodec(
                                    i ->
                                            i.group(
                                                            Kind.CODEC
                                                                    .listOf()
                                                                    .fieldOf("protections")
                                                                    .forGetter(
                                                                            SuitProtection
                                                                                    ::protections),
                                                            Armor.CODEC
                                                                    .optionalFieldOf("armor")
                                                                    .forGetter(
                                                                            SuitProtection::armor),
                                                            net.minecraft.world.effect.MobEffect
                                                                    .CODEC
                                                                    .optionalFieldOf("effect")
                                                                    .forGetter(
                                                                            SuitProtection::effect),
                                                            Codec.doubleRange(0D, Double.MAX_VALUE)
                                                                    .optionalFieldOf(
                                                                            "max_health",
                                                                            Double.MAX_VALUE)
                                                                    .forGetter(
                                                                            SuitProtection
                                                                                    ::maxHealth),
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ITEM)
                                                                    .optionalFieldOf(
                                                                            "cladding_if_modified")
                                                                    .forGetter(
                                                                            SuitProtection
                                                                                    ::claddingIfModified))
                                                    .apply(i, SuitProtection::new)))
                    .codec()
                    .validate(
                            rule -> {
                                if (rule.protections.isEmpty())
                                    return DataResult.error(
                                            () -> "suit protection needs a protection kind");
                                if (rule.armor.isEmpty() && rule.effect.isEmpty())
                                    return DataResult.error(
                                            () -> "suit protection needs armor or an effect");
                                if (rule.claddingIfModified.isPresent() && rule.armor.isEmpty())
                                    return DataResult.error(
                                            () -> "cladding condition requires armor");
                                return DataResult.success(rule);
                            });

    public record Armor(
            HolderSet<Item> head,
            HolderSet<Item> chest,
            HolderSet<Item> legs,
            HolderSet<Item> feet) {
        public static final Codec<Armor> CODEC =
                DataCodecs.strict(
                                RecordCodecBuilder.<Armor>mapCodec(
                                        i ->
                                                i.group(
                                                                RegistryCodecs.homogeneousList(
                                                                                Registries.ITEM)
                                                                        .fieldOf("head")
                                                                        .forGetter(Armor::head),
                                                                RegistryCodecs.homogeneousList(
                                                                                Registries.ITEM)
                                                                        .fieldOf("chest")
                                                                        .forGetter(Armor::chest),
                                                                RegistryCodecs.homogeneousList(
                                                                                Registries.ITEM)
                                                                        .fieldOf("legs")
                                                                        .forGetter(Armor::legs),
                                                                RegistryCodecs.homogeneousList(
                                                                                Registries.ITEM)
                                                                        .fieldOf("feet")
                                                                        .forGetter(Armor::feet))
                                                        .apply(i, Armor::new)))
                        .codec();
    }

    public enum Kind implements net.minecraft.util.StringRepresentable {
        HAZMAT,
        SEALED,
        ASBESTOS,
        DIGAMMA,
        DIGAMMA2;
        public static final Codec<Kind> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public int bit() {
            return 1 << ordinal();
        }
    }
}
