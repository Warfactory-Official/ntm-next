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

import com.hbm.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class BreakingGoal extends Goal {

    private static final double PROBE_LENGTH = 2D;
    private static final double REACH_SQR = 4D * 4D;
    private static final int PROBE_INTERVAL = 10;
    private static final int CHIP_INTERVAL = 5;

    private final Mob digger;
    private @Nullable LivingEntity target;
    private @Nullable BlockPos marked;
    private int digTick;
    private int probeCell;

    public BreakingGoal(Mob digger) {
        this.digger = digger;
    }

    @Override
    public boolean canUse() {
        target = digger.getTarget();
        if (!stuckOn(target)) return false;
        marked = probe();
        return marked != null;
    }

    private boolean stuckOn(@Nullable LivingEntity goal) {
        if (goal == null || !digger.getNavigation().isDone()) return false;
        boolean digWorthwhile = goal.onGround() || !digger.hasLineOfSight(goal);
        return digWorthwhile && digger.distanceTo(goal) > 1D;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return marked != null && digger.isAlive() && eyeDistanceSqr(marked) <= REACH_SQR;
    }

    private double eyeDistanceSqr(BlockPos pos) {
        double dx = pos.getX() - digger.getX();
        double dy = pos.getY() - (digger.getY() + digger.getEyeHeight());
        double dz = pos.getZ() - digger.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void tick() {
        if (digger.tickCount % PROBE_INTERVAL == 0) {
            var found = probe();
            if (found != null) marked = found;
        }

        var level = digger.level();
        var state = marked == null ? null : level.getBlockState(marked);
        if (state == null || state.isAir()) {
            digTick = 0;
            return;
        }

        digTick++;
        float hardness = state.getDestroySpeed(level, marked);
        if (hardness < 0) {
            marked = null;
            return;
        }
        int toughness = (int) hardness / 3;

        float progress = digTick * 0.05F / toughness;
        if (progress >= 1F) {
            breakMarked(level, state);
        } else if (digTick % CHIP_INTERVAL == 0) {
            chip(level, state, progress);
        }
    }

    private void breakMarked(Level level, BlockState state) {
        digTick = 0;
        if (level instanceof ServerLevel server
                && Platform.canEntityDestroyBlock(server, marked, state, digger)) {
            level.destroyBlock(marked, false);
        }
        if (target != null) {
            var navigation = digger.getNavigation();
            navigation.moveTo(navigation.createPath(target, 0), 1D);
        }
        marked = null;
    }

    private void chip(Level level, BlockState state, float progress) {
        var sound = state.getSoundType();
        level.playSound(
                null,
                digger.getX(),
                digger.getY(),
                digger.getZ(),
                sound.getStepSound(),
                digger.getSoundSource(),
                sound.getVolume() + 1F,
                sound.getPitch());
        ((LivingEntity) digger).swing(InteractionHand.MAIN_HAND);
        level.destroyBlockProgress(digger.getId(), marked, (int) (progress * 10F));
    }

    @Override
    public void stop() {
        digTick = 0;
        marked = null;
    }

    private @Nullable BlockPos probe() {
        int width = Mth.ceil(digger.getBbWidth());
        int layer = width * width;
        int cells = layer * Mth.ceil(digger.getBbHeight());
        int half = width / 2;

        int cell = probeCell;
        var from =
                digger.position()
                        .add(cell % width - half, cell / layer, cell % layer / width - half);
        var heading = digger.getLookAngle().scale(PROBE_LENGTH);
        var hit =
                digger.level()
                        .clip(
                                new ClipContext(
                                        from,
                                        from.add(heading),
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        digger));

        if (hit.getType() == HitResult.Type.BLOCK && breakable(hit.getBlockPos())) {
            probeCell = 0;
            return hit.getBlockPos();
        }
        probeCell = (cell + 1) % cells;
        return null;
    }

    private boolean breakable(BlockPos pos) {
        return digger.level().getBlockState(pos).getDestroySpeed(digger.level(), pos) >= 0;
    }
}
