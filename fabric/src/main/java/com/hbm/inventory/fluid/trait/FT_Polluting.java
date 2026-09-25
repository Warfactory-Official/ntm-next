// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.I18nUtil;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
    public void addInfo(List<String> info) {
        info.add(ChatFormatting.GOLD + "[" + I18nUtil.resolveKey("hbmfluid.trait.polluting") + "]");
    }

    @Override
    public void addInfoHidden(List<String> info) {
        if (!releaseMap.isEmpty()) {
            info.add(ChatFormatting.GREEN + I18nUtil.resolveKey("hbmfluid.trait.spilled") + ":");
            for (Map.Entry<PollutionType, Float> entry : releaseMap.entrySet()) {
                info.add(
                        ChatFormatting.GREEN
                                + " - "
                                + entry.getValue()
                                + " "
                                + I18nUtil.resolveKey(
                                        "pollution.trait."
                                                + entry.getKey().name().toLowerCase(Locale.US))
                                + " "
                                + I18nUtil.resolveKey("hbmfluid.trait.perMB"));
            }
        }
        if (!burnMap.isEmpty()) {
            info.add(ChatFormatting.RED + I18nUtil.resolveKey("hbmfluid.trait.burned") + ":");
            for (Map.Entry<PollutionType, Float> entry : burnMap.entrySet()) {
                info.add(
                        ChatFormatting.RED
                                + " - "
                                + entry.getValue()
                                + " "
                                + I18nUtil.resolveKey(
                                        "pollution.trait."
                                                + entry.getKey().name().toLowerCase(Locale.US))
                                + " "
                                + I18nUtil.resolveKey("hbmfluid.trait.perMB"));
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
