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

public class DigammaSmokeParticle extends SingleQuadParticle implements VisualParticle {
    private @Nullable VisualizationManager visualManager;

    protected DigammaSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mx,
            double mz,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.lifetime = 100 + this.random.nextInt(40);
        this.quadSize = 5.0F;
        this.gravity = 0.0F;
        this.friction = 0.99F;
        this.hasPhysics = false;
        this.xd = mx;
        this.yd = 0.0;
        this.zd = mz;
        this.rCol = 0.5F + this.random.nextFloat() * 0.2F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
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
        return new DigammaSmokeVisual(context, this);
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

        this.setAlpha(1.0F - (float) this.age / this.lifetime);
        if (++this.age >= this.lifetime) this.remove();

        this.xd *= 0.99D;
        this.yd *= 0.99D;
        this.zd *= 0.99D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 0);
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
            return new DigammaSmokeParticle(level, x, y, z, xa, za, this.sprites.get(random));
        }
    }
}
