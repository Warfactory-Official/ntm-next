// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.ClientEffects;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ParticleDebris extends Particle {

    public static final ParticleRenderType DEBRIS_GROUP =
            new ParticleRenderType("hbm:debris", "HD");

    private static int seedSeq;

    private final DebrisChunk chunk;
    private final DebrisMesh[] meshes;
    private final float pitchStep, yawStep;
    private final boolean trailsFlame;
    private final float flameScale;

    private float rotationPitch, prevRotationPitch;
    private float rotationYaw, prevRotationYaw;
    private boolean instanced;

    public ParticleDebris(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            DebrisChunk chunk,
            DebrisMesh[] meshes) {
        super(level, x, y, z);
        this.chunk = chunk;
        this.meshes = meshes;

        double mult = 3D;
        this.xd = mX * mult;
        this.yd = mY * mult;
        this.zd = mZ * mult;

        this.lifetime = 100;

        this.gravity = 0.15F;
        this.hasPhysics = false;

        int id = seedSeq++;
        Random rng = new Random(id);
        this.pitchStep = rng.nextFloat() * 10F;
        this.yawStep = rng.nextFloat() * 10F;
        this.trailsFlame = id % 3 == 0;
        this.flameScale = Math.max(chunk.sizeY, 6) / 16F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age > 5) this.hasPhysics = true;

        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        this.rotationPitch += this.pitchStep;
        this.rotationYaw += this.yawStep;

        if (this.trailsFlame) {
            ClientEffects.spawnRocketFlame(
                    this.level, this.x, this.y, this.z, this.flameScale, 0D, 0D, 0D, 50);
        }

        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);

        this.age++;

        if (this.onGround
                || this.age >= this.lifetime
                || this.hasPhysics
                        && this.level
                                .getBlockStates(this.getBoundingBox().deflate(0.001D))
                                .anyMatch(WebCollision::isWeb)) this.remove();
    }

    public boolean hasGeometry() {
        for (DebrisMesh mesh : meshes) {
            if (mesh != null) return true;
        }
        return false;
    }

    public void markInstanced() {
        this.instanced = true;
    }

    public boolean isInstanced() {
        return this.instanced;
    }

    public DebrisMesh[] meshes() {
        return meshes;
    }

    public DebrisChunk chunk() {
        return chunk;
    }

    public float pitch(float pt) {
        return Mth.lerp(pt, this.prevRotationPitch, this.rotationPitch);
    }

    public float yaw(float pt) {
        return Mth.lerp(pt, this.prevRotationYaw, this.rotationYaw);
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

    public static DebrisChunk carve(
            ClientLevel level, BlockPos centre, int size, int retry, RandomSource rand) {
        DebrisChunk chunk = new DebrisChunk(size, size, size, level, centre);
        if (size <= 0) return chunk;

        int middle = size / 2 - 1;
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                for (int k = 0; k < 2; k++)
                    chunk.set(
                            middle + i,
                            middle + j,
                            middle + k,
                            level.getBlockState(centre.offset(i, j, k)));

        for (int layer = 2; layer <= size / 2; layer++) {
            for (int i = 0; i < retry; i++) {
                int jx = -layer + rand.nextInt(layer * 2 + 1);
                int jy = -layer + rand.nextInt(layer * 2 + 1);
                int jz = -layer + rand.nextInt(layer * 2 + 1);

                if (touches(chunk, middle + jx, middle + jy, middle + jz)) {
                    chunk.set(
                            middle + jx,
                            middle + jy,
                            middle + jz,
                            level.getBlockState(centre.offset(jx, jy, jz)));
                }
            }
        }
        return chunk;
    }

    private static boolean touches(DebrisChunk chunk, int x, int y, int z) {
        return !chunk.get(x + 1, y, z).isAir()
                || !chunk.get(x - 1, y, z).isAir()
                || !chunk.get(x, y + 1, z).isAir()
                || !chunk.get(x, y - 1, z).isAir()
                || !chunk.get(x, y, z + 1).isAir()
                || !chunk.get(x, y, z - 1).isAir();
    }

    @Override
    public ParticleRenderType getGroup() {
        return DEBRIS_GROUP;
    }
}
