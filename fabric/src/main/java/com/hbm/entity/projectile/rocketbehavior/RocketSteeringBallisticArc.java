// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import net.minecraft.world.phys.Vec3;

public class RocketSteeringBallisticArc implements IRocketSteeringBehavior {

    private static final double TURN_SPEED = 45;

    @Override
    public void adjustCourse(EntityArtilleryRocket rocket, double speed, double maxTurn) {

        Vec3 direction = rocket.getDeltaMovement().normalize();
        double horizontalMomentum =
                Math.sqrt(
                        rocket.getDeltaMovement().x * rocket.getDeltaMovement().x
                                + rocket.getDeltaMovement().z * rocket.getDeltaMovement().z);
        Vec3 targetPos = rocket.getLastTarget();
        double deltaX = targetPos.x - rocket.getX();
        double deltaZ = targetPos.z - rocket.getZ();
        double horizontalDelta = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double stepsRequired = horizontalDelta / horizontalMomentum;
        Vec3 target =
                new Vec3(
                                targetPos.x - rocket.getX(),
                                targetPos.y - rocket.getY(),
                                targetPos.z - rocket.getZ())
                        .normalize();

        double rocketYaw = yaw(direction);
        double rocketPitch = pitch(direction);
        double targetYaw = yaw(target);
        double targetPitch = pitch(target);

        double turnSpeed = Math.min(maxTurn, TURN_SPEED / stepsRequired);

        if (stepsRequired <= 1) {
            turnSpeed = 180D;
        }

        double deltaYaw = ((targetYaw - rocketYaw) + 180D) % 360D - 180D;
        double deltaPitch = ((targetPitch - rocketPitch) + 180D) % 360D - 180D;

        double turnYaw = Math.min(Math.abs(deltaYaw), turnSpeed) * Math.signum(deltaYaw);
        double turnPitch = Math.min(Math.abs(deltaPitch), turnSpeed) * Math.signum(deltaPitch);

        Vec3 velocity =
                new Vec3(speed, 0, 0)
                        .zRot((float) -Math.toRadians(rocketPitch + turnPitch))
                        .yRot((float) Math.toRadians(rocketYaw + turnYaw + 90));

        rocket.setDeltaMovement(velocity);
    }

    private static double yaw(Vec3 vec) {
        boolean pos = vec.z >= 0;
        return Math.toDegrees(Math.atan(vec.x / vec.z)) + (pos ? 180 : 0);
    }

    private static double pitch(Vec3 vec) {
        return Math.toDegrees(Math.atan(vec.y / Math.sqrt(vec.x * vec.x + vec.z * vec.z)));
    }
}
