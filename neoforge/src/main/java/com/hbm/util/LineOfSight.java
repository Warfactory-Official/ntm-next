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

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

public final class LineOfSight {
    public static int writes;
    private static final long NO_CELL = Long.MIN_VALUE;
    private static final long NOT_AIR = Long.MIN_VALUE + 1;
    private static final int AIR = 0, PASS = 1, STOP = 2;

    private LineOfSight() {}

    public static boolean blocked(
            ServerLevel level,
            SectionAccess.LoadingReader reader,
            double x,
            double y,
            double z,
            double ex,
            double ey,
            double ez) {
        return blocked(level, reader, outline(x, y, z, ex, ey, ez));
    }

    public static boolean blocked(
            ServerLevel level, SectionAccess.LoadingReader reader, ClipContext ray) {
        return walk(level, reader, ray, true, new BlockPos.MutableBlockPos()) != NO_CELL;
    }

    private static ClipContext outline(
            double x, double y, double z, double ex, double ey, double ez) {
        return new ClipContext(
                new Vec3(x, y, z),
                new Vec3(ex, ey, ez),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                CollisionContext.empty());
    }

    private static long walk(
            ServerLevel level,
            SectionAccess.LoadingReader reader,
            ClipContext ray,
            boolean clip,
            BlockPos.MutableBlockPos pos) {
        Vec3 from = ray.getFrom();
        Vec3 to = ray.getTo();
        if (from.equals(to)) return NO_CELL;

        double toX = Mth.lerp(-1.0E-7, to.x, from.x);
        double toY = Mth.lerp(-1.0E-7, to.y, from.y);
        double toZ = Mth.lerp(-1.0E-7, to.z, from.z);
        double fromX = Mth.lerp(-1.0E-7, from.x, to.x);
        double fromY = Mth.lerp(-1.0E-7, from.y, to.y);
        double fromZ = Mth.lerp(-1.0E-7, from.z, to.z);
        int x = Mth.floor(fromX);
        int y = Mth.floor(fromY);
        int z = Mth.floor(fromZ);
        int seen = visit(level, reader, ray, clip, pos.set(x, y, z));
        if (seen == STOP) return pos.asLong();

        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        int signX = Mth.sign(dx);
        int signY = Mth.sign(dy);
        int signZ = Mth.sign(dz);
        double tDeltaX = signX == 0 ? Double.MAX_VALUE : signX / dx;
        double tDeltaY = signY == 0 ? Double.MAX_VALUE : signY / dy;
        double tDeltaZ = signZ == 0 ? Double.MAX_VALUE : signZ / dz;
        double tX = tDeltaX * (signX > 0 ? 1.0 - Mth.frac(fromX) : Mth.frac(fromX));
        double tY = tDeltaY * (signY > 0 ? 1.0 - Mth.frac(fromY) : Mth.frac(fromY));
        double tZ = tDeltaZ * (signZ > 0 ? 1.0 - Mth.frac(fromZ) : Mth.frac(fromZ));

        while (tX <= 1.0 || tY <= 1.0 || tZ <= 1.0) {
            if (tX < tY) {
                if (tX < tZ) {
                    x += signX;
                    tX += tDeltaX;
                } else {
                    z += signZ;
                    tZ += tDeltaZ;
                }
            } else if (tY < tZ) {
                y += signY;
                tY += tDeltaY;
            } else {
                z += signZ;
                tZ += tDeltaZ;
            }
            int cell = visit(level, reader, ray, clip, pos.set(x, y, z));
            if (cell == STOP) return pos.asLong();
            seen |= cell;
        }
        return clip || seen == AIR ? NO_CELL : NOT_AIR;
    }

    private static int visit(
            ServerLevel level,
            SectionAccess.LoadingReader reader,
            ClipContext ray,
            boolean clip,
            BlockPos.MutableBlockPos pos) {
        BlockState state = reader.read(pos);
        if (state.isAir()) return AIR;
        if (!clip) return ray.getBlockShape(state, level, pos) == Shapes.block() ? STOP : PASS;
        Vec3 from = ray.getFrom();
        Vec3 to = ray.getTo();
        if (ray.getBlockShape(state, level, pos).clip(from, to, pos) != null) return STOP;
        return ray.getFluidShape(state.getFluidState(), level, pos).clip(from, to, pos) != null
                ? STOP
                : PASS;
    }

