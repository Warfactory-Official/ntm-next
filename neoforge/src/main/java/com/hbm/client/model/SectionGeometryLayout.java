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

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

public final class SectionGeometryLayout {
    private static final int STRIDE = 7;
    private static final ThreadLocal<Partial> PARTIAL = ThreadLocal.withInitial(Partial::new);
    private static final ThreadLocal<BuildScratch> BUILD =
            ThreadLocal.withInitial(BuildScratch::new);
    private static final WeakHashMap<float[], WeakReference<Culls>> ORIGINALS = new WeakHashMap<>();
    final long[] sections;
    private final int[] wholeStarts;
    private final int[] wholeQuads;
    private final int[] cutStarts;
    private final int[] cutQuads;
    private final float[] vertices;
    final Culls originalCulls;
    private final Culls cutCulls;

    SectionGeometryLayout(float[] mesh, int placement) {
        int px = placement & 15, py = placement >> 4 & 15, pz = placement >> 8 & 15;
        var scratch = BUILD.get();
        var parts = scratch.parts;
        var clip = scratch.clip;
        try {
            for (int q = 0; q < mesh.length / 20; q++) {
                int base = q * 20;
                float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
                float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
                for (int v = 0; v < 4; v++) {
                    int at = base + v * 5;
                    minX = Math.min(minX, mesh[at]);
                    maxX = Math.max(maxX, mesh[at]);
                    minY = Math.min(minY, mesh[at + 1]);
                    maxY = Math.max(maxY, mesh[at + 1]);
                    minZ = Math.min(minZ, mesh[at + 2]);
                    maxZ = Math.max(maxZ, mesh[at + 2]);
                }
                int x0 = section(minX, px), x1 = section(maxX, px);
                int y0 = section(minY, py), y1 = section(maxY, py);
                int z0 = section(minZ, pz), z1 = section(maxZ, pz);
                if (x0 == x1 && y0 == y1 && z0 == z1) {
                    scratch.part(SectionPos.asLong(x0, y0, z0)).whole.add(q);
                    continue;
                }
                for (int z = z0; z <= z1; z++) {
                    for (int y = y0; y <= y1; y++) {
                        for (int x = x0; x <= x1; x++) {
                            clip.bounds(x * 16 - px, y * 16 - py, z * 16 - pz);
                            clip.output.clear();
                            clip.triangle(mesh, base, 0, 1, 2);
                            clip.triangle(mesh, base, 2, 3, 0);
                            if (clip.output.isEmpty()) continue;
                            Builder part = scratch.part(SectionPos.asLong(x, y, z));
                            for (int i = 0; i < clip.output.size() / (4 * STRIDE); i++)
                                part.cut.add(q);
                            part.vertices.addAll(clip.output);
                        }
                    }
                }
            }
            sections = parts.keySet().toLongArray();
            Arrays.sort(sections);
            wholeStarts = new int[sections.length + 1];
            cutStarts = new int[sections.length + 1];
            for (int i = 0; i < sections.length; i++) {
                var part = parts.get(sections[i]);
                wholeStarts[i + 1] = wholeStarts[i] + part.whole.size();
                cutStarts[i + 1] = cutStarts[i] + part.cut.size();
            }
            wholeQuads = new int[wholeStarts[sections.length]];
            cutQuads = new int[cutStarts[sections.length]];
            vertices = new float[cutQuads.length * 4 * STRIDE];
            for (int i = 0; i < sections.length; i++) {
                var part = parts.get(sections[i]);
                part.whole.getElements(0, wholeQuads, wholeStarts[i], part.whole.size());
                part.cut.getElements(0, cutQuads, cutStarts[i], part.cut.size());
                part.vertices.getElements(
                        0, vertices, cutStarts[i] * 4 * STRIDE, part.vertices.size());
            }
            originalCulls = originals(mesh, scratch);
            var culls = scratch.culls;
            int[] indices = new int[cutQuads.length];
            for (int i = 0; i < indices.length; i++)
                indices[i] = culls.add(vertices, i * 4 * STRIDE, STRIDE, clip);
            cutCulls = culls.finish(indices);
        } finally {
            scratch.clear();
        }
    }

