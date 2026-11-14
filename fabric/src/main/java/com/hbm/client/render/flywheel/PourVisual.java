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

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FoundryFaces;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public final class PourVisual extends AbstractVisual {
    private static final InstanceType<AffineUvTransformedInstance> TYPE =
            SimpleInstanceType.builder(AffineUvTransformedInstance::new)
                    .layout(AffineUvTransformedInstance.TYPE.layout())
                    .seed(AffineUvTransformedInstance.TYPE.seed())
                    .transform(AffineUvTransformedInstance.TYPE.transform())
                    .vertexShader(Identifier.fromNamespaceAndPath("hbm", "instance/pour.vert"))
                    .cullShader(AffineUvTransformedInstance.TYPE.cullShader())
                    .build();
    private static final MeshPart SHARED_MODEL = buildModel();
    private final BlockPos anchor;
    private final AffineUvTransformedInstance[] triangles = new AffineUvTransformedInstance[18];
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private int used, tint;
    private float x, y, z;
    private float lastLength = Float.NaN,
            lastAge = Float.NaN,
            lastBase = Float.NaN,
            lastOffset = Float.NaN;
    private Direction lastDirection;
    private int lastColor;
    private long lastTextureFrame = Long.MIN_VALUE;
    private final boolean[] visible = new boolean[18];
    private final FoundryFaces.StreamQuad emitter =
            (x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3) -> {
                triangle(x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2);
                triangle(x0, y0, z0, u0, v0, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3);
            };

    public PourVisual(VisualizationContext context, Level level, BlockPos anchor) {
        super(context, level, 0);
        this.anchor = anchor.immutable();
    }

    public static void initModels() {}

    private static MeshPart buildModel() {
        float[] data = {
            0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1,
            0, 0, 1
        };
        var mesh = PackedQuadMesh.of(data, new int[] {-1, -1, -1, -1}, new int[4]);
        return FoundryTankVisual.molten(MeshPart.create(mesh, FoundryTankVisual.MOLTEN));
    }

    public void update(
            int color,
            Direction direction,
            float length,
            float age,
            float base,
            float offset,
            float x,
            float y,
            float z) {
        long textureFrame = GameTime.now() / 100 % 16;
        if (length == lastLength
                && age == lastAge
                && base == lastBase
                && offset == lastOffset
                && direction == lastDirection
                && color == lastColor
                && x == this.x
                && y == this.y
                && z == this.z
                && textureFrame == lastTextureFrame) return;
        if (lastTextureFrame == Long.MIN_VALUE || color != lastColor)
            tint = FoundryFaces.brightenMolten(color);
        this.x = x;
        this.y = y;
        this.z = z;
        lastLength = length;
        lastAge = age;
        lastBase = base;
        lastOffset = offset;
        lastDirection = direction;
        lastColor = color;
        lastTextureFrame = textureFrame;
        used = 0;
        if (age < 20F) FoundryFaces.emitStream(emitter, direction, length, age, base, offset);
        for (int i = used; i < triangles.length; i++) hide(i);
    }

    private void triangle(
            float x0,
            float y0,
            float z0,
            float u0,
            float v0,
            float x1,
            float y1,
            float z1,
            float u1,
            float v1,
            float x2,
            float y2,
            float z2,
            float u2,
            float v2) {
        float ax = x1 - x0, ay = y1 - y0, az = z1 - z0, bx = x2 - x0, by = y2 - y0, bz = z2 - z0;
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        if (nx * nx + ny * ny + nz * nz == 0) return;
        local.identity()
                .m00(ax)
                .m01(ay)
                .m02(az)
                .m10(bx)
                .m11(by)
                .m12(bz)
                .m30(x + x0)
                .m31(y + y0)
                .m32(z + z0);
        pose.translation(
                        anchor.getX() - renderOrigin().getX(),
                        anchor.getY() - renderOrigin().getY(),
                        anchor.getZ() - renderOrigin().getZ())
                .mul(local);
        if (triangles[used] == null) {
            triangles[used] =
                    instancerProvider().instancer(TYPE, SHARED_MODEL.model()).createInstance();
            visible[used] = true;
        }
        var triangle = triangles[used];
        if (!visible[used]) {
            triangle.setVisible(true);
            visible[used] = true;
        }
        triangle.setTransform(pose).colorArgb(tint).light(LightCoordsUtil.pack(15, 0));
        triangle.uv(u1 - u0, u2 - u0, v1 - v0, v2 - v0, u0, v0).setChanged();
        used++;
    }

    public void hide() {
        for (int i = 0; i < used; i++) hide(i);
        used = 0;
        lastAge = Float.NaN;
    }

    private void hide(int i) {
        if (!visible[i]) return;
        triangles[i].setVisible(false);
        visible[i] = false;
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < triangles.length; i++) if (triangles[i] != null) triangles[i].delete();
    }
}
