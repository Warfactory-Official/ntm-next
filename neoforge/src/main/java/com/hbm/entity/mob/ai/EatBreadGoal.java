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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jspecify.annotations.Nullable;

public class EatBreadGoal extends Goal {

    private static final EntityTypeTest<Entity, ItemEntity> ITEMS =
            EntityTypeTest.forClass(ItemEntity.class);
    private static final double SEARCH_REACH = 10D;
    private static final float BITE_REACH = 1F;

    private final PigeonEntity pigeon;
    private final double speed;
    private final List<ItemEntity> sighting = new ArrayList<>(1);
    private @Nullable ItemEntity bread;

    public EatBreadGoal(PigeonEntity pigeon, double speed) {
        this.pigeon = pigeon;
        this.speed = speed;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        bread = hungry() ? firstBreadInSight() : null;
        return bread != null;
    }

    private boolean hungry() {
        return !pigeon.isFat() && !pigeon.isFlying();
    }

    private @Nullable ItemEntity firstBreadInSight() {
        sighting.clear();
        pigeon.level()
                .getEntities(
                        ITEMS,
                        pigeon.getBoundingBox().inflate(SEARCH_REACH),
                        item -> item.getItem().is(Items.BREAD),
                        sighting,
                        1);
        return sighting.isEmpty() ? null : sighting.getFirst();
    }

    @Override
    public boolean canContinueToUse() {
        return bread != null && !bread.isRemoved() && canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var meal = bread;
        pigeon.getLookControl().setLookAt(meal, 30.0F, pigeon.getMaxHeadXRot());
        if (pigeon.distanceTo(meal) > BITE_REACH) {
            pigeon.getNavigation().moveTo(meal, speed);
            return;
        }

        var random = pigeon.getRandom();
        if (random.nextInt(3) == 0) takeBite(meal);
        pigeon.setFat(true);
        pigeon.playSound(
                SoundEvents.GENERIC_EAT.value(),
                0.5F + 0.5F * random.nextInt(2),
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }

    private void takeBite(ItemEntity meal) {
        var stack = meal.getItem();
        if (stack.getCount() > 1) {
            var level = pigeon.level();
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            meal.getX(),
                            meal.getY(),
                            meal.getZ(),
                            stack.copyWithCount(stack.getCount() - 1),
                            0D,
                            0D,
                            0D));
        }
        meal.discard();
    }
}
