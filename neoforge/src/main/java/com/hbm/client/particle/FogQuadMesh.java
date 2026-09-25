// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.model.QuadIndexSequence;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class FogQuadMesh implements Mesh {

    public static final FogQuadMesh INSTANCE = new FogQuadMesh();

    private static final float[][] CORNERS = {
        {-0.5f, -0.5f, 0f, 1f},
        {0.5f, -0.5f, 1f, 1f},
        {0.5f, 0.5f, 1f, 0f},
        {-0.5f, 0.5f, 0f, 0f},
    };

    private final Vector4f boundingSphere = new Vector4f(0f, 0f, 0f, 0.71f);

    @Override
    public int vertexCount() {
        return 4;
    }

    @Override
    public void write(MutableVertexList dst) {
        for (int v = 0; v < 4; v++) {
            float[] c = CORNERS[v];
            dst.x(v, c[0]);
            dst.y(v, c[1]);
            dst.z(v, 0f);
            dst.r(v, 1f);
            dst.g(v, 1f);
            dst.b(v, 1f);
            dst.a(v, 1f);
            dst.u(v, c[2]);
            dst.v(v, c[3]);
            dst.light(v, 0);
            dst.overlay(v, OverlayTexture.NO_OVERLAY);
            dst.normalX(v, 0f);
            dst.normalY(v, 0f);
            dst.normalZ(v, 1f);
        }
    }

    @Override
    public IndexSequence indexSequence() {
        return QuadIndexSequence.INSTANCE;
    }

    @Override
    public int indexCount() {
        return 6;
    }

    @Override
    public Vector4fc boundingSphere() {
        return boundingSphere;
    }
}
