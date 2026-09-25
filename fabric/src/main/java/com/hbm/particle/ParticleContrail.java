// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleContrail extends Particle {

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

    public ParticleContrail(
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
