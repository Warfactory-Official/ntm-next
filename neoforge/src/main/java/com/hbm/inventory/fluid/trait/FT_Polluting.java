// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class FT_Polluting extends FluidTrait {

    private final EnumMap<PollutionType, Float> releaseMap = new EnumMap<>(PollutionType.class);
    private final EnumMap<PollutionType, Float> burnMap = new EnumMap<>(PollutionType.class);

    public FT_Polluting release(PollutionType type, float amount) {
        releaseMap.put(type, amount);
        return this;
    }

    public FT_Polluting burn(PollutionType type, float amount) {
        burnMap.put(type, amount);
        return this;
    }

    public Map<PollutionType, Float> getReleaseMap() {
        return releaseMap;
    }

    public Map<PollutionType, Float> getBurnMap() {
        return burnMap;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(
                Component.empty()
                        .append(Component.literal("[").withStyle(ChatFormatting.GOLD))
                        .append(
                                Component.translatable("hbmfluid.trait.polluting")
                                        .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("]").withStyle(ChatFormatting.GOLD)));
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        if (!releaseMap.isEmpty()) {
            info.add(
                    Component.empty()
                            .append(
                                    Component.translatable("hbmfluid.trait.spilled")
                                            .withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(":").withStyle(ChatFormatting.GREEN)));
            for (Map.Entry<PollutionType, Float> entry : releaseMap.entrySet()) {
                info.add(
                        Component.empty()
                                .append(
                                        Component.literal(" - " + entry.getValue() + " ")
                                                .withStyle(ChatFormatting.GREEN))
                                .append(
                                        Component.translatable(
                                                        "pollution.trait."
                                                                + entry.getKey()
                                                                        .name()
                                                                        .toLowerCase(Locale.US))
                                                .withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" ").withStyle(ChatFormatting.GREEN))
                                .append(
                                        Component.translatable("hbmfluid.trait.perMB")
                                                .withStyle(ChatFormatting.GREEN)));
            }
        }
        if (!burnMap.isEmpty()) {
            info.add(
                    Component.empty()
                            .append(
                                    Component.translatable("hbmfluid.trait.burned")
                                            .withStyle(ChatFormatting.RED))
                            .append(Component.literal(":").withStyle(ChatFormatting.RED)));
            for (Map.Entry<PollutionType, Float> entry : burnMap.entrySet()) {
                info.add(
                        Component.empty()
                                .append(
                                        Component.literal(" - " + entry.getValue() + " ")
                                                .withStyle(ChatFormatting.RED))
                                .append(
                                        Component.translatable(
                                                        "pollution.trait."
                                                                + entry.getKey()
                                                                        .name()
                                                                        .toLowerCase(Locale.US))
                                                .withStyle(ChatFormatting.RED))
                                .append(Component.literal(" ").withStyle(ChatFormatting.RED))
                                .append(
                                        Component.translatable("hbmfluid.trait.perMB")
                                                .withStyle(ChatFormatting.RED)));
            }
        }
    }

    @Override
    public void onFluidRelease(
            Level level,
            BlockPos pos,
            FluidTankNTM tank,
            int overflowAmount,
            FluidReleaseType type) {
        if (type == FluidReleaseType.VOID) return;
        Map<PollutionType, Float> map = type == FluidReleaseType.SPILL ? releaseMap : burnMap;
        for (Map.Entry<PollutionType, Float> entry : map.entrySet()) {
            PollutionHandler.incrementPollution(
                    level, pos, entry.getKey(), entry.getValue() * overflowAmount);
        }
    }
}
