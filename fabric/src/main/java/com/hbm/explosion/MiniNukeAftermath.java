/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.explosion;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.main.Polaroid;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MukeFullLayout;
import com.hbm.saveddata.satellites.RadiationBursts;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class MiniNukeAftermath {

    private static final double CLOUD_VIEW_RANGE = 250D;
    private static final int FALLOUT_CHUNK_REACH = 2;

    private MiniNukeAftermath() {}

    public static boolean balefireFlash(RandomSource random) {
        return Polaroid.isBalefireDay() || random.nextInt(100) == 0;
    }

    public static void cloud(Level level, Vec3 at, double lift, boolean balefire) {
        if (level instanceof ServerLevel server) {
            PacketWire.sendToAllAround(
                    MukeFullLayout.encode(at.x, at.y + lift, at.z, balefire),
                    new TargetPoint(server, at.x, at.y, at.z, CLOUD_VIEW_RANGE));
        }
    }

    public static void rollCloud(Level level, Vec3 at, double lift) {
        if (level instanceof ServerLevel server) {
            cloud(server, at, lift, balefireFlash(server.getRandom()));
        }
    }

    public static void report(Level level, Vec3 at) {
        if (level instanceof ServerLevel server) {
            RadiationBursts.report(server, RadiationBursts.Intensity.LOW, at.x, at.z);
        }
    }

    public static void sound(Level level, Vec3 at, float volume, float pitch) {
        WorldSounds.play(
                level,
                null,
                at.x,
                at.y,
                at.z,
                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                SoundSource.BLOCKS,
                volume,
                pitch);
    }

    public static void soundWithoutVibration(Level level, Vec3 at, float volume, float pitch) {
        level.playSound(
                null,
                at.x,
                at.y,
                at.z,
                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                SoundSource.BLOCKS,
                volume,
                pitch);
    }

    @FunctionalInterface
    public interface FalloutCell {
        void deposit(BlockPos pos, int ring);
    }

    public static void forEachFalloutCell(Vec3 at, FalloutCell cell) {
        for (int cx = -FALLOUT_CHUNK_REACH; cx <= FALLOUT_CHUNK_REACH; cx++) {
            for (int cz = -FALLOUT_CHUNK_REACH; cz <= FALLOUT_CHUNK_REACH; cz++) {
                boolean corner =
                        Math.abs(cx) == FALLOUT_CHUNK_REACH && Math.abs(cz) == FALLOUT_CHUNK_REACH;
                if (!corner) {
                    cell.deposit(
                            BlockPos.containing(at.x + (cx << 4), at.y, at.z + (cz << 4)),
                            Math.abs(cx) + Math.abs(cz));
                }
            }
        }
    }

    public static void fallout(Level level, Vec3 at, float strength) {
        if (!(level instanceof ServerLevel server)) return;
        forEachFalloutCell(
                at,
                (pos, ring) -> {
                    double emission = 50F / (ring + 1) * strength;
                    RadiationSystemNT.incrementRad(server, pos, emission, emission * 1024D + 1D);
                });
    }
}
