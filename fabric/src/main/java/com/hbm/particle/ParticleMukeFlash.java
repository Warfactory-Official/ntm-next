// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleMukeFlash extends Particle {

    public static final ParticleRenderType MUKE_FLASH_GROUP =
            new ParticleRenderType("hbm:muke_flash", "HMF");

    public final boolean bf;

    public ParticleMukeFlash(ClientLevel level, double x, double y, double z, boolean bf) {
        super(level, x, y, z);
        this.lifetime = 20;
        this.bf = bf;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        if (this.age == 15) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = this.level;

            for (double d = 0.0D; d <= 1.8D; d += 0.1) {
                mc.particleEngine.add(
                        getCloud(
                                level,
                                x,
                                y,
                                z,
                                random.nextGaussian() * 0.05,
                                d + random.nextGaussian() * 0.02,
                                random.nextGaussian() * 0.05));
            }

            for (int i = 0; i < 100; i++) {
                mc.particleEngine.add(
                        getCloud(
                                level,
                                x,
                                y + 0.5,
                                z,
                                random.nextGaussian() * 0.5,
                                random.nextInt(5) == 0 ? 0.02 : 0,
                                random.nextGaussian() * 0.5));
            }

            for (int i = 0; i < 75; i++) {
                double mx = random.nextGaussian() * 0.5;
                double mz = random.nextGaussian() * 0.5;

                if (mx * mx + mz * mz > 1.5) {
                    mx *= 0.5;
                    mz *= 0.5;
                }

                double my =
                        1.8 + (random.nextDouble() * 3 - 1.5) * (0.75 - (mx * mx + mz * mz)) * 0.5;

                mc.particleEngine.add(
                        getCloud(level, x, y, z, mx, my + random.nextGaussian() * 0.02, mz));
            }
        }
    }

    private ParticleMukeCloud getCloud(
            ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        if (this.bf) {
            return new ParticleMukeCloudBF(level, x, y, z, mx, my, mz);
        } else {
            return new ParticleMukeCloud(level, x, y, z, mx, my, mz);
        }
    }

    public float alpha(float partialTicks) {
        return 1F - ((this.age + partialTicks) / this.lifetime);
    }

    public float renderScale(float partialTicks) {
        return (this.age + partialTicks) * 3F + 1F;
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
        return MUKE_FLASH_GROUP;
    }
}
