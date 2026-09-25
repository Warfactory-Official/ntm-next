// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid.ai;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

public class GlyphidWanderGoal extends RandomStrollGoal {

    private final EntityGlyphid glyphid;

    public GlyphidWanderGoal(EntityGlyphid glyphid, double speedModifier) {
        super(glyphid, speedModifier);
        this.glyphid = glyphid;
    }

    @Override
    public boolean canUse() {
        return glyphid.getCurrentTask() == EntityGlyphid.TASK_IDLE && super.canUse();
    }
}
