// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;

public class FT_PWRModerator extends FluidTrait {

    private final double multiplier;

    public FT_PWRModerator(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public void addInfo(List<String> info) {
        info.add(
                ChatFormatting.BLUE
                        + "["
                        + I18nUtil.resolveKey("hbmfluid.trait.pwrFluxMultiplier")
                        + "]");
    }

    @Override
    public void addInfoHidden(List<String> info) {
        int mult = (int) (multiplier * 100 - 100);
        info.add(
                ChatFormatting.BLUE
                        + I18nUtil.resolveKey("hbmfluid.trait.pwrFluxCore")
                        + " "
                        + (mult >= 0 ? "+" : "")
                        + mult
                        + "%");
    }
}
