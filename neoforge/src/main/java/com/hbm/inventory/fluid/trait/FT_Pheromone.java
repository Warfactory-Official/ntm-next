// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;

public class FT_Pheromone extends FluidTrait {

    private int type;

    public FT_Pheromone() {}

    public FT_Pheromone(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Override
    public void addInfo(List<String> info) {
        if (type == 1) {
            info.add(
                    ChatFormatting.AQUA
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.glyphidPheromones")
                            + "]");
        } else {
            info.add(
                    ChatFormatting.BLUE
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.modifiedPheromones")
                            + "]");
        }
    }
}
