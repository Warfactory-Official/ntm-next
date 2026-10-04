// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.fluid.FluidBuilder;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.util.BobMathUtil;
import com.hbm.util.TooltipStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidTraitTooltip {

    private FluidTraitTooltip() {}

    public static void addPressureInfo(int pressure, Consumer<Component> adder) {
        adder.accept(Component.literal(pressure + "PU").withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.shared.pressurizedUseCompressor")
                        .withStyle(
                                BobMathUtil.getBlink()
                                        ? ChatFormatting.RED
                                        : ChatFormatting.DARK_RED));
    }

    public static void addInfo(@Nullable Fluid fluid, Consumer<Component> adder) {
        if (NTMFluidProperties.isOwn(fluid)) addGenericInfo(NTMFluidProperties.get(fluid), adder);
    }

    public static void addGenericInfo(NTMFluidProperty prop, Consumer<Component> adder) {
        List<Component> info = new ArrayList<>();

        int temperature = prop.temperature();
        if (temperature != FluidBuilder.ROOM_TEMPERATURE) {
            if (temperature < 0)
                info.add(Component.literal(temperature + "°C").withStyle(ChatFormatting.BLUE));
            if (temperature > 0)
                info.add(Component.literal(temperature + "°C").withStyle(ChatFormatting.RED));
        }

        boolean shiftHeld = ModifierKeys.detailsHeld();
        List<Component> hidden = new ArrayList<>();
        for (Class<? extends FluidTrait> clazz : FluidTrait.traitList) {
            FluidTrait trait = prop.getTrait(clazz);
            if (trait != null) {
                trait.addInfo(info);
                if (shiftHeld) trait.addInfoHidden(info);
                trait.addInfoHidden(hidden);
            }
        }
        if (!hidden.isEmpty() && !shiftHeld) {
            info.add(TooltipStyle.holdDetails());
        }

        for (Component line : info) adder.accept(line);
    }
}
