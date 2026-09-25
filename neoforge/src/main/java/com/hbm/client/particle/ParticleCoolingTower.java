// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.CoolingTowerParticleOptions;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
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

public class ParticleCoolingTower extends SingleQuadParticle implements VisualParticle {
    private final float baseScale;
    private final float maxScale;
    private final float lift;
    private final float strafe;
    private final boolean windDir;
    private final float alphaMod;
    private @Nullable VisualizationManager visualManager;

    protected ParticleCoolingTower(
            ClientLevel level,
            double x,
            double y,
            double z,
            CoolingTowerParticleOptions opts,
            int particleSetting,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.baseScale = opts.baseScale();
        this.maxScale = opts.maxScale();
        this.lift = opts.lift();
        this.strafe = opts.strafe();
        this.windDir = opts.windDir();
        this.alphaMod = opts.alphaMod();
        this.lifetime = opts.life() / (particleSetting + 1);
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        updateVisualState(0F);

        if (opts.color() == CoolingTowerParticleOptions.NO_TINT) {
            this.rCol = this.gCol = this.bCol = 0.9F + this.random.nextFloat() * 0.05F;
        } else {
            this.rCol = ARGB.redFloat(opts.color());
            this.gCol = ARGB.greenFloat(opts.color());
            this.bCol = ARGB.blueFloat(opts.color());
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
        return new CoolingTowerVisual(context, this);
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

        float ageScale = (float) this.age / (float) this.lifetime;
        updateVisualState(ageScale);

        this.age++;

        if (this.lift > 0 && this.yd < this.lift) this.yd += 0.01F;
        if (this.lift < 0 && this.yd > this.lift) this.yd -= 0.01F;

        this.xd += this.random.nextGaussian() * this.strafe * ageScale;
        this.zd += this.random.nextGaussian() * this.strafe * ageScale;

        if (this.windDir) {
            this.xd += 0.02D * ageScale;
            this.zd -= 0.01D * ageScale;
        }

        if (this.age == this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd, this.yd, this.zd);

        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }

    private void updateVisualState(float ageScale) {

        this.alpha = this.alphaMod - ageScale * this.alphaMod;
        this.quadSize =
                this.baseScale + (float) Math.pow(this.maxScale * ageScale - this.baseScale, 2);
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<CoolingTowerParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                CoolingTowerParticleOptions options,
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
            return new ParticleCoolingTower(
                    level, x, y, z, options, setting, this.sprites.get(random));
        }
    }
}
