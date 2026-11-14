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

import com.hbm.entity.projectile.ShrapnelEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ShrapnelVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    private static final Vector3f TUMBLE = new Vector3f(1F, 1F, 1F).normalize();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance instance;
    private final Matrix4f pose = new Matrix4f();
    private final NameTagComponent nameTag;
    private float lastX, lastY, lastZ, lastAngle, lastScale;
    private int lastLight;
    private boolean written;

    public ShrapnelVisual(VisualizationContext context, T entity, float partialTick) {
        super(context, entity, partialTick);
        instance =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, Models.MODEL)
                        .createInstance();
        nameTag = new NameTagComponent(context, entity);
        writeFrame(partialTick);
    }

    public static void initModels() {
        Models.init();
    }

    private static float scale(Entity entity) {
        return entity instanceof ShrapnelEntity shrapnel
                        && shrapnel.getTrail() >= ShrapnelEntity.TRAIL_VOLCANO
                ? 3F
                : 1F;
    }

    private static PackedQuadMesh mesh() {
        return new CuboidMesh(16F, 8F)
                .box(new Matrix4f(), 0F, 0F, 1F, -.5F, -.5F, 4F, 4F, 4F, 0F)
                .mesh();
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        nameTag.beginFrame(context);
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float angle = ((entity.tickCount % 360) * 10F + partialTick) * Mth.DEG_TO_RAD;
        float size = scale(entity);
        int light = computePackedLight(partialTick);
        if (written
                && lastX == visualPos.x
                && lastY == visualPos.y
                && lastZ == visualPos.z
                && lastAngle == angle
                && lastScale == size
                && lastLight == light) return;
        pose.translation(visualPos.x, visualPos.y, visualPos.z)
                .rotateX(Mth.PI)
                .rotate(angle, TUMBLE.x, TUMBLE.y, TUMBLE.z)
                .scale(size);
        instance.setTransform(pose).light(light).setChanged();
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastAngle = angle;
        lastScale = size;
        lastLight = light;
        written = true;
    }

    @Override
    protected void _delete() {
        instance.delete();
        nameTag.delete();
    }

    private static final class Models {
        private static final Model MODEL = create();

        static void init() {}

        private static Model create() {
            Material material =
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(ResourceManager.shrapnel_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .build();
            return new SingleMeshModel(mesh(), material);
        }
    }
}
