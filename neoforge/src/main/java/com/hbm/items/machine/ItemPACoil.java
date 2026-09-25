// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemPACoil extends Item {

    public final EnumCoilType type;

    public ItemPACoil(Properties properties, EnumCoilType type) {
        super(properties);
        this.type = type;
    }

    public static @Nullable EnumCoilType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemPACoil coil ? coil.type : null;
    }

    private static String num(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.item.paCoil.quadrupoleOperationalRange",
                        num(type.quadMin),
                        num(type.quadMax)));
        adder.accept(
                Component.translatable(
                        "desc.item.paCoil.dipoleOperationalRange",
                        num(type.diMin),
                        num(type.diMax)));
        adder.accept(
                Component.translatable("desc.item.paCoil.dipoleMinimumSideLength", type.diDistMin));
        adder.accept(
                Component.translatable("desc.item.paCoil.minimumsNotMetResult")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.item.paCoil.maximumsExceededResultIn")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.item.paCoil.particlesWillCrashIn")
                        .withStyle(ChatFormatting.RED));
    }

    public enum EnumCoilType {
        GOLD(0, 2_200, 0, 2_200, 15),
        NIOBIUM(1_500, 8_400, 1_500, 8_400, 21),
        BSCCO(7_500, 15_000, 7_500, 15_000, 27),
        CHLOROPHYTE(14_500, 75_000, 14_500, 75_000, 51);

        public static final EnumCoilType[] VALUES = values();

        public final int quadMin;
        public final int quadMax;
        public final int diMin;
        public final int diMax;
        public final int diDistMin;

        EnumCoilType(int quadMin, int quadMax, int diMin, int diMax, int diDistMin) {
            this.quadMin = quadMin;
            this.quadMax = quadMax;
            this.diMin = diMin;
            this.diMax = diMax;
            this.diDistMin = diDistMin;
        }
    }
}
