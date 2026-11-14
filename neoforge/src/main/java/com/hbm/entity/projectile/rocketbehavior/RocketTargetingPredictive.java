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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RocketTargetingPredictive implements RocketGuidance.Aim {
    private final double[][] targetMotion = new double[20][3];

    @Override
    public void retarget(ArtilleryRocketEntity rocket, Entity target) {

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
