// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.blockentity.machine.BlockEntityMachinePrecAss;
import com.hbm.client.sound.MachineSoundInstance;
import com.hbm.sound.ModSounds;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

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
                        play(
                                be,
                                ModSounds.ASSEMBLER_START.get(),
                                be.getVolume(0.25F),
                                1.25F + ONE_SHOT_RAND.nextFloat() * 0.25F);
                };
        BlockEntityMachinePrecAss.CLIENT_SOUND_STRIKE =
                be -> {
                    if (!be.isMuffled())
                        play(be, ModSounds.ASSEMBLER_STRIKE.get(), be.getVolume(0.5F), 1.25F);
                };
        BlockEntityMachinePrecAss.CLIENT_SOUND_STOP =
                be -> {
                    if (!be.isMuffled())
                        play(
                                be,
                                ModSounds.ASSEMBLER_STOP.get(),
                                be.getVolume(0.25F),
                                1.25F + ONE_SHOT_RAND.nextFloat() * 0.25F);
                };
    }

    private static void play(
            BlockEntityMachinePrecAss be, SoundEvent sound, float volume, float pitch) {
        BlockPos pos = be.getBlockPos();
        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        new MachineSoundInstance(
                                pos,
                                sound,
                                SoundSource.BLOCKS,
                                volume,
                                pitch,
                                SoundInstance.createUnseededRandom(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ()));
    }
}
