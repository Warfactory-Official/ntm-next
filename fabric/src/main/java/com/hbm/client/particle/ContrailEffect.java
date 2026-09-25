// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleContrail;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Random;
import net.minecraft.world.level.Level;

public final class ContrailEffect implements Effect {

    private static int seedSeq;

    final float[] mod = new float[ParticleContrail.SUBQUADS];
    final double[] gaussX = new double[ParticleContrail.SUBQUADS];
    final double[] gaussY = new double[ParticleContrail.SUBQUADS];
    final double[] gaussZ = new double[ParticleContrail.SUBQUADS];
    final double x, y, z;
    final float r, g, b, scale;
    final int maxAge;
    private final Level level;
    int age;

    public ContrailEffect(
            Level level,
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float scale,
            int maxAge) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.scale = scale;
        this.maxAge = maxAge;

        Random urandom = new Random(seedSeq++);
        for (int i = 0; i < ParticleContrail.SUBQUADS; i++) {
            this.mod[i] = urandom.nextFloat() * 0.2F + 0.2F;
            this.gaussX[i] = urandom.nextGaussian();
            this.gaussY[i] = urandom.nextGaussian();
            this.gaussZ[i] = urandom.nextGaussian();
        }
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new ContrailVisual(ctx, this);
    }

    boolean expired() {
        return age >= maxAge;
    }
}