    private static Culls originals(float[] mesh, BuildScratch scratch) {
        synchronized (ORIGINALS) {
            var reference = ORIGINALS.get(mesh);
            var known = reference == null ? null : reference.get();
            if (known != null) return known;
            int[] indices = new int[mesh.length / 20];
            for (int q = 0; q < indices.length; q++)
                indices[q] = scratch.culls.add(mesh, q * 20, 5, scratch.clip);
            var result = scratch.culls.finish(indices);
            scratch.culls.clear();
            ORIGINALS.put(mesh, new WeakReference<>(result));
            return result;
        }
    }

    static void clearOriginals() {
        synchronized (ORIGINALS) {
            ORIGINALS.clear();
        }
    }

    static final class Culls {
        final int[] indices, starts, vertexStarts;
        final byte[] faces;
        final long[] cells;
        final float[] vertices;

        Culls(CullBuilder builder, int[] indices) {
            this.indices = indices;
            starts = builder.starts.toIntArray();
            faces = builder.faces.toByteArray();
            cells = builder.cells.toLongArray();
            vertexStarts = builder.vertexStarts.toIntArray();
            vertices = builder.vertices.toFloatArray();
        }

        long bytes() {
            return 4L * (indices.length + starts.length + vertexStarts.length + vertices.length)
                    + faces.length
                    + 8L * cells.length;
        }
    }

    private static final class BuildScratch {
        final Long2ObjectOpenHashMap<Builder> parts = new Long2ObjectOpenHashMap<>();
        final ArrayList<Builder> pool = new ArrayList<>();
        final Clip clip = new Clip();
        final CullBuilder culls = new CullBuilder();
        int used;

        Builder part(long section) {
            var part = parts.get(section);
            if (part == null) {
                if (used == pool.size()) pool.add(new Builder());
                part = pool.get(used++);
                parts.put(section, part);
            }
            return part;
        }

        void clear() {
            for (int i = 0; i < used; i++) {
                var part = pool.get(i);
                part.whole.clear();
                part.cut.clear();
                part.vertices.clear();
            }
            used = 0;
            parts.clear();
            culls.clear();
            clip.output.clear();
        }
    }

    private static int section(float coordinate, int placement) {
        return (int) Math.floor(((double) coordinate + placement) / 16);
    }

    long bytes() {
        return 8L * sections.length
                + 4L
                        * (wholeStarts.length
                                + wholeQuads.length
                                + cutStarts.length
                                + cutQuads.length
                                + vertices.length)
                + cutCulls.bytes();
    }

    void emit(
            int part,
            SectionGeometry.Prepared prepared,
            int emission,
            SurfaceLight surface,
            int x,
            int y,
            int z,
            Function<ChunkSectionLayer, VertexConsumer> output) {
        float[] mesh = prepared.mesh().vertices(), normals = prepared.mesh().normals();
        byte[] layers = prepared.mesh().layers(), sampled = prepared.mesh().surface();
        int[] colors = prepared.lit().colors(), light = prepared.lit().light();
        for (int i = wholeStarts[part]; i < wholeStarts[part + 1]; i++) {
            int q = wholeQuads[i];
            if (emitCulled(
                    originalCulls,
                    originalCulls.indices[q],
                    q,
                    mesh,
                    q * 20,
                    5,
                    prepared,
                    emission,
                    surface,
                    x,
                    y,
                    z,
                    output)) continue;
            VertexConsumer consumer = output.apply(SectionGeometry.LAYERS[layers[q]]);
            for (int v = 0; v < 4; v++) {
                int at = q * 20 + v * 5;
                int normal = (q * 4 + v) * 3;
                float px = mesh[at] + x, py = mesh[at + 1] + y, pz = mesh[at + 2] + z;
                float nx = normals[normal], ny = normals[normal + 1], nz = normals[normal + 2];
                int packedLight =
                        sampled[q] < 0
                                ? light[q * 4 + v]
                                : surface.light(
                                        px, py, pz, nx, ny, nz, Math.max(sampled[q], emission));
                consumer.addVertex(
                        px,
                        py,
                        pz,
                        colors[q * 4 + v],
                        mesh[at + 3],
                        mesh[at + 4],
                        0,
                        packedLight,
                        nx,
                        ny,
                        nz);
            }
        }
        for (int i = cutStarts[part]; i < cutStarts[part + 1]; i++) {
            int q = cutQuads[i];
            if (emitCulled(
                    cutCulls,
                    cutCulls.indices[i],
                    q,
                    vertices,
                    i * 4 * STRIDE,
                    STRIDE,
                    prepared,
                    emission,
                    surface,
                    x,
                    y,
                    z,
                    output)) continue;
            emitCut(vertices, i * 4 * STRIDE, q, prepared, emission, surface, x, y, z, output);
        }
    }

