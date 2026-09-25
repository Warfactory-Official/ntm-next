// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.block;

import com.hbm.entity.item.EntityTntNtm;
import net.minecraft.world.level.Level;

public interface IFuckingExplode {

    void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity);
}
