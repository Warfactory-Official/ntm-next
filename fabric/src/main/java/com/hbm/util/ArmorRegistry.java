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

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.EquipmentProtections;
import com.hbm.hazard.Exposure;
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

    public static boolean hasProtection(LivingEntity entity, EquipmentSlot slot, Exposure clazz) {
        ItemStack stack = entity.getItemBySlot(slot);
        return !stack.isEmpty() && (protectionMask(stack) & clazz.bit) != 0;
    }

    public static List<Exposure> getProtectionFromItem(ItemInstance stack) {
        int mask = protectionMask(stack);
        List<Exposure> protection = new ArrayList<>(Integer.bitCount(mask));
        for (Exposure hazard : Exposure.values())
            if ((mask & hazard.bit) != 0) protection.add(hazard);
        return protection;
    }

    public static int protectionMask(ItemInstance stack) {
        int mask = EquipmentProtections.hazards(stack);
        if (EquipmentProtections.isMask(stack)) {
            mask |=
                    EquipmentProtections.hazards(ArmorUtil.getGasMaskFilter(stack))
                            & ~EquipmentProtections.filterExclusions(stack);
        }
        if (!ArmorModHandler.hasMods(stack)) return mask;

        var mods = ArmorModHandler.mods(stack);
        for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
            var mod = mods.template(socket);
            if (mod != null) mask |= protectionMask(mod);
        }
        return mask;
    }

    public static void addProtectionTooltip(
            ItemStack stack, boolean expanded, Consumer<Component> adder) {
        Exposure[] classes = EquipmentProtections.hazardOrder(stack);
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
        for (Exposure clazz : classes) {
            adder.accept(
                    Component.literal("  ")
                            .append(Component.translatable(clazz.translationKey))
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
