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

package com.hbm.items.weapon;

import com.hbm.handler.MissileStruct;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.CustomMissilePartItem;
import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class CustomMissileItem extends Item {

    public CustomMissileItem(Properties properties) {
        super(properties);
    }

    public static ItemStack buildMissile(
            Item chip, Item warhead, Item fuselage, Item stability, Item thruster) {

        ItemStack missile = new ItemStack(ModItems.MISSILE_CUSTOM);

        missile.set(
                ModDataComponents.MISSILE_PARTS.get(),
                new MissileStruct(
                                asPart(warhead),
                                asPart(fuselage),
                                asPart(stability),
                                asPart(thruster))
                        .sanitised());

        if (chip instanceof CustomMissilePartItem)
            missile.set(ModDataComponents.MISSILE_CHIP.get(), chip);

        return missile;
    }

    public static ItemStack buildMissile(
            ItemStack chip,
            ItemStack warhead,
            ItemStack fuselage,
            ItemStack stability,
            ItemStack thruster) {
        return buildMissile(
                chip == null ? null : chip.getItem(),
                warhead.getItem(),
                fuselage.getItem(),
                stability == null || stability.isEmpty() ? null : stability.getItem(),
                thruster.getItem());
    }

    private static CustomMissilePartItem asPart(Item item) {
        return item instanceof CustomMissilePartItem part ? part : null;
    }

    public static MissileStruct getStruct(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof CustomMissileItem)) return null;
        return stack.get(ModDataComponents.MISSILE_PARTS.get());
    }

    public static CustomMissilePartItem getChip(ItemStack stack) {
        Item chip = stack.get(ModDataComponents.MISSILE_CHIP.get());
        return chip instanceof CustomMissilePartItem part ? part : null;
    }

    public static final OverridePoint<Describer> DESCRIBE =
            OverridePoint.of(RegistryUtil.id("missile_describe"), CustomMissileItem::describe);

    @FunctionalInterface
    public interface Describer {
        void describe(
                MissileStruct parts,
                @Nullable CustomMissilePartItem chip,
                Consumer<Component> adder);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        MissileStruct parts = getStruct(stack);
        if (parts == null) return;

        DESCRIBE.get().describe(parts, getChip(stack), adder);
    }

    public static void describe(
            MissileStruct parts, @Nullable CustomMissilePartItem chip, Consumer<Component> adder) {
        if (parts.warhead() != null) {
            adder.accept(stat("warhead", parts.warhead().warheadType().getDisplay()));
            adder.accept(
                    stat(
                            "strength",
                            Component.literal(String.valueOf(parts.warhead().warheadStrength()))));
        }
        if (parts.fuselage() != null) {
            adder.accept(stat("fuelType", parts.fuselage().fuelType().getDisplay()));
            adder.accept(
                    stat("fuelAmount", Component.literal(parts.fuselage().fuelAmount() + "l")));
        }

        adder.accept(
                stat(
                        "chipInaccuracy",
                        Component.literal((chip != null ? chip.inaccuracy() * 100 : 100F) + "%")));
        adder.accept(
                stat(
                        "finInaccuracy",
                        Component.literal(
                                parts.fins() != null
                                        ? parts.fins().inaccuracy() * 100 + "%"
                                        : "100%")));

        if (parts.fuselage() != null) {
            adder.accept(
                    stat(
                            "size",
                            parts.fuselage()
                                    .top
                                    .getDisplay()
                                    .copy()
                                    .append("/")
                                    .append(parts.fuselage().bottom.getDisplay())));
        }

        float health = 0;
        if (parts.warhead() != null) health += parts.warhead().health;
        if (parts.fuselage() != null) health += parts.fuselage().health;
        if (parts.thruster() != null) health += parts.thruster().health;
        if (parts.fins() != null) health += parts.fins().health;
        adder.accept(stat("health", Component.literal(health + "HP")));
    }

    public static Component stat(String key, Component value) {
        return Component.translatable("item.hbm.missile.desc." + key)
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value);
    }
}
