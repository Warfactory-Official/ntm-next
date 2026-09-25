// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.pollution;

import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class PollutionSavedData extends SavedData {

    private static final Codec<Entry> ENTRY_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT.fieldOf("chunkX").forGetter(Entry::chunkX),
                                            Codec.INT.fieldOf("chunkZ").forGetter(Entry::chunkZ),
                                            Codec.FLOAT
                                                    .listOf()
                                                    .fieldOf("values")
                                                    .forGetter(Entry::values))
                                    .apply(i, Entry::new));
    public static final SavedDataType<PollutionSavedData> TYPE =
            new SavedDataType<>(Library.id("pollution"), PollutionSavedData::new, codec(), null);
    public final HashMap<ChunkPos, PollutionData> pollution = new HashMap<>();

    public static PollutionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static PollutionSavedData getExisting(ServerLevel level) {
        return level.getDataStorage().get(TYPE);
    }

    private static PollutionSavedData fromEntries(List<Entry> entries) {
        PollutionSavedData data = new PollutionSavedData();
        for (Entry e : entries) {
            PollutionData pd = new PollutionData();
            int n = Math.min(e.values().size(), pd.pollution.length);
            for (int i = 0; i < n; i++) pd.pollution[i] = e.values().get(i);
            data.pollution.put(new ChunkPos(e.chunkX(), e.chunkZ()), pd);
        }
        return data;
    }

    private static Codec<PollutionSavedData> codec() {
        return RecordCodecBuilder.create(
                i ->
                        i.group(
                                        ENTRY_CODEC
                                                .listOf()
                                                .optionalFieldOf("entries", List.of())
                                                .forGetter(PollutionSavedData::toEntries))
                                .apply(i, PollutionSavedData::fromEntries));
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(pollution.size());
        for (Map.Entry<ChunkPos, PollutionData> e : pollution.entrySet()) {
            List<Float> values = new ArrayList<>(PollutionType.VALUES.length);
            for (float f : e.getValue().pollution) values.add(f);
            entries.add(new Entry(e.getKey().x(), e.getKey().z(), values));
        }
        return entries;
    }

    public static class PollutionData {
        public float[] pollution = new float[PollutionType.VALUES.length];
    }

    private record Entry(int chunkX, int chunkZ, List<Float> values) {}
}
