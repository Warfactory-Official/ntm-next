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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class FluidTraitText {

    private FluidTraitText() {}

    static Component label(String key, ChatFormatting color) {
        return Component.empty()
                .append(Component.literal("[").withStyle(color))
                .append(Component.translatable(key).withStyle(color))
                .append(Component.literal("]").withStyle(color));
    }

    static Component capacity(int heat, int amount) {
        return Component.empty()
                .append(
                        Component.translatable("hbmfluid.trait.thermalCapacity")
                                .withStyle(ChatFormatting.RED))
                .append(Component.literal(": " + heat + " ").withStyle(ChatFormatting.RED))
                .append(
                        Component.translatable("hbmfluid.trait.perTU")
                                .withStyle(ChatFormatting.RED))
                .append(Component.literal(" " + amount + "mB").withStyle(ChatFormatting.RED));
    }

    static Component efficiency(String modeKey, double efficiency) {
        return Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.YELLOW))
                .append(Component.translatable(modeKey).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("] ").withStyle(ChatFormatting.YELLOW))
                .append(
                        Component.translatable("hbmfluid.trait.efficiency")
                                .withStyle(ChatFormatting.AQUA))
                .append(
                        Component.literal(": " + (int) (efficiency * 100) + "%")
                                .withStyle(ChatFormatting.AQUA));
    }

    static Component provides(String amount, ChatFormatting color) {
        return Component.empty()
                .append(Component.translatable("hbmfluid.trait.provides").withStyle(color))
                .append(Component.literal(" ").withStyle(color))
                .append(Component.literal(amount).withStyle(ChatFormatting.RED))
                .append(Component.translatable("hbmfluid.trait.perBucket").withStyle(color));
    }
}
