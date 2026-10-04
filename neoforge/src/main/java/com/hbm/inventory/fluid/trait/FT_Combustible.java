// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_Combustible extends FluidTrait {

    private FuelGrade fuelGrade;
    private long combustionEnergy;

    public FT_Combustible() {}

    public FT_Combustible(FuelGrade grade, long energy) {
        fuelGrade = grade;
        combustionEnergy = energy;
    }

    public long getCombustionEnergy() {
        return combustionEnergy;
    }

    public FuelGrade getGrade() {
        return fuelGrade;
    }

    public FuelGrade gradeOrThrow() {
        if (fuelGrade == null) {
            throw new IllegalStateException(
                    "a combustible fluid was declared with the bare FT_Combustible() "
                            + "constructor, so it has no fuel grade to serialise; pass a FuelGrade");
        }
        return fuelGrade;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(
                Component.empty()
                        .append(Component.literal("[").withStyle(ChatFormatting.GOLD))
                        .append(
                                Component.translatable("hbmfluid.trait.combustible")
                                        .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("]").withStyle(ChatFormatting.GOLD)));
        if (combustionEnergy > 0) {
            info.add(
                    Component.empty()
                            .append(
                                    Component.translatable("hbmfluid.trait.provides")
                                            .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" ").withStyle(ChatFormatting.GOLD))
                            .append(
                                    Component.literal(
                                                    BobMathUtil.getShortNumber(combustionEnergy)
                                                            + "HE ")
                                            .withStyle(ChatFormatting.RED))
                            .append(
                                    Component.translatable("hbmfluid.trait.perBucket")
                                            .withStyle(ChatFormatting.GOLD)));
            info.add(
                    Component.empty()
                            .append(
                                    Component.translatable("hbmfluid.trait.fuelGrade")
                                            .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                            .append(
                                    Component.translatable(fuelGrade.getGrade())
                                            .withStyle(ChatFormatting.RED)));
        }
    }

    public enum FuelGrade {
        LOW("hbmfluid.trait.fuel.low"),
        MEDIUM("hbmfluid.trait.fuel.medium"),
        HIGH("hbmfluid.trait.fuel.high"),
        AERO("hbmfluid.trait.fuel.aviation"),
        GAS("hbmfluid.trait.fuel.gaseous");

        public static final FuelGrade[] VALUES = values();

        private final String grade;

        FuelGrade(String grade) {
            this.grade = grade;
        }

        public String getGrade() {
            return grade;
        }
    }
}
