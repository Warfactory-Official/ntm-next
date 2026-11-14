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

import com.hbm.particle.RBMKFlameParticle;
import com.hbm.particle.RBMKJetParticle;
import com.hbm.particle.RbmkJetShape;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class RBMKJetVisual implements EffectVisual<RBMKJetParticle>, SimpleDynamicVisual {
    private final RBMKJetParticle particle;
    private final InstanceParticle instance;
    private final Vec3i origin;

    public RBMKJetVisual(
            VisualizationContext context, RBMKJetParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(
                                InstanceParticle.RBMK_JET,
                                particle instanceof RBMKFlameParticle
                                        ? AdditiveParticleModels.RBMK_FLAME
                                        : ParticleModels.RBMK_STEAM)
                        .createInstance();
        RbmkJetShape shape = particle.shape();
        float extent =
                Math.max(
                        Math.max(Math.abs(shape.x0()), Math.abs(shape.x1())),
                        Math.max(Math.abs(shape.y0()), Math.abs(shape.y1())));
        instance.size(4F * (extent + 2F));
        instance.animation(shape.x0(), shape.x1(), shape.y0(), shape.y1());
        instance.light(LightCoordsUtil.pack(15, 0));
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
        RbmkJetShape shape = particle.shape();
        float u = shape.uMin(particle.clampedAge());
        instance.previousPosition(x, y, z);
        instance.position(x, y, z);
        instance.uvRegion(u + shape.uStep(), 0F, -shape.uStep(), 1F);
        instance.color(1F, 1F, 1F, shape.alpha(particle.clampedAge())).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
