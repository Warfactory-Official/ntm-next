// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.Level;

public final class RocketFlameEffect implements Effect {

    private static final double DRAG = 0.91D;

    final double spawnX, spawnY, spawnZ;
    final float baseScale;
    final int maxAge;
    private final Level level;
    private double x, y, z;
    private double prevX, prevY, prevZ;
    private double vx, vy, vz;
    int age;

    public RocketFlameEffect(
            Level level,
            double x,
            double y,
            double z,
            float baseScale,
            double vx,
            double vy,
            double vz,
            int maxAge) {
        this.level = level;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.z = this.prevZ = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.baseScale = baseScale;
        this.maxAge = maxAge;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new RocketFlameVisual(ctx, this);
    }

    void tick() {
        prevX = x;
        prevY = y;
        prevZ = z;

        age++;
        if (expired()) return;

        vx *= DRAG;
        vy *= DRAG;
        vz *= DRAG;

        x += vx;
        y += vy;
        z += vz;
    }

    double interpX(float partialTick) {
        return prevX + (x - prevX) * partialTick;
    }

    double interpY(float partialTick) {
        return prevY + (y - prevY) * partialTick;
    }

    double interpZ(float partialTick) {
        return prevZ + (z - prevZ) * partialTick;
    }

    boolean expired() {
        return age >= maxAge;
    }
}
