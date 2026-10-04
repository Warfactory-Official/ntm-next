// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_PWRModerator extends FluidTrait {

    private final double multiplier;

    public FT_PWRModerator(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(
                Component.empty()
                        .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                        .append(
                                Component.translatable("hbmfluid.trait.pwrFluxMultiplier")
                                        .withStyle(ChatFormatting.BLUE))
                        .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        int mult = (int) (multiplier * 100 - 100);
        info.add(
                Component.empty()
                        .append(
                                Component.translatable("hbmfluid.trait.pwrFluxCore")
                                        .withStyle(ChatFormatting.BLUE))
                        .append(
                                Component.literal(" " + (mult >= 0 ? "+" : "") + mult + "%")
                                        .withStyle(ChatFormatting.BLUE)));
    }
}
