// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleDebugLine extends Particle {

    public static final ParticleRenderType DEBUG_LINE_GROUP =
            new ParticleRenderType("hbm:debug_line", "HDBL");

    private final int color;

    public ParticleDebugLine(
            ClientLevel level,
            double x,
            double y,
            double z,
            double lx,
            double ly,
            double lz,
            int color) {
        super(level, x, y, z, lx, ly, lz);
        this.xd = lx;
        this.yd = ly;
        this.zd = lz;
        this.color = color;
        this.lifetime = 60;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) this.remove();
    }

    public int rgb() {
        return this.color & 0xFFFFFF;
    }

    public float brightness(float pt) {
        return 1F - (this.age + pt) / this.lifetime;
    }

    public double dx() {
        return this.xd;
    }

    public double dy() {
        return this.yd;
    }

    public double dz() {
        return this.zd;
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
        return DEBUG_LINE_GROUP;
    }
}
