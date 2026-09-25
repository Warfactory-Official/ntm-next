// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.util.function.Function.FunctionLogarithmic;
import com.hbm.util.function.Function.FunctionSqrt;
import com.hbm.util.function.Function;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemPWRFuel extends Item {

    public final EnumPWRFuel fuel;

    public ItemPWRFuel(Properties props, EnumPWRFuel fuel) {
        super(props);
        this.fuel = fuel;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.shared.heatPerFlux", fuel.heatEmission));
        adder.accept(
                Component.translatable(
                        "desc.shared.reactionFunction", fuel.function.getLabelForFuel()));
        adder.accept(
                Component.translatable("desc.shared.fuelType", fuel.function.getDangerFromFuel()));
    }

    public enum EnumPWRFuel {
        MEU("meu", 5.0D, new FunctionLogarithmic(20 * 30).withDiv(2_500)),
        HEU233("heu233", 7.5D, new FunctionSqrt(25)),
        HEU235("heu235", 7.5D, new FunctionSqrt(22.5)),
        MEN("men", 7.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
        HEN237("hen237", 7.5D, new FunctionSqrt(27.5)),
        MOX("mox", 7.5D, new FunctionLogarithmic(20 * 30).withDiv(2_500)),
        MEP("mep", 7.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
        HEP239("hep239", 10.0D, new FunctionSqrt(22.5)),
        HEP241("hep241", 10.0D, new FunctionSqrt(25)),
        MEA("mea", 7.5D, new FunctionLogarithmic(25 * 30).withDiv(2_500)),
        HEA242("hea242", 10.0D, new FunctionSqrt(25)),
        HES326("hes326", 12.5D, new FunctionSqrt(27.5)),
        HES327("hes327", 12.5D, new FunctionSqrt(30)),
        BFB_AM_MIX("bfb_am_mix", 2.5D, new FunctionSqrt(15), 250_000_000),
        BFB_PU241("bfb_pu241", 2.5D, new FunctionSqrt(15), 250_000_000);

        public static final EnumPWRFuel[] VALUES = values();

        public final String id;
        public final double yield;
        public final double heatEmission;
        public final Function function;

        EnumPWRFuel(String id, double heatEmission, Function function, double yield) {
            this.id = id;
            this.heatEmission = heatEmission;
            this.function = function;
            this.yield = yield;
        }

        EnumPWRFuel(String id, double heatEmission, Function function) {
            this(id, heatEmission, function, 1_000_000_000);
        }
    }
}
