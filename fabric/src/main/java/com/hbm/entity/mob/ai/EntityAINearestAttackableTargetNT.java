// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;

public class EntityAINearestAttackableTargetNT<T extends LivingEntity>
        extends NearestAttackableTargetGoal<T> {

    public EntityAINearestAttackableTargetNT(
            Mob mob,
            Class<T> targetType,
            int randomInterval,
            boolean mustSee,
            boolean mustReach,
            TargetingConditions.@Nullable Selector selector) {
        super(mob, targetType, randomInterval, mustSee, mustReach, selector);
    }

    @Override
    public void stop() {
        this.mob.setTarget(this.target);
    }
}
