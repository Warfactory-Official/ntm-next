// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.Locale;
import net.minecraft.SharedConstants;

public enum EnumBatteryPack {
    BATTERY_REDSTONE("battery_redstone", 100L, false),
    BATTERY_LEAD("battery_lead", 1_000L, false),
    BATTERY_LITHIUM("battery_lithium", 10_000L, false),
    BATTERY_SODIUM("battery_sodium", 50_000L, false),
    BATTERY_SCHRABIDIUM("battery_schrabidium", 250_000L, false),
    BATTERY_QUANTUM("battery_quantum", 1_000_000L, 60 * SharedConstants.TICKS_PER_MINUTE),

    CAPACITOR_COPPER("capacitor_copper", 1_000L, true),
    CAPACITOR_GOLD("capacitor_gold", 10_000L, true),
    CAPACITOR_NIOBIUM("capacitor_niobium", 100_000L, true),
    CAPACITOR_TANTALUM("capacitor_tantalum", 500_000L, true),
    CAPACITOR_BISMUTH("capacitor_bismuth", 2_500_000L, true),
    CAPACITOR_SPARK("capacitor_spark", 10_000_000L, true);

    public static final EnumBatteryPack[] VALUES = values();
    public final String tex;
    public final long capacity;
    public final long chargeRate;
    public final long dischargeRate;
    public final String id = name().toLowerCase(Locale.ROOT);

    EnumBatteryPack(String tex, long dischargeRate, boolean capacitor) {
        this(
                tex,
                capacitor
                        ? (dischargeRate * SharedConstants.TICKS_PER_SECOND * 30)
                        : (dischargeRate * SharedConstants.TICKS_PER_MINUTE * 15),
                capacitor ? dischargeRate : dischargeRate * 10,
                dischargeRate);
    }

    EnumBatteryPack(String tex, long dischargeRate, long duration) {
        this(tex, dischargeRate * duration, dischargeRate * 10, dischargeRate);
    }

    EnumBatteryPack(String tex, long capacity, long chargeRate, long dischargeRate) {
        this.tex = tex;
        this.capacity = capacity;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
    }

    public boolean isCapacitor() {
        return this.ordinal() > BATTERY_QUANTUM.ordinal();
    }
}
