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

import com.hbm.util.NumberFormatter;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record CombustibleTrait(Grade grade, long energyPerBucket) implements FluidTrait {
    public enum Grade {
        LOW("hbmfluid.trait.fuel.low"),
        MEDIUM("hbmfluid.trait.fuel.medium"),
        HIGH("hbmfluid.trait.fuel.high"),
        AERO("hbmfluid.trait.fuel.aviation"),
        GAS("hbmfluid.trait.fuel.gaseous");

        private final String labelKey;

        Grade(String labelKey) {
            this.labelKey = labelKey;
        }

        public String labelKey() {
            return labelKey;
        }
    }

    public CombustibleTrait {
        Objects.requireNonNull(grade, "an engine fuel needs a grade");
    }

    @Override
    public void describe(Consumer<Component> lines) {
        lines.accept(FluidTraitText.label("hbmfluid.trait.combustible", ChatFormatting.GOLD));
        if (energyPerBucket > 0) {
            lines.accept(
                    FluidTraitText.provides(
                            NumberFormatter.formatCompact(energyPerBucket) + "HE ",
                            ChatFormatting.GOLD));
            lines.accept(
                    Component.empty()
                            .append(
                                    Component.translatable("hbmfluid.trait.fuelGrade")
                                            .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                            .append(
                                    Component.translatable(grade.labelKey)
                                            .withStyle(ChatFormatting.RED)));
        }
    }
}
