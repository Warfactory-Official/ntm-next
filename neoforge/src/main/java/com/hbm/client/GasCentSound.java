// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineGasCent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class GasCentSound {

    private GasCentSound() {}

    public static void tick(BlockEntityMachineGasCent be) {
        be.audioDuration += be.isProgressing ? 2 : -3;
        be.audioDuration = Mth.clamp(be.audioDuration, 0, 60);

        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.audioDuration > 10
                        && me != null
                        && me.getEyePosition()
                                        .distanceToSqr(
                                                pos.getX() + 0.5,
                                                pos.getY() + 0.5,
                                                pos.getZ() + 0.5)
                                < 25 * 25,
                1F,
                (be.audioDuration - 10) / 100F + 0.5F);
    }
}
