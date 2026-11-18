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

package com.hbm.hazard;

import com.hbm.hazard.type.HazardKind;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public record HazardAssignment(
        HolderSet<Item> targets, List<Entry> entries, int mutex, int priority) {

    public static final ResourceKey<Registry<HazardAssignment>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("hazard"));

    public static final Codec<HazardAssignment> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            RegistryCodecs.<Item>holderSet(
                                                            (ResourceKey<? extends Registry<Item>>)
                                                                    Registries.ITEM)
                                                    .fieldOf("targets")
                                                    .forGetter(HazardAssignment::targets),
                                            Entry.CODEC
                                                    .listOf()
                                                    .fieldOf("entries")
                                                    .forGetter(HazardAssignment::entries),
                                            Codec.INT
                                                    .optionalFieldOf("mutex", 0)
                                                    .forGetter(HazardAssignment::mutex),
                                            Codec.INT
                                                    .optionalFieldOf(
                                                            "priority", HazardData.DEFAULT_PRIORITY)
                                                    .forGetter(HazardAssignment::priority))
                                    .apply(instance, HazardAssignment::new));

    public static HazardAssignment of(HolderSet<Item> targets, HazardData data) {
        return new HazardAssignment(targets, entriesOf(data), data.getMutex(), data.priority);
    }

    private static List<Entry> entriesOf(HazardData data) {
        return data.entries.stream()
                .map(
                        e -> {
                            if (!e.getMods().isEmpty()) {
                                throw new IllegalArgumentException(
                                        "hazard entry for "
                                                + e.type
                                                + " carries modifiers, which this record cannot project");
                            }
                            return new Entry(e.type, e.baseLevel);
                        })
                .toList();
    }

    public HazardData toData() {
        HazardData data = new HazardData();
        for (Entry entry : entries) data.addEntry(entry.type(), entry.level());
        return data.setMutex(mutex).setPriority(priority);
    }

    public record Entry(HazardKind type, double level) {

        public static final Codec<Entry> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                HazardRegistry.TYPE_CODEC
                                                        .fieldOf("type")
                                                        .forGetter(Entry::type),
                                                Codec.DOUBLE
                                                        .fieldOf("level")
                                                        .forGetter(Entry::level))
                                        .apply(instance, Entry::new));
    }
}
