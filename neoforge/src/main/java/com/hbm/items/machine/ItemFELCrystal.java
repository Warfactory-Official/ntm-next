// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemFELCrystal extends Item {

    public final EnumWavelengths wavelength;

    public ItemFELCrystal(Properties props, EnumWavelengths wavelength) {
        super(props);
        this.wavelength = wavelength;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        adder.accept(
                Component.literal(
                        wavelength == EnumWavelengths.DRX
                                ? ChatFormatting.OBFUSCATED + "THERADIANCEOFATHOUSANDSUNS"
                                : I18nUtil.resolveKey(this.getDescriptionId() + ".desc")));
        adder.accept(
                Component.literal(
                        wavelength.textColor
                                + I18nUtil.resolveKey(wavelength.name)
                                + " - "
                                + wavelength.textColor
                                + I18nUtil.resolveKey(wavelength.wavelengthRange)));
    }
}
