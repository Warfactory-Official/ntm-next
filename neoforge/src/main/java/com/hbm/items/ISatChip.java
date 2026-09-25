// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public interface ISatChip {

    static int getFreqS(ItemStack stack) {
        return stack.getItem() instanceof ISatChip chip ? chip.getFreq(stack) : 0;
    }

    static void setFreqS(ItemStack stack, int freq) {
        if (stack.getItem() instanceof ISatChip chip) chip.setFreq(stack, freq);
    }

    default int getFreq(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.PERSISTENT_DATA.get());
        return data == null ? 0 : data.copyTag().getIntOr("freq", 0);
    }

    default void setFreq(ItemStack stack, int freq) {
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(), stack, tag -> tag.putInt("freq", freq));
    }
}
