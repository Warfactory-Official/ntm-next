// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;

public class FT_Flammable extends FluidTrait {

    private long energy;

    public FT_Flammable() {}

    public FT_Flammable(long energy) {
        this.energy = energy;
    }

    public long getHeatEnergy() {
        return energy;
    }

    @Override
    public void addInfo(List<String> info) {
        info.add(
                ChatFormatting.YELLOW
                        + "["
                        + I18nUtil.resolveKey("hbmfluid.trait.flammable")
                        + "]");
        if (energy > 0) {
            info.add(
                    ChatFormatting.YELLOW
                            + I18nUtil.resolveKey("hbmfluid.trait.provides")
                            + " "
                            + ChatFormatting.RED
                            + BobMathUtil.getShortNumber(energy)
                            + "TU "
                            + ChatFormatting.YELLOW
                            + I18nUtil.resolveKey("hbmfluid.trait.perBucket"));
        }
    }
}
