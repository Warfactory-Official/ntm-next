/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity.mob.ai;

import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class MaskmanMinigunGoal extends Goal {

    private final PathfinderMob owner;
    private final int delay;
    private int timer;

    public MaskmanMinigunGoal(PathfinderMob owner, int delay) {
        this.owner = owner;
        this.delay = delay;
        this.timer = delay;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.owner.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = Math.sqrt(this.owner.distanceToSqr(target));
        return dist > 5D && dist < 10D;
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

        this.timer--;

        LivingEntity target = this.owner.getTarget();
        if (target != null) this.owner.getLookControl().setLookAt(target, 15F, 15F);

        if (this.timer > 0) return;
        this.timer = this.delay;

        BulletBaseMK4Entity bullet =
                new BulletBaseMK4Entity(
                        this.owner, XFactory762mm.r762_fmj, 5F, 0.075F, -1.5D, -1.5D, 0D);

        this.owner.level().addFreshEntity(bullet);
        WorldSounds.play(
                this.owner.level(),
                null,
                this.owner.getX(),
                this.owner.getY(),
                this.owner.getZ(),
                ModSounds.GUN_MINIGUN_FIRE.get(),
                SoundSource.HOSTILE,
                1F,
                1F);
    }
}
