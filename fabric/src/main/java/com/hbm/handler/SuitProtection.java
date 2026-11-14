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

package com.hbm.handler;

import com.hbm.util.DataCodecs;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;

public record SuitProtection(
        List<Kind> protections,
        Optional<Armor> armor,
        Optional<Holder<MobEffect>> effect,
        double maxHealth,
        Optional<HolderSet<Item>> claddingIfModified) {
    public static final ResourceKey<Registry<SuitProtection>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("suit_protection"));
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
                                                            MobEffect.CODEC
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
                                                            RegistryCodecs.<Item>homogeneousList(
                                                                            (ResourceKey<
                                                                                            ? extends
                                                                                                    Registry<
                                                                                                            Item>>)
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
                                                                RegistryCodecs
                                                                        .<Item>homogeneousList(
                                                                                (ResourceKey<
                                                                                                ? extends
                                                                                                        Registry<
                                                                                                                Item>>)
                                                                                        Registries
                                                                                                .ITEM)
                                                                        .fieldOf("head")
                                                                        .forGetter(Armor::head),
                                                                RegistryCodecs
                                                                        .<Item>homogeneousList(
                                                                                (ResourceKey<
                                                                                                ? extends
                                                                                                        Registry<
                                                                                                                Item>>)
                                                                                        Registries
                                                                                                .ITEM)
                                                                        .fieldOf("chest")
                                                                        .forGetter(Armor::chest),
                                                                RegistryCodecs
                                                                        .<Item>homogeneousList(
                                                                                (ResourceKey<
                                                                                                ? extends
                                                                                                        Registry<
                                                                                                                Item>>)
                                                                                        Registries
                                                                                                .ITEM)
                                                                        .fieldOf("legs")
                                                                        .forGetter(Armor::legs),
                                                                RegistryCodecs
                                                                        .<Item>homogeneousList(
                                                                                (ResourceKey<
                                                                                                ? extends
                                                                                                        Registry<
                                                                                                                Item>>)
                                                                                        Registries
                                                                                                .ITEM)
                                                                        .fieldOf("feet")
                                                                        .forGetter(Armor::feet))
                                                        .apply(i, Armor::new)))
                        .codec();
    }

    public enum Kind implements StringRepresentable {
        HAZMAT,
        SEALED,
        ASBESTOS,
        DIGAMMA,
        DIGAMMA2;
        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int bit() {
            return 1 << ordinal();
        }
    }
}
