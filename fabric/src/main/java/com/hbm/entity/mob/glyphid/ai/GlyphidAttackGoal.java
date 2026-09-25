// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid.ai;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class GlyphidAttackGoal extends MeleeAttackGoal {

    public GlyphidAttackGoal(EntityGlyphid glyphid) {
        super(glyphid, 1.0D, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (!canPerformAttack(target)) return;
        resetAttackCooldown();
        mob.doHurtTarget(getServerLevel(mob), target);
    }
}