    private boolean emitCulled(
            Culls culls,
            int cull,
            int q,
            float[] data,
            int base,
            int stride,
            SectionGeometry.Prepared prepared,
            int emission,
            SurfaceLight surface,
            int x,
            int y,
            int z,
            Function<ChunkSectionLayer, VertexConsumer> output) {
        if (cull < 0) return false;
        var face = Direction.VALUES[culls.faces[cull]];
        int start = culls.starts[cull], end = culls.starts[cull + 1], hidden = 0;
        for (int i = start; i < end; i++) {
            long cell = culls.cells[i];
            if (surface.occludes(
                    BlockPos.getX(cell) + x,
                    BlockPos.getY(cell) + y,
                    BlockPos.getZ(cell) + z,
                    face)) hidden++;
        }
        if (hidden == 0) return false;
        if (hidden != end - start) {
            if (hidden * 4 <= end - start && end - start >= 8) {
                var partial = PARTIAL.get();
                if (partial.prepare(culls.cells, start, end, face, surface, x, y, z)) {
                    for (int i = 0; i < partial.rectangles.size(); i += 4) {
                        partial.clip.bounds(
                                partial.axis,
                                partial.plane,
                                partial.rectangles.getInt(i),
                                partial.rectangles.getInt(i + 1),
                                partial.rectangles.getInt(i + 2),
                                partial.rectangles.getInt(i + 3));
                        partial.clip.output.clear();
                        partial.clip.triangle(data, base, stride, 0, 1, 2);
                        partial.clip.triangle(data, base, stride, 2, 3, 0);
                        for (int at = 0; at < partial.clip.output.size(); at += 4 * STRIDE) {
                            emitCut(
                                    partial.clip.output.elements(),
                                    at,
                                    q,
                                    prepared,
                                    emission,
                                    surface,
                                    x,
                                    y,
                                    z,
                                    output);
                        }
                    }
                    return true;
                }
            }
            for (int i = start; i < end; i++) {
                long cell = culls.cells[i];
                if (surface.occludes(
                        BlockPos.getX(cell) + x,
                        BlockPos.getY(cell) + y,
                        BlockPos.getZ(cell) + z,
                        face)) continue;
                for (int at = culls.vertexStarts[i];
                        at < culls.vertexStarts[i + 1];
                        at += 4 * STRIDE) {
                    emitCut(culls.vertices, at, q, prepared, emission, surface, x, y, z, output);
                }
            }
        }
        return true;
    }

