// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class AutoloaderSound {

    private static final float VOLUME = 0.75F;
    private static final int PLUMES = 3;

    private AutoloaderSound() {}

    public static void tick(BlockEntityRBMKAutoloader be) {
        BlockPos pos = be.getBlockPos();

        if (be.renderPiston > 0.01 && be.renderPiston < 0.99) {
            if (be.audioLift == null || !be.audioLift.isPlaying()) {
                be.audioLift =
                        AudioSystem.getLoopedSound(
                                ModSounds.DOOR_WGH_START.get(),
                                SoundSource.BLOCKS,
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                be.getVolume(VOLUME),
                                25F,
                                1.0F,
                                5);
                if (be.audioLift != null) be.audioLift.startSound();
            }
            if (be.audioLift != null) {
                be.audioLift.updateVolume(be.getVolume(VOLUME));
                be.audioLift.keepAlive();
            }
        } else if (be.audioLift != null) {
            be.audioLift.stopSound();
            be.audioLift = null;

            Minecraft.getInstance()
                    .getSoundManager()
                    .play(
                            new SimpleSoundInstance(
                                    ModSounds.DOOR_WGH_STOP.get(),
                                    SoundSource.BLOCKS,
                                    be.getVolume(2F),
                                    1F,
                                    RandomSource.create(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()));
        }
    }

    public static void plume(BlockEntityRBMKAutoloader be) {
        BlockPos pos = be.getBlockPos();
        RandomSource rand = be.getLevel().getRandom();

        for (int i = 0; i < PLUMES; i++) {
            CoolingTowerParticleOptions opts =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(0F)
                            .setBaseScale(0.25F)
                            .setMaxScale(1.5F)
                            .setLife(70 + rand.nextInt(30))
                            .setStrafe(0.05F)
                            .noWind()
                            .alphaMod(2F)
                            .build();
            be.getLevel()
                    .addParticle(
                            opts,
                            pos.getX() + 0.5 + rand.nextGaussian() * 0.125,
                            pos.getY() + 0.25,
                            pos.getZ() + 0.5 + rand.nextGaussian() * 0.125,
                            0,
                            0,
                            0);
        }
    }
}
