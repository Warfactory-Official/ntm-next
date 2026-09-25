// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.TorexFlash;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.model.QuadIndexSequence;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class TorexFlashMesh implements Mesh {

    public static final TorexFlashMesh INSTANCE = new TorexFlashMesh();

    private static final int VERTICES = TorexFlash.CONES * 3 * 4;

    private final float[] positions = new float[VERTICES * 3];
    private final float[] alphas = new float[VERTICES];

    private final Vector4f boundingSphere = new Vector4f(0F, 0F, 0F, 7.1F);

    private TorexFlashMesh() {
        float[] corners = TorexFlash.CORNERS;
        int v = 0;
        for (int i = 0; i < TorexFlash.CONES; i++) {
            int at = i * 9;
            v = quad(v, corners, at, at + 3);
            v = quad(v, corners, at + 3, at + 6);
            v = quad(v, corners, at + 6, at);
        }
    }

    private int quad(int v, float[] corners, int b, int c) {
        v = vertex(v, 0F, 0F, 0F, 1F);
        v = vertex(v, corners[b], corners[b + 1], corners[b + 2], 0F);
        v = vertex(v, corners[c], corners[c + 1], corners[c + 2], 0F);
        v = vertex(v, corners[c], corners[c + 1], corners[c + 2], 0F);
        return v;
    }

    private int vertex(int v, float x, float y, float z, float a) {
        positions[v * 3] = x;
        positions[v * 3 + 1] = y;
        positions[v * 3 + 2] = z;
        alphas[v] = a;
        return v + 1;
    }

    @Override
    public int vertexCount() {
        return VERTICES;
    }

    @Override
    public void write(MutableVertexList dst) {
        for (int v = 0; v < VERTICES; v++) {
            dst.x(v, positions[v * 3]);
            dst.y(v, positions[v * 3 + 1]);
            dst.z(v, positions[v * 3 + 2]);
            dst.r(v, 1F);
            dst.g(v, 1F);
            dst.b(v, 1F);
            dst.a(v, alphas[v]);

            dst.u(v, 0F);
            dst.v(v, 0F);
            dst.light(v, LightCoordsUtil.FULL_BRIGHT);
            dst.overlay(v, OverlayTexture.NO_OVERLAY);
            dst.normalX(v, 0F);
            dst.normalY(v, 1F);
            dst.normalZ(v, 0F);
        }
    }

    @Override
    public IndexSequence indexSequence() {
        return QuadIndexSequence.INSTANCE;
    }

    @Override
    public int indexCount() {
        return VERTICES / 4 * 6;
    }

    @Override
    public Vector4fc boundingSphere() {
        return boundingSphere;
    }
}
