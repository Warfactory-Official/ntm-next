// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;

public class FT_Corrosive extends FluidTrait {

    private int rating;

    public FT_Corrosive() {}

    public FT_Corrosive(int rating) {
        this.rating = rating;
    }

    public int getRating() {
        return rating;
    }

    public boolean isHighlyCorrosive() {
        return rating > 50;
    }

    @Override
    public void addInfo(List<String> info) {
        if (isHighlyCorrosive()) {
            info.add(
                    ChatFormatting.GOLD
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.corrosiveStrong")
                            + "]");
        } else {
            info.add(
                    ChatFormatting.YELLOW
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.corrosive")
                            + "]");
        }
    }
}
