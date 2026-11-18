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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public record HazmatProtection(HolderSet<Item> targets, double resistance) {

    public static final ResourceKey<Registry<HazmatProtection>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("hazmat_protection"));

    public static final Codec<HazmatProtection> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<HazmatProtection>mapCodec(
                                    instance ->
                                            instance.group(
                                                            RegistryCodecs.<Item>holderSet(
                                                                            (ResourceKey<
                                                                                            ? extends
                                                                                                    Registry<
                                                                                                            Item>>)
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
