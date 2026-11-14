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
