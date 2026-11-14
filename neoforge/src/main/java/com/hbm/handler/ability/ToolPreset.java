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

package com.hbm.handler.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ToolPreset(
        ToolAreaAbility area, int areaLevel, ToolHarvestAbility harvest, int harvestLevel) {

    public static final ToolPreset NONE =
            new ToolPreset(ToolAreaAbility.NONE, 0, ToolHarvestAbility.NONE, 0);

    public static final Codec<ToolPreset> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ToolAreaAbility.CODEC
                                                    .optionalFieldOf("area", ToolAreaAbility.NONE)
                                                    .forGetter(ToolPreset::area),
                                            Codec.INT
                                                    .optionalFieldOf("areaLevel", 0)
                                                    .forGetter(ToolPreset::areaLevel),
                                            ToolHarvestAbility.CODEC
                                                    .optionalFieldOf(
                                                            "harvest", ToolHarvestAbility.NONE)
                                                    .forGetter(ToolPreset::harvest),
                                            Codec.INT
                                                    .optionalFieldOf("harvestLevel", 0)
                                                    .forGetter(ToolPreset::harvestLevel))
                                    .apply(instance, ToolPreset::new));

    public static final StreamCodec<ByteBuf, ToolPreset> STREAM_CODEC =
            StreamCodec.composite(
                    ToolAreaAbility.STREAM_CODEC,
                    ToolPreset::area,
                    ByteBufCodecs.VAR_INT,
                    ToolPreset::areaLevel,
                    ToolHarvestAbility.STREAM_CODEC,
                    ToolPreset::harvest,
                    ByteBufCodecs.VAR_INT,
                    ToolPreset::harvestLevel,
                    ToolPreset::new);

    public static ToolPreset ofArea(ToolAreaAbility area, int level) {
        return new ToolPreset(area, level, ToolHarvestAbility.NONE, 0);
    }

    public static ToolPreset ofHarvest(ToolHarvestAbility harvest, int level) {
        return new ToolPreset(ToolAreaAbility.NONE, 0, harvest, level);
    }

    public boolean isNone() {
        return area == ToolAreaAbility.NONE && harvest == ToolHarvestAbility.NONE;
    }

    public ToolPreset restrictTo(AvailableAbilities available) {
        int areaFit = fit(available, area, areaLevel);
        ToolAreaAbility fittedArea = areaFit < 0 ? ToolAreaAbility.NONE : area;
        int fittedAreaLevel = Math.max(areaFit, 0);

        boolean harvestAllowed = fittedArea.permitsHarvest();
        ToolHarvestAbility wanted = harvestAllowed ? harvest : ToolHarvestAbility.NONE;
        int harvestFit = fit(available, wanted, harvestAllowed ? harvestLevel : 0);
        return new ToolPreset(
                fittedArea,
                fittedAreaLevel,
                harvestFit < 0 ? ToolHarvestAbility.NONE : wanted,
                Math.max(harvestFit, 0));
    }

    private static int fit(AvailableAbilities available, BaseAbility ability, int level) {
        int max = available.maxLevel(ability);
        return max == -1 ? -1 : Math.clamp(level, 0, max);
    }

    public Component message() {
        if (isNone())
            return Component.translatable("chat.toolAbility.deactivated")
                    .withStyle(ChatFormatting.GOLD);

        MutableComponent enabled = Component.empty();
        if (area != ToolAreaAbility.NONE) enabled.append(name(area, areaLevel));
        if (harvest != ToolHarvestAbility.NONE) {
            if (!enabled.getSiblings().isEmpty()) enabled.append(" + ");
            enabled.append(name(harvest, harvestLevel));
        }
        return Component.translatable("chat.toolAbility.enabled", enabled)
                .withStyle(ChatFormatting.YELLOW);
    }

    public static MutableComponent name(BaseAbility ability, int level) {
        return Component.translatable(ability.translationKey())
                .append(Component.literal(ability.extension(level)));
    }
}
