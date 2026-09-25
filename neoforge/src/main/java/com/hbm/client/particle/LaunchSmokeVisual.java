// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.Random;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class LaunchSmokeVisual
        implements EffectVisual<ParticleLaunchSmoke>, SimpleTickableVisual {
    private static final int WISPS = 6;

    private final ParticleLaunchSmoke particle;
    private final ParticleInstance[] instances = new ParticleInstance[WISPS];
    private final float[] offsetX = new float[WISPS];
    private final float[] offsetY = new float[WISPS];
    private final float[] offsetZ = new float[WISPS];
    private final float[] grey = new float[WISPS];
    private final Vec3i origin;

    public LaunchSmokeVisual(VisualizationContext context, ParticleLaunchSmoke particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        var instancer =
                context.instancerProvider().instancer(ParticleInstance.TYPE, ParticleModels.UNLIT);
        Random random = new Random(particle.visualParticleId());
        for (int i = 0; i < WISPS; i++) {
            grey[i] = random.nextFloat() * .75F + .1F;
            offsetX[i] = (float) random.nextGaussian() * .5F;
            offsetY[i] = (float) random.nextGaussian() * .5F;
            offsetZ[i] = (float) random.nextGaussian() * .5F;
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
        for (int i = 0; i < WISPS; i++) {
            ParticleInstance instance = instances[i];
            instance.setVisible(visible);
            if (!visible) continue;
            instance.previousPosition(
                    (float) (particle.xo - origin.getX()) + offsetX[i] * scale,
                    (float) (particle.yo - origin.getY()) + offsetY[i] * scale,
                    (float) (particle.zo - origin.getZ()) + offsetZ[i] * scale);
            instance.position(
                    (float) (particle.x - origin.getX()) + offsetX[i] * scale,
                    (float) (particle.y - origin.getY()) + offsetY[i] * scale,
                    (float) (particle.z - origin.getZ()) + offsetZ[i] * scale);
            instance.size(2F * scale);
            instance.animation(particle.oRoll, particle.roll, particle.age, particle.getLifetime());
            instance.uvRegion(
                    particle.visualU0(),
                    particle.visualV0(),
                    particle.visualU1() - particle.visualU0(),
                    particle.visualV1() - particle.visualV0());
            instance.color(grey[i], grey[i], grey[i], particle.alpha)
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
