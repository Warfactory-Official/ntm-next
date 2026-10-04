// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
        Double eff = efficiency.get(type);
        return eff != null ? eff : 0.0D;
    }

    public HeatingStep getFirstStep() {
        return steps.get(0);
    }

    public List<HeatingStep> steps() {
        return Collections.unmodifiableList(steps);
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(
                Component.empty()
                        .append(
                                Component.translatable("hbmfluid.trait.thermalCapacity")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.literal(": " + getFirstStep().heatReq + " ")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.translatable("hbmfluid.trait.perTU")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.literal(" " + getFirstStep().amountReq + "mB")
                                        .withStyle(ChatFormatting.RED)));
        for (HeatingType type : HeatingType.values()) {
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
                                        Component.literal(": " + ((int) (eff * 100D)) + "%")
                                                .withStyle(ChatFormatting.AQUA)));
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

        public Component getLocalizedName() {
            return Component.translatable("hbmfluid.trait." + keySuffix);
        }
    }

    public static final class HeatingStep {
        public final int amountReq;
        public final int heatReq;
        public final int amountProduced;
        private final Supplier<Fluid> typeProduced;

        public HeatingStep(int req, int heat, Supplier<Fluid> type, int prod) {
            amountReq = req;
            heatReq = heat;
            typeProduced = type;
            amountProduced = prod;
        }

        public Fluid typeProduced() {
            return typeProduced.get();
        }
    }
}