    public static final class Batch {
        private static final int LEAF = 8;
        private static final int MIN_CONE = 16;
        private final ServerLevel level;
        private final SectionAccess.LoadingReader reader;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private double ox, oy, oz;
        private Vec3 origin = Vec3.ZERO;
        private ClipContext.Block shape = ClipContext.Block.OUTLINE;
        private CollisionContext context = CollisionContext.empty();
        private boolean inward;
        private long[] groups = new long[256];
        private int resolvedEnd;
        private int epoch;
        private double[] xs = new double[256];
        private double[] ys = new double[256];
        private double[] zs = new double[256];
        private boolean[] blocked = new boolean[256];
        private int[] order = new int[256];
        private int size;
        private final double[] box = new double[6];
        private final double[] window = new double[2];

        public Batch(ServerLevel level, SectionAccess.LoadingReader reader) {
            this.level = level;
            this.reader = reader;
        }

        public void reset(double x, double y, double z) {
            reset(x, y, z, ClipContext.Block.OUTLINE, CollisionContext.empty(), false);
        }

        public void reset(
                double x,
                double y,
                double z,
                ClipContext.Block shape,
                CollisionContext context,
                boolean inward) {
            ox = x;
            oy = y;
            oz = z;
            origin = new Vec3(x, y, z);
            this.shape = shape;
            this.context = context;
            this.inward = inward;
            size = 0;
            resolvedEnd = 0;
        }

        public int add(double x, double y, double z, long group) {
            if (size == xs.length) {
                int capacity = size << 1;
                groups = Arrays.copyOf(groups, capacity);
                xs = Arrays.copyOf(xs, capacity);
                ys = Arrays.copyOf(ys, capacity);
                zs = Arrays.copyOf(zs, capacity);
                blocked = Arrays.copyOf(blocked, capacity);
                order = Arrays.copyOf(order, capacity);
            }
            groups[size] = group;
            xs[size] = x;
            ys[size] = y;
            zs[size] = z;
            return size++;
        }

        public boolean blocked(int index) {
            return blocked(index, xs[index], ys[index], zs[index]);
        }

        public boolean blocked(int index, double x, double y, double z) {
            if (index >= resolvedEnd || LineOfSight.writes != epoch) {
                int end = resolvedEnd;
                if (index >= end) {
                    end = index + 1;
                    while (end < size && groups[end] == groups[index]) end++;
                }
                epoch = LineOfSight.writes;
                resolve(index, end);
                resolvedEnd = end;
            }
            return x == xs[index] && y == ys[index] && z == zs[index]
                    ? blocked[index]
                    : ray(x, y, z);
        }

        public boolean blocked(double x, double y, double z) {
            return ray(x, y, z);
        }

        private void resolve(int from, int to) {
            for (int i = from; i < to; i++) order[i] = i;
            node(from, to);
        }

