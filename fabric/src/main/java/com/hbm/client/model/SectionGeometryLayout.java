// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.SectionPos;

public final class SectionGeometryLayout {
    private static final int STRIDE = 7;
    final long[] sections;
    private final int[] wholeStarts;
    private final int[] wholeQuads;
    private final int[] cutStarts;
    private final int[] cutQuads;
    private final float[] vertices;

    SectionGeometryLayout(float[] mesh, int placement) {
        int px = placement & 15, py = placement >> 4 & 15, pz = placement >> 8 & 15;
        var parts = new Long2ObjectOpenHashMap<Builder>();
        var clip = new Clip();
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
                parts.computeIfAbsent(SectionPos.asLong(x0, y0, z0), key -> new Builder())
                        .whole
                        .add(q);
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
                        Builder part =
                                parts.computeIfAbsent(
                                        SectionPos.asLong(x, y, z), key -> new Builder());
                        for (int i = 0; i < clip.output.size() / (4 * STRIDE); i++) part.cut.add(q);
                        part.vertices.addAll(clip.output);
                    }
                }
            }
        }
        sections = parts.keySet().toLongArray();
        Arrays.sort(sections);
        wholeStarts = new int[sections.length + 1];
        cutStarts = new int[sections.length + 1];
        var whole = new IntArrayList();
        var cut = new IntArrayList();
        var points = new FloatArrayList();
        for (int i = 0; i < sections.length; i++) {
            Builder part = parts.get(sections[i]);
            whole.addAll(part.whole);
            cut.addAll(part.cut);
            points.addAll(part.vertices);
            wholeStarts[i + 1] = whole.size();
            cutStarts[i + 1] = cut.size();
        }
        wholeQuads = whole.toIntArray();
        cutQuads = cut.toIntArray();
        vertices = points.toFloatArray();
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
                                + vertices.length);
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
            VertexConsumer consumer = output.apply(SectionGeometry.LAYERS[layers[q]]);
            for (int v = 0; v < 4; v++) {
                int at = (i * 4 + v) * STRIDE;
                int color =
                        interpolate(colors, q * 4, at + 3, 0, 255)
                                | interpolate(colors, q * 4, at + 3, 8, 255) << 8
                                | interpolate(colors, q * 4, at + 3, 16, 255) << 16
                                | interpolate(colors, q * 4, at + 3, 24, 255) << 24;
                float px = vertices[at] + x, py = vertices[at + 1] + y, pz = vertices[at + 2] + z;
                float nx = interpolateAttribute(normals, q * 12, 3, at + 3);
                float ny = interpolateAttribute(normals, q * 12 + 1, 3, at + 3);
                float nz = interpolateAttribute(normals, q * 12 + 2, 3, at + 3);
                int packedLight =
                        sampled[q] >= 0
                                ? surface.light(
                                        px, py, pz, nx, ny, nz, Math.max(sampled[q], emission))
                                : interpolate(light, q * 4, at + 3, 0, 65535)
                                        | interpolate(light, q * 4, at + 3, 16, 65535) << 16;
                consumer.addVertex(
                        px,
                        py,
                        pz,
                        color,
                        interpolateAttribute(mesh, q * 20 + 3, 5, at + 3),
                        interpolateAttribute(mesh, q * 20 + 4, 5, at + 3),
                        0,
                        packedLight,
                        nx,
                        ny,
                        nz);
            }
        }
    }

    private float interpolateAttribute(float[] data, int source, int stride, int weights) {
        float value = 0;
        for (int i = 0; i < 4; i++) value += data[source + i * stride] * vertices[weights + i];
        return value;
    }

    private int interpolate(int[] values, int source, int weights, int shift, int mask) {
        float value = 0;
        for (int i = 0; i < 4; i++)
            value += (values[source + i] >>> shift & mask) * vertices[weights + i];
        return Math.round(value);
    }

    private static final class Builder {
        final IntArrayList whole = new IntArrayList();
        final IntArrayList cut = new IntArrayList();
        final FloatArrayList vertices = new FloatArrayList();
    }

    private static final class Clip {
        final float[] a = new float[12 * STRIDE];
        final float[] b = new float[12 * STRIDE];
        final float[] bounds = new float[6];
        final FloatArrayList output = new FloatArrayList();

        void bounds(int x, int y, int z) {
            bounds[0] = x;
            bounds[1] = y;
            bounds[2] = z;
            bounds[3] = x + 16;
            bounds[4] = y + 16;
            bounds[5] = z + 16;
        }

        void triangle(float[] mesh, int base, int p, int q, int r) {
            vertex(mesh, base, p, 0);
            vertex(mesh, base, q, STRIDE);
            vertex(mesh, base, r, STRIDE * 2);
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

        private void vertex(float[] mesh, int base, int v, int out) {
            System.arraycopy(mesh, base + v * 5, a, out, 3);
            for (int i = 0; i < 4; i++) a[out + 3 + i] = i == v ? 1 : 0;
        }
    }
}
