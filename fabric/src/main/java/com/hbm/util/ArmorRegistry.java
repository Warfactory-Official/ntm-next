// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.EquipmentProtections;
import com.hbm.hazard.HazardClass;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;

public final class ArmorRegistry {

    private ArmorRegistry() {}

    public static boolean hasProtection(
            LivingEntity entity, EquipmentSlot slot, HazardClass clazz) {
        ItemStack stack = entity.getItemBySlot(slot);
        return !stack.isEmpty() && (protectionMask(stack) & clazz.bit()) != 0;
    }

    public static List<HazardClass> getProtectionFromItem(ItemInstance stack) {
        List<HazardClass> protection =
                new ArrayList<>(List.of(EquipmentProtections.hazardOrder(stack)));
        if (EquipmentProtections.isMask(stack)) {
            ItemStack filter = ArmorUtil.getGasMaskFilter(stack);
            int exclusions = EquipmentProtections.filterExclusions(stack);
            for (HazardClass hazard : EquipmentProtections.hazardOrder(filter)) {
                if ((exclusions & hazard.bit()) == 0) protection.add(hazard);
            }
        }
        if (ArmorModHandler.hasMods(stack)) {
            var mods = ArmorModHandler.mods(stack);
            for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                var mod = mods.template(socket);
                if (mod != null) protection.addAll(getProtectionFromItem(mod));
            }
        }
        return protection;
    }

    public static int protectionMask(ItemInstance stack) {
        int mask = EquipmentProtections.hazards(stack);
        if (EquipmentProtections.isMask(stack)) {
            mask |=
                    EquipmentProtections.hazards(ArmorUtil.getGasMaskFilter(stack))
                            & ~EquipmentProtections.filterExclusions(stack);
        }
        if (ArmorModHandler.hasMods(stack)) {
            var mods = ArmorModHandler.mods(stack);
            for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                var mod = mods.template(socket);
                if (mod != null) mask |= protectionMask(mod);
            }
        }
        return mask;
    }

    public static void addProtectionTooltip(
            ItemStack stack, boolean expanded, Consumer<Component> adder) {
        HazardClass[] classes = EquipmentProtections.hazardOrder(stack);
        if (classes.length == 0) return;
        if (!expanded) {
            adder.accept(
                    Component.translatable(
                            "tooltip.hbm.armor.protection_hint",
                            KeyHints.key("key.hbm.tooltipDetails", "key.keyboard.left.shift")
                                    .copy()
                                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));
            return;
        }
        adder.accept(Component.translatable("hazard.prot").withStyle(ChatFormatting.GOLD));
        for (HazardClass clazz : classes) {
            adder.accept(
                    Component.literal("  ")
                            .append(Component.translatable(clazz.lang))
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
