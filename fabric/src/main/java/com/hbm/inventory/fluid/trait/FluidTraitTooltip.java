/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.fluid.trait;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.fluid.FluidBuilder;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.util.Blink;
import com.hbm.util.TooltipStyle;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidTraitTooltip {

    private FluidTraitTooltip() {}

    public static void describe(@Nullable Fluid fluid, Consumer<Component> lines) {
        if (!NTMFluidProperties.isOwn(fluid)) return;
        var property = NTMFluidProperties.get(fluid);
        var temperature = property.temperature();
        if (temperature != FluidBuilder.ROOM_TEMPERATURE && temperature != 0) {
            lines.accept(
                    Component.literal(temperature + "\u00B0C")
                            .withStyle(temperature < 0 ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        var hidden = new ArrayList<Component>();
        Consumer<Component> details = ModifierKeys.detailsHeld() ? lines : hidden::add;
        for (var kind : FluidTraitRegistry.kinds()) {
            var trait = property.getTrait(kind);
            if (trait == null) continue;
            trait.describe(lines);
            trait.describeDetails(details);
        }
        if (!hidden.isEmpty()) lines.accept(TooltipStyle.holdDetails());
    }

    public static void pressure(int pressure, Consumer<Component> lines) {
        lines.accept(Component.literal(pressure + "PU").withStyle(ChatFormatting.RED));
        lines.accept(
                Component.translatable("desc.shared.pressurizedUseCompressor")
                        .withStyle(Blink.lit() ? ChatFormatting.RED : ChatFormatting.DARK_RED));
    }
}
