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

package com.hbm.sound;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GunSoundLayout;
import com.hbm.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public final class GunSoundEvents {

    private GunSoundEvents() {}

    public static void play(
            LivingEntity entity, SoundEvent sound, float volume, float pitch, boolean legacy) {
        ServerLevel level = (ServerLevel) entity.level();
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        EntitySounds.Emission emission =
                Platform.soundAtPosition(
                        level,
                        x,
                        y,
                        z,
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                        SoundSource.PLAYERS,
                        volume,
                        pitch);
        if (emission == null) return;
        WorldSounds.entityNoise(entity, x, y, z, emission.volume());
        PacketWire.sendToAllAround(
                GunSoundLayout.encode(
                        legacy,
                        entity.getId(),
                        emission.sound(),
                        emission.source(),
                        (int) (x * 8.0),
                        (int) (y * 8.0),
                        (int) (z * 8.0),
                        emission.volume(),
                        emission.pitch(),
                        level.soundSeedGenerator.nextLong()),
                new TargetPoint(
                        level, x, y, z, emission.sound().value().getRange(emission.volume())));
    }
}
