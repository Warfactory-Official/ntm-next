// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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
    public void addInfo(List<Component> info) {
        if (type == 1) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.AQUA))
                            .append(
                                    Component.translatable("hbmfluid.trait.glyphidPheromones")
                                            .withStyle(ChatFormatting.AQUA))
                            .append(Component.literal("]").withStyle(ChatFormatting.AQUA)));
        } else {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.modifiedPheromones")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }
}