    private static void emitCut(
            float[] points,
            int base,
            int q,
            SectionGeometry.Prepared prepared,
            int emission,
            SurfaceLight surface,
            int x,
            int y,
            int z,
            Function<ChunkSectionLayer, VertexConsumer> output) {
        float[] mesh = prepared.mesh().vertices(), normals = prepared.mesh().normals();
        int[] colors = prepared.lit().colors(), light = prepared.lit().light();
        byte sampled = prepared.mesh().surface()[q];
        VertexConsumer consumer = output.apply(SectionGeometry.LAYERS[prepared.mesh().layers()[q]]);
        for (int v = 0; v < 3; v++) {
            int at = base + v * STRIDE;
            int color =
                    interpolate(points, colors, q * 4, at + 3, 0, 255)
                            | interpolate(points, colors, q * 4, at + 3, 8, 255) << 8
                            | interpolate(points, colors, q * 4, at + 3, 16, 255) << 16
                            | interpolate(points, colors, q * 4, at + 3, 24, 255) << 24;
            float px = points[at] + x, py = points[at + 1] + y, pz = points[at + 2] + z;
            float nx = interpolateAttribute(points, normals, q * 12, 3, at + 3);
            float ny = interpolateAttribute(points, normals, q * 12 + 1, 3, at + 3);
            float nz = interpolateAttribute(points, normals, q * 12 + 2, 3, at + 3);
            int packedLight =
                    sampled >= 0
                            ? surface.light(px, py, pz, nx, ny, nz, Math.max(sampled, emission))
                            : interpolate(points, light, q * 4, at + 3, 0, 65535)
                                    | interpolate(points, light, q * 4, at + 3, 16, 65535) << 16;
            float u = interpolateAttribute(points, mesh, q * 20 + 3, 5, at + 3);
            float textureV = interpolateAttribute(points, mesh, q * 20 + 4, 5, at + 3);
            for (int copy = 0; copy < (v == 2 ? 2 : 1); copy++) {
                consumer.addVertex(px, py, pz, color, u, textureV, 0, packedLight, nx, ny, nz);
            }
        }
    }

    private static float interpolateAttribute(
            float[] points, float[] data, int source, int stride, int weights) {
        float value = 0;
        for (int i = 0; i < 4; i++) value += data[source + i * stride] * points[weights + i];
        return value;
    }

    private static int interpolate(
            float[] points, int[] values, int source, int weights, int shift, int mask) {
        float value = 0;
        for (int i = 0; i < 4; i++)
            value += (values[source + i] >>> shift & mask) * points[weights + i];
        return Math.round(value);
    }

    private static final class Partial {
        final Clip clip = new Clip();
        final int[] rows = new int[32];
        final IntArrayList rectangles = new IntArrayList();
        int axis, plane;

        boolean prepare(
                long[] cells,
                int start,
                int end,
                Direction face,
                SurfaceLight surface,
                int x,
                int y,
                int z) {
            axis = face.getAxis().ordinal();
            plane =
                    coordinate(cells[start], axis)
                            + (face.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1 : 0);
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            int minU = Integer.MAX_VALUE, minV = minU, maxU = Integer.MIN_VALUE, maxV = maxU;
            for (int i = start; i < end; i++) {
                minU = Math.min(minU, coordinate(cells[i], u));
                maxU = Math.max(maxU, coordinate(cells[i], u));
                minV = Math.min(minV, coordinate(cells[i], v));
                maxV = Math.max(maxV, coordinate(cells[i], v));
            }
            Arrays.fill(rows, 0);
            int full = -1 >>> (31 - (maxU - minU));
            Arrays.fill(rows, 0, maxV - minV + 1, full);
            rectangles.clear();
            int visible = 0;
            for (int i = start; i < end; i++) {
                long cell = cells[i];
                if (surface.occludes(
                        BlockPos.getX(cell) + x,
                        BlockPos.getY(cell) + y,
                        BlockPos.getZ(cell) + z,
                        face)) {
                    rows[coordinate(cell, v) - minV] &= ~(1 << (coordinate(cell, u) - minU));
                } else visible++;
            }
            for (int row = 0; row <= maxV - minV; row++) {
                while (rows[row] != 0) {
                    int first = Integer.numberOfTrailingZeros(rows[row]);
                    int width = Integer.numberOfTrailingZeros(~(rows[row] >>> first));
                    int mask = (-1 >>> (32 - width)) << first, height = 1;
                    while (row + height <= maxV - minV && (rows[row + height] & mask) == mask)
                        height++;
                    for (int j = 0; j < height; j++) rows[row + j] &= ~mask;
                    rectangles.add(minU + first);
                    rectangles.add(minV + row);
                    rectangles.add(width);
                    rectangles.add(height);
                    if (rectangles.size() / 4 * 2 > visible) return false;
                }
            }
            return true;
        }

        private static int coordinate(long cell, int axis) {
            return axis == 0
                    ? BlockPos.getX(cell)
                    : axis == 1 ? BlockPos.getY(cell) : BlockPos.getZ(cell);
        }
    }

