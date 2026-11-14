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

package com.hbm.entity.item;

import com.hbm.items.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BoatRubberEntity extends Boat {

    private double speedMultiplier = 0.07D;

    public BoatRubberEntity(EntityType<? extends BoatRubberEntity> type, Level level) {
        super(type, level, () -> ModItems.BOAT_RUBBER.get());
    }

    @Override
    public void controlBoat() {
        if (!isVehicle()) return;

        int forward = inputUp == inputDown ? 0 : inputUp ? 1 : -1;
        int strafe = inputLeft == inputRight ? 0 : inputLeft ? 1 : -1;
        Vec3 motion = getDeltaMovement();
        double previousSpeed = motion.horizontalDistance();

        if (forward != 0 || strafe != 0) {
            float heading = (float) (-(getYRot() + 90F) * Math.PI / 180D);
            double push = speedMultiplier * forward * 0.05D;
            motion = motion.add(Mth.sin(heading) * push, 0D, Mth.cos(heading) * push);

            float rotated = -strafe * 3F;
            setYRot(getYRot() + rotated);
            float turn = (float) (-rotated * Math.PI / 180D);
            motion = motion.yRot(turn);
        }

        double speed = motion.horizontalDistance();
        if (speed > 0.5D) {
            motion = new Vec3(motion.x * (0.5D / speed), motion.y, motion.z * (0.5D / speed));
            speed = 0.5D;
        }

        if (speed > previousSpeed && speedMultiplier < 0.5D) {
            speedMultiplier = Math.min(0.5D, speedMultiplier + (0.5D - speedMultiplier) / 50D);
        } else {
            speedMultiplier = Math.max(0.07D, speedMultiplier - (speedMultiplier - 0.07D) / 35D);
        }

        setDeltaMovement(motion);
        setPaddleState(inputRight && !inputLeft || inputUp, inputLeft && !inputRight || inputUp);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger instanceof Player player)
            player.setYBodyRot(Mth.wrapDegrees(getYRot() + 90F));
    }
}
