// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.items.armor.ItemModCladding;
import com.hbm.potion.HbmPotion;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class HazmatRegistry {

    private static final Map<Item, Double> resistance = new HashMap<>();

    private HazmatRegistry() {}

    public static void register(ItemLike item, double value) {
        resistance.put(item.asItem(), value);
    }

    public static double getResistance(ItemStack stack) {
        Double r = resistance.get(stack.getItem());
        return (r != null ? r : 0D) + getCladding(stack);
    }

    public static double getCladding(ItemStack stack) {
        ItemStack cladding = ArmorModHandler.pryMod(stack, ArmorModHandler.CLADDING);
        return cladding.getItem() instanceof ItemModCladding mod ? mod.rad : 0D;
    }

    public static float getResistance(LivingEntity entity) {
        float sum = 0F;
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) sum += getResistance(stack);
        }

        if (entity.hasEffect(HbmPotion.radx())) sum += 0.2F;
        return sum;
    }
}
