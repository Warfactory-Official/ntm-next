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

package com.hbm.saveddata.satellites;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class RadiationBursts {

    private static final Map<ResourceKey<Level>, Track> TRACKS = new HashMap<>();

    private RadiationBursts() {}

    public enum Intensity {
        LOW(10_000, 300),
        MEDIUM(2_500, 10),
        HIGH(500, 1_200);

        private final int error;
        private final int lifetime;

        Intensity(int error, int lifetime) {
            this.error = error;
            this.lifetime = lifetime;
        }

        public int lifetime() {
            return lifetime;
        }
    }

    public record Burst(long expiresOn, Intensity intensity, int x, int z) {}

    public static void report(ServerLevel level, Intensity intensity, double x, double z) {
        report(level, intensity.lifetime, intensity, x, z);
    }

    public static void report(
            ServerLevel level, int lifetime, Intensity intensity, double x, double z) {
        RandomSource random = level.getRandom();
        double errorX = Mth.clamp(random.nextGaussian(), -1D, 1D) * intensity.error;
        double errorZ = Mth.clamp(random.nextGaussian(), -1D, 1D) * intensity.error;
        TRACKS.computeIfAbsent(level.dimension(), _ -> new Track())
                .add(
                        new Burst(
                                level.getGameTime() + lifetime,
                                intensity,
                                (int) (Mth.floor(x) + errorX),
                                (int) (Mth.floor(z) + errorZ)));
    }

    public static void collect(ServerLevel level, Collection<Burst> into) {
        var track = TRACKS.get(level.dimension());
        if (track != null) track.collect(into);
    }

    public static void expire(MinecraftServer server) {
        if (TRACKS.isEmpty()) return;
        for (ServerLevel level : server.getAllLevels()) {
            var track = TRACKS.get(level.dimension());
            if (track != null) track.expire(level.getGameTime());
        }
    }

    public static void clear() {
        TRACKS.clear();
    }

    private static final class Track {
        private static final Comparator<Node> BY_EXPIRY =
                Comparator.comparingLong(node -> node.burst.expiresOn());

        private final PriorityQueue<Node> byExpiry = new PriorityQueue<>(BY_EXPIRY);
        private @Nullable Node first;
        private @Nullable Node last;

        void add(Burst burst) {
            var node = new Node(burst);
            node.previous = last;
            if (last == null) first = node;
            else last.next = node;
            last = node;
            byExpiry.add(node);
        }

        void expire(long now) {
            for (var node = byExpiry.peek();
                    node != null && node.burst.expiresOn() < now;
                    node = byExpiry.peek()) {
                byExpiry.poll();
                unlink(node);
            }
        }

        void collect(Collection<Burst> into) {
            for (var node = first; node != null; node = node.next) into.add(node.burst);
        }

        private void unlink(Node node) {
            if (node.previous == null) first = node.next;
            else node.previous.next = node.next;
            if (node.next == null) last = node.previous;
            else node.next.previous = node.previous;
        }
    }

    private static final class Node {
        final Burst burst;
        @Nullable Node previous;
        @Nullable Node next;

        Node(Burst burst) {
            this.burst = burst;
        }
    }
}
