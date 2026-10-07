// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import net.minecraft.world.level.Level;

public interface IEntityLookOverlay {

    void buildLookOverlay(Level level, ILookOverlay.LookInfo info);
}
