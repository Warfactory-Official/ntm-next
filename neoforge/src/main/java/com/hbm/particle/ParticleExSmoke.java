// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.particle.ExSmokeVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import org.jspecify.annotations.Nullable;

public class ParticleExSmoke extends LegacyMoveParticle implements VisualParticle {

    public static final ParticleRenderType EX_SMOKE_GROUP =
            new ParticleRenderType("hbm:ex_smoke", "HXS");

    private static int seedSeq;

    public final int seed;
    private @Nullable VisualizationManager visualManager;

    public ParticleExSmoke(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 100 + this.random.nextInt(40);
        this.seed = seedSeq++;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.7599999785423279D;
        this.yd *= 0.7599999785423279D;
        this.zd *= 0.7599999785423279D;

        this.move(this.xd, this.yd, this.zd);
    }

    public float alpha(float partialTicks) {
        return 1F - (float) this.age / (float) this.lifetime;
    }

    public int light(float partialTicks) {
        return getLightCoords(partialTicks);
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
        return new ExSmokeVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return EX_SMOKE_GROUP;
    }
}
