// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachinePUREX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class PurexSound {

    private PurexSound() {}

    public static void tick(BlockEntityMachinePUREX machine) {
        LocalPlayer player = Minecraft.getInstance().player;
        BlockPos pos = machine.getBlockPos();
        machine.audioLoop(
                machine.isProgressing
                        && player != null
                        && player.position().distanceToSqr(pos.getX(), pos.getY(), pos.getZ())
                                < 25 * 25,
                1F,
                0.75F);
    }
}
