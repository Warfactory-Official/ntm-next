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

package com.hbm.entity.mob.botprime;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

final class WormTypes {

    private static final Map<ServerLevel, Int2ObjectMap<Worm>> LEVELS = new WeakHashMap<>();

    private static final class Worm {
        final IntArrayList heads = new IntArrayList(1);
        final Int2ObjectMap<IntArrayList> parts = new Int2ObjectOpenHashMap<>();

        boolean isEmpty() {
            return heads.isEmpty() && parts.isEmpty();
        }
    }

    private WormTypes() {}

    static void enlist(ServerLevel level, WormBaseNTEntity segment) {
        var worm =
                LEVELS.computeIfAbsent(
                                level, _ -> new Int2ObjectOpenHashMap<>())
                        .computeIfAbsent(
                                segment.getHeadID(), _ -> new Worm());
        var ids =
                segment.getIsHead()
                        ? worm.heads
                        : worm.parts.computeIfAbsent(
                                segment.getPartNumber(),
                                _ -> new IntArrayList(1));
        ids.add(segment.getId());
    }

    static void discharge(ServerLevel level, WormBaseNTEntity segment) {
        var worms = LEVELS.get(level);
        var worm = worms.get(segment.getHeadID());
        if (segment.getIsHead()) {
            worm.heads.rem(segment.getId());
        } else {
            var ids = worm.parts.get(segment.getPartNumber());
            ids.rem(segment.getId());
            if (ids.isEmpty()) worm.parts.remove(segment.getPartNumber());
        }
        if (worm.isEmpty()) worms.remove(segment.getHeadID());
        if (worms.isEmpty()) LEVELS.remove(level);
    }

    static @Nullable WormBaseNTEntity head(ServerLevel level, int headId, AABB reach) {
        var worm = worm(level, headId);
        return worm == null ? null : latest(level, worm.heads, reach);
    }

    static @Nullable WormBaseNTEntity part(ServerLevel level, int headId, int part, AABB reach) {
        var worm = worm(level, headId);
        return worm == null ? null : latest(level, worm.parts.get(part), reach);
    }

    private static @Nullable Worm worm(ServerLevel level, int headId) {
        var worms = LEVELS.get(level);
        return worms == null ? null : worms.get(headId);
    }

    private static @Nullable WormBaseNTEntity latest(
            ServerLevel level, @Nullable IntArrayList ids, AABB reach) {
        if (ids == null) return null;
        for (int i = ids.size() - 1; i >= 0; i--) {
            if (level.getEntity(ids.getInt(i)) instanceof WormBaseNTEntity segment
                    && segment.getBoundingBox().intersects(reach)) {
                return segment;
            }
        }
        return null;
    }
}
