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

import com.hbm.entity.effect.EMPBlastEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class EmpBlastVisual extends HbmDynamicEntityVisual<EMPBlastEntity> {
    private static final Model MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(
                            ResourceManager.emp_ring,
                            ResourceManager.emp_ring.partId("Circle_Circle.001")),
                    SimpleMaterial.builder()
                            .texture(ResourceManager.emp_ring_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .useOverlay(false)
                            .useLight(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .backfaceCulling(false)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance ring;
    private final Matrix4f pose = new Matrix4f();
    private float lastX = Float.NaN, lastY, lastZ, lastScale;

    public EmpBlastVisual(VisualizationContext ctx, EMPBlastEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
        ring = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        ring.overlay(OverlayTexture.NO_OVERLAY);
        ring.light(LightCoordsUtil.FULL_BRIGHT);
        ring.colorArgb(ARGB.color(255, 255, 255, 255));
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        var sphere = MODEL.boundingSphere();
        float scale = entity.scale;
        float x = sphere.x() * scale, z = sphere.z() * scale;
        float reach =
                (float) Math.sqrt(x * x + sphere.y() * sphere.y() + z * z)
                        + sphere.w() * Math.max(Math.abs(scale), 1F);
        return sphereVisible(frustum, 0F, reach);
    }

    @Override
    protected void frame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float scale = entity.scale;
        if (visualPos.x == lastX
                && visualPos.y == lastY
                && visualPos.z == lastZ
                && scale == lastScale) return;
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastScale = scale;
        ring.setTransform(
                pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(scale, 1F, scale));
        ring.setChanged();
    }

    @Override
    protected void _delete() {
        ring.delete();
    }
}
