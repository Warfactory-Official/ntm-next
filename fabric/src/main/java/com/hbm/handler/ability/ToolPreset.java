// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        ToolAreaAbility newArea = area;
        int newAreaLevel = areaLevel;
        int maxArea = available.maxLevel(area);

        if (maxArea == -1) {
            newArea = ToolAreaAbility.NONE;
            newAreaLevel = 0;
        } else {
            newAreaLevel = Math.clamp(areaLevel, 0, maxArea);
        }

        ToolHarvestAbility newHarvest = harvest;
        int newHarvestLevel = harvestLevel;
        if (!newArea.allowsHarvest(newAreaLevel)) {
            newHarvest = ToolHarvestAbility.NONE;
            newHarvestLevel = 0;
        }

        int maxHarvest = available.maxLevel(newHarvest);
        if (maxHarvest == -1) {
            newHarvest = ToolHarvestAbility.NONE;
            newHarvestLevel = 0;
        } else {
            newHarvestLevel = Math.clamp(newHarvestLevel, 0, maxHarvest);
        }

        return new ToolPreset(newArea, newAreaLevel, newHarvest, newHarvestLevel);
    }

    public Component message() {
        if (isNone()) {
            return Component.translatable("chat.toolAbility.deactivated")
                    .withStyle(ChatFormatting.GOLD);
        }

        MutableComponent abilities = Component.empty();
        if (area != ToolAreaAbility.NONE) abilities.append(name(area, areaLevel));
        if (area != ToolAreaAbility.NONE && harvest != ToolHarvestAbility.NONE) {
            abilities.append(Component.literal(" + "));
        }
        if (harvest != ToolHarvestAbility.NONE) abilities.append(name(harvest, harvestLevel));

        return Component.translatable("chat.toolAbility.enabled", abilities)
                .withStyle(ChatFormatting.YELLOW);
    }

    public static MutableComponent name(BaseAbility ability, int level) {
        return Component.translatable(ability.translationKey())
                .append(Component.literal(ability.extension(level)));
    }
}
