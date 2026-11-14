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

import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public final class ArcFurnaceSound {

    private ArcFurnaceSound() {}

    public static void tick(ArcFurnaceBlockEntity be) {
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
                if (be.audioLid != null) be.audioLid.start(pos);
            }
            if (be.audioLid != null) be.audioLid.refresh();
        } else if (be.audioLid != null) {
            be.audioLid.stop();
            be.audioLid = null;
        }

        if ((be.lid == 1F || be.lid == 0F)
                && be.lid != be.prevLid
                && !(be.prevLid == 0F && be.lid == 1F)) {
            AudioSystem.playMachineSound(
                    be.getLevel(),
                    pos,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    ModSounds.DOOR_WGH_STOP.get(),
                    SoundSource.BLOCKS,
                    be.getVolume(1F),
                    1F);
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
                if (be.audioProgress != null) be.audioProgress.start(pos);
            }
            if (be.audioProgress != null) {
                be.audioProgress.setPitch(0.75F);
                be.audioProgress.refresh();
            }
        } else if (be.audioProgress != null) {
            be.audioProgress.stop();
            be.audioProgress = null;
        }
    }
}
