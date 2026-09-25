// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ParticleAmatFlash extends Particle {

    public static final ParticleRenderType AMAT_FLASH_GROUP =
            new ParticleRenderType("hbm:amat_flash", "HAF");

    public static final int SPIKES = 100;

    public static final float[] RIM = buildRim();

    private final float scale;

    public ParticleAmatFlash(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 10;
        this.scale = scale;
    }

    public int light(float partialTicks) {
        return getLightCoords(partialTicks);
    }

    private static float[] buildRim() {
        Random rand = new Random(432L);
        Matrix4f pose = new Matrix4f();
        Vector3f corner = new Vector3f();
        float[] rim = new float[SPIKES * 3 * 3];

        for (int i = 0; i < SPIKES; i++) {

            pose.rotateX(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateY(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateZ(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateX(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateY(rand.nextFloat() * 360F * Mth.DEG_TO_RAD);

            float len = rand.nextFloat() * 20F + 15F;
            float width = rand.nextFloat() * 2F + 3F;

            put(rim, i, 0, pose, corner.set(-0.866F * width, len, -0.5F * width));
            put(rim, i, 1, pose, corner.set(0.866F * width, len, -0.5F * width));
            put(rim, i, 2, pose, corner.set(0F, len, width));
        }
        return rim;
    }

    private static void put(float[] rim, int spike, int corner, Matrix4f pose, Vector3f v) {
        pose.transformPosition(v);
        int base = (spike * 3 + corner) * 3;
        rim[base] = v.x;
        rim[base + 1] = v.y;
        rim[base + 2] = v.z;
    }

    public float extent(float partialTicks) {
        return 0.1F * this.scale * intensity(partialTicks);
    }

    public float intensity(float partialTicks) {
        return (this.age + partialTicks) / (float) this.lifetime;
    }

    public float coreAlpha(float partialTicks) {
        return 1F - intensity(partialTicks);
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
        return AMAT_FLASH_GROUP;
    }
}
