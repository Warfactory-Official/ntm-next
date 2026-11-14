/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.ArtilleryRocketEntity;
import com.hbm.entity.projectile.rocketbehavior.RocketGuidance;
import net.minecraft.world.phys.Vec3;

public class RocketSteeringBallisticArc implements RocketGuidance.Steer {

    private static final double TURN_SPEED = 45;

    @Override
    public void steer(ArtilleryRocketEntity rocket, double speed, double maxTurn) {

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
