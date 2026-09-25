// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleMukeWave extends Particle {

    public static final ParticleRenderType MUKE_WAVE_GROUP =
            new ParticleRenderType("hbm:muke_wave", "HMW");

    public float waveScale = 45F;

    public ParticleMukeWave(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 25;
        this.hasPhysics = false;
    }

    public ParticleMukeWave setup(float scale, int maxAge) {
        this.waveScale = scale;
        this.lifetime = maxAge;
        return this;
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
        return (float) (1 - Math.pow(Math.E, (this.age + partialTicks) * -0.125)) * this.waveScale;
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
        return MUKE_WAVE_GROUP;
    }
}
