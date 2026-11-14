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

package com.hbm.client.model;

import java.util.Arrays;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

final class SurfaceLight {
    private static final int SPAN = 19;
    private static final int SOLID = 1 << 8;
    private static final float EPSILON = 1e-5F;
    private static final int[][] FACES = {
        {1, 4, 10, 13}, {0, 3, 9, 12}, {9, 12, 10, 13}, {0, 3, 1, 4}, {3, 12, 4, 13}, {0, 9, 1, 10}
    };
    private static final ThreadLocal<SurfaceLight> LOCAL =
            ThreadLocal.withInitial(SurfaceLight::new);
    private final int[] cells = new int[SPAN * SPAN * SPAN];
    private final int[] stamps = new int[cells.length];
    private final int[] around = new int[27];
    private final float[] corners = new float[16];
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private @Nullable BlockAndTintGetter region;
    private int epoch, originX, originY, originZ;
    private float block, sky;

    private SurfaceLight() {}

    static SurfaceLight open(BlockAndTintGetter region, BlockPos origin) {
        SurfaceLight light = LOCAL.get();
        light.region = region;
        light.originX = origin.getX();
        light.originY = origin.getY();
        light.originZ = origin.getZ();
        light.epoch += 2;
        if (light.epoch == 0) {
            Arrays.fill(light.stamps, 0);
            light.epoch = 2;
        }
        return light;
    }

    private static int pack(float block, float sky, int emission) {
        int floor = emission << 4;
        return Math.max(Math.min(Math.round(block * 16), 240), floor)
                | Math.max(Math.min(Math.round(sky * 16), 240), floor) << 16;
    }

    void close() {
        region = null;
    }

    boolean occludes(int x, int y, int z, Direction face) {
        int value = cell(x, y, z, false);
        if ((value & SOLID) != 0) return true;
        int known = 1 << (9 + face.ordinal()), hidden = 1 << (15 + face.ordinal());
        if ((value & known) == 0) {
            cursor.set(originX + x, originY + y, originZ + z);
            value |= known;
            var shape = region.getBlockState(cursor).getFaceOcclusionShape(face.getOpposite());
            if (!shape.isEmpty() && Block.isShapeFullBlock(shape)) {
                value |= hidden;
            }
            cells[x + 1 + (z + 1) * SPAN + (y + 1) * SPAN * SPAN] = value;
        }
        return (value & hidden) != 0;
    }

    int light(float x, float y, float z, float nx, float ny, float nz, int emission) {
        if (emission >= 15) return LightCoordsUtil.FULL_BRIGHT;
        int bx = Mth.floor(x), by = Mth.floor(y), bz = Mth.floor(z);
        if (x == bx && y == by && z == bz) {
            int cardinal =
                    nx == 1 && ny == 0 && nz == 0
                            ? 0
                            : nx == -1 && ny == 0 && nz == 0
                                    ? 1
                                    : ny == 1 && nx == 0 && nz == 0
                                            ? 2
                                            : ny == -1 && nx == 0 && nz == 0
                                                    ? 3
                                                    : nz == 1 && nx == 0 && ny == 0
                                                            ? 4
                                                            : nz == -1 && nx == 0 && ny == 0
                                                                    ? 5
                                                                    : -1;
            if (cardinal >= 0) return corner(bx, by, bz, cardinal, emission);
        }
        boolean open = false;
        for (int dy = -1, i = 0; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++, i++) {
                    int cell = cell(bx + dx, by + dy, bz + dz, true);
                    around[i] = cell;
                    open |= (cell & SOLID) == 0;
                }
            }
        }
        float length = nx * nx + ny * ny + nz * nz;
        if (!open) {
            block = sky = 0;
        } else if (length < EPSILON * EPSILON) {
            block = around[13] & 15;
            sky = around[13] >> 4 & 15;
        } else {
            float scale = 1 / (float) Math.sqrt(length);
            float fx = x - bx, fy = y - by, fz = z - bz;
            float totalBlock = 0, totalSky = 0;
            for (int axis = 0; axis < 3; axis++) {
                float n = (axis == 0 ? nx : axis == 1 ? ny : nz) * scale;
                if (n <= EPSILON && n >= -EPSILON) continue;
                direction(fx, fy, fz, FACES[axis * 2 + (n > 0 ? 0 : 1)]);
                totalBlock += block * n * n;
                totalSky += sky * n * n;
            }
            block = totalBlock;
            sky = totalSky;
        }
        return pack(block, sky, emission);
    }

    private int corner(int x, int y, int z, int cardinal, int emission) {
        int solid = 0, totalBlock = 0, totalSky = 0;
        for (int offset : FACES[cardinal]) {
            int value = cell(x + offset % 3 - 1, y + offset / 9 - 1, z + offset / 3 % 3 - 1, true);
            solid += value & SOLID;
            totalBlock += value & 15;
            totalSky += value >> 4 & 15;
        }
        int valid = 4 - (solid >> 8);
        float normalizer = valid == 0 ? 0 : 1F / valid;
        return pack(totalBlock * normalizer, totalSky * normalizer, emission);
    }

    private void direction(float fx, float fy, float fz, int[] face) {
        for (int k = 0; k < 8; k++) {
            int corner = (k & 1) + (k >> 1 & 1) * 3 + (k >> 2) * 9;
            int a = around[face[0] + corner], b = around[face[1] + corner];
            int c = around[face[2] + corner], d = around[face[3] + corner];
            int valid = 4 - ((a & SOLID) + (b & SOLID) + (c & SOLID) + (d & SOLID) >> 8);
            float normalizer = valid == 0 ? 0 : 1F / valid;
            corners[k] = ((a & 15) + (b & 15) + (c & 15) + (d & 15)) * normalizer;
            corners[k + 8] =
                    ((a >> 4 & 15) + (b >> 4 & 15) + (c >> 4 & 15) + (d >> 4 & 15)) * normalizer;
        }
        block = trilinear(0, fx, fy, fz);
        sky = trilinear(8, fx, fy, fz);
    }

    private float trilinear(int at, float fx, float fy, float fz) {
        float c00 = Mth.lerp(fx, corners[at], corners[at + 1]),
                c01 = Mth.lerp(fx, corners[at + 2], corners[at + 3]);
        float c10 = Mth.lerp(fx, corners[at + 4], corners[at + 5]),
                c11 = Mth.lerp(fx, corners[at + 6], corners[at + 7]);
        return Mth.lerp(fy, Mth.lerp(fz, c00, c01), Mth.lerp(fz, c10, c11));
    }

    private int cell(int x, int y, int z, boolean lighting) {
        int index = x + 1 + (z + 1) * SPAN + (y + 1) * SPAN * SPAN;
        int ready = epoch | 1, stamp = stamps[index];
        if (stamp == ready || stamp == epoch && !lighting) return cells[index];
        cursor.set(originX + x, originY + y, originZ + z);
        var view = region;
        int cell =
                stamp == epoch
                        ? cells[index]
                        : view.getBlockState(cursor).isSolidRender() ? SOLID : 0;
        if (lighting)
            cell |=
                    view.getBrightness(LightLayer.BLOCK, cursor)
                            | view.getBrightness(LightLayer.SKY, cursor) << 4;
        stamps[index] = lighting ? ready : epoch;
        return cells[index] = cell;
    }
}
