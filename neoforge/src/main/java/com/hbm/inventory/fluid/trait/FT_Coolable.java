// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
        Double eff = efficiency.get(type);
        return eff != null ? eff : 0.0D;
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(
                Component.empty()
                        .append(
                                Component.translatable("hbmfluid.trait.thermalCapacity")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.literal(": " + heatEnergy + " ")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.translatable("hbmfluid.trait.perTU")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.literal(" " + amountReq + "mB")
                                        .withStyle(ChatFormatting.RED)));
        for (CoolingType type : CoolingType.values()) {
            double eff = getEfficiency(type);
            if (eff > 0) {
                info.add(
                        Component.empty()
                                .append(Component.literal("[").withStyle(ChatFormatting.YELLOW))
                                .append(
                                        type.getLocalizedName()
                                                .copy()
                                                .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal("] ").withStyle(ChatFormatting.YELLOW))
                                .append(
                                        Component.translatable("hbmfluid.trait.efficiency")
                                                .withStyle(ChatFormatting.AQUA))
                                .append(
                                        Component.literal(": " + ((int) (eff * 100.0D)) + "%")
                                                .withStyle(ChatFormatting.AQUA)));
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

        public Component getLocalizedName() {
            return Component.translatable("hbmfluid.trait." + keySuffix);
        }
    }
}
