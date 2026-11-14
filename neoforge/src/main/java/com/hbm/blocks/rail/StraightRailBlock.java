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

package com.hbm.blocks.rail;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class StraightRailBlock extends NtmRailBlock {
    public enum Profile {
        FLAT,
        BUFFER,
        RAMP
    }

    private final RailGauge gauge;
    private final int reach;
    private final Profile profile;
    private final StraightRun run;

    public StraightRailBlock(Properties props, RailGauge gauge, int reach, Profile profile) {
        super(props);
        this.gauge = gauge;
        this.reach = reach;
        this.profile = profile;
        boolean standard = gauge == RailGauge.STANDARD;
        run =
                new StraightRun(
                        -reach, reach + 1, reach + 1, standard ? 0.1875 : 0, standard ? 0.5 : 0);
    }

    @Override
    public RailGauge gauge(Level level, int x, int y, int z) {
        return gauge;
    }

    @Override
    public void travel(
            Level level,
            int x,
            int y,
            int z,
            double trainX,
            double trainY,
            double trainZ,
            double headingX,
            double headingY,
            double headingZ,
            double speed,
            RailStep step,
            double collisionReach) {
        travelStraight(run, y, trainX, trainZ, headingX, headingZ, speed, step, collisionReach);
    }

    @Override
    protected double straightFloor(Owner owner, int y, int core, double target) {
        if (profile != Profile.RAMP) return y;
        double rise = (core + run.to() - target) / (run.to() - run.from());
        double floor = facesPositive(owner.facing) ? owner.coreY + rise : owner.coreY + 1 - rise;
        return Mth.clamp(floor, owner.coreY, owner.coreY + 1);
    }

    @Override
    protected int straightExitY(Owner owner, int y, boolean towardPositive) {
        if (profile != Profile.RAMP) return y;
        return towardPositive != facesPositive(owner.facing) ? owner.coreY + 1 : owner.coreY;
    }

    @Override
    protected boolean straightStop(
            int core,
            double target,
            double along,
            double across,
            double height,
            RailStep step,
            double collisionReach) {
        if (profile != Profile.BUFFER) return false;
        double stopped =
                facesPositive(step.owner.facing)
                        ? Mth.clamp(target, core + 1 + collisionReach, core + run.to())
                        : Mth.clamp(target, core + run.from(), core - collisionReach);
        if (stopped == along) return false;
        placeOnRun(step, stopped, across, height);
        return true;
    }

    private static boolean facesPositive(Direction facing) {
        return facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
    }
}
