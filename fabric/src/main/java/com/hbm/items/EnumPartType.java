// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

public enum EnumPartType {
    PISTON_PNEUMATIC("piston_pneumatic"),
    PISTON_HYDRAULIC("piston_hydraulic"),
    PISTON_ELECTRIC("piston_electric"),
    LDE("low_density_element"),
    HDE("heavy_duty_element"),
    GLASS_POLARIZED("glass_polarized");

    public final String texture;

    EnumPartType(String texture) {
        this.texture = texture;
    }
}
