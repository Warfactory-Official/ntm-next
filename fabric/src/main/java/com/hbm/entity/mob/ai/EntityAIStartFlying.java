// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import com.hbm.entity.mob.IFlyingCreature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class EntityAIStartFlying extends Goal {

    private final LivingEntity living;
    private final IFlyingCreature flying;

    public EntityAIStartFlying(LivingEntity living, IFlyingCreature flying) {
        this.living = living;
        this.flying = flying;
    }

    @Override
    public boolean canUse() {

        return flying.getFlyingState() == IFlyingCreature.STATE_WALKING
                && (living.getLastHurtByMob() != null
                        || living.isOnFire()
                        || living.getRandom().nextInt(600) == 0);
    }

    @Override
    public void start() {
        flying.setFlyingState(IFlyingCreature.STATE_FLYING);
    }
}
