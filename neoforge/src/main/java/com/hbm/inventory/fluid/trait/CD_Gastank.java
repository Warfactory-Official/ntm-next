// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

public class CD_Gastank extends FluidTrait {

    public final int bottleColor;
    public final int labelColor;

    public CD_Gastank(int bottleColor, int labelColor) {
        this.bottleColor = bottleColor;
        this.labelColor = labelColor;
    }
}
