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
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public class ParticleHadron extends SingleQuadParticle implements VisualParticle {
    private static final int MAX_AGE = 10;
    private static final float GROWTH_PER_TICK = 0.15F;
    private float scale = 1F;
    private @Nullable VisualizationManager visualManager;

    protected ParticleHadron(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.get(0, 1));
        this.lifetime = MAX_AGE;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.xd = this.yd = this.zd = 0.0D;
    }

    public ParticleHadron makeSmall(boolean small) {
        if (!small) return this;
        this.scale = 0.5F;
        this.lifetime = 5;
        return this;
    }

    float scale() {
        return scale;
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
        return new HadronVisual(context, this);
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

    @Override
    protected void extractRotatedQuad(
            QuadParticleRenderState state,
            Quaternionf rotation,
            float x,
            float y,
            float z,
            float partialTick) {
        this.alpha = 1F - (this.age + partialTick) / this.lifetime;
        super.extractRotatedQuad(state, rotation, x, y, z, partialTick);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return (this.age + partialTicks) * GROWTH_PER_TICK * this.scale;
    }

    @Override
    public int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 0);
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.ADDITIVE;
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
            return new ParticleHadron(level, x, y, z, this.sprites);
        }
    }
}
