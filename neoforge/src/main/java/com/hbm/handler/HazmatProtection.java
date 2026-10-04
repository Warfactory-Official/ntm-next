// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public record HazmatProtection(HolderSet<Item> targets, double resistance) {

    public static final ResourceKey<Registry<HazmatProtection>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("hazmat_protection"));

    public static final Codec<HazmatProtection> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<HazmatProtection>mapCodec(
                                    instance ->
                                            instance.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ITEM)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            HazmatProtection
                                                                                    ::targets),
                                                            Codec.doubleRange(0D, Double.MAX_VALUE)
                                                                    .fieldOf("resistance")
                                                                    .forGetter(
                                                                            HazmatProtection
                                                                                    ::resistance))
                                                    .apply(instance, HazmatProtection::new)))
                    .codec();
}
