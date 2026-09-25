// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import net.minecraft.world.phys.Vec3;

public class CasingEjector implements Cloneable {

    private Vec3 posOffset = Vec3.ZERO;
    private Vec3 initialMotion = Vec3.ZERO;
    private float randomYaw;
    private float randomPitch;

    public CasingEjector setOffset(double x, double y, double z) {
        this.posOffset = new Vec3(x, y, z);
        return this;
    }

    public CasingEjector setMotion(double x, double y, double z) {
        this.initialMotion = new Vec3(x, y, z);
        return this;
    }

    public CasingEjector setAngleRange(float yaw, float pitch) {
        this.randomYaw = yaw;
        this.randomPitch = pitch;
        return this;
    }

    public Vec3 getOffset() {
        return posOffset;
    }

    public Vec3 getMotion() {
        return initialMotion;
    }

    public float getYawFactor() {
        return randomYaw;
    }

    public float getPitchFactor() {
        return randomPitch;
    }

    @Override
    public CasingEjector clone() {
        try {
            return (CasingEjector) super.clone();
        } catch (CloneNotSupportedException e) {
            return new CasingEjector();
        }
    }
}
