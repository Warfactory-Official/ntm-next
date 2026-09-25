// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.interfaces.HalfLifeType;
import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class RTGUtil {

    public static final boolean RTG_DECAY = true;
    public static final boolean SCALE_POWER = false;

    public static short getPower(ItemRTGPellet fuel, ItemStack stack) {
        return SCALE_POWER ? ItemRTGPellet.getScaledPower(fuel, stack) : fuel.getHeat();
    }

    public static boolean hasHeat(NonNullList<ItemStack> inventory, int[] rtgSlots) {
        for (int slot : rtgSlots) {
            ItemStack stack = inventory.get(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemRTGPellet) return true;
        }
        return false;
    }

    public static int updateRTGs(NonNullList<ItemStack> inventory, int[] rtgSlots) {
        int newHeat = 0;
        for (int slot : rtgSlots) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemRTGPellet pellet)) continue;
            newHeat += getPower(pellet, stack);
            inventory.set(slot, ItemRTGPellet.handleDecay(stack, pellet));
        }
        return newHeat;
    }

    public static long getLifespan(float halfLife, HalfLifeType type, boolean realYears) {
        float life = 0;
        switch (type) {
            case LONG:
                life = (48000 * (realYears ? 365 : 100) * 100) * halfLife;
                break;
            case MEDIUM:
                life = (48000 * (realYears ? 365 : 100)) * halfLife;
                break;
            case SHORT:
                life = 48000 * halfLife;
                break;
        }
        return (long) life;
    }
}
