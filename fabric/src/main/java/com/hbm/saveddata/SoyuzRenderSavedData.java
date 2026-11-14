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

import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class SoyuzRenderSavedData extends SavedData {
    private static final Codec<Snapshot> SNAPSHOT_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG.fieldOf("core").forGetter(Snapshot::core),
                                            Codec.FLOAT
                                                    .listOf()
                                                    .fieldOf("positions")
                                                    .forGetter(Snapshot::positions),
                                            Codec.INT.fieldOf("skin").forGetter(Snapshot::skin),
                                            Codec.INT.fieldOf("status").forGetter(Snapshot::status),
                                            Codec.INT
                                                    .fieldOf("block_light")
                                                    .forGetter(Snapshot::blockLight),
                                            Codec.INT
                                                    .fieldOf("sky_light")
                                                    .forGetter(Snapshot::skyLight))
                                    .apply(i, Snapshot::new));
    public static final SavedDataType<SoyuzRenderSavedData> TYPE =
            new SavedDataType<>(
                    RegistryUtil.id("soyuz_render"), SoyuzRenderSavedData::new, codec(), null);

    private final Long2ObjectOpenHashMap<Snapshot> snapshots = new Long2ObjectOpenHashMap<>();

    private SoyuzRenderSavedData() {}

    private SoyuzRenderSavedData(List<Snapshot> entries) {
        for (Snapshot entry : entries) {
            if (entry.positions().size() != 8
                    || entry.status() < 0
                    || entry.status() >= LaunchpadSoyuzBlockEntity.SoyuzStatus.values().length
                    || snapshots.put(entry.core(), entry) != null)
                throw new IllegalArgumentException(
                        "Invalid Soyuz render snapshot at " + entry.core());
        }
    }

    private static Codec<SoyuzRenderSavedData> codec() {
        return RecordCodecBuilder.create(
                i ->
                        i.group(
                                        SNAPSHOT_CODEC
                                                .listOf()
                                                .fieldOf("snapshots")
                                                .forGetter(
                                                        d -> new ArrayList<>(d.snapshots.values())))
                                .apply(i, SoyuzRenderSavedData::new));
    }

    public static SoyuzRenderSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static @Nullable SoyuzRenderSavedData getExisting(ServerLevel level) {
        return level.getDataStorage().get(TYPE);
    }

    public @Nullable Snapshot snapshot(long core) {
        return snapshots.get(core);
    }

    public void update(
            long core,
            float[] positions,
            int skin,
            LaunchpadSoyuzBlockEntity.SoyuzStatus status,
            int blockLight,
            int skyLight) {
        Snapshot old = snapshots.get(core);
        if (old != null
                && old.skin() == skin
                && old.status() == status.ordinal()
                && old.blockLight() == blockLight
                && old.skyLight() == skyLight) {
            boolean equal = true;
            for (int i = 0; i < positions.length; i++) {
                if (Float.floatToIntBits(old.positions().get(i))
                        != Float.floatToIntBits(positions[i])) {
                    equal = false;
                    break;
                }
            }
            if (equal) return;
        }
        var copy = new ArrayList<Float>(positions.length);
        for (float position : positions) copy.add(position);
        snapshots.put(
                core,
                new Snapshot(
                        core, List.copyOf(copy), skin, status.ordinal(), blockLight, skyLight));
        setDirty();
    }

    public void remove(long core) {
        if (snapshots.remove(core) != null) setDirty();
    }

    public record Snapshot(
            long core, List<Float> positions, int skin, int status, int blockLight, int skyLight) {}
}
