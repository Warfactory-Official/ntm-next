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

import com.hbm.client.render.RenderTextures;
import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.particle.AmatFlashParticle;
import com.hbm.render.util.Colors;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import org.joml.Matrix4f;

public final class AmatFlashVisual implements EffectVisual<AmatFlashParticle>, SimpleDynamicVisual {
    private static final Model MODEL =
            new SingleMeshModel(
                    mesh(), AdditiveParticleModels.material(RenderTextures.WHITE, true, true));

    private final AmatFlashParticle particle;
    private final TransformedInstance instance;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public AmatFlashVisual(
            VisualizationContext context, AmatFlashParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MODEL)
                        .createInstance();
        writeFrame(partialTick);
    }

    private static Mesh mesh() {
        PackedQuadMesh.Builder mesh =
                PackedQuadMesh.builder(AmatFlashParticle.SPIKES * 2).normal(0F, 0F, 1F);
        float[] rim = AmatFlashParticle.RIM;
        for (int i = 0; i < AmatFlashParticle.SPIKES; i++) {
            int b = i * 9;
            mesh.vertex(0D, 0D, 0D, 0xFFFFFFFF);
            mesh.vertex(rim[b], rim[b + 1], rim[b + 2], 0x00FFFFFF);
            mesh.vertex(rim[b + 3], rim[b + 4], rim[b + 5], 0x00FFFFFF);
            mesh.vertex(rim[b + 6], rim[b + 7], rim[b + 8], 0x00FFFFFF);

            mesh.vertex(0D, 0D, 0D, 0xFFFFFFFF);
            mesh.vertex(rim[b + 6], rim[b + 7], rim[b + 8], 0x00FFFFFF);
            mesh.vertex(rim[b], rim[b + 1], rim[b + 2], 0x00FFFFFF);
            mesh.vertex(rim[b], rim[b + 1], rim[b + 2], 0x00FFFFFF);
        }
        return mesh.build();
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
        instance.setTransform(pose.translation(x, y, z).scale(particle.extent(partialTick)));
        instance.colorArgb(Colors.argb(particle.coreAlpha(partialTick), 1F, 1F, 1F));
        instance.light(particle.light(partialTick)).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
