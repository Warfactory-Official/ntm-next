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

import com.hbm.particle.ExplosionSmallParticle;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;

public final class ExplosionSmallVisual
        implements EffectVisual<ExplosionSmallParticle>, SimpleDynamicVisual {
    private final ExplosionSmallParticle particle;
    private final InstanceParticle instance;
    private final Vec3i origin;

    public ExplosionSmallVisual(
            VisualizationContext context, ExplosionSmallParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceParticle.TYPE, ParticleModels.LIT_PARTICLE_BASE)
                        .createInstance();
        instance.uvRegion(0F, 0F, 1F, 1F);
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
        float roll = (float) Math.toRadians(particle.roll(partialTick));
        int color = particle.color(partialTick);
        instance.position(x, y, z);
        instance.previousPosition(x, y, z);
        instance.size(2F * particle.renderScale(partialTick));
        instance.animation(roll, roll, 0F, 1F);
        instance.color(
                ARGB.red(color) / 255F,
                ARGB.green(color) / 255F,
                ARGB.blue(color) / 255F,
                particle.alpha(partialTick));
        instance.light(particle.light(partialTick)).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
