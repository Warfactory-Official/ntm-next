// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticlePlasmaBlast extends Particle {

    public static final ParticleRenderType PLASMA_BLAST_GROUP =
            new ParticleRenderType("hbm:plasma_blast", "HPB");

    public final float red, green, blue;
    public final float pitch, yaw;
    public float scale = 1F;

    public ParticlePlasmaBlast(
            ClientLevel level,
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float pitch,
            float yaw) {
        super(level, x, y, z);
        this.lifetime = 20;
        this.red = r;
        this.green = g;
        this.blue = b;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.remove();
    }

    public float alpha(float partialTicks) {
        return 1F - ((this.age + partialTicks) / this.lifetime);
    }

    public float renderScale(float partialTicks) {
        return (float) (1 - Math.pow(Math.E, (this.age + partialTicks) * -0.125)) * this.scale;
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
        return PLASMA_BLAST_GROUP;
    }
}
