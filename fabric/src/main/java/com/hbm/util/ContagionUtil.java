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

package com.hbm.util;

import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.Exposure;
import com.hbm.items.ModDataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ContagionUtil {

    private ContagionUtil() {}

    public static boolean isContagious(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.CONTAGION.get()));
    }

    public static void taint(ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.set(ModDataComponents.CONTAGION.get(), true);
    }

    public static void cure(ItemStack stack) {
        stack.remove(ModDataComponents.CONTAGION.get());
    }

    public static boolean isContagious(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        LivingHazards props = LivingHazards.getIfPresent(living);
        return props != null && props.contagion() > 0;
    }

    public static void onDeath(LivingEntity entity) {
        if (!isContagious(entity)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            taint(entity.getItemBySlot(slot));
        }
    }

    public static boolean isProtected(LivingEntity entity) {
        return ArmorUtil.checkForHaz2(entity)
                && ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, Exposure.BACTERIA);
    }
}
