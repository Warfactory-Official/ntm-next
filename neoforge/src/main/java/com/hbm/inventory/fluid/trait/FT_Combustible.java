// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;

public class FT_Combustible extends FluidTrait {

    private FuelGrade fuelGrade;
    private long combustionEnergy;

    public FT_Combustible() {}

    public FT_Combustible(FuelGrade grade, long energy) {
        this.fuelGrade = grade;
        this.combustionEnergy = energy;
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
    public void addInfo(List<String> info) {
        info.add(
                ChatFormatting.GOLD
                        + "["
                        + I18nUtil.resolveKey("hbmfluid.trait.combustible")
                        + "]");
        if (combustionEnergy > 0) {
            info.add(
                    ChatFormatting.GOLD
                            + I18nUtil.resolveKey("hbmfluid.trait.provides")
                            + " "
                            + ChatFormatting.RED
                            + BobMathUtil.getShortNumber(combustionEnergy)
                            + "HE "
                            + ChatFormatting.GOLD
                            + I18nUtil.resolveKey("hbmfluid.trait.perBucket"));
            info.add(
                    ChatFormatting.GOLD
                            + I18nUtil.resolveKey("hbmfluid.trait.fuelGrade")
                            + ": "
                            + ChatFormatting.RED
                            + I18nUtil.resolveKey(fuelGrade.getGrade()));
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
