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

import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ContrailParticle extends Particle {

    public static final ParticleRenderType CONTRAIL_GROUP =
            new ParticleRenderType("hbm:contrail", "HC");

    public static final int SUBQUADS = 6;

    private static int seedSeq;

    public final float[] mod = new float[SUBQUADS];
    public final double[] gaussX = new double[SUBQUADS];
    public final double[] gaussY = new double[SUBQUADS];
    public final double[] gaussZ = new double[SUBQUADS];

    private final float scale;
    private final float red, green, blue;
    private float alpha = 1F;

    public ContrailParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float scale) {
        super(level, x, y, z);
        this.lifetime = 100 + this.random.nextInt(40);
        this.red = r;
        this.green = g;
        this.blue = b;
        this.scale = scale;
        this.xd = this.yd = this.zd = 0;

        Random urandom = new Random(seedSeq++);
        for (int i = 0; i < SUBQUADS; i++) {
            this.mod[i] = urandom.nextFloat() * 0.2F + 0.2F;
            this.gaussX[i] = urandom.nextGaussian();
            this.gaussY[i] = urandom.nextGaussian();
            this.gaussZ[i] = urandom.nextGaussian();
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.alpha = 1F - (float) this.age / (float) this.lifetime;

        this.age++;
        if (this.age == this.lifetime) this.remove();
    }

    public float alpha() {
        return this.alpha;
    }

    public float quadScale() {
        return (this.alpha + 0.5F) * this.scale;
    }

    public float spread() {
        return this.scale;
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

    public float red() {
        return this.red;
    }

    public float green() {
        return this.green;
    }

    public float blue() {
        return this.blue;
    }

    @Override
    public ParticleRenderType getGroup() {
        return CONTRAIL_GROUP;
    }
}
