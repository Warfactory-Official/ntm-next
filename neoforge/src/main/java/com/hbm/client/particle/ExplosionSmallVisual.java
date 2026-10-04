// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleExplosionSmall;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;

public final class ExplosionSmallVisual
        implements EffectVisual<ParticleExplosionSmall>, SimpleDynamicVisual {
    private final ParticleExplosionSmall particle;
    private final ParticleInstance instance;
    private final Vec3i origin;

    public ExplosionSmallVisual(
            VisualizationContext context, ParticleExplosionSmall particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(ParticleInstance.TYPE, ParticleModels.LIT_PARTICLE_BASE)
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
