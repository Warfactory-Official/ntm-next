// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.Entity;

public interface ICustomDamageHandler {

    void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled);
}
