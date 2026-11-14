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

package com.hbm.saveddata;

import com.hbm.registration.OverridePoint;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class SatelliteSavedData extends SavedData {

    private static final Codec<Entry> ENTRY_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT.fieldOf("freq").forGetter(Entry::freq),
                                            SatelliteType.PERSISTED_CODEC
                                                    .fieldOf("sat")
                                                    .forGetter(Entry::sat))
                                    .apply(i, Entry::new));
    public static final Codec<SatelliteSavedData> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ENTRY_CODEC
                                                    .listOf()
                                                    .optionalFieldOf("sats", List.of())
                                                    .forGetter(SatelliteSavedData::toEntries))
                                    .apply(i, SatelliteSavedData::fromEntries));
    public static final SavedDataType<SatelliteSavedData> TYPE =
            new SavedDataType<>(
                    RegistryUtil.id("satellites"), SatelliteSavedData::new, CODEC, null);

    public final Map<Integer, Satellite> sats = new HashMap<>();

    public static SatelliteSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static final OverridePoint<Locator> AT =
            OverridePoint.of(RegistryUtil.id("satellites_at"), (level, x, z) -> get(level));

    public static final OverridePoint<Runnable> CHANGED =
            OverridePoint.of(RegistryUtil.id("satellites_changed"), () -> {});

    @FunctionalInterface
    public interface Locator {
        SatelliteSavedData at(ServerLevel level, int x, int z);

        default List<ServerLevel> levels(ServerLevel level, int x, int z) {
            return List.of(level);
        }
    }

    public static SatelliteSavedData at(ServerLevel level, int x, int z) {
        return AT.get().at(level, x, z);
    }

    public static List<ServerLevel> levels(ServerLevel level, int x, int z) {
        return AT.get().levels(level, x, z);
    }

    public static SatelliteSavedData at(ServerLevel level, int x, int z, int frequency) {
        var local = at(level, x, z);
        if (local.isFreqTaken(frequency)) return local;
        for (var candidate : levels(level, x, z)) {
            var data = get(candidate);
            if (data != local && data.isFreqTaken(frequency)) return data;
        }
        return local;
    }

    private static SatelliteSavedData fromEntries(List<Entry> entries) {
        SatelliteSavedData data = new SatelliteSavedData();
        for (Entry entry : entries) data.store(entry.freq(), entry.sat());
        return data;
    }

    public boolean isFreqTaken(int freq) {
        return getSatFromFreq(freq) != null;
    }

    public @Nullable Satellite getSatFromFreq(int freq) {
        return sats.get(freq);
    }

    public void put(int freq, Satellite sat) {
        store(freq, sat);
        setDirty();
        CHANGED.get().run();
    }

    private void store(int freq, Satellite sat) {
        sat.attach(this);
        sats.put(freq, sat);
    }

    public boolean descend(int freq) {
        if (sats.remove(freq) == null) return false;
        setDirty();
        CHANGED.get().run();
        return true;
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(sats.size());
        for (Map.Entry<Integer, Satellite> sat : sats.entrySet()) {
            entries.add(new Entry(sat.getKey(), sat.getValue()));
        }
        return entries;
    }

    private record Entry(int freq, Satellite sat) {}
}
