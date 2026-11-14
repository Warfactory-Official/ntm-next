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

import java.util.function.Predicate;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

public class WanderConditionalGoal extends RandomStrollGoal {

    private final Predicate<PathfinderMob> condition;

    public WanderConditionalGoal(
            PathfinderMob creature, double speed, Predicate<PathfinderMob> condition) {
        super(creature, speed);
        this.condition = condition;
    }

    @Override
    public boolean canUse() {
        return condition.test(mob) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return condition.test(mob) && super.canContinueToUse();
    }
}
