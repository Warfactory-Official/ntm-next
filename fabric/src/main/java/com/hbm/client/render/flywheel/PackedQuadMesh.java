// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.animloader.AnimatedModel;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.model.QuadIndexSequence;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public final class PackedQuadMesh implements Mesh {
    private final float[] data;
    private final int vertexCount;
    private final Vector4f boundingSphere;
    private final int @Nullable [] colors;
    private final int @Nullable [] light;

    private PackedQuadMesh(float[] data, int @Nullable [] colors, int @Nullable [] light) {
        this.data = data;
        this.vertexCount = data.length / GroupObject.STRIDE;
        this.boundingSphere = sphere(data);
        this.colors = colors;
        this.light = light;
    }

    public static PackedQuadMesh of(
            float[] vertices, int @Nullable [] colors, int @Nullable [] light) {
        return new PackedQuadMesh(vertices, colors, light);
    }

    public static PackedQuadMesh of(HFRWavefrontObject model, int part) {
        return new PackedQuadMesh(model.groups[part].quads(model.smoothing()), null, null);
    }

    public static PackedQuadMesh of(GroupObject group, boolean smooth) {
        return new PackedQuadMesh(group.quads(smooth), null, null);
    }

    public static PackedQuadMesh of(AnimatedModel.Mesh node) {
        float[] src = node.vertexData;
        int tris = src.length / (3 * GroupObject.STRIDE);
        float[] out = new float[tris * 4 * GroupObject.STRIDE];
        for (int t = 0; t < tris; t++) {
            int in = t * 3 * GroupObject.STRIDE;
            int dst = t * 4 * GroupObject.STRIDE;
            System.arraycopy(src, in, out, dst, 3 * GroupObject.STRIDE);
            System.arraycopy(
                    src,
                    in + 2 * GroupObject.STRIDE,
                    out,
                    dst + 3 * GroupObject.STRIDE,
                    GroupObject.STRIDE);
        }
        return new PackedQuadMesh(out, null, null);
    }

    public static Builder builder(int quads) {
        return new Builder(quads);
    }

    private static Vector4f sphere(float[] d) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < d.length; i += GroupObject.STRIDE) {
            minX = Math.min(minX, d[i]);
            maxX = Math.max(maxX, d[i]);
            minY = Math.min(minY, d[i + 1]);
            maxY = Math.max(maxY, d[i + 1]);
            minZ = Math.min(minZ, d[i + 2]);
            maxZ = Math.max(maxZ, d[i + 2]);
        }
        float cx = (minX + maxX) * 0.5F, cy = (minY + maxY) * 0.5F, cz = (minZ + maxZ) * 0.5F;
        float dx = maxX - cx, dy = maxY - cy, dz = maxZ - cz;
        return new Vector4f(cx, cy, cz, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    @Override
    public int vertexCount() {
        return vertexCount;
    }

    @Override
    public void write(MutableVertexList dst) {
        for (int v = 0, i = 0; v < vertexCount; v++, i += GroupObject.STRIDE) {
            dst.x(v, data[i]);
            dst.y(v, data[i + 1]);
            dst.z(v, data[i + 2]);
            int color = colors == null ? -1 : colors[v];
            dst.r(v, (color >>> 16 & 255) / 255F);
            dst.g(v, (color >>> 8 & 255) / 255F);
            dst.b(v, (color & 255) / 255F);
            dst.a(v, (color >>> 24) / 255F);
            dst.u(v, data[i + 3]);
            dst.v(v, data[i + 4]);
            dst.light(v, light == null ? 0 : light[v]);
            dst.overlay(v, OverlayTexture.NO_OVERLAY);
            dst.normalX(v, data[i + 5]);
            dst.normalY(v, data[i + 6]);
            dst.normalZ(v, data[i + 7]);
        }
    }

    @Override
    public IndexSequence indexSequence() {
        return QuadIndexSequence.INSTANCE;
    }

    @Override
    public int indexCount() {
        return vertexCount / 4 * 6;
    }

    @Override
    public Vector4fc boundingSphere() {
        return boundingSphere;
    }

    public static final class Builder {
        private final float[] data;
        private final int[] colors;
        private int vertex;
        private float normalX = 0F, normalY = 1F, normalZ = 0F;

        private Builder(int quads) {
            data = new float[quads * 4 * GroupObject.STRIDE];
            colors = new int[quads * 4];
        }

        public Builder normal(float x, float y, float z) {
            normalX = x;
            normalY = y;
            normalZ = z;
            return this;
        }

        public Builder vertex(double x, double y, double z, double u, double v, int argb) {
            int i = vertex * GroupObject.STRIDE;
            data[i] = (float) x;
            data[i + 1] = (float) y;
            data[i + 2] = (float) z;
            data[i + 3] = (float) u;
            data[i + 4] = (float) v;
            data[i + 5] = normalX;
            data[i + 6] = normalY;
            data[i + 7] = normalZ;
            colors[vertex] = argb;
            vertex++;
            return this;
        }

        public Builder vertex(double x, double y, double z, int argb) {
            return vertex(x, y, z, 0D, 0D, argb);
        }

        public PackedQuadMesh build() {
            if (vertex != colors.length) {
                throw new IllegalStateException(
                        "PackedQuadMesh wants " + colors.length + " vertices, got " + vertex);
            }
            return new PackedQuadMesh(data, colors, null);
        }
    }
}
