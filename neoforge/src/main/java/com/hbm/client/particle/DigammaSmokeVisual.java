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

public final class DigammaSmokeVisual
        implements EffectVisual<DigammaSmokeParticle>, SimpleTickableVisual {
    private final DigammaSmokeParticle particle;
    private final InstanceParticle instance;
    private final Vec3i origin;

    public DigammaSmokeVisual(VisualizationContext context, DigammaSmokeParticle particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceParticle.TYPE, ParticleModels.UNLIT)
                        .createInstance();
        writeTick();
    }

    @Override
    public void tick(Context context) {
        writeTick();
    }

    private void writeTick() {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        instance.position(
                (float) (particle.x - origin.getX()),
                (float) (particle.y - origin.getY()),
                (float) (particle.z - origin.getZ()));
        instance.previousPosition(
                (float) (particle.xo - origin.getX()),
                (float) (particle.yo - origin.getY()),
                (float) (particle.zo - origin.getZ()));
        instance.size(2F * particle.quadSize);
        instance.animation(particle.oRoll, particle.roll, particle.age, particle.getLifetime());
        instance.uvRegion(
                particle.visualU0(),
                particle.visualV0(),
                particle.visualU1() - particle.visualU0(),
                particle.visualV1() - particle.visualV0());
        instance.color(particle.rCol, particle.gCol, particle.bCol, particle.alpha)
                .light(LightCoordsUtil.pack(15, 0))
                .setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
