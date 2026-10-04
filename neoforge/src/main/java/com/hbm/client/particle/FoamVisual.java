// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleFoam;
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

public final class FoamVisual implements EffectVisual<ParticleFoam>, SimpleDynamicVisual {
    private final ParticleFoam particle;
    private BillboardInstance[] instances = new BillboardInstance[0];
    private final Instancer<BillboardInstance> instancer;
    private final Vec3i origin;
    private int active;

    public FoamVisual(VisualizationContext context, ParticleFoam particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instancer =
                context.instancerProvider()
                        .instancer(InstanceTypes.BILLBOARD, ParticleModels.LIT_PARTICLE_BASE);
        writeFrame(partialTick);
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
            for (ParticleFoam.Point point : particle.trail()) {
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
            int b = i * ParticleFoam.BUBBLE_STRIDE;
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
