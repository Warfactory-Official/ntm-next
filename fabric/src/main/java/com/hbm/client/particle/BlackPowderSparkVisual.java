// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class BlackPowderSparkVisual
        implements EffectVisual<ParticleBlackPowderSpark>, SimpleTickableVisual {
    private final ParticleBlackPowderSpark particle;
    private final ParticleInstance instance;
    private final Vec3i origin;

    public BlackPowderSparkVisual(VisualizationContext context, ParticleBlackPowderSpark particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(ParticleInstance.TYPE, ParticleModels.UNLIT)
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
        instance.animation(0F, 0F, 0F, 1F);
        instance.uvRegion(
                particle.visualU0(),
                particle.visualV0(),
                particle.visualU1() - particle.visualU0(),
                particle.visualV1() - particle.visualV0());
        instance.color(particle.rCol, particle.gCol, particle.bCol, particle.alpha)
                .light(LightCoordsUtil.FULL_BRIGHT)
                .setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
