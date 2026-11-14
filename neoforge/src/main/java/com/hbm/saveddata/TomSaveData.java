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

import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class TomSaveData extends SavedData {

    public static final SavedDataType<TomSaveData> TYPE =
            new SavedDataType<>(RegistryUtil.id("impact_data"), TomSaveData::new, codec(), null);

    public volatile float dust;
    public volatile float fire;
    public volatile boolean impact;

    public TomSaveData() {}

    private TomSaveData(float dust, float fire, boolean impact) {
        this.dust = dust;
        this.fire = fire;
        this.impact = impact;
    }

    private static final Map<ResourceKey<Level>, TomSaveData> PUBLISHED = new ConcurrentHashMap<>();

    public static TomSaveData get(ServerLevel level) {
        TomSaveData data = level.getDataStorage().computeIfAbsent(TYPE);
        PUBLISHED.put(level.dimension(), data);
        return data;
    }

    public static TomSaveData getExisting(ServerLevel level) {
        return level.getDataStorage().get(TYPE);
    }

    public static boolean impact(ServerLevel level) {
        TomSaveData data = PUBLISHED.get(level.dimension());
        return data != null && data.impact;
    }

    public static @Nullable TomSaveData published(ServerLevel level) {
        return PUBLISHED.get(level.dimension());
    }

    public static void publish(ServerLevel level) {
        TomSaveData data = level.getDataStorage().get(TYPE);
        if (data != null) PUBLISHED.put(level.dimension(), data);
    }

    public static void onServerStopping() {
        PUBLISHED.clear();
    }

    private static Codec<TomSaveData> codec() {
        return RecordCodecBuilder.create(
                i ->
                        i.group(
                                        Codec.FLOAT
                                                .optionalFieldOf("dust", 0F)
                                                .forGetter(d -> d.dust),
                                        Codec.FLOAT
                                                .optionalFieldOf("fire", 0F)
                                                .forGetter(d -> d.fire),
                                        Codec.BOOL
                                                .optionalFieldOf("impact", false)
                                                .forGetter(d -> d.impact))
                                .apply(i, TomSaveData::new));
    }
}
