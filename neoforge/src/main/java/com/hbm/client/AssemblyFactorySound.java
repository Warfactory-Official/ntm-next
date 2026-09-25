// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyFactory;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class AssemblyFactorySound {

    private static final Random ONE_SHOT_RAND = new Random();

    private AssemblyFactorySound() {}

    public static void tick(BlockEntityMachineAssemblyFactory be) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.anyProcessing()
                        && me != null
                        && me.getEyePosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ())
                                < 50 * 50,
                0.5F,
                0.75F);
    }

    public static void installOneShots() {
        BlockEntityMachineAssemblyFactory.CLIENT_SOUND_START =
                be ->
                        play(
                                be,
                                ModSounds.ASSEMBLER_START.get(),
                                be.getVolume(0.25F),
                                1.25F + ONE_SHOT_RAND.nextFloat() * 0.25F);
        BlockEntityMachineAssemblyFactory.CLIENT_SOUND_STRIKE =
                be -> play(be, ModSounds.ASSEMBLER_STRIKE.get(), be.getVolume(0.5F), 1F);
        BlockEntityMachineAssemblyFactory.CLIENT_SOUND_CUT =
                be ->
                        play(
                                be,
                                ModSounds.ASSEMBLER_CUT.get(),
                                be.getVolume(0.5F),
                                1F + ONE_SHOT_RAND.nextFloat() * 0.25F);
    }

    private static void play(
            BlockEntityMachineAssemblyFactory be, SoundEvent sound, float volume, float pitch) {
        BlockPos pos = be.getBlockPos();
        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        new SimpleSoundInstance(
                                sound,
                                SoundSource.BLOCKS,
                                volume,
                                pitch,
                                RandomSource.create(),
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5));
    }
}
