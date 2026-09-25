// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.DebrisMesh;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.model.QuadIndexSequence;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class DebrisFlywheelMesh implements Mesh {

    private final DebrisMesh source;
    private final Vector4f boundingSphere;

    public DebrisFlywheelMesh(DebrisMesh source, float radius) {
        this.source = source;
        this.boundingSphere = new Vector4f(0F, 0F, 0F, radius);
    }

    @Override
    public int vertexCount() {
        return source.vertexCount();
    }

    @Override
    public void write(MutableVertexList dst) {
        source.write(dst);
    }

    @Override
    public IndexSequence indexSequence() {
        return QuadIndexSequence.INSTANCE;
    }

    @Override
    public int indexCount() {
        return source.vertexCount() / 4 * 6;
    }

    @Override
    public Vector4fc boundingSphere() {
        return boundingSphere;
    }
}
