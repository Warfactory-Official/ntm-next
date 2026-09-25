// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.lib.Library;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

public class ParticleHaze extends Particle {

    public static final ParticleRenderType HAZE_GROUP = new ParticleRenderType("hbm:haze", "HHZ");

    static final Identifier TEXTURE = Library.id("textures/particle/haze.png");

    static final Quad[] QUADS = buildQuads();

    private static final float PARTICLE_SCALE = 10F;

    public ParticleHaze(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 600 + this.random.nextInt(100);
    }

    private static Quad[] buildQuads() {
        Random rand = new Random(50);
        Quad[] quads = new Quad[25];
        double walkX = 0, walkY = 0, walkZ = 0;
        for (int i = 0; i < quads.length; i++) {
            walkX += rand.nextGaussian() * 2.5D;
            walkY += rand.nextGaussian() * 0.15D;
            walkZ += rand.nextGaussian() * 2.5D;
            float sizeFactor = (float) (rand.nextDouble() * 0.25D + 0.75D);
            quads[i] =
                    new Quad(
                            (float) (walkX + rand.nextGaussian() * 0.5D),
                            (float) (walkY + rand.nextGaussian() * 0.5D),
                            (float) (walkZ + rand.nextGaussian() * 0.5D),
                            sizeFactor);
        }
        return quads;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) this.remove();

        int px = Mth.floor(this.x) + this.random.nextInt(15) - 7;
        int pz = Mth.floor(this.z) + this.random.nextInt(15) - 7;

        int py = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
        this.level.addParticle(
                ParticleTypes.LAVA,
                px + this.random.nextDouble(),
                py + 0.1D,
                pz + this.random.nextDouble(),
                0D,
                0D,
                0D);
    }

    float alpha() {
        return Math.max(0F, (float) Math.sin(this.age * Math.PI / 400D) * 0.25F) * 0.1F;
    }

    float scale() {
        return PARTICLE_SCALE;
    }

    double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ParticleRenderType getGroup() {
        return HAZE_GROUP;
    }

    record Quad(float offsetX, float offsetY, float offsetZ, float sizeFactor) {}
}
