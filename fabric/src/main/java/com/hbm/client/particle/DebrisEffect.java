// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleDebris;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.Level;

public final class DebrisEffect implements Effect {

    final ParticleDebris particle;
    private final Level level;

    public DebrisEffect(Level level, ParticleDebris particle) {
        this.level = level;
        this.particle = particle;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new DebrisVisual(ctx, this);
    }

    boolean expired() {
        return !particle.isAlive();
    }
}
