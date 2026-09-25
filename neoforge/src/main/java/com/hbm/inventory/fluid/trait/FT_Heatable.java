// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.I18nUtil;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.material.Fluid;

public class FT_Heatable extends FluidTrait {

    private final List<HeatingStep> steps = new ArrayList<>();
    private final Map<HeatingType, Double> efficiency = new EnumMap<>(HeatingType.class);

    public FT_Heatable addStep(int heat, int req, Supplier<Fluid> type, int prod) {
        steps.add(new HeatingStep(req, heat, type, prod));
        return this;
    }

    public FT_Heatable setEff(HeatingType type, double eff) {
        efficiency.put(type, eff);
        return this;
    }

    public double getEfficiency(HeatingType type) {
        Double eff = this.efficiency.get(type);
        return eff != null ? eff : 0.0D;
    }

    public HeatingStep getFirstStep() {
        return this.steps.get(0);
    }

    public List<HeatingStep> steps() {
        return Collections.unmodifiableList(this.steps);
    }

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(
                ChatFormatting.RED
                        + I18nUtil.resolveKey("hbmfluid.trait.thermalCapacity")
                        + ": "
                        + getFirstStep().heatReq
                        + " "
                        + I18nUtil.resolveKey("hbmfluid.trait.perTU")
                        + " "
                        + getFirstStep().amountReq
                        + "mB");
        for (HeatingType type : HeatingType.values()) {
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

    public enum HeatingType {
        BOILER("boilable"),
        HEATEXCHANGER("heatable"),
        PWR("coolantPWR"),
        ICF("coolantICF"),
        PA("coolantPA");

        private final String keySuffix;

        HeatingType(String keySuffix) {
            this.keySuffix = keySuffix;
        }

        public String getLocalizedName() {
            return I18nUtil.resolveKey("hbmfluid.trait." + keySuffix);
        }
    }

    public static final class HeatingStep {
        public final int amountReq;
        public final int heatReq;
        public final int amountProduced;
        private final Supplier<Fluid> typeProduced;

        public HeatingStep(int req, int heat, Supplier<Fluid> type, int prod) {
            this.amountReq = req;
            this.heatReq = heat;
            this.typeProduced = type;
            this.amountProduced = prod;
        }

        public Fluid typeProduced() {
            return typeProduced.get();
        }
    }
}
