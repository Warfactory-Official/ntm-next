// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineAutosaw;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class AutosawSound {

    private AutosawSound() {}

    public static void tick(BlockEntityMachineAutosaw be) {
        Player player = Minecraft.getInstance().player;
        boolean near =
                player != null
                        && player.getEyePosition()
                                        .distanceToSqr(
                                                be.getBlockPos().getX() + 0.5,
                                                be.getBlockPos().getY() + 0.5,
                                                be.getBlockPos().getZ() + 0.5)
                                < BlockEntityMachineAutosaw.AUDIO_RANGE_SQ;

        be.audioLoop(be.isOn && !be.isSuspended && near, 1F);
    }
}
