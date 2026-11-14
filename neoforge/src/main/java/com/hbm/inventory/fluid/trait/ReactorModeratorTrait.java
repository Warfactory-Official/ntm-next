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

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record ReactorModeratorTrait(double multiplier) implements FluidTrait {

    @Override
    public void describe(Consumer<Component> lines) {
        lines.accept(FluidTraitText.label("hbmfluid.trait.pwrFluxMultiplier", ChatFormatting.BLUE));
    }

    @Override
    public void describeDetails(Consumer<Component> lines) {
        var percent = (int) (multiplier * 100 - 100);
        lines.accept(
                Component.empty()
                        .append(
                                Component.translatable("hbmfluid.trait.pwrFluxCore")
                                        .withStyle(ChatFormatting.BLUE))
                        .append(
                                Component.literal(" " + (percent >= 0 ? "+" : "") + percent + "%")
                                        .withStyle(ChatFormatting.BLUE)));
    }
}
