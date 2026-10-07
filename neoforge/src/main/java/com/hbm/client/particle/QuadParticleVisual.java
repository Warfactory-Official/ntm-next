// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.IQuadParticle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;

public final class QuadParticleVisual<T extends Particle & IQuadParticle & VisualParticle>
        implements EffectVisual<T>, SimpleDynamicVisual {
    private final T particle;
    private final ParticleInstance instance;
    private final Vec3i origin;

    public static <T extends Particle & IQuadParticle & VisualParticle>
            QuadParticleVisual<T> particleBase(
                    VisualizationContext context, T particle, float partialTick) {
        return new QuadParticleVisual<>(
                context, particle, ParticleModels.LIT_PARTICLE_BASE, partialTick);
    }

    public static <T extends Particle & IQuadParticle & VisualParticle>
            QuadParticleVisual<T> deadLeaf(
                    VisualizationContext context, T particle, float partialTick) {
        return new QuadParticleVisual<>(
                context, particle, ParticleModels.LIT_DEAD_LEAF, partialTick);
    }

    public static Model texturedLitModel(Identifier texture) {
        return ParticleModels.texturedLit(texture);
    }

    public QuadParticleVisual(
            VisualizationContext context, T particle, Model model, float partialTick) {
        this(context, particle, model, ParticleInstance.TYPE, partialTick);
    }

    public QuadParticleVisual(
            VisualizationContext context,
            T particle,
            Model model,
            InstanceType<ParticleInstance> type,
            float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance = context.instancerProvider().instancer(type, model).createInstance();
        instance.uvRegion(
                particle.u0(),
                particle.v0(),
                particle.u1() - particle.u0(),
                particle.v1() - particle.v0());
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
        float roll = (float) Math.toRadians(particle.spin(partialTick));
        instance.position(x, y, z);
        instance.previousPosition(x, y, z);
        instance.size(2F * particle.size(partialTick));
        instance.animation(roll, roll, 0F, 1F);
        instance.colorArgb(particle.argb(partialTick));
        instance.light(particle.light(partialTick)).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
