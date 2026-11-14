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

package com.hbm.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.ChunkPos;

public final class ChunkShapeHelper {

    private static final int[] NEIGHBOUR_X = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] NEIGHBOUR_Z = {-1, -1, -1, 0, 0, 1, 1, 1};

    private ChunkShapeHelper() {}

    public static LongOpenHashSet alongSegment(int x0, int z0, int x1, int z1, double padding) {
        var cover = new LongOpenHashSet();
        long dx = Math.abs((long) x1 - x0);
        long dz = -Math.abs((long) z1 - z0);
        int stepX = x0 < x1 ? 1 : -1;
        int stepZ = z0 < z1 ? 1 : -1;
        long error = dx + dz;
        int x = x0;
        int z = z0;
        long lastChunk = 0;
        boolean first = true;
        while (true) {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunk = ChunkPos.pack(chunkX, chunkZ);
            if (first || chunk != lastChunk) {
                first = false;
                lastChunk = chunk;
                cover.add(chunk);
                for (int i = 0; i < NEIGHBOUR_X.length; i++) {
                    int neighbourX = chunkX + NEIGHBOUR_X[i];
                    int neighbourZ = chunkZ + NEIGHBOUR_Z[i];
                    long neighbour = ChunkPos.pack(neighbourX, neighbourZ);
                    if (!cover.contains(neighbour)
                            && cornerDistance(neighbourX, neighbourZ, x0, z0, x1, z1) < padding) {
                        cover.add(neighbour);
                    }
                }
            }
            long doubled = 2 * error;
            if (doubled >= dz) {
                if (x == x1) break;
                error += dz;
                x += stepX;
            }
            if (doubled <= dx) {
                if (z == z1) break;
                error += dx;
                z += stepZ;
            }
        }
        return cover;
    }

    private static double cornerDistance(int chunkX, int chunkZ, int x0, int z0, int x1, int z1) {
        double originX = chunkX * 16.0;
        double originZ = chunkZ * 16.0;
        double least = Double.POSITIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            double qx = originX + (corner & 1) * 16;
            double qz = originZ + (corner >> 1) * 16;
            least = Math.min(least, segmentDistance(qx, qz, x0, z0, x1, z1));
        }
        return least;
    }

    private static double segmentDistance(
            double qx, double qz, double ax, double az, double bx, double bz) {
        double abX = bx - ax;
        double abZ = bz - az;
        double lengthSq = abX * abX + abZ * abZ;
        if (lengthSq == 0) return length(qx - ax, qz - az);
        double t = ((qx - ax) * abX + (qz - az) * abZ) / lengthSq;
        if (t < 0) return length(qx - ax, qz - az);
        if (t > 1) return length(qx - bx, qz - bz);
        return length(qx - (ax + t * abX), qz - (az + t * abZ));
    }

    private static double length(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }
}
