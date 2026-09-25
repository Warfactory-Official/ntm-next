// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RocketTargetingPredictive implements IRocketTargetingBehavior {

    private final double[][] targetMotion = new double[20][3];

    @Override
    public void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target) {

        Vec3 speed = rocket.getDeltaMovement();
        Vec3 delta =
                new Vec3(
                        target.getX() - rocket.getX(),
                        target.getY() - rocket.getY(),
                        target.getZ() - rocket.getZ());
        double eta = delta.length() - speed.length();

        double motionX = target.getDeltaMovement().x;
        double motionY = target.getDeltaMovement().y;
        double motionZ = target.getDeltaMovement().z;

        for (int i = 1; i < 20; i++) {
            targetMotion[i - 1] = targetMotion[i];
            motionX += targetMotion[i][0];
            motionY += targetMotion[i][1];
            motionZ += targetMotion[i][2];
        }

        targetMotion[19][0] = target.getDeltaMovement().x;
        targetMotion[19][1] = target.getDeltaMovement().y;
        targetMotion[19][2] = target.getDeltaMovement().z;

        if (eta <= 1) {

            rocket.setTarget(
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            return;
        }

        double predX = target.getX() + (motionX / 20D) * eta;
        double predY = target.getY() + target.getBbHeight() * 0.5D + (motionY / 20D) * eta;
        double predZ = target.getZ() + (motionZ / 20D) * eta;

        rocket.setTarget(predX, predY, predZ);
    }
}
