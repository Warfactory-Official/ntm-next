// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.items.armor.IGasMask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class ArmorRegistry {

    private static final Map<Item, List<HazardClass>> hazardClasses = new HashMap<>();

    private ArmorRegistry() {}

    public static void registerHazard(ItemLike item, HazardClass... classes) {
        hazardClasses
                .computeIfAbsent(item.asItem(), k -> new ArrayList<>())
                .addAll(List.of(classes));
    }

    public static boolean hasProtection(
            LivingEntity entity, EquipmentSlot slot, HazardClass clazz) {
        ItemStack stack = entity.getItemBySlot(slot);
        return !stack.isEmpty() && getProtectionFromItem(stack).contains(clazz);
    }

    public static List<HazardClass> getProtectionFromItem(ItemStack stack) {
        List<HazardClass> protection = new ArrayList<>();
        List<HazardClass> direct = hazardClasses.get(stack.getItem());
        if (direct != null) protection.addAll(direct);
        if (stack.getItem() instanceof IGasMask mask) {
            ItemStack filter = ArmorUtil.getGasMaskFilter(stack);
            List<HazardClass> filterProtection = hazardClasses.get(filter.getItem());
            if (!filter.isEmpty() && filterProtection != null) {
                List<HazardClass> filtered = new ArrayList<>(filterProtection);
                filtered.removeAll(List.of(mask.filterBlacklist()));
                protection.addAll(filtered);
            }
        }
        if (ArmorModHandler.hasMods(stack)) {
            for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
                if (!mod.isEmpty()) protection.addAll(getProtectionFromItem(mod));
            }
        }
        return protection;
    }

    public static void addProtectionTooltip(
            ItemStack stack, boolean expanded, Consumer<Component> adder) {
        List<HazardClass> classes = hazardClasses.get(stack.getItem());
        if (classes == null) return;
        if (!expanded) {
            adder.accept(Component.translatable("desc.item.armorRegistry.holdLshiftToDisplay"));
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
