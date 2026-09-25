// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;

public class ParticleBlackPowderSmoke extends Particle implements IQuadParticle {

    public static final ParticleRenderType BLACK_POWDER_SMOKE_GROUP =
            new ParticleRenderType("hbm:bp_smoke", "HBS");

    private static int seedSeq;

    private final int id;
    private final float hue;
    private final float scale;
    private float rotationPitch, prevRotationPitch;

    public ParticleBlackPowderSmoke(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 30 + this.random.nextInt(15);
        this.scale = scale * 0.9F + this.random.nextFloat() * 0.2F;
        this.gravity = 0F;
        this.hue = 20F + this.random.nextFloat() * 20F;
        this.hasPhysics = false;
        this.id = seedSeq++;
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
        this.prevRotationPitch = this.rotationPitch;

        float ageScaled = (float) this.age / (float) this.lifetime;
        this.rotationPitch += (1F - ageScaled) * 2F * ((this.id % 2) - 0.5F);

        this.xd *= 0.65D;
        this.yd *= 0.65D;
        this.zd *= 0.65D;

        this.move(this.xd, this.yd, this.zd);
    }

    private float ageScaled(float pt) {
        return (this.age + pt) / (float) this.lifetime;
    }

    @Override
    public int argb(float pt) {
        float t = ageScaled(pt);
        float saturation = Math.max(1F - t * 4F, 0F);
        float brightness = Mth.clamp(1.25F - t * 2F, 0.7F, 1F);
        int rgb = Mth.hsvToRgb(this.hue / 255F, saturation, brightness) & 0xFFFFFF;

        float alpha = (float) Math.pow(1F - Math.min(t, 1F), 0.25) * 0.25F;
        return Mth.clamp((int) (alpha * 255F), 0, 255) << 24 | rgb;
    }

    @Override
    public float size(float pt) {
        return (float) ((0.25D + ageScaled(pt) + (this.age + pt) * 0.025D) * this.scale);
    }

    @Override
    public float spin(float pt) {
        return this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * pt;
    }

    @Override
    public int light(float pt) {
        return getLightCoords(pt);
    }

    @Override
    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    @Override
    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    @Override
    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ParticleRenderType getGroup() {
        return BLACK_POWDER_SMOKE_GROUP;
    }
}
