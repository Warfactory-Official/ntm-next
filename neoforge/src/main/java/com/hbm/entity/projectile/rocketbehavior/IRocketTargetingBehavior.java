// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import net.minecraft.world.entity.Entity;

public interface IRocketTargetingBehavior {

    void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target);
}
