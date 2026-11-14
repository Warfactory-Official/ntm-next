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

package com.hbm.animloader;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;

public final class MeshEmitter {
    private final Matrix3f normal = new Matrix3f();

    public void emit(
            TriangleSource mesh, Matrix4fc pose, VertexConsumer vertices, int light, int overlay) {
        normal.set(pose).invert().transpose();
        for (int triangle = 0; triangle < mesh.getCornerCount(); triangle += 3) {
            for (int targetCorner = 0; targetCorner < 4; targetCorner++) {
                int corner = triangle + Math.min(targetCorner, 2);
                float x = mesh.getComponent(corner, 0);
                float y = mesh.getComponent(corner, 1);
                float z = mesh.getComponent(corner, 2);
                float nx = mesh.getComponent(corner, 5);
                float ny = mesh.getComponent(corner, 6);
                float nz = mesh.getComponent(corner, 7);
                float posedX = pose.m00() * x + pose.m10() * y + pose.m20() * z + pose.m30();
                float posedY = pose.m01() * x + pose.m11() * y + pose.m21() * z + pose.m31();
                float posedZ = pose.m02() * x + pose.m12() * y + pose.m22() * z + pose.m32();
                float normalX = normal.m00() * nx + normal.m10() * ny + normal.m20() * nz;
                float normalY = normal.m01() * nx + normal.m11() * ny + normal.m21() * nz;
                float normalZ = normal.m02() * nx + normal.m12() * ny + normal.m22() * nz;
                float inverseLength =
                        1
                                / (float)
                                        Math.sqrt(
                                                normalX * normalX
                                                        + normalY * normalY
                                                        + normalZ * normalZ);
                vertices.addVertex(
                        posedX,
                        posedY,
                        posedZ,
                        -1,
                        mesh.getComponent(corner, 3),
                        mesh.getComponent(corner, 4),
                        overlay,
                        light,
                        normalX * inverseLength,
                        normalY * inverseLength,
                        normalZ * inverseLength);
            }
        }
    }
}
