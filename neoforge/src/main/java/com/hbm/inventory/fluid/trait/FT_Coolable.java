// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.I18nUtil;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.material.Fluid;

public class FT_Coolable extends FluidTrait {

    public final int amountReq;
    public final int amountProduced;
    public final int heatEnergy;
    private final Supplier<Fluid> coolsTo;
    private final Map<CoolingType, Double> efficiency = new EnumMap<>(CoolingType.class);

    public FT_Coolable(Supplier<Fluid> coolsTo, int amountReq, int amountProduced, int heatEnergy) {
        this.coolsTo = coolsTo;
        this.amountReq = amountReq;
        this.amountProduced = amountProduced;
        this.heatEnergy = heatEnergy;
    }

    public Fluid coolsTo() {
        return coolsTo.get();
    }

    public FT_Coolable setEff(CoolingType type, double eff) {
        efficiency.put(type, eff);
        return this;
    }

    public double getEfficiency(CoolingType type) {
        Double eff = this.efficiency.get(type);
        return eff != null ? eff : 0.0D;
    }

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(
                ChatFormatting.RED
                        + I18nUtil.resolveKey("hbmfluid.trait.thermalCapacity")
                        + ": "
                        + heatEnergy
                        + " "
                        + I18nUtil.resolveKey("hbmfluid.trait.perTU")
                        + " "
                        + amountReq
                        + "mB");
        for (CoolingType type : CoolingType.values()) {
            double eff = getEfficiency(type);
            if (eff > 0) {
                info.add(
                        ChatFormatting.YELLOW
                                + "["
                                + type.getLocalizedName()
                                + "] "
                                + ChatFormatting.AQUA
                                + I18nUtil.resolveKey("hbmfluid.trait.efficiency")
                                + ": "
                                + ((int) (eff * 100D))
                                + "%");
            }
        }
    }

    public enum CoolingType {
        TURBINE("steam"),
        HEATEXCHANGER("coolable");

        private final String keySuffix;

        CoolingType(String keySuffix) {
            this.keySuffix = keySuffix;
        }

        public String getLocalizedName() {
            return I18nUtil.resolveKey("hbmfluid.trait." + keySuffix);
        }
    }
}
