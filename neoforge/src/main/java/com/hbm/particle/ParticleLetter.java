// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleLetter extends Particle {

    public static final ParticleRenderType LETTER_GROUP =
            new ParticleRenderType("hbm:letter", "HLT");

    private final int color;
    private final String glyph;

    public ParticleLetter(ClientLevel level, double x, double y, double z, int color, char c) {
        super(level, x, y, z);
        this.lifetime = 30;
        this.color = color;
        this.glyph = String.valueOf(c);
    }

    public String text() {
        return this.glyph;
    }

    public int argb(float pt) {
        float alpha = 1F - (this.age + pt) / (float) this.lifetime;
        if (alpha < 0F) alpha = 0F;
        int a = (int) (alpha * 255F);
        if (a > 255) a = 255;
        if (a < 10) a = 10;
        return a << 24 | (this.color & 0xFFFFFF);
    }

    public boolean shadow() {
        return false;
    }

    public float renderScale(float pt) {
        float time = (this.age + pt) * 4F / this.lifetime;
        return (float) (1D - 1D / Math.pow(Math.E, time));
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
        return LETTER_GROUP;
    }
}
