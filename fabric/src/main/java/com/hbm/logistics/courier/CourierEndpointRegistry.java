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

package com.hbm.logistics.courier;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class CourierEndpointRegistry {
    public static final int SCAN_INTERVAL = 20;
    private static final long PUBLICATION_LIFETIME = 40;
    private static final double MAX_DISTANCE_SQUARED = 24.0 * 24.0;
    private static final Map<
                    ResourceKey<Level>, Long2ObjectOpenHashMap<HashMap<BlockPos, CourierEndpoint>>>
            PUBLICATIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, VisibilityTable> VISIBILITY = new HashMap<>();
    private static long clock;
    private static int ticksUntilVisibilitySweep = SCAN_INTERVAL;

    private CourierEndpointRegistry() {}

    public static void updateEndpoint(ServerLevel level, CourierEndpoint publication) {
        var dimension = level.dimension();
        var chunks = PUBLICATIONS.get(dimension);
        if (chunks == null) {
            chunks = new Long2ObjectOpenHashMap<>();
            PUBLICATIONS.put(dimension, chunks);
        }

        var chunkKey = ChunkPos.pack(publication.position());
        var population = chunks.get(chunkKey);
        if (population == null) {
            population = new HashMap<>();
            chunks.put(chunkKey, population);
        }

        publication.publishedAt = clock;
        population.put(publication.position(), publication);
    }

    public static Map<BlockPos, CourierEndpoint> getNearbyEndpoints(
            ServerLevel level, BlockPos centre, int chunkRadius) {
        var chunks = PUBLICATIONS.get(level.dimension());
        if (chunks == null) {
            return Collections.emptyMap();
        }

        var population = new HashMap<BlockPos, CourierEndpoint>();
        int centreX = SectionPos.blockToSectionCoord(centre.getX());
        int centreZ = SectionPos.blockToSectionCoord(centre.getZ());
        int lastX = centreX + chunkRadius;
        int lastZ = centreZ + chunkRadius;
        for (int chunkX = centreX - chunkRadius; chunkX <= lastX; chunkX++) {
            for (int chunkZ = centreZ - chunkRadius; chunkZ <= lastZ; chunkZ++) {
                var chunk = chunks.get(ChunkPos.pack(chunkX, chunkZ));
                if (chunk != null) {
                    population.putAll(chunk);
                }
            }
        }
        return population;
    }

    public static boolean hasLineOfSight(ServerLevel level, BlockPos from, BlockPos to) {
        long first = from.asLong();
        long second = to.asLong();
        BlockPos start = from;
        BlockPos end = to;
        if (first > second) {
            long position = first;
            first = second;
            second = position;
            start = to;
            end = from;
        }

        var dimension = level.dimension();
        var visibility = VISIBILITY.get(dimension);
        if (visibility == null) {
            visibility = new VisibilityTable();
            VISIBILITY.put(dimension, visibility);
        }
        return visibility.answer(level, first, second, start, end, clock);
    }

    public static void preTick(MinecraftServer server) {
        clock++;
        var dimensions = PUBLICATIONS.values().iterator();
        while (dimensions.hasNext()) {
            var chunks = dimensions.next();
            var populations = chunks.long2ObjectEntrySet().fastIterator();
            while (populations.hasNext()) {
                var population = populations.next().getValue();
                var publications = population.values().iterator();
                while (publications.hasNext()) {
                    var publication = publications.next();
                    if (Long.compareUnsigned(clock - publication.publishedAt, PUBLICATION_LIFETIME)
                            > 0) {
                        publications.remove();
                    }
                }
                if (population.isEmpty()) {
                    populations.remove();
                }
            }
            if (chunks.isEmpty()) {
                dimensions.remove();
            }
        }

        ticksUntilVisibilitySweep--;
        if (ticksUntilVisibilitySweep == 0) {
            var visibility = VISIBILITY.values().iterator();
            while (visibility.hasNext()) {
                if (visibility.next().expire(clock)) {
                    visibility.remove();
                }
            }
            ticksUntilVisibilitySweep = SCAN_INTERVAL;
        }
    }

    public static void clear() {
        PUBLICATIONS.clear();
        VISIBILITY.clear();
        clock = 0;
        ticksUntilVisibilitySweep = SCAN_INTERVAL;
    }

    private static boolean collisionFree(ServerLevel level, BlockPos start, BlockPos end) {
        double dx = (double) start.getX() - end.getX();
        double dy = (double) start.getY() - end.getY();
        double dz = (double) start.getZ() - end.getZ();
        if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQUARED) {
            return false;
        }

        var context =
                new ClipContext(
                        Vec3.atCenterOf(start),
                        Vec3.atCenterOf(end),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        CollisionContext.empty());
        return level.clip(context).getType() == HitResult.Type.MISS;
    }

    private static final class VisibilityTable {
        private static final byte EMPTY = 0;
        private static final byte BLOCKED = 1;
        private static final byte CLEAR = 2;
        private static final byte DELETED = 3;
        private long[] starts = new long[16];
        private long[] ends = new long[16];
        private long[] checkedAt = new long[16];
        private byte[] answers = new byte[16];
        private int entries;
        private int occupied;

        private boolean answer(
                ServerLevel level,
                long first,
                long second,
                BlockPos start,
                BlockPos end,
                long now) {
            int index = locate(first, second);
            byte previous = answers[index];
            boolean present = previous == BLOCKED || previous == CLEAR;
            if (present && Long.compareUnsigned(now - checkedAt[index], SCAN_INTERVAL) < 0) {
                return previous == CLEAR;
            }

            boolean visible = collisionFree(level, start, end);
            if (!present) {
                if (occupied >= answers.length / 2) {
                    int capacity = answers.length;
                    if (entries >= capacity / 4) {
                        capacity *= 2;
                    }
                    resize(capacity);
                    index = locate(first, second);
                }
                if (answers[index] == EMPTY) {
                    occupied++;
                }
                entries++;
                starts[index] = first;
                ends[index] = second;
            }
            checkedAt[index] = now;
            answers[index] = visible ? CLEAR : BLOCKED;
            return visible;
        }

        private int locate(long first, long second) {
            int mask = answers.length - 1;
            int index = (int) HashCommon.mix(first ^ Long.rotateLeft(second, 32)) & mask;
            int deleted = -1;
            while (true) {
                byte answer = answers[index];
                if (answer == EMPTY) {
                    return deleted >= 0 ? deleted : index;
                }
                if (answer == DELETED) {
                    if (deleted < 0) {
                        deleted = index;
                    }
                } else if (starts[index] == first && ends[index] == second) {
                    return index;
                }
                index = (index + 1) & mask;
            }
        }

        private boolean expire(long now) {
            for (int index = 0; index < answers.length; index++) {
                byte answer = answers[index];
                if ((answer == BLOCKED || answer == CLEAR)
                        && Long.compareUnsigned(now - checkedAt[index], SCAN_INTERVAL) >= 0) {
                    answers[index] = DELETED;
                    entries--;
                }
            }
            return entries == 0;
        }

        private void resize(int capacity) {
            var oldStarts = starts;
            var oldEnds = ends;
            var oldCheckedAt = checkedAt;
            var oldAnswers = answers;
            starts = new long[capacity];
            ends = new long[capacity];
            checkedAt = new long[capacity];
            answers = new byte[capacity];
            occupied = entries;
            for (int index = 0; index < oldAnswers.length; index++) {
                byte answer = oldAnswers[index];
                if (answer == EMPTY || answer == DELETED) {
                    continue;
                }
                int destination = locate(oldStarts[index], oldEnds[index]);
                starts[destination] = oldStarts[index];
                ends[destination] = oldEnds[index];
                checkedAt[destination] = oldCheckedAt[index];
                answers[destination] = answer;
            }
        }
    }
}
