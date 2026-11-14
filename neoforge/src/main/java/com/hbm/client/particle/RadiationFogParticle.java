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

import com.hbm.particle.HbmParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RadiationFogParticle extends SingleQuadParticle {

    protected RadiationFogParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            float size,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.lifetime = 400;
        this.quadSize = size;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.setColor(0.85F, 0.9F, 0.5F);
        this.setAlpha(alphaForAge(0));
    }

    public static void spawnCluster(Level level, double x, double y, double z) {
        for (int i = 0; i < RadiationFogPattern.QUAD_COUNT; i++) {
            level.addParticle(
                    HbmParticles.RAD_FOG.get(),
                    true,
                    false,
                    x + RadiationFogPattern.OFF_X[i] + RadiationFogPattern.JIT_X[i],
                    y + RadiationFogPattern.OFF_Y[i] + RadiationFogPattern.JIT_Y[i],
                    z + RadiationFogPattern.OFF_Z[i] + RadiationFogPattern.JIT_Z[i],
                    0.0D,
                    RadiationFogPattern.SIZE[i],
                    0.0D);
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (++this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        this.setAlpha(alphaForAge(this.age));
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 0);
    }

    private float alphaForAge(int age) {
        float a = (float) Math.sin(age * Math.PI / this.lifetime) * 0.125F;
        return a < 0.0F ? 0.0F : a;
    }

    @Override
    protected Layer getLayer() {
        return ParticleLayers.TRANSLUCENT_SEPARATE;
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
            return new RadiationFogParticle(level, x, y, z, (float) ya, this.sprites.get(random));
        }
    }
}
