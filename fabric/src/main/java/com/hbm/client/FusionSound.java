// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystronCreative;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionMHDT;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FusionSound {

    private FusionSound() {}

    public static void tick(BlockEntityFusionTorus be) {
        loop(be, be.magnetSpeed / 30F, 50F, 2.5D);
    }

    public static void tick(BlockEntityFusionKlystron be) {
        loop(be, be.fanSpeed / 5F, 30F, 2.5D);
    }

    public static void tick(BlockEntityFusionKlystronCreative be) {
        loop(be, be.fanSpeed / 5F, 30F, 2.5D);
    }

    public static void tick(BlockEntityFusionMHDT be) {
        loop(be, be.rotorSpeed / 15F, 30F, 2.5D);
    }

    private static <T extends BlockEntity & AudioLoop> void loop(
            T be, float speed, float range, double centreY) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                speed > 0F
                        && me != null
                        && me.getEyePosition()
                                        .distanceToSqr(
                                                pos.getX() + 0.5,
                                                pos.getY() + centreY,
                                                pos.getZ() + 0.5)
                                < range * range,
                speed,
                speed);
    }
}
