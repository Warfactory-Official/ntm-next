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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class GlyphidAttackGoal extends MeleeAttackGoal {

    public GlyphidAttackGoal(GlyphidEntity glyphid) {
        super(glyphid, 1.0D, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (!canPerformAttack(target)) return;
        resetAttackCooldown();
        mob.doHurtTarget(getServerLevel(mob), target);
    }
}
