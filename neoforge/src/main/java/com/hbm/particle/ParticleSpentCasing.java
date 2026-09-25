// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.lib.Library;
import com.hbm.util.BobMathUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class ParticleSpentCasing extends Particle {

    public static final ParticleRenderType CASING_GROUP =
            new ParticleRenderType("hbm:casing", "HCA");

    public static final Random rand = new Random();
    public static final float dScale = 0.05F;
    private static final float smokeJitter = 0.001F;
    public final List<SmokeNode> smokeNodes = new ArrayList<>();
    public final SpentCasing config;
    public float rotationPitch, rotationYaw;
    public float prevRotationPitch, prevRotationYaw;
    private int maxSmokeGen = 120;
    private double smokeLift = 0.5D;
    private int nodeLife = 30;
    private final boolean isSmoking;
    private float momentumPitch, momentumYaw;

    private boolean setupDeltas = false;
    private double prevRenderX;
    private double prevRenderY;
    private double prevRenderZ;

    public ParticleSpentCasing(
            ClientLevel level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            float momentumPitch,
            float momentumYaw,
            SpentCasing config,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife) {
        super(level, x, y, z);
        this.momentumPitch = momentumPitch;
        this.momentumYaw = momentumYaw;
        this.config = config;

        this.lifetime = config.getMaxAge();
        this.setSize(
                2 * dScale * Math.max(config.getScaleX(), config.getScaleZ()),
                dScale * config.getScaleY());

        this.isSmoking = smoking;
        this.maxSmokeGen = smokeLife;
        this.smokeLift = smokeLift;
        this.nodeLife = nodeLife;

        this.xd = mx;
        this.yd = my;
        this.zd = mz;

        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;

        this.gravity = 1F;
    }

    @Override
    public ParticleRenderType getGroup() {
        return CASING_GROUP;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        }

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;

        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;

            this.rotationPitch = (float) (Math.floor(this.rotationPitch / 180F + 0.5F)) * 180F;
            this.momentumYaw *= 0.7F;
            this.onGround = false;
        }

        if (age > maxSmokeGen && !smokeNodes.isEmpty()) smokeNodes.clear();

        if (isSmoking && age <= maxSmokeGen) {

            for (SmokeNode node : smokeNodes) {
                node.x += rand.nextGaussian() * smokeJitter;
                node.z += rand.nextGaussian() * smokeJitter;
                node.y += smokeLift * dScale;

                node.alpha = Math.max(0, node.alpha - (1D / (double) nodeLife));
            }

            if (age < maxSmokeGen) smokeNodes.add(new SmokeNode(smokeNodes.isEmpty() ? 0.0D : 1D));
        }

        prevRotationPitch = rotationPitch;
        prevRotationYaw = rotationYaw;

        rotationPitch += momentumPitch;
        rotationYaw += momentumYaw;

        if (Math.abs(prevRotationPitch - rotationPitch) > 180) {
            if (prevRotationPitch < rotationPitch) prevRotationPitch += 360;
            if (prevRotationPitch > rotationPitch) prevRotationPitch -= 360;
        }

        if (Math.abs(prevRotationYaw - rotationYaw) > 180) {
            if (prevRotationYaw < rotationYaw) prevRotationYaw += 360;
            if (prevRotationYaw > rotationYaw) prevRotationYaw -= 360;
        }
    }

    @Override
    public void move(double motionX, double motionY, double motionZ) {
        double initMoX = motionX;
        double initMoY = motionY;
        double initMoZ = motionZ;

        Vec3 collided =
                Entity.collideBoundingBox(
                        CollisionContext.positionContext(this.y),
                        new Vec3(motionX, motionY, motionZ),
                        this.getBoundingBox(),
                        this.level,
                        List.of());
        motionX = collided.x;
        motionY = collided.y;
        motionZ = collided.z;

        this.setBoundingBox(this.getBoundingBox().move(motionX, motionY, motionZ));
        this.setLocationFromBoundingbox();

        boolean collidedVertically = initMoY != motionY;
        this.onGround = collidedVertically && initMoY < 0.0D;

        if (initMoX != motionX) {
            this.xd *= -0.25D;

            if (Math.abs(momentumYaw) > 1e-7) momentumYaw *= -0.75F;
            else momentumYaw = (float) rand.nextGaussian() * 10F * this.config.getBounceYaw();
        }

        if (initMoY != motionY) {
            this.yd *= -0.5D;

            boolean rotFromSpeed = Math.abs(this.yd) > 0.04;
            if (rotFromSpeed || Math.abs(momentumPitch) > 1e-7) {
                momentumPitch *= -0.75F;
                if (rotFromSpeed) {
                    float mult = (float) Math.clamp(initMoY / 0.2F, -1F, 1F);
                    momentumPitch +=
                            rand.nextGaussian() * 10F * this.config.getBouncePitch() * mult;
                    momentumYaw +=
                            (float) rand.nextGaussian() * 10F * this.config.getBounceYaw() * mult;
                }
            }
        }

        if (initMoZ != motionZ) {
            this.zd *= -0.25D;

            if (Math.abs(momentumYaw) > 1e-7) momentumYaw *= -0.75F;
            else momentumYaw = (float) rand.nextGaussian() * 10F * this.config.getBounceYaw();
        }

        if (this.config.getSound() != null && collidedVertically && Math.abs(initMoY) >= 0.2) {
            SoundEvent sound =
                    BuiltInRegistries.SOUND_EVENT.getValue(Library.id(this.config.getSound()));
            if (sound != null) {
                this.level.playLocalSound(
                        x,
                        y,
                        z,
                        sound,
                        SoundSource.PLAYERS,
                        SpentCasing.PLINK_LARGE.equals(this.config.getSound()) ? 1F : 0.5F,
                        1F + rand.nextFloat() * 0.2F,
                        false);
            }
        }
    }

    public void dragSmokeNodes(double pX, double pY, double pZ) {
        if (!setupDeltas) {
            prevRenderX = pX;
            prevRenderY = pY;
            prevRenderZ = pZ;
            setupDeltas = true;
        }

        double deltaX = prevRenderX - pX;
        double deltaY = prevRenderY - pY;
        double deltaZ = prevRenderZ - pZ;

        for (SmokeNode node : smokeNodes) {
            node.x += deltaX;
            node.y += deltaY;
            node.z += deltaZ;
        }

        prevRenderX = pX;
        prevRenderY = pY;
        prevRenderZ = pZ;
    }

    public float interpYaw(float interp) {
        return (float) BobMathUtil.interp(prevRotationYaw, rotationYaw, interp);
    }

    public float interpPitch(float interp) {
        return (float) BobMathUtil.interp(prevRotationPitch, rotationPitch, interp);
    }

    public double interpX(float interp) {
        return BobMathUtil.interp(xo, x, interp);
    }

    public double interpY(float interp) {
        return BobMathUtil.interp(yo, y, interp);
    }

    public double interpZ(float interp) {
        return BobMathUtil.interp(zo, z, interp);
    }

    public float height() {
        return this.bbHeight;
    }

    public int age() {
        return this.age;
    }

    public int maxSmokeGen() {
        return this.maxSmokeGen;
    }

    public boolean hasSmoke() {
        return !smokeNodes.isEmpty();
    }

    public int lightCoords(float interp) {
        return this.getLightCoords(interp);
    }

    public static final class SmokeNode {
        public double x, y, z;
        public double alpha;

        SmokeNode(double alpha) {
            this.alpha = alpha;
        }
    }
}
