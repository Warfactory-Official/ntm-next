/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client;

import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.client.sound.MachineSoundInstance;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class AutoloaderSound {

    private static final float VOLUME = 0.75F;
    private static final int PLUMES = 3;

    private AutoloaderSound() {}

    public static void tick(RBMKAutoloaderBlockEntity be) {
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
                if (be.audioLift != null) be.audioLift.start(pos);
            }
            if (be.audioLift != null) {
                be.audioLift.setVolume(be.getVolume(VOLUME));
                be.audioLift.refresh();
            }
        } else if (be.audioLift != null) {
            be.audioLift.stop();
            be.audioLift = null;
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(
                            new MachineSoundInstance(
                                    pos,
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

    public static void plume(RBMKAutoloaderBlockEntity be) {
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
