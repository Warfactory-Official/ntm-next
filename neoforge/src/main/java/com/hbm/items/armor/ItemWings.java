// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemWings extends ItemArmorMod {

    public enum Kind {
        MURK,
        LIMP
    }

    public final Kind kind;

    public ItemWings(Properties properties, Kind kind) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
        this.kind = kind;
    }

    @Override
    protected boolean hasTooltipSpacer() {
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(
                Component.translatable("item.hbm.jetpack.wearable").withStyle(ChatFormatting.GOLD));
    }
}
