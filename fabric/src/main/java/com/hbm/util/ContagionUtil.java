// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
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
        HbmLivingProps props = HbmLivingProps.peek(living);
        return props != null && props.contagion > 0;
    }

    public static void onDeath(LivingEntity entity) {
        if (!isContagious(entity)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            taint(entity.getItemBySlot(slot));
        }
    }

    public static boolean isProtected(LivingEntity entity) {
        return ArmorUtil.checkForHaz2(entity)
                && ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.BACTERIA);
    }
}
