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

import com.hbm.particle.DebrisChunk;
import com.hbm.particle.DebrisMesh;
import com.hbm.particle.DebrisParticle;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class DebrisVisual implements EffectVisual<DebrisEffect>, SimpleDynamicVisual {

    private final DebrisEffect effect;
    private final List<TransformedInstance> instances = new ArrayList<>(3);
    private final Matrix4f pose = new Matrix4f();
    private final int originX, originY, originZ;
    private final float halfX, halfY, halfZ;

    public DebrisVisual(VisualizationContext ctx, DebrisEffect effect) {
        this.effect = effect;
        Vec3i origin = ctx.renderOrigin();
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();

        DebrisParticle particle = effect.particle;
        DebrisChunk chunk = particle.chunk();
        this.halfX = chunk.sizeX / 2F;
        this.halfY = chunk.sizeY / 2F;
        this.halfZ = chunk.sizeZ / 2F;
        float radius = Mth.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);

        ChunkSectionLayer[] layers = ChunkSectionLayer.values();
        DebrisMesh[] meshes = particle.meshes();
        for (int i = 0; i < layers.length; i++) {
            DebrisMesh mesh = meshes[i];
            if (mesh == null) continue;
            Model model =
                    new SingleMeshModel(new DebrisFlywheelMesh(mesh, radius), material(layers[i]));
            Instancer<TransformedInstance> instancer =
                    ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, model);
            TransformedInstance instance = instancer.createInstance();
            instance.overlay(OverlayTexture.NO_OVERLAY).light(0).colorArgb(0xFFFFFFFF);
            instances.add(instance);
        }
        writeFrame(0F);
    }

    private static Material material(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> Materials.SOLID_BLOCK;
            case CUTOUT -> Materials.CUTOUT_MIPPED_BLOCK;
            case TRANSLUCENT -> Materials.TRANSLUCENT_BLOCK;
        };
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        if (instances.isEmpty()) return;
        DebrisParticle particle = effect.particle;

        pose.translation(
                        (float) (particle.interpX(partialTick) - originX),
                        (float) (particle.interpY(partialTick) - originY),
                        (float) (particle.interpZ(partialTick) - originZ))
                .rotateY(particle.pitch(partialTick) * Mth.DEG_TO_RAD)
                .rotateZ(particle.yaw(partialTick) * Mth.DEG_TO_RAD)
                .translate(-halfX, -halfY, -halfZ);

        for (TransformedInstance instance : instances) {
            instance.setTransform(pose);
            instance.setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }
}
