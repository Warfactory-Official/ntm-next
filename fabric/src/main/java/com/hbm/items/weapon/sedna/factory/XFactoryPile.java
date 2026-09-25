// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.particle.helper.FlameCreator;
import net.minecraft.world.level.Level;

public final class XFactoryPile {
    public static BulletConfig debris;

    private XFactoryPile() {}

    public static void init() {
        debris =
                new BulletConfig()
                        .setLife(200)
                        .setVel(1F)
                        .setGrav(0.1D)
                        .setOnUpdate(
                                bullet -> {
                                    if (bullet.level().isClientSide()
                                            && bullet.level()
                                                            .getNearestPlayer(
                                                                    bullet.getX(),
                                                                    bullet.getY(),
                                                                    bullet.getZ(),
                                                                    100D,
                                                                    false)
                                                    != null)
                                        FlameCreator.composeEffectClient(
                                                bullet.level(),
                                                bullet.getX(),
                                                bullet.getY() - 0.125D,
                                                bullet.getZ(),
                                                FlameCreator.META_FIRE);
                                })
                        .setOnImpact(
                                (bullet, hit) -> {
                                    bullet.level()
                                            .explode(
                                                    bullet,
                                                    bullet.getX(),
                                                    bullet.getY(),
                                                    bullet.getZ(),
                                                    5F,
                                                    true,
                                                    Level.ExplosionInteraction.NONE);
                                    bullet.discard();
                                });
    }
}
