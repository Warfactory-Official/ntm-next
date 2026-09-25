// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.level.Level;

public interface IExplosionSFX {

    void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size);
}
