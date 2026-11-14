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

import com.hbm.entity.projectile.SiegeLaserEntity;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SiegeLaserVisual extends HbmDynamicEntityVisual<SiegeLaserEntity> {
    private static final int SOLID = 0xFFFFFFFF;
    private static final int CLEAR = 0x00FFFFFF;
    private static final float SCALE_X = .5F;
    private static final Model MODEL =
            new SingleMeshModel(
                    dart(),
                    SimpleMaterial.builderOf(EffectVisuals.Shared.BEAM_ADDITIVE)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .useLight(true)
                            .light(LightShaders.FLAT)
                            .build());
    private static final float REACH = reach();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance dart;
    private final Matrix4f pose = new Matrix4f();
    private float lastX = Float.NaN, lastY, lastZ, lastYaw, lastPitch;
    private int lastColor, lastLight;

    public SiegeLaserVisual(VisualizationContext ctx, SiegeLaserEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
        dart = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        dart.overlay(OverlayTexture.NO_OVERLAY);
        writeFrame(partialTick);
    }

    private static Mesh dart() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(12);
        for (int s = -1; s <= 1; s += 2) {
            tri(mesh, 6F, 0F, 0F, SOLID, 3F, -1F, s, CLEAR, 3F, 1F, s, CLEAR);
            tri(mesh, 6F, 0F, 0F, SOLID, 3F, s, -1F, CLEAR, 3F, s, 1F, CLEAR);
            tri(mesh, 6F, 0F, 0F, SOLID, 4F, -0.5F, s * 0.5F, SOLID, 4F, 0.5F, s * 0.5F, SOLID);
            tri(mesh, 6F, 0F, 0F, SOLID, 4F, s * 0.5F, -0.5F, SOLID, 4F, s * 0.5F, 0.5F, SOLID);
        }
        for (int s = -1; s <= 1; s += 2) {
            quad(
                    mesh, 4F, s * 0.5F, -0.5F, 4F, s * 0.5F, 0.5F, 0F, s * 0.5F, 0.5F, 0F, s * 0.5F,
                    -0.5F);
            quad(
                    mesh, 4F, -0.5F, s * 0.5F, 4F, 0.5F, s * 0.5F, 0F, 0.5F, s * 0.5F, 0F, -0.5F,
                    s * 0.5F);
        }
        return mesh.build();
    }

    private static float reach() {
        var sphere = MODEL.boundingSphere();
        return SCALE_X * (new Vector3f(sphere.x(), sphere.y(), sphere.z()).length() + sphere.w());
    }

    private static void tri(
            PackedQuadMesh.Builder mesh,
            float ax,
            float ay,
            float az,
            int ac,
            float bx,
            float by,
            float bz,
            int bc,
            float cx,
            float cy,
            float cz,
            int cc) {
        mesh.vertex(ax, ay, az, ac);
        mesh.vertex(bx, by, bz, bc);
        mesh.vertex(cx, cy, cz, cc);
        mesh.vertex(cx, cy, cz, cc);
    }

    private static void quad(
            PackedQuadMesh.Builder mesh,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz) {
        mesh.vertex(ax, ay, az, SOLID);
        mesh.vertex(bx, by, bz, SOLID);
        mesh.vertex(cx, cy, cz, CLEAR);
        mesh.vertex(dx, dy, dz, CLEAR);
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 0F, REACH);
    }

    @Override
    protected void frame(Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        int color = entity.getColor() | 0xFF000000;
        int light = computePackedLight(partialTick);
        if (visualPos.x == lastX
                && visualPos.y == lastY
                && visualPos.z == lastZ
                && yaw == lastYaw
                && pitch == lastPitch
                && color == lastColor
                && light == lastLight) return;
        dart.setTransform(
                pose.translation(visualPos.x, visualPos.y, visualPos.z)
                        .rotateY((yaw - 90F) * Mth.DEG_TO_RAD)
                        .rotateZ((pitch + 180F) * Mth.DEG_TO_RAD)
                        .scale(-SCALE_X, 0.25F, 0.25F));
        dart.colorArgb(color);
        dart.light(light);
        dart.setChanged();
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastYaw = yaw;
        lastPitch = pitch;
        lastColor = color;
        lastLight = light;
    }

    @Override
    protected void _delete() {
        dart.delete();
    }
}
