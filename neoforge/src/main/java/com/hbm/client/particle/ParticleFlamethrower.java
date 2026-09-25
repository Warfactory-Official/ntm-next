// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.FlameParticleOptions;
import com.hbm.particle.helper.FlameCreator;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.awt.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class ParticleFlamethrower extends SingleQuadParticle implements VisualParticle {
    public final int type;
    private final float baseR, baseG, baseB;
    private final float spin;
    private @Nullable VisualizationManager visualManager;

    protected ParticleFlamethrower(
            ClientLevel level, double x, double y, double z, int type, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(level.getRandom()));
        this.type = type;
        this.lifetime = 20 + this.random.nextInt(10);
        this.quadSize = 0.5F;
        this.gravity = 0F;
        this.friction = 1F;

        this.xd = this.random.nextGaussian() * 0.02;
        this.yd = 0;
        this.zd = this.random.nextGaussian() * 0.02;

        float initialColor = 15F + this.random.nextFloat() * 25F;
        if (type == FlameCreator.META_BALEFIRE) initialColor = 65F + this.random.nextFloat() * 35F;
        if (type == FlameCreator.META_DIGAMMA) initialColor = 0F - this.random.nextFloat() * 15F;

        Color color = Color.getHSBColor(initialColor / 255F, 1F, 1F);
        this.baseR = color.getRed() / 255F;
        this.baseG = color.getGreen() / 255F;
        this.baseB = color.getBlue() / 255F;

        this.spin = (this.random.nextBoolean() ? 30F : -30F) * 0.5F * ((float) Math.PI / 180F);

        updateVisuals();
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
        return new FlamethrowerVisual(context, this);
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

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        this.yd += 0.01D;

        this.oRoll = this.roll;
        this.roll += this.spin;

        this.move(this.xd, this.yd, this.zd);
        updateVisuals();
    }

    private void updateVisuals() {
        double ageScaled = (double) this.age / (double) this.lifetime;

        if (type == FlameCreator.META_OXY) {
            this.alpha = (float) (1 - ageScaled);
            float add = (float) ageScaled * 1.25F - 0.25F;
            setColor(1F - add, 1F - add * 0.75F, 1F);
        } else if (type == FlameCreator.META_BLACK) {
            this.alpha = (float) (1 - ageScaled);
            float add = (float) ageScaled * 2F - 0.25F;
            setColor(1F - add * 0.75F, 1F - add, 1F - add * 0.5F);
        } else {
            this.alpha = (float) Math.pow(1 - Math.min(ageScaled, 1), 0.5) * 0.5F;
            float add = 0.75F - (float) ageScaled;
            setColor(baseR + add, baseG + add, baseB + add);
        }
    }

    @Override
    public void setColor(float r, float g, float b) {
        super.setColor(Mth.clamp(r, 0F, 1F), Mth.clamp(g, 0F, 1F), Mth.clamp(b, 0F, 1F));
    }

    @Override
    public float getQuadSize(float partialTicks) {
        double ageScaled = (double) this.age / (double) this.lifetime;
        return (float) ((ageScaled * 1.25 + 0.25) * this.quadSize);
    }

    @Override
    public int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 0);
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<FlameParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                FlameParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xa,
                double ya,
                double za,
                RandomSource random) {
            return new ParticleFlamethrower(level, x, y, z, options.meta(), this.sprites);
        }
    }
}
