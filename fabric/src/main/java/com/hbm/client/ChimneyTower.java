// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.tileentity.machine.BlockEntityChimneyBase;
import net.minecraft.core.BlockPos;

public final class ChimneyTower {

    private ChimneyTower() {}

    public static void tick(BlockEntityChimneyBase be) {
        BlockPos pos = be.getBlockPos();
        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(10F)
                        .setBaseScale(be.plumeBaseScale())
                        .setMaxScale(3F)
                        .setLife(250 + be.getLevel().getRandom().nextInt(50))
                        .setColor(0x404040)
                        .build();

        be.getLevel()
                .addParticle(
                        opts,
                        true,
                        false,
                        pos.getX() + 0.5,
                        pos.getY() + be.plumeHeight(),
                        pos.getZ() + 0.5,
                        0,
                        0,
                        0);
    }
}
