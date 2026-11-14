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

import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.main.ResourceManager;
import com.hbm.particle.SkeletonParticle;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class SkeletonVisual implements EffectVisual<SkeletonParticle>, SimpleDynamicVisual {
    private static final Model[] CLEAN = models(ResourceManager.skeleton_tex);
    private static final Model[] GIB = models(ResourceManager.skeleton_blood_tex);
    private static final Model VILLAGER =
            model(SkeletonParticle.PART_SKULL_VILLAGER, ResourceManager.skoilet_tex);
    private static final Model VILLAGER_GIB =
            model(SkeletonParticle.PART_SKULL_VILLAGER, ResourceManager.skoilet_blood_tex);

    private final SkeletonParticle particle;
    private final TransformedInstance instance;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public SkeletonVisual(
            VisualizationContext context, SkeletonParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        int part = particle.part();
        Model model =
                part == SkeletonParticle.PART_SKULL_VILLAGER
                        ? particle.getGroup() == SkeletonParticle.GIB_EXT_GROUP
                                ? VILLAGER_GIB
                                : VILLAGER
                        : particle.getGroup() == SkeletonParticle.GIB_GROUP
                                ? GIB[part]
                                : CLEAN[part];
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, model)
                        .createInstance();
        writeFrame(partialTick);
    }

    private static Model[] models(Identifier texture) {
        Model[] out = new Model[3];
        for (int i = 0; i < out.length; i++) out[i] = model(i, texture);
        return out;
    }

    private static Model model(int part, Identifier texture) {
        return new SingleMeshModel(
                PackedQuadMesh.of(ResourceManager.skeleton_obj, SkeletonParticle.partId(part)),
                SimpleMaterial.builder()
                        .texture(texture)
                        .mipmap(false)
                        .useOverlay(false)
                        .transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .cutout(new SimpleCutoutShader(RegistryUtil.id("cutout/zero.glsl")))
                        .light(LightShaders.FLAT)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.ENTITY)
                        .backfaceCulling(true)
                        .build());
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        instance.setTransform(
                pose.translation(x, y, z)
                        .rotateY((float) Math.toRadians(particle.yaw(partialTick)))
                        .rotateX((float) Math.toRadians(particle.pitch(partialTick)))
                        .rotateY(-(float) Math.PI / 2F));
        instance.colorArgb(particle.argb(partialTick))
                .light(particle.light(partialTick))
                .setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
