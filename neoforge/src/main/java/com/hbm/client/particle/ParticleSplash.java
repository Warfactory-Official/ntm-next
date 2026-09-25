// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.SplashParticleOptions;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class ParticleSplash extends SingleQuadParticle implements VisualParticle {

    private static final VarHandle NEXT_PARTICLE_ID;
    private static long nextParticleId;

    static {
        try {
            NEXT_PARTICLE_ID =
                    MethodHandles.lookup()
                            .findStaticVarHandle(
                                    ParticleSplash.class, "nextParticleId", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final boolean flipU;
    private final boolean flipV;
    private @Nullable VisualizationManager visualManager;

    protected ParticleSplash(
            ClientLevel level,
            double x,
            double y,
            double z,
            SplashParticleOptions opts,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.alpha = 0.5F;
        this.quadSize = 0.4F;
        this.lifetime = 200 + this.random.nextInt(50);
        this.gravity = 0.4F;

        long id = (long) NEXT_PARTICLE_ID.getAndAdd(1L);
        this.flipU = id % 2 == 0;
        this.flipV = id % 4 < 2;

        if (opts.color() == SplashParticleOptions.NO_TINT) {
            this.rCol = this.gCol = this.bCol = 1.0F - this.random.nextFloat() * 0.2F;
        } else {
            float f = 1F - this.random.nextFloat() * 0.2F;
            this.rCol = ARGB.redFloat(opts.color()) * f;
            this.gCol = ARGB.greenFloat(opts.color()) * f;
            this.bCol = ARGB.blueFloat(opts.color()) * f;
        }
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
        return new SplashVisual(context, this);
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
        super.tick();
        if (this.onGround) {
            this.remove();
        } else {
            this.xd += this.random.nextGaussian() * 0.002D;
            this.zd += this.random.nextGaussian() * 0.002D;
            if (this.yd < -0.5D) this.yd = -0.5D;
        }
    }

    @Override
    protected float getU0() {
        return this.flipU ? super.getU1() : super.getU0();
    }

    @Override
    protected float getU1() {
        return this.flipU ? super.getU0() : super.getU1();
    }

    @Override
    protected float getV0() {
        return this.flipV ? super.getV1() : super.getV0();
    }

    @Override
    protected float getV1() {
        return this.flipV ? super.getV0() : super.getV1();
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SplashParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                SplashParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xa,
                double ya,
                double za,
                RandomSource random) {

            int setting = Minecraft.getInstance().options.particles().get().ordinal();
            if (!(setting == 0 || (setting == 1 && random.nextBoolean()))) return null;
            return new ParticleSplash(level, x, y, z, options, this.sprites.get(random));
        }
    }
}
