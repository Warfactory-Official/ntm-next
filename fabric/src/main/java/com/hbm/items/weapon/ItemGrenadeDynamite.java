// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ItemGrenadeDynamite extends ItemGenericGrenade {

    public ItemGrenadeDynamite(Properties properties, int fuse) {
        super(properties, fuse);
    }

    @Override
    public void explode(
            Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
        ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, 5, thrower);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 15));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }
}
