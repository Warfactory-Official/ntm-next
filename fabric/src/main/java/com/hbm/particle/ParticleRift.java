// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleRift extends Particle {

    public static final ParticleRenderType RIFT_GROUP = new ParticleRenderType("hbm:rift", "HRI");

    public static final float[] SHELLS = {
        1F, 1.02F, 1.02F * 1.05F, 1.02F * 1.05F * 1.02F, 1.02F * 1.05F * 1.02F * 1.05F
    };

    public ParticleRift(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 10;
    }

    public float shellScale(float pt) {
        return (this.age + pt) * 0.5F;
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
        return RIFT_GROUP;
    }
}
