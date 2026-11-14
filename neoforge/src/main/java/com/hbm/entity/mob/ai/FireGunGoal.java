/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.handler.KeybindType;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

public final class FireGunGoal extends Goal {

    private final Mob host;

    public double attackMoveSpeed = 1.0D;
    public double maxRange = 20D;
    public int burstTime = 10;
    public int minWait = 10;
    public int maxWait = 40;
    public float inaccuracy = 30F;
    public boolean randomBurst = true;

    private int attackTimer;
    private FireState state = FireState.IDLE;
    private int stateTimer;

    public FireGunGoal(Mob host) {
        this.host = host;
    }

    @Override
    public boolean canUse() {
        return host.getTarget() != null && getYerGun() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = host.getTarget();
        ItemStack stack = host.getMainHandItem();
        GunBaseNTItem gun = getYerGun();
        if (target == null || gun == null || !(host.level() instanceof ServerLevel level)) return;

        gun.inventoryTick(stack, level, host, EquipmentSlot.MAINHAND);

        double distanceToTargetSquared =
                host.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSeeTarget = host.getSensing().hasLineOfSight(target);
        if (canSeeTarget) attackTimer++;
        else attackTimer = 0;

        if (distanceToTargetSquared < maxRange * maxRange && attackTimer > 20) {
            host.getNavigation().stop();
        } else {
            host.getNavigation().moveTo(target, attackMoveSpeed);
        }
        host.getLookControl().setLookAt(target, 30F, 30F);

        stateTimer--;
        if (stateTimer < 0) {
            stateTimer = 0;
            if (state == FireState.WAIT) {
                updateState(FireState.IDLE, 0, gun, stack);
            } else if (state != FireState.IDLE) {
                updateState(
                        FireState.WAIT,
                        host.getRandom().nextInt(maxWait - minWait) + minWait,
                        gun,
                        stack);
            }
        } else if (state == FireState.FIRING) {
            updateKeybind(gun, stack, KeybindType.GUN_PRIMARY);
        }

        if (canSeeTarget
                && distanceToTargetSquared < maxRange * maxRange
                && state == FireState.IDLE) {
            GunConfig config = gun.getConfig(stack, 0);
            Receiver receiver = config.getReceivers(stack).get(0);
            if (receiver.getMagazine(stack).getAmount(stack, null) <= 0) {
                updateState(FireState.RELOADING, 20, gun, stack);
            } else if (GunBaseNTItem.getState(stack, 0) == GunState.IDLE) {
                int time = randomBurst ? host.getRandom().nextInt(burstTime) : burstTime;
                updateState(FireState.FIRING, time, gun, stack);
            }
        }
    }

    private void updateState(FireState toState, int time, GunBaseNTItem gun, ItemStack stack) {
        state = toState;
        stateTimer = time;
        switch (state) {
            case FIRING:
                updateKeybind(gun, stack, KeybindType.GUN_PRIMARY);
            case RELOADING:
                updateKeybind(gun, stack, KeybindType.RELOAD);
            default:
                clearKeybinds(gun, stack);
        }
    }

    private void clearKeybinds(GunBaseNTItem gun, ItemStack stack) {
        updateKeybind(gun, stack, null);
    }

    private void updateKeybind(GunBaseNTItem gun, ItemStack stack, KeybindType bind) {
        if (bind != null && bind != KeybindType.RELOAD) {
            float spread =
                    inaccuracy
                            * getYerGun()
                                    .getConfig(stack, 0)
                                    .getReceivers(stack)
                                    .get(0)
                                    .getHipfireSpread(stack)
                            * 20F;
            host.yHeadRot += (host.getRandom().nextFloat() - 0.5F) * spread;
            host.xRot += (host.getRandom().nextFloat() - 0.5F) * spread;
            host.yRot = host.yHeadRot;
            host.yBodyRot = host.yRot;
        }

        gun.handleKeybind(
                host, null, stack, KeybindType.GUN_PRIMARY, bind == KeybindType.GUN_PRIMARY);
        gun.handleKeybind(
                host, null, stack, KeybindType.GUN_SECONDARY, bind == KeybindType.GUN_SECONDARY);
        gun.handleKeybind(
                host, null, stack, KeybindType.GUN_TERTIARY, bind == KeybindType.GUN_TERTIARY);
        gun.handleKeybind(host, null, stack, KeybindType.RELOAD, bind == KeybindType.RELOAD);
    }

    public GunBaseNTItem getYerGun() {
        ItemStack stack = host.getMainHandItem();
        return stack.getItem() instanceof GunBaseNTItem gun ? gun : null;
    }

    private enum FireState {
        IDLE,
        WAIT,
        FIRING,
        RELOADING
    }
}
