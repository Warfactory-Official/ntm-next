// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.config.BalanceConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.data.MachineData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class RBMKConfig {

    private static int columnHeight = 4;

    private RBMKConfig() {}

    public static void publishColumnHeight() {
        int height = MachineData.RBMK_COLUMN_HEIGHT.get();
        if (height == columnHeight) return;
        columnHeight = height;
        RBMKBase.rebakeColumns();
    }

    public static double getPassiveCooling(Level world) {
        return MachineData.RBMK_PASSIVE_COOLING.get();
    }

    public static double getPassiveCoolingInner(Level world) {
        return MachineData.RBMK_PASSIVE_COOLING_INNER.get();
    }

    public static double getColumnHeatFlow(Level world) {
        return MachineData.RBMK_COLUMN_HEAT_FLOW.get();
    }

    public static double getFuelDiffusionMod(Level world) {
        return MachineData.RBMK_FUEL_DIFFUSION_MOD.get();
    }

    public static double getFuelHeatProvision(Level world) {
        return MachineData.RBMK_HEAT_PROVISION.get();
    }

    public static int getColumnHeight(LevelAccessor world) {
        return columnHeight - 1;
    }

    public static int getColumnHeightRuleValue(Level world) {
        return columnHeight;
    }

    public static boolean getPermaScrap(Level world) {
        return MachineData.RBMK_PERMANENT_SCRAP.get();
    }

    public static double getBoilerHeatConsumption(Level world) {
        return MachineData.RBMK_BOILER_HEAT_CONSUMPTION.get();
    }

    public static double getControlSpeed(Level world) {
        return MachineData.RBMK_CONTROL_SPEED.get();
    }

    public static double getReactivityMod(Level world) {
        return MachineData.RBMK_REACTIVITY_MOD.get();
    }

    public static double getOutgasserMod(Level world) {
        return MachineData.RBMK_OUTGASSER_MOD.get();
    }

    public static double getSurgeMod(Level world) {
        return MachineData.RBMK_SURGE_MOD.get();
    }

    public static int getFluxRange(Level world) {
        return MachineData.RBMK_FLUX_RANGE.get();
    }

    public static boolean getReasimBoilers(Level world) {
        return MachineData.RBMK_REASIM_BOILERS.get() || BalanceConfig.enable528ReasimBoilers;
    }

    public static double getReaSimBoilerSpeed(Level world) {
        return MachineData.RBMK_REASIM_BOILER_SPEED.get();
    }

    public static boolean getMeltdownsDisabled(Level world) {
        return MachineData.RBMK_DISABLE_MELTDOWNS.get();
    }

    public static boolean getOverpressure(Level world) {
        return MachineData.RBMK_ENABLE_MELTDOWN_OVERPRESSURE.get();
    }

    public static double getModeratorEfficiency(Level world) {
        return MachineData.RBMK_MODERATOR_EFFICIENCY.get();
    }

    public static double getAbsorberEfficiency(Level world) {
        return MachineData.RBMK_ABSORBER_EFFICIENCY.get();
    }

    public static double getReflectorEfficiency(Level world) {
        return MachineData.RBMK_REFLECTOR_EFFICIENCY.get();
    }

    public static double getAbsorberHeatConversion(Level world) {
        return MachineData.RBMK_ABSORBER_HEAT_CONVERSION.get();
    }

    public static boolean getDepletion(Level world) {
        return !MachineData.RBMK_DISABLE_DEPLETION.get();
    }

    public static boolean getXenon(Level world) {
        return !MachineData.RBMK_DISABLE_XENON.get();
    }
}
