// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import com.hbm.interfaces.injected.IBufferBuilderExtension;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class GroupObject {

    public static final int STRIDE = 8;
    public static final int QUAD = 4 * STRIDE;

    private static final int WHOLLY_IN = 0;

    private static final int WHOLLY_OUT = 1;
    private static final int CUT = 2;

    public final String name;

    private final float[] quads;
    private final float[] faceNormals;
    private final float[] bounds;
    private final Vector3f position = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private float[] flatQuads;
    private float[] flatShadedQuads;

    private float[] clipIn;
    private float[] clipOut;
    private float[] clipPos;
    private float[] clipNorm;
    private float[][] onePlane;

    public GroupObject(String name, float[] quads, float[] faceNormals) {
        this(name, quads, faceNormals, computeBounds(quads));
    }

    GroupObject(String name, float[] quads, float[] faceNormals, float[] bounds) {
        this.name = name;
        this.quads = quads;
        this.faceNormals = faceNormals;
        this.bounds = bounds;
    }

    public static GroupObject ofPosUv(String name, float[] posUv) {
        int faces = posUv.length / (4 * 5);
        float[] quads = new float[faces * QUAD];
        float[] faceNormals = new float[faces * 3];
        for (int f = 0; f < faces; f++) {
            int src = f * 4 * 5;
            normal(posUv, src, 5, faceNormals, f * 3);
            for (int c = 0; c < 4; c++) {
                int s = src + c * 5, d = f * QUAD + c * STRIDE;
                quads[d] = posUv[s];
                quads[d + 1] = posUv[s + 1];
                quads[d + 2] = posUv[s + 2];
                quads[d + 3] = posUv[s + 3];
                quads[d + 4] = posUv[s + 4];
                quads[d + 5] = faceNormals[f * 3];
                quads[d + 6] = faceNormals[f * 3 + 1];
                quads[d + 7] = faceNormals[f * 3 + 2];
            }
        }
        return new GroupObject(name, quads, faceNormals);
    }

    static void normal(float[] src, int base, int stride, float[] out, int at) {
        float ax = src[base + stride] - src[base],
                ay = src[base + stride + 1] - src[base + 1],
                az = src[base + stride + 2] - src[base + 2];
        float bx = src[base + 2 * stride] - src[base],
                by = src[base + 2 * stride + 1] - src[base + 1],
                bz = src[base + 2 * stride + 2] - src[base + 2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-6F) len = 1F;
        out[at] = nx / len;
        out[at + 1] = ny / len;
        out[at + 2] = nz / len;
    }

    private static float[] computeBounds(float[] quads) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < quads.length; i += STRIDE) {
            minX = Math.min(minX, quads[i]);
            maxX = Math.max(maxX, quads[i]);
            minY = Math.min(minY, quads[i + 1]);
            maxY = Math.max(maxY, quads[i + 1]);
            minZ = Math.min(minZ, quads[i + 2]);
            maxZ = Math.max(maxZ, quads[i + 2]);
        }
        return new float[] {minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static int classify(float[] src, int q, float[][] planes) {
        int fate = WHOLLY_IN;
        for (float[] plane : planes) {
            float nx = plane[0], ny = plane[1], nz = plane[2], d = plane[3];
            int inside = 0;
            for (int c = 0; c < 4; c++) {
                int v = q + c * STRIDE;
                if (src[v] * nx + src[v + 1] * ny + src[v + 2] * nz - d >= 0) inside++;
            }
            if (inside == 0) return WHOLLY_OUT;
            if (inside < 4) fate = CUT;
        }
        return fate;
    }

    GroupObject copy() {
        return new GroupObject(name, quads, faceNormals, bounds);
    }

    public GroupObject refined() {
        float[] vertices = MeshSubdivision.refine(quads, STRIDE);
        if (vertices == quads) return this;
        float[] normals = new float[vertices.length / QUAD * 3];
        for (int face = 0; face < normals.length / 3; face++)
            normal(vertices, face * QUAD, STRIDE, normals, face * 3);
        return new GroupObject(name, vertices, normals, bounds);
    }

    public GroupObject flatShaded() {
        return new GroupObject(name, flatShadedQuads().clone(), faceNormals, bounds);
    }

    public float[] flatShadedQuads() {
        float[] cached = flatShadedQuads;
        if (cached != null) return cached;

        float[] flat = quads.clone();
        for (int face = 0; face < flat.length; face += QUAD) {
            int last = face + 3 * STRIDE;
            for (int vertex = face; vertex < last; vertex += STRIDE) {
                flat[vertex + 5] = flat[last + 5];
                flat[vertex + 6] = flat[last + 6];
                flat[vertex + 7] = flat[last + 7];
            }
        }
        return flatShadedQuads = flat;
    }

    public float[] quads(boolean smoothing) {
        return smoothing ? quads : flat();
    }

    public float[] bounds() {
        return bounds;
    }

    public int faceCount() {
        return faceNormals.length / 3;
    }

    float[] faceNormals() {
        return faceNormals;
    }

    private float[] flat() {
        float[] cached = flatQuads;
        if (cached != null) return cached;

        float[] d = quads.clone();
        for (int f = 0; f * QUAD < d.length; f++) {
            float nx = faceNormals[f * 3], ny = faceNormals[f * 3 + 1], nz = faceNormals[f * 3 + 2];
            for (int c = 0; c < 4; c++) {
                int v = f * QUAD + c * STRIDE;
                d[v + 5] = nx;
                d[v + 6] = ny;
                d[v + 7] = nz;
            }
        }
        return flatQuads = d;
    }

    public void emit(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int color, boolean smoothing) {
        emit(pose, buffer, light, color, OverlayTexture.NO_OVERLAY, smoothing);
    }

    public void emit(
            PoseStack.Pose pose, VertexConsumer buffer, int[] light, int color, boolean smoothing) {
        float[] vertices = quads(smoothing);
        assert light.length == vertices.length / STRIDE;
        IBufferBuilderExtension block =
                buffer instanceof IBufferBuilderExtension fast && fast.hbm$beginBlock(light.length)
                        ? fast
                        : null;
        for (int i = 0; i < vertices.length; i += STRIDE) {
            if (block != null) {
                Vertices.emitBlock(
                        block,
                        pose,
                        vertices[i],
                        vertices[i + 1],
                        vertices[i + 2],
                        color,
                        vertices[i + 3],
                        vertices[i + 4],
                        OverlayTexture.NO_OVERLAY,
                        light[i / STRIDE],
                        vertices[i + 5],
                        vertices[i + 6],
                        vertices[i + 7]);
            } else {
                Vertices.emit(
                        buffer,
                        pose,
                        vertices[i],
                        vertices[i + 1],
                        vertices[i + 2],
                        color,
                        vertices[i + 3],
                        vertices[i + 4],
                        light[i / STRIDE],
                        vertices[i + 5],
                        vertices[i + 6],
                        vertices[i + 7]);
            }
        }
        if (block != null) block.hbm$endBlock();
    }

    public void emit(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            int overlay,
            boolean smoothing) {
        emit(pose, buffer, light, color, overlay, 1F, 1F, 0F, 0F, smoothing);
    }

    public void emit(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            float uScale,
            float vScale,
            float uOff,
            float vOff,
            boolean smoothing) {
        emit(
                pose,
                buffer,
                light,
                color,
                OverlayTexture.NO_OVERLAY,
                uScale,
                vScale,
                uOff,
                vOff,
                smoothing);
    }

    private void emit(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            int overlay,
            float uScale,
            float vScale,
            float uOff,
            float vOff,
            boolean smoothing) {
        float[] d = quads(smoothing);
        if (buffer instanceof IBufferBuilderExtension fast
                && fast.hbm$putPart(
                        d,
                        STRIDE,
                        d.length / STRIDE,
                        pose,
                        color,
                        overlay,
                        light,
                        uScale,
                        vScale,
                        uOff,
                        vOff)) return;

        Matrix4fc matrix = pose.pose();
        Vector3f p = position;
        Vector3f n = normal;
        for (int i = 0; i < d.length; i += STRIDE) {
            matrix.transformPosition(d[i], d[i + 1], d[i + 2], p);

            pose.transformNormal(d[i + 5], d[i + 6], d[i + 7], n);
            buffer.addVertex(
                    p.x,
                    p.y,
                    p.z,
                    color,
                    d[i + 3] * uScale + uOff,
                    d[i + 4] * vScale + vOff,
                    overlay,
                    light,
                    n.x,
                    n.y,
                    n.z);
        }
    }

    public void emitClipped(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            float nx,
            float ny,
            float nz,
            float d,
            boolean smoothing) {
        float[][] planes = onePlane;
        if (planes == null) planes = onePlane = new float[][] {new float[4]};
        float[] plane = planes[0];
        plane[0] = nx;
        plane[1] = ny;
        plane[2] = nz;
        plane[3] = d;
        emitClipped(pose, buffer, light, color, smoothing, planes);
    }

    public void emitClipped(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            boolean smoothing,
            float[][] planes) {
        float[] src = quads(smoothing);

        int cap = 4 + planes.length + 4;
        if (clipIn == null || clipIn.length < cap * STRIDE) {
            clipIn = new float[cap * STRIDE];
            clipOut = new float[cap * STRIDE];
            clipPos = new float[cap * 3];
            clipNorm = new float[cap * 3];
        }
        float[] in = clipIn;
        float[] out = clipOut;
        for (int q = 0; q < src.length; q += QUAD) {
            int fate = classify(src, q, planes);
            if (fate == WHOLLY_OUT) continue;
            if (fate == WHOLLY_IN) {
                for (int c = 0; c < 4; c++)
                    emitVertex(pose, buffer, light, color, src, q + c * STRIDE);
                continue;
            }
            System.arraycopy(src, q, in, 0, QUAD);
            int n = 4;
            for (float[] plane : planes) {
                float nx = plane[0], ny = plane[1], nz = plane[2], d = plane[3];
                int m = 0;
                for (int i = 0; i < n; i++) {
                    int a = i * STRIDE, b = ((i + 1) % n) * STRIDE;
                    float da = in[a] * nx + in[a + 1] * ny + in[a + 2] * nz - d;
                    float db = in[b] * nx + in[b + 1] * ny + in[b + 2] * nz - d;
                    if (da >= 0) {
                        System.arraycopy(in, a, out, m * STRIDE, STRIDE);
                        m++;
                    }
                    if ((da >= 0) != (db >= 0)) {
                        float t = da / (da - db);
                        for (int k = 0; k < STRIDE; k++)
                            out[m * STRIDE + k] = in[a + k] + (in[b + k] - in[a + k]) * t;
                        m++;
                    }
                }
                System.arraycopy(out, 0, in, 0, m * STRIDE);
                n = m;
                if (n == 0) break;
            }
            emitFan(pose, buffer, light, color, in, n);
        }
    }

    private void emitFan(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int color, float[] poly, int n) {
        Matrix4fc matrix = pose.pose();
        for (int i = 0; i < n; i++) {
            int a = i * STRIDE, v = i * 3;
            matrix.transformPosition(poly[a], poly[a + 1], poly[a + 2], position);
            pose.transformNormal(poly[a + 5], poly[a + 6], poly[a + 7], normal);
            clipPos[v] = position.x;
            clipPos[v + 1] = position.y;
            clipPos[v + 2] = position.z;
            clipNorm[v] = normal.x;
            clipNorm[v + 1] = normal.y;
            clipNorm[v + 2] = normal.z;
        }
        for (int i = 1; i + 1 < n; i += 2) {
            emitTransformed(buffer, light, color, poly, 0);
            emitTransformed(buffer, light, color, poly, i);
            emitTransformed(buffer, light, color, poly, i + 1);
            emitTransformed(buffer, light, color, poly, i + 2 < n ? i + 2 : i + 1);
        }
    }

    private void emitTransformed(VertexConsumer buffer, int light, int color, float[] poly, int i) {
        int a = i * STRIDE, v = i * 3;
        buffer.addVertex(
                clipPos[v],
                clipPos[v + 1],
                clipPos[v + 2],
                color,
                poly[a + 3],
                poly[a + 4],
                OverlayTexture.NO_OVERLAY,
                light,
                clipNorm[v],
                clipNorm[v + 1],
                clipNorm[v + 2]);
    }

    private void emitVertex(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int color, float[] d, int i) {
        pose.pose().transformPosition(d[i], d[i + 1], d[i + 2], position);
        pose.transformNormal(d[i + 5], d[i + 6], d[i + 7], normal);
        buffer.addVertex(
                position.x,
                position.y,
                position.z,
                color,
                d[i + 3],
                d[i + 4],
                OverlayTexture.NO_OVERLAY,
                light,
                normal.x,
                normal.y,
                normal.z);
    }
}
