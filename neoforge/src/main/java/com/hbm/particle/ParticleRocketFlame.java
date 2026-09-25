// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleRocketFlame extends Particle {

    public static final ParticleRenderType ROCKET_FLAME_GROUP =
            new ParticleRenderType("hbm:rocket_flame", "HRO");

    private static int seedSeq;

    public final int seed;
    private float baseScale = 1F;

    public ParticleRocketFlame(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 300 + this.random.nextInt(50);
        this.seed = seedSeq++;
    }

    public ParticleRocketFlame setScale(float scale) {
        this.baseScale = scale;
        return this;
    }

    public ParticleRocketFlame setMaxAge(int maxAge) {
        this.lifetime = maxAge;
        return this;
    }

    public ParticleRocketFlame noClip() {
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
