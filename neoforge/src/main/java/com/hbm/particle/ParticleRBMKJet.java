// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.particle.RBMKJetVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.jspecify.annotations.Nullable;

public abstract class ParticleRBMKJet extends Particle implements VisualParticle {

    private final RbmkJetShape shape;
    private @Nullable VisualizationManager visualManager;

    protected ParticleRBMKJet(ClientLevel level, double x, double y, double z, RbmkJetShape shape) {
        super(level, x, y, z);
        this.shape = shape;
    }

    public RbmkJetShape shape() {
        return this.shape;
    }

    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    public int clampedAge() {
        return Math.min(this.age, this.lifetime);
    }

    @Override
    public ClientLevel level() {
        return level;
    }

    @Override
    public @Nullable VisualizationManager visualManager() {
        return visualManager;
    }

    @Override
    public void visualManager(@Nullable VisualizationManager manager) {
        visualManager = manager;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext context, float partialTick) {
        return new RBMKJetVisual(context, this, partialTick);
    }
}
