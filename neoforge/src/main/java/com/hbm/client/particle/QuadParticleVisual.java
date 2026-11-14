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

import com.hbm.particle.QuadParticle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;

public final class QuadParticleVisual<T extends Particle & QuadParticle & VisualParticle>
        implements EffectVisual<T>, SimpleDynamicVisual {
    private final T particle;
    private final InstanceParticle instance;
    private final Vec3i origin;

    public static <T extends Particle & QuadParticle & VisualParticle>
            QuadParticleVisual<T> particleBase(
                    VisualizationContext context, T particle, float partialTick) {
        return new QuadParticleVisual<>(
                context, particle, ParticleModels.LIT_PARTICLE_BASE, partialTick);
    }

    public static <T extends Particle & QuadParticle & VisualParticle>
            QuadParticleVisual<T> deadLeaf(
                    VisualizationContext context, T particle, float partialTick) {
        return new QuadParticleVisual<>(
                context, particle, ParticleModels.LIT_DEAD_LEAF, partialTick);
    }

    public static Model texturedLitModel(Identifier texture) {
        return ParticleModels.texturedLit(texture);
    }

    public QuadParticleVisual(
            VisualizationContext context, T particle, Model model, float partialTick) {
        this(context, particle, model, InstanceParticle.TYPE, partialTick);
    }

    public QuadParticleVisual(
            VisualizationContext context,
            T particle,
            Model model,
            InstanceType<InstanceParticle> type,
            float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance = context.instancerProvider().instancer(type, model).createInstance();
        instance.uvRegion(
                particle.u0(),
                particle.v0(),
                particle.u1() - particle.u0(),
                particle.v1() - particle.v0());
        writeFrame(partialTick);
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
        float roll = (float) Math.toRadians(particle.spin(partialTick));
        instance.position(x, y, z);
        instance.previousPosition(x, y, z);
        instance.size(2F * particle.size(partialTick));
        instance.animation(roll, roll, 0F, 1F);
        instance.colorArgb(particle.argb(partialTick));
        instance.light(particle.light(partialTick)).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
