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

package com.hbm.entity.mob.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MaskmanCasualApproachGoal extends MeleeAttackGoal {

    public MaskmanCasualApproachGoal(PathfinderMob attacker, double speed, boolean longMemory) {
        super(attacker, speed, longMemory);
    }

    @Override
    public boolean canUse() {
        var navigation = (Navigation) mob.getNavigation();
        navigation.approach = this;
        try {
            return super.canUse();
        } finally {
            navigation.approach = null;
        }
    }

    @Override
    public void tick() {
        var navigation = (Navigation) mob.getNavigation();
        navigation.approach = this;
        try {
            super.tick();
        } finally {
            navigation.approach = null;
        }
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {}

    public Vec3 approachPos(LivingEntity target) {
        var away = mob.position().subtract(target.position());
        double range = Math.min(away.length(), 20D) - 10D;
        away = away.normalize();
        return new Vec3(
                mob.getX() + away.x * range + mob.getRandom().nextGaussian() * 2D,
                mob.getY() + away.y - 5D + mob.getRandom().nextInt(11),
                mob.getZ() + away.z * range + mob.getRandom().nextGaussian() * 2D);
    }

    public static final class Navigation extends GroundPathNavigation {
        private @Nullable MaskmanCasualApproachGoal approach;

        public Navigation(PathfinderMob mob, Level level) {
            super(mob, level);
        }

        @Override
        public @Nullable Path createPath(Entity target, int reachRange) {
            if (approach == null) return super.createPath(target, reachRange);
            var destination = approach.approachPos((LivingEntity) target);
            return createPath(destination.x, destination.y, destination.z, reachRange);
        }
    }
}
