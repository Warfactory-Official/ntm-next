// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageClass;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class XFactoryWarhead {

    public static BulletConfig warhead_turbine;

    public static final float TURBINE_BLADE_DAMAGE = 125F;

    private XFactoryWarhead() {}

    public static void init() {

        warhead_turbine =
                new BulletConfig()
                        .setVel(1F)
                        .setGrav(0D)
                        .setLife(200)
                        .setSpread(0F)
                        .setRicochetCount(0)
                        .setupDamageClass(DamageClass.PHYSICAL)
                        .setOnImpact(
                                (blade, mop) -> {
                                    if (!(mop instanceof BlockHitResult hit)) return;

                                    BlockState state =
                                            blade.level().getBlockState(hit.getBlockPos());
                                    if (state.getDestroySpeed(blade.level(), hit.getBlockPos())
                                            <= 120F) {
                                        blade.level().destroyBlock(hit.getBlockPos(), false);
                                    }
                                });
    }
}
