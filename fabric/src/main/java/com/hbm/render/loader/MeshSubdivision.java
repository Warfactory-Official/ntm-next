// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.Arrays;

public final class MeshSubdivision {
    private MeshSubdivision() {}

    public static float[] refine(float[] source, int stride) {
        FloatArrayList output = null;
        for (int base = 0; base < source.length; base += stride * 4) {
            if (span(source, base, stride, 4) <= 1) {
                if (output != null) output.addElements(output.size(), source, base, stride * 4);
                continue;
            }
            if (output == null) {
                output = new FloatArrayList(source.length);
                output.addElements(0, source, 0, base);
            }
            polygon(corners(source, base, stride, 0, 1, 2), stride, output);
            polygon(corners(source, base, stride, 2, 3, 0), stride, output);
        }
        return output == null ? source : output.toFloatArray();
    }

    private static float span(float[] source, int base, int stride, int count) {
        float extent = 0;
        for (int axis = 0; axis < 3; axis++) {
            float min = source[base + axis], max = min;
            for (int v = 1; v < count; v++) {
                min = Math.min(min, source[base + v * stride + axis]);
                max = Math.max(max, source[base + v * stride + axis]);
            }
            extent = Math.max(extent, max - min);
        }
        return extent;
    }

    private static float[] corners(float[] source, int base, int stride, int a, int b, int c) {
        float[] result = new float[stride * 3];
        System.arraycopy(source, base + a * stride, result, 0, stride);
        System.arraycopy(source, base + b * stride, result, stride, stride);
        System.arraycopy(source, base + c * stride, result, stride * 2, stride);
        return result;
    }

    private static void polygon(float[] vertices, int stride, FloatArrayList output) {
        int count = vertices.length / stride;
        if (!hasArea(vertices, stride, count)) return;
        int split = -1;
        float extent = 1, low = 0, high = 0;
        for (int axis = 0; axis < 3; axis++) {
            float min = vertices[axis], max = min;
            for (int v = 1; v < count; v++) {
                min = Math.min(min, vertices[v * stride + axis]);
                max = Math.max(max, vertices[v * stride + axis]);
            }
            if (max - min > extent) {
                split = axis;
                extent = max - min;
                low = min;
                high = max;
            }
        }
        if (split == -1) {
            emit(vertices, stride, count, output);
            return;
        }

        float plane = (float) (Math.floor(((double) low + high) * 0.5) + 0.5);
        assert plane > low && plane < high;
        for (int side = 0; side < 2; side++) {
            float[] clipped = new float[stride * (count + 2)];
            int produced = 0, previous = stride * (count - 1);
            float previousDistance =
                    side == 0
                            ? plane - vertices[previous + split]
                            : vertices[previous + split] - plane;
            for (int i = 0; i < count; i++) {
                int current = i * stride;
                float distance =
                        side == 0
                                ? plane - vertices[current + split]
                                : vertices[current + split] - plane;
                if ((distance >= 0) != (previousDistance >= 0)) {
                    float t = previousDistance / (previousDistance - distance);
                    for (int component = 0; component < stride; component++)
                        clipped[produced * stride + component] =
                                vertices[previous + component]
                                        + t
                                                * (vertices[current + component]
                                                        - vertices[previous + component]);
                    clipped[produced++ * stride + split] = plane;
                }
                if (distance >= 0)
                    System.arraycopy(vertices, current, clipped, produced++ * stride, stride);
                previous = current;
                previousDistance = distance;
            }
            if (produced >= 3) polygon(Arrays.copyOf(clipped, produced * stride), stride, output);
        }
    }

    private static boolean collinear(float[] vertices, int a, int b, int c) {
        float ax = vertices[b] - vertices[a],
                ay = vertices[b + 1] - vertices[a + 1],
                az = vertices[b + 2] - vertices[a + 2];
        float bx = vertices[c] - vertices[a],
                by = vertices[c + 1] - vertices[a + 1],
                bz = vertices[c + 2] - vertices[a + 2];
        return ay * bz - az * by == 0 && az * bx - ax * bz == 0 && ax * by - ay * bx == 0;
    }

    private static boolean hasArea(float[] vertices, int stride, int count) {
        for (int i = 1; i + 1 < count; i++)
            if (!collinear(vertices, 0, i * stride, (i + 1) * stride)) return true;
        return false;
    }

    private static void emit(float[] vertices, int stride, int count, FloatArrayList output) {
        boolean changed = true;
        while (changed && count > 3) {
            changed = false;
            for (int i = 0; i < count; i++) {
                if (collinear(
                        vertices,
                        ((i + count - 1) % count) * stride,
                        i * stride,
                        ((i + 1) % count) * stride)) {
                    System.arraycopy(
                            vertices,
                            (i + 1) * stride,
                            vertices,
                            i * stride,
                            (count - i - 1) * stride);
                    count--;
                    changed = true;
                    break;
                }
            }
        }
        for (int i = 1; i + 1 < count; i += 2) {
            output.addElements(output.size(), vertices, 0, stride);
            output.addElements(output.size(), vertices, i * stride, stride);
            output.addElements(output.size(), vertices, (i + 1) * stride, stride);
            output.addElements(
                    output.size(), vertices, Math.min(i + 2, count - 1) * stride, stride);
        }
    }
}
