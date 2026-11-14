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

import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class MaskmanLasergunGoal extends Goal {

    private final PathfinderMob owner;
    private Attack attack;
    private int timer;
    private int attackCount;

    public MaskmanLasergunGoal(PathfinderMob owner) {
        this.owner = owner;
        this.attack = Attack.values()[owner.getRandom().nextInt(3)];
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.owner.getTarget();
        return target != null && Math.sqrt(this.owner.distanceToSqr(target)) > 10D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !this.owner.getNavigation().isDone();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {

        LivingEntity target = this.owner.getTarget();
        if (target == null) return;

        this.timer--;

        if (this.timer <= 0) {
            this.timer = this.attack.delay;
            fire(target);
            this.attackCount++;

            if (this.attackCount >= this.attack.amount) {
                this.attackCount = 0;
                int next =
                        this.attack.ordinal()
                                + this.owner.getRandom().nextInt(Attack.values().length - 1);
                this.attack = Attack.values()[next % Attack.values().length];
            }
        }
    }

    private void fire(LivingEntity target) {

        switch (this.attack) {
            case ORB -> {
                BulletBaseMK4Entity orb =
                        XFactoryNPC.aimed(
                                this.owner,
                                XFactoryNPC.maskman_orb,
                                XFactoryNPC.MASKMAN_ORB_DAMAGE,
                                target,
                                0F);
                if (orb == null) return;
                orb.setDeltaMovement(orb.getDeltaMovement().add(0D, 0.5D, 0D));
                this.owner.level().addFreshEntity(orb);
                play(ModSounds.GUN_TESLA_FIRE.get());
            }

            case MISSILE -> {
                BulletBaseMK4Entity missile =
                        XFactoryNPC.aimed(
                                this.owner,
                                XFactoryNPC.maskman_rocket,
                                XFactoryNPC.MASKMAN_ROCKET_DAMAGE,
                                target,
                                0F);
                if (missile == null) return;
                Vec3 flat =
                        new Vec3(
                                target.getX() - this.owner.getX(),
                                0D,
                                target.getZ() - this.owner.getZ());
                missile.setDeltaMovement(
                        flat.x * 0.05D,
                        0.5D + this.owner.getRandom().nextDouble() * 0.5D,
                        flat.z * 0.05D);
                this.owner.level().addFreshEntity(missile);
                play(ModSounds.GUN_UNDERBARREL_FIRE.get());
            }

            case SPLASH -> {
                for (int i = 0; i < 5; i++) {
                    BulletBaseMK4Entity tracer =
                            XFactoryNPC.aimed(
                                    this.owner,
                                    XFactoryNPC.maskman_tracer,
                                    XFactoryNPC.MASKMAN_TRACER_DAMAGE,
                                    target,
                                    0.05F);
                    if (tracer != null) this.owner.level().addFreshEntity(tracer);
                }
            }
        }
    }

    private void play(SoundEvent sound) {
        WorldSounds.play(
                this.owner.level(),
                null,
                this.owner.getX(),
                this.owner.getY(),
                this.owner.getZ(),
                sound,
                SoundSource.HOSTILE,
                1F,
                1F);
    }

    private enum Attack {
        ORB(60, 5),
        MISSILE(10, 10),
        SPLASH(40, 3);

        final int delay;
        final int amount;

        Attack(int delay, int amount) {
            this.delay = delay;
            this.amount = amount;
        }
    }
}