        private void node(int lo, int hi) {
            if (hi - lo <= LEAF) {
                for (int i = lo; i < hi; i++) single(order[i]);
                return;
            }
            bound(lo, hi);
            var centre =
                    new Vec3(
                            (box[0] + box[3]) * 0.5,
                            (box[1] + box[4]) * 0.5,
                            (box[2] + box[5]) * 0.5);
            long cell =
                    walk(
                            level,
                            reader,
                            new ClipContext(origin, centre, shape, ClipContext.Fluid.NONE, context),
                            false,
                            pos);
            if (cell == NO_CELL) {
                if (hi - lo >= MIN_CONE && allClear(hi - lo)) {
                    for (int i = lo; i < hi; i++) blocked[order[i]] = false;
                    return;
                }
            } else if (cell != NOT_AIR) {
                int bx = BlockPos.getX(cell), by = BlockPos.getY(cell), bz = BlockPos.getZ(cell);
                if (boundsCross(bx, by, bz)) {
                    for (int i = lo; i < hi; i++) blocked[order[i]] = true;
                    return;
                }
                int rest = lo;
                for (int i = lo; i < hi; i++) {
                    int e = order[i];
                    if (crossesInterior(xs[e], ys[e], zs[e], bx, by, bz)) {
                        blocked[e] = true;
                    } else {
                        order[i] = order[rest];
                        order[rest++] = e;
                    }
                }
                if (rest - lo <= LEAF) {
                    for (int i = lo; i < rest; i++) single(order[i]);
                    return;
                }
                if (rest != hi) bound(lo, rest);
                hi = rest;
            }
            int axis = 0;
            for (int a = 1; a < 3; a++) {
                if (box[a + 3] - box[a] > box[axis + 3] - box[axis]) axis = a;
            }
            if (box[axis + 3] == box[axis]) {
                for (int i = lo; i < hi; i++) single(order[i]);
                return;
            }
            double[] values = axis == 0 ? xs : axis == 1 ? ys : zs;
            double mid = (box[axis] + box[axis + 3]) * 0.5;
            int split = lo;
            for (int i = lo; i < hi; i++) {
                if (values[order[i]] < mid) {
                    int swap = order[split];
                    order[split++] = order[i];
                    order[i] = swap;
                }
            }
            node(lo, split);
            node(split, hi);
        }

        private void bound(int lo, int hi) {
            Arrays.fill(box, 0, 3, Double.POSITIVE_INFINITY);
            Arrays.fill(box, 3, 6, Double.NEGATIVE_INFINITY);
            for (int i = lo; i < hi; i++) {
                int e = order[i];
                box[0] = Math.min(box[0], xs[e]);
                box[1] = Math.min(box[1], ys[e]);
                box[2] = Math.min(box[2], zs[e]);
                box[3] = Math.max(box[3], xs[e]);
                box[4] = Math.max(box[4], ys[e]);
                box[5] = Math.max(box[5], zs[e]);
            }
        }

        private boolean boundsCross(int bx, int by, int bz) {
            double gx = Math.max(0.0, Math.max(box[0] - ox, ox - box[3]));
            double gy = Math.max(0.0, Math.max(box[1] - oy, oy - box[4]));
            double gz = Math.max(0.0, Math.max(box[2] - oz, oz - box[5]));
            if (gx * gx + gy * gy + gz * gz < 1.0E-6) return false;
            for (int corner = 0; corner < 8; corner++) {
                if (!crossesInterior(
                        box[(corner & 1) * 3],
                        box[1 + (corner >> 1 & 1) * 3],
                        box[2 + (corner >> 2) * 3],
                        bx,
                        by,
                        bz)) return false;
            }
            return true;
        }

        private void single(int e) {
            blocked[e] = ray(xs[e], ys[e], zs[e]);
        }

        private boolean ray(double x, double y, double z) {
            var end = new Vec3(x, y, z);
            return LineOfSight.blocked(
                    level,
                    reader,
                    inward
                            ? new ClipContext(end, origin, shape, ClipContext.Fluid.NONE, context)
                            : new ClipContext(origin, end, shape, ClipContext.Fluid.NONE, context));
        }

        private boolean crossesInterior(double ex, double ey, double ez, int bx, int by, int bz) {
            double dx = ex - ox, dy = ey - oy, dz = ez - oz;
            if (dx * dx + dy * dy + dz * dz < 1.0E-6) return false;
            window[0] = inward ? 0.0 : 0.001;
            window[1] = inward ? 0.999 : 1.0;
            return slab(window, ox, dx, bx) && slab(window, oy, dy, by) && slab(window, oz, dz, bz);
        }

        private static boolean slab(double[] window, double origin, double delta, int cell) {
            final double inset = 1.0E-6;
            double low = cell + inset - origin;
            double high = cell + 1 - inset - origin;
            if (delta == 0.0) return low < 0.0 && high > 0.0;
            double ta = low / delta, tb = high / delta;
            window[0] = Math.max(window[0], Math.min(ta, tb));
            window[1] = Math.min(window[1], Math.max(ta, tb));
            return window[0] < window[1];
        }

