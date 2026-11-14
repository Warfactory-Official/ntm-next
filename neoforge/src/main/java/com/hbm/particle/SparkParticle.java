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
import net.minecraft.client.particle.ParticleRenderType;

public class SparkParticle extends LegacyMoveParticle {

    public static final ParticleRenderType SPARK_GROUP = new ParticleRenderType("hbm:spark", "HSP");

    private static final int MAX_TRAIL = 6;
    private static final double FALL = 0.04D * 0.5D;
    private static final double BOUNCE = -0.8D;

    private final double[] trail = new double[MAX_TRAIL * 3];
    private int oldest;
    private int kept;
    private int trailLength;

    public SparkParticle(
            ClientLevel level, double x, double y, double z, double mX, double mY, double mZ) {
        super(level, x, y, z, mX, mY, mZ);
        trailLength = 4 + random.nextInt(3);
        remember();
        lifetime = 20 + random.nextInt(10);
    }

    public SparkParticle makeSmall(boolean small) {
        if (!small) return this;
        setSize(0.01F, 0.01F);
        trailLength = 3;
        lifetime = 2 + random.nextInt(3);
        yd = -Math.abs(yd);
        return this;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) remove();
        remember();
        yd -= FALL;
        double impact = yd;
        move(xd, yd, zd);
        if (!onGround) return;
        onGround = false;
        yd = impact * BOUNCE;
    }

    private void remember() {
        for (; kept >= trailLength; kept--) oldest = (oldest + 1) % MAX_TRAIL;
        int slot = (oldest + kept++) % MAX_TRAIL * 3;
        trail[slot] = xd;
        trail[slot + 1] = yd;
        trail[slot + 2] = zd;
    }

    public int stepCount() {
        return kept;
    }

    public double step(int step, int axis) {
        return trail[(oldest + step) % MAX_TRAIL * 3 + axis];
    }

    public double interpX(float pt) {
        return xo + (x - xo) * pt;
    }

    public double interpY(float pt) {
        return yo + (y - yo) * pt;
    }

    public double interpZ(float pt) {
        return zo + (z - zo) * pt;
    }

    @Override
    public ParticleRenderType getGroup() {
        return SPARK_GROUP;
    }
}
