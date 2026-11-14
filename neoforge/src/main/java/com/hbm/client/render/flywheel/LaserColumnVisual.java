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

import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class LaserColumnVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    protected static final int HEIGHT = 250;
    private static final int WHITE = ARGB.color(255, 255, 255, 255);
    private static final Model COLUMN =
            new SingleMeshModel(
                    column(),
                    SimpleMaterial.builderOf(EffectVisuals.Shared.BEAM_ADDITIVE)
                            .backfaceCulling(true)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Vector3f lastPosition = new Vector3f(Float.NaN);
    private final TransformedInstance outer;
    private final TransformedInstance inner;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();

    protected LaserColumnVisual(
            VisualizationContext ctx, T entity, float partialTick, int outerRgb, int innerRgb) {
        super(ctx, entity, partialTick);
        outer = instance(COLUMN, 0xFF000000 | outerRgb);
        inner = instance(COLUMN, 0xFF000000 | innerRgb);
        writeColumns(partialTick);
    }

    public static void initModels() {}

    private static Mesh column() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(8);
        Vec3 vector = new Vec3(1D, 0D, 0D);
        for (int i = 0; i < 8; i++) {
            Vec3 next = vector.yRot(45F);
            mesh.vertex(vector.x, HEIGHT, vector.z, WHITE);
            mesh.vertex(vector.x, 0D, vector.z, WHITE);
            mesh.vertex(next.x, 0D, next.z, WHITE);
            mesh.vertex(next.x, HEIGHT, next.z, WHITE);
            vector = next;
        }
        return mesh.build();
    }

    protected final TransformedInstance instance(Model model, int color) {
        TransformedInstance created =
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        created.overlay(OverlayTexture.NO_OVERLAY);
        created.light(LightCoordsUtil.FULL_BRIGHT);
        created.colorArgb(color);
        return created;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        if (isFirstPersonCameraEntity()) return false;
        var origin = renderOrigin();
        float x = (float) (entity.getX() - origin.getX());
        float y = (float) (entity.getY() - origin.getY());
        float z = (float) (entity.getZ() - origin.getZ());
        return frustum.testAab(x - 1F, y, z - 1F, x + 1F, y + HEIGHT, z + 1F);
    }

    @Override
    protected void frame(Context ctx) {
        writeColumns(ctx.partialTick());
    }

    private void writeColumns(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        if (visualPos.equals(lastPosition)) return;
        lastPosition.set(visualPos);
        pose.translation(visualPos.x, visualPos.y, visualPos.z);
        outer.setTransform(scratch.set(pose).scale(0.5F, 1F, 0.5F));
        outer.setChanged();
        inner.setTransform(scratch.set(pose).rotateY(8 * 45F).scale(0.25F, 1F, 0.25F));
        inner.setChanged();
    }

    @Override
    protected void _delete() {
        outer.delete();
        inner.delete();
    }
}
