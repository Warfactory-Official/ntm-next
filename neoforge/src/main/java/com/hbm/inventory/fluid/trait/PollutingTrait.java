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

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public record PollutingTrait(Map<PollutionType, Float> spilled, Map<PollutionType, Float> burned)
        implements FluidTrait {

    public PollutingTrait {
        spilled = ordered(spilled);
        burned = ordered(burned);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<PollutionType, Float> of(FluidReleaseType kind) {
        return switch (kind) {
            case DISCARD -> Map.of();
            case BURN -> burned;
            case SPILL -> spilled;
        };
    }

    @Override
    public void describe(Consumer<Component> lines) {
        lines.accept(FluidTraitText.label("hbmfluid.trait.polluting", ChatFormatting.GOLD));
    }

    @Override
    public void describeDetails(Consumer<Component> lines) {
        table(lines, spilled, "hbmfluid.trait.spilled", ChatFormatting.GREEN);
        table(lines, burned, "hbmfluid.trait.burned", ChatFormatting.RED);
    }

    @Override
    public void onRelease(
            Level level, BlockPos pos, FluidTankNTM tank, int amount, FluidReleaseType kind) {
        for (var entry : of(kind).entrySet()) {
            PollutionHandler.add(level, pos, entry.getKey(), entry.getValue() * amount);
        }
    }

    private static void table(
            Consumer<Component> lines,
            Map<PollutionType, Float> table,
            String headerKey,
            ChatFormatting color) {
        if (table.isEmpty()) return;
        lines.accept(
                Component.empty()
                        .append(Component.translatable(headerKey).withStyle(color))
                        .append(Component.literal(":").withStyle(color)));
        for (var entry : table.entrySet()) {
            lines.accept(
                    Component.empty()
                            .append(
                                    Component.literal(" - " + entry.getValue() + " ")
                                            .withStyle(color))
                            .append(
                                    Component.translatable(entry.getKey().traitKey())
                                            .withStyle(color))
                            .append(Component.literal(" ").withStyle(color))
                            .append(
                                    Component.translatable("hbmfluid.trait.perMB")
                                            .withStyle(color)));
        }
    }

    private static Map<PollutionType, Float> ordered(Map<PollutionType, Float> table) {
        var out = new EnumMap<PollutionType, Float>(PollutionType.class);
        out.putAll(table);
        return Collections.unmodifiableMap(out);
    }

    public static final class Builder {

        private final Map<PollutionType, Float> spilled = new EnumMap<>(PollutionType.class);
        private final Map<PollutionType, Float> burned = new EnumMap<>(PollutionType.class);

        private Builder() {}

        public Builder spill(PollutionType type, float perMb) {
            spilled.put(type, perMb);
            return this;
        }

        public Builder burn(PollutionType type, float perMb) {
            burned.put(type, perMb);
            return this;
        }

        public PollutingTrait build() {
            return new PollutingTrait(spilled, burned);
        }
    }
}
