// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineArcFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public final class ArcFurnaceSound {

    private ArcFurnaceSound() {}

    public static void tick(BlockEntityMachineArcFurnace be) {
        BlockPos pos = be.getBlockPos();

        if (be.lid != be.prevLid) {
            if (be.audioLid == null || !be.audioLid.isPlaying()) {
                be.audioLid =
                        AudioSystem.getLoopedSound(
                                ModSounds.DOOR_WGH_START.get(),
                                SoundSource.BLOCKS,
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                be.getVolume(0.75F),
                                15F,
                                1.0F,
                                5);
                if (be.audioLid != null) be.audioLid.startSound();
            }
            if (be.audioLid != null) be.audioLid.keepAlive();
        } else if (be.audioLid != null) {
            be.audioLid.stopSound();
            be.audioLid = null;
        }

        if ((be.lid == 1F || be.lid == 0F)
                && be.lid != be.prevLid
                && !(be.prevLid == 0F && be.lid == 1F)) {
            be.getLevel()
                    .playLocalSound(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            ModSounds.DOOR_WGH_STOP.get(),
                            SoundSource.BLOCKS,
                            be.getVolume(1F),
                            1F,
                            false);
        }

        if (be.isProgressing) {
            if (be.audioProgress == null || !be.audioProgress.isPlaying()) {
                be.audioProgress =
                        AudioSystem.getLoopedSound(
                                ModSounds.ELECTRIC_HUM_LOOP.get(),
                                SoundSource.BLOCKS,
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                be.getVolume(1.5F),
                                15F,
                                0.75F,
                                5);
                if (be.audioProgress != null) be.audioProgress.startSound();
            }
            if (be.audioProgress != null) {
                be.audioProgress.updatePitch(0.75F);
                be.audioProgress.keepAlive();
            }
        } else if (be.audioProgress != null) {
            be.audioProgress.stopSound();
            be.audioProgress = null;
        }
    }
}
