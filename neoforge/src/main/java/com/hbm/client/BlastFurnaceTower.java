// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.tileentity.machine.BlockEntityMachineBlastFurnace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class BlastFurnaceTower {

    private BlastFurnaceTower() {}

    public static void tick(BlockEntityMachineBlastFurnace be) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return;

        BlockPos pos = be.getBlockPos();
        double x = pos.getX() + 0.5, y = pos.getY() + 7, z = pos.getZ() + 0.5;
        if (me.getEyePosition().distanceToSqr(x, y, z) >= 100 * 100) return;

        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(10F)
                        .setBaseScale(0.25F)
                        .setMaxScale(2.5F)
                        .setLife(100 + be.getLevel().getRandom().nextInt(20))
                        .setColor(0x202020)
                        .build();

        be.getLevel().addParticle(opts, true, false, x, y, z, 0, 0, 0);
    }
}
