/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.particle;

import com.hbm.client.particle.CasingSmokeVisual;
import com.hbm.client.particle.VisualParticle;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingImpactSound;
import com.hbm.particle.casing.CasingSmokeTrail;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public final class CasingParticle extends Particle implements VisualParticle {

    public static final ParticleRenderType GROUP = new ParticleRenderType("hbm:casing", "HCA");

    private static final double GRAVITY = 0.04D;
    private static final double DRAG = 0.98D;
    private static final double GROUND_FRICTION = 0.7D;
    private static final float GROUND_YAW_DAMPING = 0.7F;
    private static final double HORIZONTAL_BOUNCE = -0.25D;
    private static final double VERTICAL_BOUNCE = -0.5D;
    private static final float SPIN_REVERSAL = -0.75F;
    private static final double SPIN_AT_REST = 1.0E-7D;
    private static final double FAST_BOUNCE = 0.04D;
    private static final float SPIN_KICK = 10F;
    private static final double KICK_SPEED = 0.2D;
    private static final double PLINK_SPEED = 0.2D;

    private final Casing casing;
    private final @Nullable CasingSmokeTrail smoke;
    private float yaw;
    private float yawO;
    private float pitch;
    private float pitchO;
    private float spinPitch;
    private float spinYaw;
    private @Nullable VisualizationManager visualManager;

    public CasingParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ,
            float yaw,
            float pitch,
            float spinPitch,
            float spinYaw,
            Casing casing,
            @Nullable CasingSmokeTrail smoke) {
        super(level, x, y, z);
        this.casing = casing;
        this.smoke = smoke;
        this.bbWidth = casing.width();
        this.bbHeight = casing.height();
        setPos(x, y, z);
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.yaw = this.yawO = yaw;
        this.pitch = this.pitchO = pitch;
        this.spinPitch = spinPitch;
        this.spinYaw = spinYaw;
        this.lifetime = casing.lifetime();
    }

    public Casing getCasing() {
        return casing;
    }

    public @Nullable CasingSmokeTrail getSmokeTrail() {
        return smoke;
    }

    @Override
    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        float halfWidth = bbWidth / 2F;
        float halfHeight = bbHeight / 2F;
        setBoundingBox(
                new AABB(
                        x - halfWidth,
                        y - halfHeight,
                        z - halfWidth,
                        x + halfWidth,
                        y + halfHeight,
                        z + halfWidth));
    }

    @Override
    protected void setLocationFromBoundingbox() {
        AABB box = getBoundingBox();
        x = (box.minX + box.maxX) / 2D;
        y = box.minY + bbHeight / 2F;
        z = (box.minZ + box.maxZ) / 2D;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age >= lifetime) remove();
        age++;

        yd -= GRAVITY;
        move(xd, yd, zd);
        xd *= DRAG;
        yd *= DRAG;
        zd *= DRAG;

        if (onGround) {
            xd *= GROUND_FRICTION;
            zd *= GROUND_FRICTION;
            pitch = 180F * (float) Math.floor(pitch / 180F + 0.5F);
            spinYaw *= GROUND_YAW_DAMPING;
            onGround = false;
        }

        if (smoke != null) smoke.tick(age, random);

        yawO = yaw;
        pitchO = pitch;
        pitch += spinPitch;
        yaw += spinYaw;
        yawO = shortWay(yawO, yaw);
        pitchO = shortWay(pitchO, pitch);
    }

    @Override
    public void move(double dx, double dy, double dz) {
        double ax = dx;
        double ay = dy;
        double az = dz;
        if (dx != 0D || dy != 0D || dz != 0D) {
            AABB box = getBoundingBox();
            Vec3 moved =
                    Entity.collideBoundingBox(
                            CollisionContext.positionContext(box.minY),
                            new Vec3(dx, dy, dz),
                            box,
                            level,
                            List.of());
            ax = moved.x;
            ay = moved.y;
            az = moved.z;
            if (ax != 0D || ay != 0D || az != 0D) {
                setBoundingBox(box.move(ax, ay, az));
                setLocationFromBoundingbox();
            }
        }

        boolean vertical = ay != dy;
        onGround = vertical && dy < 0D;
        if (ax != dx) bounceHorizontally(true);
        if (vertical) bounceVertically(dy);
        if (az != dz) bounceHorizontally(false);
        if (vertical && Math.abs(dy) >= PLINK_SPEED) plink();
    }

    private void bounceHorizontally(boolean alongX) {
        if (alongX) xd *= HORIZONTAL_BOUNCE;
        else zd *= HORIZONTAL_BOUNCE;
        spinYaw =
                Math.abs(spinYaw) > SPIN_AT_REST
                        ? spinYaw * SPIN_REVERSAL
                        : (float) (random.nextGaussian() * SPIN_KICK * casing.bounceYaw());
    }

    private void bounceVertically(double dy) {
        yd *= VERTICAL_BOUNCE;
        boolean fast = Math.abs(yd) > FAST_BOUNCE;
        if (fast || Math.abs(spinPitch) > SPIN_AT_REST) spinPitch *= SPIN_REVERSAL;
        if (!fast) return;
        float strength = Mth.clamp((float) (dy / KICK_SPEED), -1F, 1F);
        spinPitch += random.nextGaussian() * SPIN_KICK * casing.bouncePitch() * strength;
        spinYaw += random.nextGaussian() * SPIN_KICK * casing.bounceYaw() * strength;
    }

    private void plink() {
        CasingImpactSound plink = casing.plink();
        level.playLocalSound(
                x,
                y,
                z,
                plink.getSoundEvent(),
                SoundSource.PLAYERS,
                plink.getVolume(),
                1F + 0.2F * random.nextFloat(),
                false);
    }

    static float shortWay(float previous, float current) {
        if (Math.abs(previous - current) <= 180F) return previous;
        return previous < current ? previous + 360F : previous - 360F;
    }

    public double getRenderX(float partialTick) {
        return xo + (x - xo) * partialTick;
    }

    public double getRenderY(float partialTick) {
        return yo + (y - yo) * partialTick;
    }

    public double getRenderZ(float partialTick) {
        return zo + (z - zo) * partialTick;
    }

    public float getRenderYaw(float partialTick) {
        return yawO + (yaw - yawO) * partialTick;
    }

    public float getRenderPitch(float partialTick) {
        return pitchO + (pitch - pitchO) * partialTick;
    }

    public int getLight(float partialTick) {
        return getLightCoords(partialTick);
    }

    boolean prepareSmoke(double x, double y, double z, float viewCos, float viewSin, int light) {
        if (smoke == null) return false;
        smoke.setAnchor(x, y, z);
        float half = casing.smokeHalfWidth();
        boolean visual = ownsVisual();
        smoke.publish(
                age, x, y - casing.height() / 4F, z, half * viewCos, half * viewSin, light, visual);
        return !visual && smoke.getSegmentCount() > 0;
    }

    @Override
    public void refreshVisual() {
        if (smoke != null) VisualParticle.super.refreshVisual();
    }

    @Override
    public ClientLevel level() {
        return level;
    }

    @Override
    public @Nullable VisualizationManager visualManager() {
        return visualManager;
    }

    @Override
    public void visualManager(@Nullable VisualizationManager manager) {
        visualManager = manager;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext context, float partialTick) {
        return new CasingSmokeVisual(context, this);
    }

    @Override
    public ParticleRenderType getGroup() {
        return GROUP;
    }
}
