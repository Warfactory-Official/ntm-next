// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

final class CuboidMesh {
    private static final int[][] FACES = {
        {5, 1, 2, 6}, {0, 4, 7, 3}, {5, 4, 0, 1}, {2, 3, 7, 6}, {1, 0, 3, 2}, {4, 5, 6, 7}
    };
    private final float textureWidth;
    private final float textureHeight;
    private final FloatArrayList vertices = new FloatArrayList();

    CuboidMesh(float textureWidth, float textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    CuboidMesh box(
            Matrix4fc pose,
            float u,
            float v,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            float inflate) {
        float x0 = (x - inflate) / 16F, y0 = (y - inflate) / 16F, z0 = (z - inflate) / 16F;
        float x1 = (x + width + inflate) / 16F, y1 = (y + height + inflate) / 16F;
        float z1 = (z + depth + inflate) / 16F;
        Vector3f[] corners = {
            new Vector3f(x0, y0, z0),
            new Vector3f(x1, y0, z0),
            new Vector3f(x1, y1, z0),
            new Vector3f(x0, y1, z0),
            new Vector3f(x0, y0, z1),
            new Vector3f(x1, y0, z1),
            new Vector3f(x1, y1, z1),
            new Vector3f(x0, y1, z1)
        };
        float[][] uv = {
            {u + depth + width, v + depth, u + depth + width + depth, v + depth + height},
            {u, v + depth, u + depth, v + depth + height},
            {u + depth, v, u + depth + width, v + depth},
            {u + depth + width, v + depth, u + depth + width * 2, v},
            {u + depth, v + depth, u + depth + width, v + depth + height},
            {u + depth * 2 + width, v + depth, u + depth * 2 + width * 2, v + depth + height}
        };
        Matrix3f normalMatrix = pose.normal(new Matrix3f());
        Vector3f position = new Vector3f();
        for (int f = 0; f < FACES.length; f++) {
            int[] face = FACES[f];
            Vector3f normal =
                    new Vector3f(corners[face[1]])
                            .sub(corners[face[0]])
                            .cross(new Vector3f(corners[face[2]]).sub(corners[face[0]]));
            normalMatrix.transform(normal).normalize();
            for (int i = 0; i < 4; i++) {
                pose.transformPosition(corners[face[i]], position);
                vertices.add(position.x);
                vertices.add(position.y);
                vertices.add(position.z);
                vertices.add(uv[f][i == 0 || i == 3 ? 2 : 0] / textureWidth);
                vertices.add(uv[f][i < 2 ? 1 : 3] / textureHeight);
                vertices.add(normal.x);
                vertices.add(normal.y);
                vertices.add(normal.z);
            }
        }
        return this;
    }

    PackedQuadMesh mesh() {
        return PackedQuadMesh.of(vertices.toFloatArray(), null, null);
    }
}
