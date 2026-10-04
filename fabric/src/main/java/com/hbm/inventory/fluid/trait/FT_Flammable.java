// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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
    public void addInfo(List<Component> info) {
        info.add(
                Component.empty()
                        .append(Component.literal("[").withStyle(ChatFormatting.YELLOW))
                        .append(
                                Component.translatable("hbmfluid.trait.flammable")
                                        .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("]").withStyle(ChatFormatting.YELLOW)));
        if (energy > 0) {
            info.add(
                    Component.empty()
                            .append(
                                    Component.translatable("hbmfluid.trait.provides")
                                            .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" ").withStyle(ChatFormatting.YELLOW))
                            .append(
                                    Component.literal(BobMathUtil.getShortNumber(energy) + "TU ")
                                            .withStyle(ChatFormatting.RED))
                            .append(
                                    Component.translatable("hbmfluid.trait.perBucket")
                                            .withStyle(ChatFormatting.YELLOW)));
        }
    }
}
