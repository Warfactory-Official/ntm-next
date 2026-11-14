/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.awt.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class GasFlameParticle extends SingleQuadParticle implements VisualParticle {
    private static SpriteSet activeSprites;
    private final SpriteSet sprites;
    private final float colorMod;
    private @Nullable VisualizationManager visualManager;

    protected GasFlameParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            float scale,
            SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.get(0, 1));
        this.sprites = sprites;
        this.xd = this.xd * 0.1D + mx;
        this.yd = this.yd * 0.1D + my * 1.5D;
        this.zd = this.zd * 0.1D + mz;
        this.quadSize *= 0.75F * scale;
        this.lifetime = 30 + this.random.nextInt(13);
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.colorMod = 0.8F + this.random.nextFloat() * 0.2F;
        setColor(1F, 1F, 0F);
        setSpriteFromAge(sprites);
    }

    public static void spawn(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            float scale) {
        Minecraft.getInstance()
                .particleEngine
                .add(new GasFlameParticle(level, x, y, z, mx, my, mz, scale, activeSprites));
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
        return new GasFlameVisual(context, this);
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

        setSpriteFromAge(sprites);

        double previousYd = this.yd;
        this.move(this.xd, this.yd + 0.004D, this.zd);
        this.xd *= 0.96D * 0.75D;
        this.yd = previousYd + 0.005D;
        this.zd *= 0.96D * 0.75D;
        updateColor();
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.quadSize
                * Mth.clamp((this.age + partialTicks) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    private void updateColor() {
        float time = (float) this.age / (float) this.lifetime;
        Color color =
                Color.getHSBColor(
                        Math.max((60 - time * 100) / 360F, 0.0F),
                        1 - time * 0.25F,
                        1 - time * 0.5F);
        this.rCol = (color.getRed() / 255F) * colorMod;
        this.gCol = (color.getGreen() / 255F) * colorMod;
        this.bCol = (color.getBlue() / 255F) * colorMod;
    }

    @Override
    public int getLightCoords(float partialTicks) {
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
            activeSprites = sprites;
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
            return new GasFlameParticle(level, x, y, z, xa, ya, za, 6.5F, this.sprites);
        }
    }
}
