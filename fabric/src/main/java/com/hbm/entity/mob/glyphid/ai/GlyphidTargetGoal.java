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

package com.hbm.entity.mob.glyphid.ai;

import com.hbm.entity.mob.glyphid.GlyphidEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public class GlyphidTargetGoal extends Goal {

    private final GlyphidEntity glyphid;

    public GlyphidTargetGoal(GlyphidEntity glyphid) {
        this.glyphid = glyphid;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (glyphid.getTarget() == null && !glyphid.hasHighPriorityWaypoint()) {
            @Nullable Player found = glyphid.findTargetCandidate();
            if (found != null) glyphid.setTarget(found);
        }

        int id = glyphid.getId();
        if (id % 3 > 0 && (id + glyphid.tickCount) % 100 == 0) {
            @Nullable Player found = glyphid.findTargetCandidate();
            if (found != null) glyphid.setTarget(found);
        }
    }
}