        private boolean allClear(int count) {
            double[] o = {ox, oy, oz};
            double[] lo = {box[0], box[1], box[2]};
            double[] hi = {box[3], box[4], box[5]};
            int u = 0;
            double best = -1.0;
            for (int a = 0; a < 3; a++) {
                double distance = Math.abs((lo[a] + hi[a]) * 0.5 - o[a]);
                if (distance > best) {
                    best = distance;
                    u = a;
                }
            }
            int v = (u + 1) % 3, w = (u + 2) % 3;
            int s = (lo[u] + hi[u]) * 0.5 > o[u] ? 1 : -1;
            double near = s > 0 ? lo[u] : hi[u];
            double far = s > 0 ? hi[u] : lo[u];
            if ((near - o[u]) * s < 1.0) return false;
            double length =
                    Math.sqrt(
                            sq(hi[0] - o[0], lo[0] - o[0])
                                    + sq(hi[1] - o[1], lo[1] - o[1])
                                    + sq(hi[2] - o[2], lo[2] - o[2]));
            double eps = length * 1.0E-7 + 1.0E-6;
            long budget = (long) count * (long) (Math.abs(far - o[u]) + 1) / 2;
            int k = Mth.floor(o[u] - s * eps);
            int end = Mth.floor(far + s * eps);
            int[] cell = new int[3];
            double[] range = new double[4];
            while (true) {
                double a = Math.max(Math.min(k, k + 1), Math.min(o[u], far) - eps);
                double b = Math.min(k + 1, Math.max(o[u], far) + eps);
                range[0] = range[2] = Double.POSITIVE_INFINITY;
                range[1] = range[3] = Double.NEGATIVE_INFINITY;
                section(range, o, lo, hi, u, v, w, near, far, a);
                section(range, o, lo, hi, u, v, w, near, far, b);
                if (b >= Math.min(near, far) && a <= Math.max(near, far)) {
                    include(range, lo[v], hi[v], lo[w], hi[w]);
                }
                if (o[u] >= a && o[u] <= b) include(range, o[v], o[v], o[w], o[w]);
                int v0 = Mth.floor(range[0] - eps), v1 = Mth.floor(range[1] + eps);
                int w0 = Mth.floor(range[2] - eps), w1 = Mth.floor(range[3] + eps);
                budget -= (long) (v1 - v0 + 1) * (w1 - w0 + 1);
                if (budget < 0) return false;
                cell[u] = k;
                for (int cv = v0; cv <= v1; cv++) {
                    cell[v] = cv;
                    for (int cw = w0; cw <= w1; cw++) {
                        cell[w] = cw;
                        if (!reader.read(cell[0], cell[1], cell[2]).isAir()) return false;
                    }
                }
                if (k == end) return true;
                k += s;
            }
        }

        private static double sq(double a, double b) {
            double m = Math.max(Math.abs(a), Math.abs(b));
            return m * m;
        }

        private static void section(
                double[] range,
                double[] o,
                double[] lo,
                double[] hi,
                int u,
                int v,
                int w,
                double near,
                double far,
                double at) {
            double reach = at - o[u];
            if (reach * (far - o[u]) < 0.0) return;
            double l0 = reach / (far - o[u]);
            double l1 = Math.abs(at - o[u]) < Math.abs(near - o[u]) ? reach / (near - o[u]) : 1.0;
            l0 = Math.clamp(l0, 0.0, 1.0);
            l1 = Math.clamp(l1, 0.0, 1.0);
            include(
                    range,
                    o[v] + l0 * (lo[v] - o[v]),
                    o[v] + l0 * (hi[v] - o[v]),
                    o[w] + l0 * (lo[w] - o[w]),
                    o[w] + l0 * (hi[w] - o[w]));
            include(
                    range,
                    o[v] + l1 * (lo[v] - o[v]),
                    o[v] + l1 * (hi[v] - o[v]),
                    o[w] + l1 * (lo[w] - o[w]),
                    o[w] + l1 * (hi[w] - o[w]));
        }

        private static void include(double[] range, double va, double vb, double wa, double wb) {
            range[0] = Math.min(range[0], Math.min(va, vb));
            range[1] = Math.max(range[1], Math.max(va, vb));
            range[2] = Math.min(range[2], Math.min(wa, wb));
            range[3] = Math.max(range[3], Math.max(wa, wb));
        }
    }
}
