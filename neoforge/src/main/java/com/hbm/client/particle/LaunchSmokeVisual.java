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

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class LaunchSmokeVisual
        implements EffectVisual<LaunchSmokeParticle>, SimpleTickableVisual {
    private final LaunchSmokeParticle particle;
    private final InstanceParticle[] instances = new InstanceParticle[LaunchSmokeParticle.WISPS];
    private final Vec3i origin;

    public LaunchSmokeVisual(VisualizationContext context, LaunchSmokeParticle particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        var instancer =
                context.instancerProvider().instancer(InstanceParticle.TYPE, ParticleModels.UNLIT);
        for (int i = 0; i < instances.length; i++) {
            instances[i] = instancer.createInstance();
        }
        writeTick();
    }

    @Override
    public void tick(Context context) {
        writeTick();
    }

    private void writeTick() {
        boolean visible = particle.isAlive();
        float scale = particle.quadSize;
        float[] pattern = particle.wispPattern();
        for (int i = 0; i < instances.length; i++) {
            int at = i * 4;
            InstanceParticle instance = instances[i];
            instance.setVisible(visible);
            if (!visible) continue;
            instance.previousPosition(
                    (float) (particle.xo - origin.getX()) + pattern[at] * scale,
                    (float) (particle.yo - origin.getY()) + pattern[at + 1] * scale,
                    (float) (particle.zo - origin.getZ()) + pattern[at + 2] * scale);
            instance.position(
                    (float) (particle.x - origin.getX()) + pattern[at] * scale,
                    (float) (particle.y - origin.getY()) + pattern[at + 1] * scale,
                    (float) (particle.z - origin.getZ()) + pattern[at + 2] * scale);
            instance.size(2F * scale);
            instance.animation(particle.oRoll, particle.roll, particle.age, particle.getLifetime());
            instance.uvRegion(
                    particle.visualU0(),
                    particle.visualV0(),
                    particle.visualU1() - particle.visualU0(),
                    particle.visualV1() - particle.visualV0());
            float grey = pattern[at + 3];
            instance.color(grey, grey, grey, particle.alpha)
                    .light(LightCoordsUtil.pack(15, 0))
                    .setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (InstanceParticle instance : instances) instance.delete();
    }
}
