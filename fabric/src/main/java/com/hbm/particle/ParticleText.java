// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleText extends Particle {

    public static final ParticleRenderType TEXT_GROUP = new ParticleRenderType("hbm:text", "HTX");

    private final int color;
    private final String text;
    private float scale;

    public ParticleText(ClientLevel level, double x, double y, double z, int color, String text) {
        super(level, x, y, z);
        this.scale = (this.random.nextFloat() * 0.5F + 0.5F) * 2F;
        this.lifetime = 100;
        this.color = color;
        this.text = text;
        this.yd = 0.01D;
        this.hasPhysics = false;
    }

    public ParticleText setScale(float scale) {
        this.scale *= scale;
        return this;
    }

    public String text() {
        return this.text;
    }

    public int argb() {
        return 0xFF000000 | (this.color & 0xFFFFFF);
    }

    public boolean shadow() {
        return true;
    }

    public float renderScale() {
        return this.scale * 0.01F;
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
        return TEXT_GROUP;
    }
}
