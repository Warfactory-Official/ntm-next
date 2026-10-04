// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class LaunchSmokeVisual
        implements EffectVisual<ParticleLaunchSmoke>, SimpleTickableVisual {
    private final ParticleLaunchSmoke particle;
    private final ParticleInstance[] instances = new ParticleInstance[ParticleLaunchSmoke.WISPS];
    private final Vec3i origin;

    public LaunchSmokeVisual(VisualizationContext context, ParticleLaunchSmoke particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        var instancer =
                context.instancerProvider().instancer(ParticleInstance.TYPE, ParticleModels.UNLIT);
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
            ParticleInstance instance = instances[i];
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
        for (ParticleInstance instance : instances) instance.delete();
    }
}
