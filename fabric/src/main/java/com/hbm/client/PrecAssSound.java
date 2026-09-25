// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachinePrecAss;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;

public final class PrecAssSound {

    private static final Random ONE_SHOT_RAND = new Random();

    private PrecAssSound() {}

    public static void tick(BlockEntityMachinePrecAss be) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.isProgressing
                        && me != null
                        && me.getEyePosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ())
                                < 50 * 50,
                0.5F,
                0.75F);
    }

    public static void installOneShots() {
        BlockEntityMachinePrecAss.CLIENT_SOUND_START =
                be -> {
                    if (!be.isMuffled())
                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(
                                        SimpleSoundInstance.forUI(
                                                ModSounds.ASSEMBLER_START.get(),
                                                1.25F + ONE_SHOT_RAND.nextFloat() * 0.25F));
                };
        BlockEntityMachinePrecAss.CLIENT_SOUND_STRIKE =
                be -> {
                    if (!be.isMuffled())
                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(
                                        SimpleSoundInstance.forUI(
                                                ModSounds.ASSEMBLER_STRIKE.get(), 1.25F));
                };
        BlockEntityMachinePrecAss.CLIENT_SOUND_STOP =
                be -> {
                    if (!be.isMuffled())
                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(
                                        SimpleSoundInstance.forUI(
                                                ModSounds.ASSEMBLER_STOP.get(),
                                                1.25F + ONE_SHOT_RAND.nextFloat() * 0.25F));
                };
    }
}
