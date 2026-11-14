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
import com.hbm.particle.AshesParticle;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class AshesVisual implements EffectVisual<AshesParticle>, SimpleDynamicVisual {
    private static final int WHITE = 0xFFFFFFFF;
    private static final Model FLAT = settledModel(ParticleModels.LIT_PARTICLE_BASE);

    private final AshesParticle particle;
    private final InstanceParticle falling;
    private final TransformedInstance settled;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public static Model fallingModel(Identifier texture) {
        return ParticleModels.texturedLit(texture);
    }

    public static Model settledModel(Model fallingModel) {
        return new SingleMeshModel(
                PackedQuadMesh.builder(1)
                        .normal(0F, 1F, 0F)
                        .vertex(1D, 0D, 1D, 1D, 1D, WHITE)
                        .vertex(1D, 0D, -1D, 1D, 0D, WHITE)
                        .vertex(-1D, 0D, -1D, 0D, 0D, WHITE)
                        .vertex(-1D, 0D, 1D, 0D, 1D, WHITE)
                        .build(),
                fallingModel.meshes().getFirst().material());
    }

    public AshesVisual(VisualizationContext context, AshesParticle particle, float partialTick) {
        this(context, particle, partialTick, ParticleModels.LIT_PARTICLE_BASE, FLAT);
    }

    public AshesVisual(
            VisualizationContext context,
            AshesParticle particle,
            float partialTick,
            Model fallingModel,
            Model settledModel) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.falling =
                context.instancerProvider()
                        .instancer(InstanceParticle.TYPE, fallingModel)
                        .createInstance();
        this.settled =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, settledModel)
                        .createInstance();
        falling.uvRegion(0F, 0F, 1F, 1F);
        writeFrame(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean flat = particle.isAlive() && particle.settled();
        boolean fallingVisible = particle.isAlive() && !flat;
        falling.setVisible(fallingVisible);
        settled.setVisible(flat);
        if (!particle.isAlive()) return;

        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        float scale = particle.size();
        float spin = (float) Math.toRadians(particle.spin(partialTick));
        int shade = (int) (particle.shade() * 255F);
        int alpha = (int) (particle.alpha(partialTick) * 255F);
        int color = alpha << 24 | shade << 16 | shade << 8 | shade;
        int light = particle.light(partialTick);

        if (flat) {
            settled.setTransform(
                    pose.translation(x, particle.settledY(y), z).rotateY(spin).scale(scale));
            settled.colorArgb(color).light(light).setChanged();
        } else {
            falling.position(x, y, z);
            falling.previousPosition(x, y, z);
            falling.size(2F * scale);
            falling.animation(spin, spin, 0F, 1F);
            falling.colorArgb(color).light(light).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        falling.delete();
        settled.delete();
    }
}
