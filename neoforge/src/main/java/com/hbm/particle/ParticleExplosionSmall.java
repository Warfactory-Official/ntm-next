// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;

public class ParticleExplosionSmall extends Particle {

    public static final ParticleRenderType EXPLOSION_SMALL_GROUP =
            new ParticleRenderType("hbm:explosion_small", "HES");

    private static int spinSeq;

    public final float hue;
    public final float baseScale;
    private final float spinDir;
    public float roll, rollO;

    public ParticleExplosionSmall(
            ClientLevel level, double x, double y, double z, float scale, float speedMult) {
        super(level, x, y, z);
        this.lifetime = 25 + random.nextInt(10);
        this.baseScale = scale * 0.9F + random.nextFloat() * 0.2F;
        this.xd = random.nextGaussian() * speedMult;
        this.zd = random.nextGaussian() * speedMult;
        this.gravity = random.nextFloat() * -0.01F;
        this.hue = 20F + random.nextFloat() * 20F;
        this.spinDir = (spinSeq++ % 2) - 0.5F;
        this.hasPhysics = false;
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

        this.yd -= this.gravity;
        this.rollO = this.roll;
        float ageScaled = (float) this.age / (float) this.lifetime;
        this.roll += (1 - ageScaled) * 5 * this.spinDir;

        this.xd *= 0.65D;
        this.zd *= 0.65D;
        this.move(this.xd, this.yd, this.zd);
    }

    public int color(float partialTicks) {
        float ageScaled = (this.age + partialTicks) / this.lifetime;
        float sat = Math.max(1F - ageScaled * 2F, 0F);
        float val = Mth.clamp(1.25F - ageScaled * 2F, this.hue * 0.01F - 0.1F, 1F);
        return Mth.hsvToRgb(this.hue / 255F, sat, val);
    }

    public float alpha(float partialTicks) {
        float ageScaled = (this.age + partialTicks) / this.lifetime;
        return (float) Math.pow(1 - Math.min(ageScaled, 1F), 0.25) * 0.5F;
    }

    public float renderScale(float partialTicks) {
        double ageScaled = (double) (this.age + partialTicks) / this.lifetime;
        return (float)
                ((0.25 + 1 - Math.pow(1 - ageScaled, 4) + (this.age + partialTicks) * 0.02)
                        * this.baseScale);
    }

    public float roll(float partialTicks) {
        return this.rollO + (this.roll - this.rollO) * partialTicks;
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
        return EXPLOSION_SMALL_GROUP;
    }
}
