// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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
    public void addInfo(List<Component> info) {
        if (isHighlyCorrosive()) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.GOLD))
                            .append(
                                    Component.translatable("hbmfluid.trait.corrosiveStrong")
                                            .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("]").withStyle(ChatFormatting.GOLD)));
        } else {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.YELLOW))
                            .append(
                                    Component.translatable("hbmfluid.trait.corrosive")
                                            .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("]").withStyle(ChatFormatting.YELLOW)));
        }
    }
}