    private static final class Builder {
        final IntArrayList whole = new IntArrayList();
        final IntArrayList cut = new IntArrayList();
        final FloatArrayList vertices = new FloatArrayList();
    }

    private static final class CullBuilder {
        final Long2ObjectOpenHashMap<int[]> singleCells = new Long2ObjectOpenHashMap<>(4);
        final int[] pos = new int[3];
        final IntArrayList starts = new IntArrayList(new int[] {0});
        final ByteArrayList faces = new ByteArrayList();
        final LongArrayList cells = new LongArrayList();
        final IntArrayList vertexStarts = new IntArrayList(new int[] {0});
        final FloatArrayList vertices = new FloatArrayList();

        Culls finish(int[] indices) {
            return new Culls(this, indices);
        }

        void clear() {
            singleCells.clear();
            starts.clear();
            starts.add(0);
            faces.clear();
            cells.clear();
            vertexStarts.clear();
            vertexStarts.add(0);
            vertices.clear();
        }

        int add(float[] data, int base, int stride, Clip clip) {
            for (int axis = 0; axis < 3; axis++) {
                float plane = data[base + axis];
                if (plane != Math.floor(plane)
                        || data[base + stride + axis] != plane
                        || data[base + stride * 2 + axis] != plane
                        || data[base + stride * 3 + axis] != plane) continue;
                int u = (axis + 1) % 3, v = (axis + 2) % 3;
                float cross =
                        (data[base + stride + u] - data[base + u])
                                        * (data[base + stride * 2 + v] - data[base + v])
                                - (data[base + stride + v] - data[base + v])
                                        * (data[base + stride * 2 + u] - data[base + u]);
                if (cross == 0) continue;
                var face =
                        Direction.fromAxisAndDirection(
                                Direction.Axis.VALUES[axis],
                                cross > 0
                                        ? Direction.AxisDirection.POSITIVE
                                        : Direction.AxisDirection.NEGATIVE);
                float minU = Float.POSITIVE_INFINITY,
                        minV = minU,
                        maxU = Float.NEGATIVE_INFINITY,
                        maxV = maxU;
                for (int i = 0; i < 4; i++) {
                    minU = Math.min(minU, data[base + i * stride + u]);
                    minV = Math.min(minV, data[base + i * stride + v]);
                    maxU = Math.max(maxU, data[base + i * stride + u]);
                    maxV = Math.max(maxV, data[base + i * stride + v]);
                }
                pos[axis] = (int) plane;
                int u0 = (int) Math.floor(minU), u1 = (int) Math.ceil(maxU);
                int v0 = (int) Math.floor(minV), v1 = (int) Math.ceil(maxV);
                int firstCell = cells.size(), firstVertex = vertices.size();
                if (u1 == u0 + 1 && v1 == v0 + 1) {
                    pos[u] = u0;
                    pos[v] = v0;
                    long position = position(pos, axis, cross < 0);
                    var known = singleCells.get(position);
                    int facing = face.ordinal();
                    if (known != null && known[facing] != 0) return known[facing] - 1;
                    if (known == null) {
                        known = new int[Direction.VALUES.length];
                        singleCells.put(position, known);
                    }
                    known[facing] = faces.size() + 1;
                    cell(position);
                } else
                    for (int j = v0; j < v1; j++) {
                        for (int i = u0; i < u1; i++) {
                            pos[u] = i;
                            pos[v] = j;
                            clip.bounds(pos[0], pos[1], pos[2], 1);
                            clip.output.clear();
                            clip.triangle(data, base, stride, 0, 1, 2);
                            clip.triangle(data, base, stride, 2, 3, 0);
                            if (clip.output.isEmpty()) continue;
                            vertices.addAll(clip.output);
                            cell(position(pos, axis, cross < 0));
                        }
                    }
                if (cells.size() == firstCell + 1) {
                    vertices.size(firstVertex);
                    vertexStarts.set(vertexStarts.size() - 1, firstVertex);
                }
                int index = faces.size();
                faces.add((byte) face.ordinal());
                starts.add(cells.size());
                return index;
            }
            return -1;
        }

