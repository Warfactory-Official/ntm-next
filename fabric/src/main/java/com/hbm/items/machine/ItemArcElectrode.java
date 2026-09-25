// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemArcElectrode extends Item {

    public final EnumElectrodeType type;

    public ItemArcElectrode(Properties props, EnumElectrodeType type) {
        super(props);
        this.type = type;
    }

    public static int getDurability(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARC_ELECTRODE_DURABILITY.get(), 0);
    }

    public static boolean damage(ItemStack stack) {
        int durability = getDurability(stack) + 1;
        stack.set(ModDataComponents.ARC_ELECTRODE_DURABILITY.get(), durability);
        return durability >= getMaxDurability(stack);
    }

    public static int getMaxDurability(ItemStack stack) {
        return ((ItemArcElectrode) stack.getItem()).type.durability;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurability(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac =
                Math.min(1D, (double) getDurability(stack) / (double) getMaxDurability(stack));
        return (int) Math.round(13.0D * (1.0D - frac));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double frac =
                Math.min(1D, (double) getDurability(stack) / (double) getMaxDurability(stack));
        return Mth.hsvToRgb((float) ((1.0D - frac) / 3.0D), 1.0F, 1.0F);
    }

    public enum EnumElectrodeType {
        GRAPHITE(10),
        LANTHANIUM(100),
        DESH(500),
        SATURNITE(1500);

        public static final EnumElectrodeType[] VALUES = values();

        public final int durability;

        EnumElectrodeType(int dura) {
            this.durability = dura;
        }
    }
}
