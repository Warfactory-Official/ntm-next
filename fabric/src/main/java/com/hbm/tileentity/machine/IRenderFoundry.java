// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.inventory.material.NTMMaterial;

public interface IRenderFoundry {

    boolean shouldRender();

    double getMoltenLevel();

    NTMMaterial getMat();

    double minX();

    double maxX();

    double minZ();

    double maxZ();

    double moldHeight();

    double outHeight();
}
