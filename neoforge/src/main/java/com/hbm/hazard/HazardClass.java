// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

public enum HazardClass implements net.minecraft.util.StringRepresentable {
    GAS_LUNG("hazard.gasChlorine"),
    GAS_MONOXIDE("hazard.gasMonoxide"),
    GAS_INERT("hazard.gasInert"),
    PARTICLE_COARSE("hazard.particleCoarse"),
    PARTICLE_FINE("hazard.particleFine"),
    BACTERIA("hazard.bacteria"),
    GAS_BLISTERING("hazard.corrosive"),
    SAND("hazard.sand"),
    LIGHT("hazard.light");

    public static final com.mojang.serialization.Codec<HazardClass> CODEC =
            net.minecraft.util.StringRepresentable.fromEnum(HazardClass::values);

    public int bit() {
        return 1 << ordinal();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public final String lang;

    HazardClass(String lang) {
        this.lang = lang;
    }
}
