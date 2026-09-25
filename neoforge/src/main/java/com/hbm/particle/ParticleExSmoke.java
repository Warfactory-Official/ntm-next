// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleExSmoke extends Particle {

    public static final ParticleRenderType EX_SMOKE_GROUP =
            new ParticleRenderType("hbm:ex_smoke", "HXS");

    private static int seedSeq;

    public final int seed;

    public ParticleExSmoke(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 100 + this.random.nextInt(40);
        this.seed = seedSeq++;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.7599999785423279D;
        this.yd *= 0.7599999785423279D;
        this.zd *= 0.7599999785423279D;

        this.move(this.xd, this.yd, this.zd);
    }

    public float alpha(float partialTicks) {
        return 1F - (float) this.age / (float) this.lifetime;
    }

    public int light(float partialTicks) {
        return getLightCoords(partialTicks);
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
        return EX_SMOKE_GROUP;
    }
}
