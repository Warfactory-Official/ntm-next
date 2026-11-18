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

import com.hbm.hazard.Exposure;
import com.hbm.util.DataCodecs;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

public record EquipmentProtection(
        HolderSet<Item> targets,
        List<Exposure> hazards,
        Optional<List<Exposure>> filterExclusions,
        boolean filter,
        boolean filterTooltipAfterItem,
        Optional<Cladding> cladding) {

    public static final ResourceKey<Registry<EquipmentProtection>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("equipment_protection"));

    public static final Codec<EquipmentProtection> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<EquipmentProtection>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.<Item>holderSet(
                                                                            (ResourceKey<
                                                                                            ? extends
                                                                                                    Registry<
                                                                                                            Item>>)
                                                                                    Registries.ITEM)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::targets),
                                                            Exposure.CODEC
                                                                    .listOf()
                                                                    .optionalFieldOf(
                                                                            "hazards", List.of())
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::hazards),
                                                            Exposure.CODEC
                                                                    .listOf()
                                                                    .optionalFieldOf(
                                                                            "filter_exclusions")
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::filterExclusions),
                                                            Codec.BOOL
                                                                    .optionalFieldOf(
                                                                            "filter", false)
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::filter),
                                                            Codec.BOOL
                                                                    .optionalFieldOf(
                                                                            "filter_tooltip_after_item",
                                                                            false)
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::filterTooltipAfterItem),
                                                            Cladding.CODEC
                                                                    .optionalFieldOf("cladding")
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::cladding))
                                                    .apply(i, EquipmentProtection::new)))
                    .codec();

    public record Cladding(double resistance, List<ArmorSlot> slots, boolean showTooltip) {
        public static final Codec<Cladding> CODEC =
                DataCodecs.strict(
                                RecordCodecBuilder.<Cladding>mapCodec(
                                        i ->
                                                i.group(
                                                                Codec.doubleRange(
                                                                                0D,
                                                                                Double.MAX_VALUE)
                                                                        .fieldOf("resistance")
                                                                        .forGetter(
                                                                                Cladding
                                                                                        ::resistance),
                                                                ArmorSlot.CODEC
                                                                        .listOf()
                                                                        .fieldOf("slots")
                                                                        .forGetter(Cladding::slots),
                                                                Codec.BOOL
                                                                        .optionalFieldOf(
                                                                                "show_tooltip",
                                                                                true)
                                                                        .forGetter(
                                                                                Cladding
                                                                                        ::showTooltip))
                                                        .apply(i, Cladding::new)))
                        .codec()
                        .validate(
                                c ->
                                        c.slots.isEmpty()
                                                ? DataResult.error(
                                                        () ->
                                                                "cladding must allow at least one armor slot")
                                                : DataResult.success(c));
    }

    public enum ArmorSlot implements StringRepresentable {
        HEAD,
        CHEST,
        LEGS,
        FEET;
        public static final Codec<ArmorSlot> CODEC =
                StringRepresentable.fromEnum(ArmorSlot::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int bit() {
            return 1 << ordinal();
        }
    }
}
