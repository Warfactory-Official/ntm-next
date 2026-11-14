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

import com.hbm.particle.PlasmaBlastParticle;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

public final class PlasmaBlastVisual
        implements EffectVisual<PlasmaBlastParticle>, SimpleDynamicVisual {
    private final PlasmaBlastParticle particle;
    private final TransformedInstance instance;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public PlasmaBlastVisual(
            VisualizationContext context, PlasmaBlastParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, AdditiveParticleModels.SHOCKWAVE)
                        .createInstance();
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
        float s = particle.renderScale(partialTick);
        instance.setTransform(
                pose.translation(x, y, z)
                        .rotateY((float) Math.toRadians(particle.yaw))
                        .rotateX((float) Math.toRadians(particle.pitch))
                        .scale(s));
        instance.colorArgb(
                        ARGB.colorFromFloat(
                                particle.alpha(partialTick),
                                particle.red,
                                particle.green,
                                particle.blue))
                .setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
