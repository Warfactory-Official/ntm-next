// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

public enum HazardClass {
    GAS_LUNG("hazard.gasChlorine"),
    GAS_MONOXIDE("hazard.gasMonoxide"),
    GAS_INERT("hazard.gasInert"),
    PARTICLE_COARSE("hazard.particleCoarse"),
    PARTICLE_FINE("hazard.particleFine"),
    BACTERIA("hazard.bacteria"),
    GAS_BLISTERING("hazard.corrosive"),
    SAND("hazard.sand"),
    LIGHT("hazard.light");

    public final String lang;

    HazardClass(String lang) {
        this.lang = lang;
    }
}
