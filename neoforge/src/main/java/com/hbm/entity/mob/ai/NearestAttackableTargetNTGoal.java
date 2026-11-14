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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;

public class NearestAttackableTargetNTGoal<T extends LivingEntity>
        extends NearestAttackableTargetGoal<T> {

    public NearestAttackableTargetNTGoal(
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
