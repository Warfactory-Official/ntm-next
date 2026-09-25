// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public final class ParticleSchrabFog {
    private ParticleSchrabFog() {}

    static final class SchrabFogParticle extends SuspendedParticle implements VisualParticle {
        private @Nullable VisualizationManager visualManager;

        private SchrabFogParticle(
                ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
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
            return new SchrabFogVisual(context, this);
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
                double xa,
                double ya,
                double za,
                RandomSource random) {
            SuspendedParticle particle =
                    new SchrabFogParticle(level, x, y, z, this.sprites.get(random));
            particle.setColor(0.0F, 1.0F, 1.0F);
            return particle;
        }
    }
}
