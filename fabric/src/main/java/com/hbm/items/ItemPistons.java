// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemPistons extends Item {

    public final EnumPistonType type;

    public ItemPistons(Properties properties, EnumPistonType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(efficiencyHeader());
        for (int i = 0; i < type.eff.length; i++)
            adder.accept(efficiencyLine(FuelGrade.VALUES[i], type.eff[i]));
    }

    public static Component efficiencyHeader() {
        return Component.translatable("item.hbm.piston_set.eff").withStyle(ChatFormatting.YELLOW);
    }

    public static Component efficiencyLine(FuelGrade grade, double efficiency) {
        return Component.literal("-")
                .append(Component.translatable(grade.getGrade()))
                .append(": ")
                .withStyle(ChatFormatting.YELLOW)
                .append(
                        Component.literal((int) (efficiency * 100) + "%")
                                .withStyle(ChatFormatting.RED));
    }

    public enum EnumPistonType {
        STEEL(1.00, 0.75, 0.25, 0.00, 0.00),
        DURA(0.50, 1.00, 0.90, 0.50, 0.00),
        DESH(0.00, 0.50, 1.00, 0.75, 0.00),
        STARMETAL(0.50, 0.75, 1.00, 0.90, 0.50);

        public static final EnumPistonType[] VALUES = values();
        public final double[] eff;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumPistonType(double... eff) {
            this.eff = new double[Math.min(FuelGrade.VALUES.length, eff.length)];
            System.arraycopy(eff, 0, this.eff, 0, this.eff.length);
        }
    }
}
