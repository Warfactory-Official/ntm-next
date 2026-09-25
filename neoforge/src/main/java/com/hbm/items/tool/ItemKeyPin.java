// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemKeyPin extends Item {

    private static final String PINS = "pins";

    public ItemKeyPin(Properties properties) {
        super(properties);
    }

    public static int getPins(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.PERSISTENT_DATA.get());
        return data == null ? 0 : data.copyTag().getIntOr(PINS, 0);
    }

    public static void setPins(ItemStack stack, int pins) {
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(), stack, tag -> tag.putInt(PINS, pins));
    }

    public boolean canTransfer() {
        return this != ModItems.KEY_FAKE.get();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        int pins = getPins(stack);
        adder.accept(
                pins != 0
                        ? Component.translatable("desc.item.keyPin.pinConfiguration", pins)
                        : Component.translatable("desc.item.keyPin.pinsNotSet"));
        if (this == ModItems.KEY_FAKE.get()) {
            adder.accept(Component.empty());
            adder.accept(Component.translatable("desc.item.keyPin.pinsCanNeitherBe"));
        }
    }
}
