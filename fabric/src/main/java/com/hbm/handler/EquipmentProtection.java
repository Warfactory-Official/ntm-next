// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.hazard.HazardClass;
import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public record EquipmentProtection(
        HolderSet<Item> targets,
        List<HazardClass> hazards,
        Optional<List<HazardClass>> filterExclusions,
        boolean filter,
        boolean filterTooltipAfterItem,
        Optional<Cladding> cladding) {

    public static final ResourceKey<Registry<EquipmentProtection>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("equipment_protection"));

    public static final Codec<EquipmentProtection> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<EquipmentProtection>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ITEM)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::targets),
                                                            HazardClass.CODEC
                                                                    .listOf()
                                                                    .optionalFieldOf(
                                                                            "hazards", List.of())
                                                                    .forGetter(
                                                                            EquipmentProtection
                                                                                    ::hazards),
                                                            HazardClass.CODEC
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

    public enum ArmorSlot implements net.minecraft.util.StringRepresentable {
        HEAD,
        CHEST,
        LEGS,
        FEET;
        public static final Codec<ArmorSlot> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(ArmorSlot::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public int bit() {
            return 1 << ordinal();
        }
    }
}
