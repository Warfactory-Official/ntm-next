// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleRBMKFlame;
import com.hbm.particle.ParticleRBMKJet;
import com.hbm.particle.RbmkJetShape;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class RBMKJetVisual implements EffectVisual<ParticleRBMKJet>, SimpleDynamicVisual {
    private final ParticleRBMKJet particle;
    private final ParticleInstance instance;
    private final Vec3i origin;

    public RBMKJetVisual(
            VisualizationContext context, ParticleRBMKJet particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(
                                ParticleInstance.RBMK_JET,
                                particle instanceof ParticleRBMKFlame
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
