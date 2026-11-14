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

import com.hbm.particle.FoamParticle;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Arrays;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;

public final class FoamVisual implements EffectVisual<FoamParticle>, SimpleDynamicVisual {
    private final FoamParticle particle;
    private BillboardInstance[] instances = new BillboardInstance[0];
    private final Instancer<BillboardInstance> instancer;
    private final Vec3i origin;
    private int active;

    public FoamVisual(VisualizationContext context, FoamParticle particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instancer =
                context.instancerProvider()
                        .instancer(InstanceTypes.BILLBOARD, ParticleModels.LIT_PARTICLE_BASE);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        int count = 0;
        if (particle.isAlive()) {
            int bubbles = particle.bubbleCount();
            int needed = bubbles * Math.max(1, particle.trail().size());
            if (needed > instances.length) {
                int old = instances.length;
                instances = Arrays.copyOf(instances, needed);
                for (int i = old; i < needed; i++) {
                    instances[i] = instancer.createInstance();
                    instances[i].uvRegion(0F, 0F, 1F, 1F).setVisible(false);
                }
            }
            float spread = particle.spread();
            int light = particle.light(partialTick);
            count =
                    cluster(
                            count,
                            particle.headX(),
                            particle.headY(),
                            particle.headZ(),
                            particle.scale(),
                            particle.alpha(),
                            spread,
                            bubbles,
                            light,
                            particle.headBubbles());
            int i = 0;
            for (FoamParticle.Point point : particle.trail()) {
                if (i > 0) {
                    float fade = 1F - (float) i / particle.trailLength();
                    count =
                            cluster(
                                    count,
                                    point.x(),
                                    point.y(),
                                    point.z(),
                                    particle.scale() * fade,
                                    particle.alpha() * fade * 0.7F,
                                    spread,
                                    bubbles,
                                    light,
                                    point.bubbles());
                }
                i++;
            }
        }
        for (int i = count; i < active; i++) instances[i].setVisible(false);
        active = count;
    }

    private int cluster(
            int count,
            double x,
            double y,
            double z,
            float scale,
            float alpha,
            float spread,
            int bubbles,
            int light,
            float[] data) {
        if (alpha <= 0F) return count;
        int a = Mth.clamp((int) (alpha * 255F), 0, 255);
        for (int i = 0; i < bubbles; i++) {
            int b = i * FoamParticle.BUBBLE_STRIDE;
            int white = Mth.clamp((int) (data[b + 4] * 255F), 0, 255);
            int color = a << 24 | white << 16 | white << 8 | white;
            float s = scale * data[b + 3];
            BillboardInstance instance = instances[count++];
            instance.setVisible(true);
            instance.position(
                    (float) (x - origin.getX()) + data[b] * spread,
                    (float) (y - origin.getY()) + data[b + 1] * spread * 0.7F,
                    (float) (z - origin.getZ()) + data[b + 2] * spread);
            instance.size(2F * s).colorArgb(color).light(light).setChanged();
        }
        return count;
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance instance : instances) instance.delete();
    }
}
