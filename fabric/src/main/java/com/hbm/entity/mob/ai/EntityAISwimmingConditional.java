// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class EntityAISwimmingConditional extends Goal {

    private final Mob living;
    private final Predicate<Mob> condition;

    public EntityAISwimmingConditional(Mob living, Predicate<Mob> condition) {
        this.living = living;
        this.condition = condition;
        setFlags(EnumSet.of(Goal.Flag.JUMP));
        living.getNavigation().setCanFloat(true);
    }

    @Override
    public boolean canUse() {
        return (living.isInWater() || living.isInLava()) && condition.test(living);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (living.getRandom().nextFloat() < 0.8F) living.getJumpControl().jump();
    }
}
