/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.machine;

import java.util.Locale;
import net.minecraft.SharedConstants;

public enum BatteryPackType {
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

    public static final BatteryPackType[] VALUES = values();
    public final String tex;
    public final long capacity;
    public final long chargeRate;
    public final long dischargeRate;
    public final String id = name().toLowerCase(Locale.ROOT);
    private final boolean capacitor;

    BatteryPackType(String tex, long dischargeRate, boolean capacitor) {
        this(
                tex,
                capacitor
                        ? (dischargeRate * SharedConstants.TICKS_PER_SECOND * 30)
                        : (dischargeRate * SharedConstants.TICKS_PER_MINUTE * 15),
                capacitor ? dischargeRate : dischargeRate * 10,
                dischargeRate,
                capacitor);
    }

    BatteryPackType(String tex, long dischargeRate, long duration) {
        this(tex, dischargeRate * duration, dischargeRate * 10, dischargeRate, false);
    }

    BatteryPackType(
            String tex, long capacity, long chargeRate, long dischargeRate, boolean capacitor) {
        this.tex = tex;
        this.capacity = capacity;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
        this.capacitor = capacitor;
    }

    public boolean isCapacitor() {
        return capacitor;
    }
}