        private static long position(int[] pos, int axis, boolean negative) {
            return BlockPos.asLong(
                    pos[0] - (axis == 0 && negative ? 1 : 0),
                    pos[1] - (axis == 1 && negative ? 1 : 0),
                    pos[2] - (axis == 2 && negative ? 1 : 0));
        }

        private void cell(long position) {
            cells.add(position);
            vertexStarts.add(vertices.size());
        }
    }

    private static final class Clip {
        final float[] a = new float[12 * STRIDE];
        final float[] b = new float[12 * STRIDE];
        final float[] bounds = new float[6];
        final FloatArrayList output = new FloatArrayList();

        void bounds(int x, int y, int z) {
            bounds(x, y, z, 16);
        }

        void bounds(int x, int y, int z, int size) {
            bounds[0] = x;
            bounds[1] = y;
            bounds[2] = z;
            bounds[3] = x + size;
            bounds[4] = y + size;
            bounds[5] = z + size;
        }

        void bounds(int axis, int plane, int minU, int minV, int width, int height) {
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            bounds[axis] = plane;
            bounds[axis + 3] = plane + 1;
            bounds[u] = minU;
            bounds[u + 3] = minU + width;
            bounds[v] = minV;
            bounds[v + 3] = minV + height;
        }

        void triangle(float[] mesh, int base, int p, int q, int r) {
            triangle(mesh, base, 5, p, q, r);
        }

        void triangle(float[] mesh, int base, int stride, int p, int q, int r) {
            vertex(mesh, base, stride, p, 0);
            vertex(mesh, base, stride, q, STRIDE);
            vertex(mesh, base, stride, r, STRIDE * 2);
            for (int axis = 0; axis < 3; axis++) {
                if (a[axis] == bounds[axis + 3]
                        && a[STRIDE + axis] == bounds[axis + 3]
                        && a[2 * STRIDE + axis] == bounds[axis + 3]) return;
            }
            int count = 3;
            float[] in = a, out = b;
            for (int plane = 0; plane < 6 && count != 0; plane++) {
                int axis = plane % 3, produced = 0, previous = (count - 1) * STRIDE;
                float edge = bounds[plane];
                float prevDistance =
                        plane < 3 ? in[previous + axis] - edge : edge - in[previous + axis];
                for (int i = 0; i < count; i++) {
                    int current = i * STRIDE;
                    float distance =
                            plane < 3 ? in[current + axis] - edge : edge - in[current + axis];
                    if ((distance >= 0) != (prevDistance >= 0)) {
                        float t = prevDistance / (prevDistance - distance);
                        int dst = produced++ * STRIDE;
                        for (int component = 0; component < STRIDE; component++) {
                            out[dst + component] =
                                    in[previous + component]
                                            + t
                                                    * (in[current + component]
                                                            - in[previous + component]);
                        }
                        out[dst + axis] = edge;
                    }
                    if (distance >= 0)
                        System.arraycopy(in, current, out, produced++ * STRIDE, STRIDE);
                    previous = current;
                    prevDistance = distance;
                }
                count = produced;
                float[] swap = in;
                in = out;
                out = swap;
            }
            for (int i = 1; i + 1 < count; i++) {
                int first = i * STRIDE, second = (i + 1) * STRIDE;
                float ax = in[first] - in[0],
                        ay = in[first + 1] - in[1],
                        az = in[first + 2] - in[2];
                float bx = in[second] - in[0],
                        by = in[second + 1] - in[1],
                        bz = in[second + 2] - in[2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                if (nx * nx + ny * ny + nz * nz == 0) continue;
                output.addElements(output.size(), in, 0, STRIDE);
                output.addElements(output.size(), in, first, STRIDE);
                output.addElements(output.size(), in, second, STRIDE);
                output.addElements(output.size(), in, second, STRIDE);
            }
        }

        private void vertex(float[] mesh, int base, int stride, int v, int out) {
            System.arraycopy(mesh, base + v * stride, a, out, 3);
            if (stride == STRIDE) System.arraycopy(mesh, base + v * stride + 3, a, out + 3, 4);
            else for (int i = 0; i < 4; i++) a[out + 3 + i] = i == v ? 1 : 0;
        }
    }
}
