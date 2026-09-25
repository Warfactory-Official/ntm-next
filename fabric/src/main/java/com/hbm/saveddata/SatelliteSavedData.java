// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata;

import com.hbm.lib.Library;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
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
            new SavedDataType<>(Library.id("satellites"), SatelliteSavedData::new, CODEC, null);

    public final Map<Integer, Satellite> sats = new HashMap<>();

    public static SatelliteSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static SatelliteSavedData fromEntries(List<Entry> entries) {
        SatelliteSavedData data = new SatelliteSavedData();
        for (Entry entry : entries) data.sats.put(entry.freq(), entry.sat());
        return data;
    }

    public boolean isFreqTaken(int freq) {
        return getSatFromFreq(freq) != null;
    }

    public @Nullable Satellite getSatFromFreq(int freq) {
        return sats.get(freq);
    }

    public void put(int freq, Satellite sat) {
        sats.put(freq, sat);
        setDirty();
    }

    public boolean descend(int freq) {
        if (sats.remove(freq) == null) return false;
        setDirty();
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
