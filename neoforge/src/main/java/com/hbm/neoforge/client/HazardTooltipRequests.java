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

package com.hbm.neoforge.client;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

final class HazardTooltipRequests {
    private static final int REQUEST_PERIOD = 20;
    private static final Object2LongOpenHashMap<UUID> storage = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<UUID> backpacks = new Object2LongOpenHashMap<>();
    private static ResourceKey<Level> lastDimension;
    private static long lastPruneTick;
    private static long lastObservedTick;

    private HazardTooltipRequests() {}

    static void reset() {
        storage.clear();
        backpacks.clear();
        lastDimension = null;
        lastPruneTick = 0L;
        lastObservedTick = 0L;
    }

    static boolean reserve(UUID id, boolean backpack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return false;
        long tick = minecraft.level.getGameTime();
        ResourceKey<Level> dimension = minecraft.level.dimension();
        if (!dimension.equals(lastDimension) || tick < lastObservedTick) {
            reset();
            lastDimension = dimension;
            lastPruneTick = tick;
        } else if (tick - lastPruneTick >= REQUEST_PERIOD) {
            expire(storage, tick);
            expire(backpacks, tick);
            lastPruneTick = tick;
        }
        lastObservedTick = tick;
        Object2LongOpenHashMap<UUID> requests = backpack ? backpacks : storage;
        long previous = requests.getOrDefault(id, Long.MIN_VALUE);
        if (previous != Long.MIN_VALUE && tick >= previous && tick - previous < REQUEST_PERIOD)
            return false;
        requests.put(id, tick);
        return true;
    }

    private static void expire(Object2LongMap<UUID> requests, long tick) {
        var iterator = Object2LongMaps.fastIterator(requests);
        while (iterator.hasNext()) {
            if (tick - iterator.next().getLongValue() >= REQUEST_PERIOD) iterator.remove();
        }
    }
}
