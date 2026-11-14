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

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class RocketFlameParticle extends Particle {

    public static final ParticleRenderType ROCKET_FLAME_GROUP =
            new ParticleRenderType("hbm:rocket_flame", "HRO");

    private static int seedSeq;

    public final int seed;
    private float baseScale = 1F;
    private RocketFlameColor color = RocketFlameColor.FLAME;

    public RocketFlameParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 300 + this.random.nextInt(50);
        this.seed = seedSeq++;
    }

    public RocketFlameParticle setScale(float scale) {
        this.baseScale = scale;
        return this;
    }

    public RocketFlameParticle setMaxAge(int maxAge) {
        this.lifetime = maxAge;
        return this;
    }

    public RocketFlameParticle color(RocketFlameColor color) {
        this.color = color;
        return this;
    }

    public RocketFlameColor color() {
        return color;
    }

    public RocketFlameParticle noClip() {
        this.hasPhysics = false;
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age == this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;

        this.move(this.xd, this.yd, this.zd);
    }

    public float baseScale() {
        return this.baseScale;
    }

    public float alpha() {
        return (float) Math.pow(1 - Math.min((float) this.age / this.lifetime, 1F), 0.5) * 0.75F;
    }

    public float dark() {
        return 1F - Math.min((float) this.age / (this.lifetime * 0.25F), 1F);
    }

    public float spread() {
        return ((float) Math.pow((float) this.age / this.lifetime * 4F, 1.5) + 1F) * this.baseScale;
    }

    public float growth() {
        return (float) this.age / this.lifetime * 2F;
    }

    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ROCKET_FLAME_GROUP;
    }
}
