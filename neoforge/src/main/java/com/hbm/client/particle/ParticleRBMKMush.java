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
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class ParticleRBMKMush extends SingleQuadParticle implements VisualParticle {
    private static final int FRAMES = 30;
    private @Nullable VisualizationManager visualManager;

    protected ParticleRBMKMush(
            ClientLevel level,
            double x,
            double y,
            double z,
            float scale,
            TextureAtlasSprite sheet) {
        super(level, x, y, z, sheet);
        this.lifetime = 50;
        this.quadSize = scale;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.rCol = this.gCol = this.bCol = 1.0F;
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
        return new RBMKMushVisual(context, this);
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

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) this.remove();
    }

    private int frame() {
        return Math.min(FRAMES - 1, this.age * FRAMES / this.lifetime);
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 0);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV((float) frame() / FRAMES);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((float) (frame() + 1) / FRAMES);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return ParticleLayers.ADDITIVE_NO_FOG;
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

            float scale = (float) Math.max(1.0D, ya);
            return new ParticleRBMKMush(level, x, y + scale, z, scale, this.sprites.get(0, 1));
        }
    }
}
