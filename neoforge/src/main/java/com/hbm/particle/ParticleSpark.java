// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleSpark extends Particle {

    public static final ParticleRenderType SPARK_GROUP = new ParticleRenderType("hbm:spark", "HSP");

    private final Deque<double[]> steps = new ArrayDeque<>();
    private int thresh;

    public ParticleSpark(
            ClientLevel level, double x, double y, double z, double mX, double mY, double mZ) {
        super(level, x, y, z, mX, mY, mZ);
        this.thresh = 4 + this.random.nextInt(3);
        this.steps.addLast(new double[] {this.xd, this.yd, this.zd});
        this.lifetime = 20 + this.random.nextInt(10);
        this.gravity = 0.5F;
    }

    public ParticleSpark makeSmall(boolean small) {
        if (!small) return this;
        this.setSize(0.01F, 0.01F);
        this.thresh = 3;
        this.lifetime = 2 + this.random.nextInt(3);
        this.yd = -Math.abs(this.yd);
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) this.remove();

        this.steps.addLast(new double[] {this.xd, this.yd, this.zd});
        while (this.steps.size() > this.thresh) this.steps.removeFirst();

        this.yd -= 0.04D * this.gravity;
        double lastY = this.yd;

        this.stoppedByCollision = false;
        this.move(this.xd, this.yd, this.zd);

        if (this.onGround) {
            this.onGround = false;
            this.yd = -lastY * 0.8D;
        }
    }

    public Deque<double[]> steps() {
        return this.steps;
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
        return SPARK_GROUP;
    }
}
