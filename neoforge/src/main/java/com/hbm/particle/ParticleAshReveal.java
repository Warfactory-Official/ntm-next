// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.particle.AshRevealVisual;
import com.hbm.client.particle.ParticleLayers;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class ParticleAshReveal extends SingleQuadParticle implements VisualParticle {

    private static final float BASE_SCALE = 0.1F * 7.5F;

    private static final float TINT_JITTER = 0.1F;
    private static final float RAMP = 32.0F;

    private final SpriteSet sprites;

    private @Nullable VisualizationManager visualManager;

    private ParticleAshReveal(
            ClientLevel level,
            double x,
            double y,
            double z,
            AshRevealParticleOptions tint,
            SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.first());
        this.sprites = sprites;
        this.friction = 0.96F;
        this.quadSize = BASE_SCALE;
        this.hasPhysics = false;

        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        float jitter = this.random.nextFloat() * TINT_JITTER;
        setColor(tint.red() + jitter, tint.green() + jitter, tint.blue() + jitter);

        int base = (int) (8.0D / (this.random.nextFloat() * 0.8D + 0.3D));
        this.lifetime = (int) Math.max(base * 2.5F, 1.0F);
        setSpriteFromAge(sprites);
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
        return new AshRevealVisual(context, this);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        if (!ownsVisual()) super.extract(state, camera, partialTick);
    }

    public float visualU0() {
        return getU0();
    }

    public float visualU1() {
        return getU1();
    }

    public float visualV0() {
        return getV0();
    }

    public float visualV1() {
        return getV1();
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return ParticleLayers.TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float partialTick) {
        return this.quadSize
                * Mth.clamp((this.age + partialTick) / this.lifetime * RAMP, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) setSpriteFromAge(this.sprites);
    }

    public static class Provider implements ParticleProvider<AshRevealParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                AshRevealParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xa,
                double ya,
                double za,
                RandomSource random) {
            return new ParticleAshReveal(level, x, y, z, options, this.sprites);
        }
    }
}
