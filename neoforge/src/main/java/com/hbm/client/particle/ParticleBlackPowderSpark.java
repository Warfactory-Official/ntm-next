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

public class ParticleBlackPowderSpark extends SingleQuadParticle implements VisualParticle {
    private @Nullable VisualizationManager visualManager;

    protected ParticleBlackPowderSpark(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;

        float f = this.random.nextFloat() * 0.1F + 0.2F;
        this.rCol = f + 0.7F;
        this.gCol = f + 0.5F;
        this.bCol = f;
        this.setSize(0.02F, 0.02F);
        this.quadSize *= this.random.nextFloat() * 0.6F + 0.5F;
        this.lifetime = 15 + this.random.nextInt(5);
        this.gravity = 0.01F;
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
        return new BlackPowderSparkVisual(context, this);
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
        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.95D;
        this.yd *= 0.95D;
        this.zd *= 0.95D;

        if (this.lifetime-- <= 0) this.remove();
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.TRANSLUCENT;
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
            return new ParticleBlackPowderSpark(
                    level, x, y, z, xa, ya, za, this.sprites.get(random));
        }
    }
}
