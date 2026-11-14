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

import com.hbm.entity.mob.PigeonEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class StopFlyingGoal extends Goal {

    private final PigeonEntity pigeon;

    public StopFlyingGoal(PigeonEntity pigeon) {
        this.pigeon = pigeon;
    }

    @Override
    public boolean canUse() {
        return pigeon.isFlying() && pigeon.getRandom().nextInt(200) == 0;
    }

    @Override
    public void start() {
        pigeon.setFlying(false);
    }
}
