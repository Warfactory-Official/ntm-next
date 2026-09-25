// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CustomDamageHandlerAmat implements ICustomDamageHandler {

    protected float radiation;

    public CustomDamageHandlerAmat(float radiation) {
        this.radiation = radiation;
    }

    @Override
    public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(
                    living,
                    HazardType.RADIATION,
                    ContaminationType.CREATIVE,
                    radiation * (1D - distanceScaled) * explosion.size);
        }
    }
}
