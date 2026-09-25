// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class ParticleVolcanoSmoke extends BaseAshSmokeParticle implements VisualParticle {
    private @Nullable VisualizationManager visualManager;

    private ParticleVolcanoSmoke(
            ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(
                level, x, y, z, 0.1F, 0.1F, 0.1F, 0.0D, 0.0D, 0.0D, 1.0F, sprites, 0.3F, 8, -0.1F,
                true);

        this.quadSize = 10.0F;
        this.lifetime = 200 + this.random.nextInt(50);
        this.hasPhysics = false;
        this.xd = this.random.nextGaussian() * 0.2D;
        this.yd = 2.5D + this.random.nextDouble();
        this.zd = this.random.nextGaussian() * 0.2D;
    }

    @Override
    public Layer getLayer() {
        return ParticleLayers.TRANSLUCENT;
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
        return new VolcanoSmokeVisual(context, this);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        if (!ownsVisual()) super.extract(state, camera, partialTick);
    }

    float visualU0() {
        return getU0();
    }

    float visualU1() {
        return getU1();
    }

    float visualV0() {
        return getV0();
    }

    float visualV1() {
        return getV1();
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                SimpleParticleType options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                RandomSource random) {
            return new ParticleVolcanoSmoke(level, x, y, z, this.sprites);
        }
    }
}
