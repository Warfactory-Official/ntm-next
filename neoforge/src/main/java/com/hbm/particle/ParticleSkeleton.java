// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.main.ResourceManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class ParticleSkeleton extends Particle {

    public static final ParticleRenderType SKELETON_GROUP =
            new ParticleRenderType("hbm:skeleton", "HSK");
    public static final ParticleRenderType SKELETON_EXT_GROUP =
            new ParticleRenderType("hbm:skeleton_ext", "HSKE");
    public static final ParticleRenderType GIB_GROUP =
            new ParticleRenderType("hbm:skeleton_gib", "HSKG");
    public static final ParticleRenderType GIB_EXT_GROUP =
            new ParticleRenderType("hbm:skeleton_gib_ext", "HSKGE");

    public static final int PART_SKULL = 0;
    public static final int PART_TORSO = 1;
    public static final int PART_LIMB = 2;
    public static final int PART_SKULL_VILLAGER = 3;

    private static final String[] PART_NAMES = {"Skull", "Torso", "Limb", "SkullVillager"};

    private final int part;
    private final float red, green, blue;
    private final float momentumYaw, momentumPitch;
    private int initialDelay;
    private boolean gib;
    private float rotationPitch, prevRotationPitch;
    private float rotationYaw, prevRotationYaw;

    public ParticleSkeleton(
            ClientLevel level, double x, double y, double z, float r, float g, float b, int part) {
        super(level, x, y, z);
        this.part = part;
        this.red = r;
        this.green = g;
        this.blue = b;
        this.lifetime = 1200 + this.random.nextInt(20);
        this.gravity = 0.02F;
        this.initialDelay = 20;
        this.momentumPitch = this.random.nextFloat() * 5F * (this.random.nextBoolean() ? 1 : -1);
        this.momentumYaw = this.random.nextFloat() * 5F * (this.random.nextBoolean() ? 1 : -1);
    }

    public static int partId(int part) {
        return ResourceManager.skeleton_obj.partId(PART_NAMES[part]);
    }

    public ParticleSkeleton makeGib() {
        makeGibKeepingTexture();
        this.gib = true;
        return this;
    }

    public ParticleSkeleton makeGibKeepingTexture() {

        this.initialDelay = -2;
        this.gravity = 0.04F;
        this.lifetime = 600 + this.random.nextInt(20);
        return this;
    }

    public void setInitialRotation(float yaw, float pitch) {
        this.rotationYaw = this.prevRotationYaw = yaw;
        this.rotationPitch = this.prevRotationPitch = pitch;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;

        if (this.initialDelay-- > 0) return;

        if (this.initialDelay == -1) {
            this.xd = this.random.nextGaussian() * 0.025D;
            this.zd = this.random.nextGaussian() * 0.025D;
        }

        if (this.age++ >= this.lifetime) this.remove();
        boolean wasOnGround = this.onGround;

        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;

        if (!this.onGround) {
            this.rotationPitch += this.momentumPitch;
            this.rotationYaw += this.momentumYaw;
        } else {
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;

            if (!wasOnGround) {
                this.level.playLocalSound(
                        this.x,
                        this.y,
                        this.z,
                        SoundEvents.SKELETON_HURT,
                        SoundSource.NEUTRAL,
                        0.25F,
                        0.8F + this.random.nextFloat() * 0.4F,
                        false);
            }
        }
    }

    public int part() {
        return this.part;
    }

    public Identifier texture() {
        boolean villager = this.part == PART_SKULL_VILLAGER;
        if (this.gib)
            return villager
                    ? ResourceManager.skoilet_blood_tex
                    : ResourceManager.skeleton_blood_tex;
        return villager ? ResourceManager.skoilet_tex : ResourceManager.skeleton_tex;
    }

    public int argb(float pt) {
        float timeLeft = this.lifetime - (this.age + pt);
        float alpha = timeLeft < 40F ? timeLeft / 40F : 1F;
        return clamp8(alpha) << 24
                | clamp8(this.red) << 16
                | clamp8(this.green) << 8
                | clamp8(this.blue);
    }

    private static int clamp8(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255F)));
    }

    public float pitch(float pt) {
        return this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * pt;
    }

    public float yaw(float pt) {
        return this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * pt;
    }

    public int light(float pt) {
        return getLightCoords(pt);
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
        boolean villager = this.part == PART_SKULL_VILLAGER;
        if (this.gib) return villager ? GIB_EXT_GROUP : GIB_GROUP;
        return villager ? SKELETON_EXT_GROUP : SKELETON_GROUP;
    }
}
