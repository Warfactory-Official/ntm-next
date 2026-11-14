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

import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class WorldSounds {
    private WorldSounds() {}

    public static void entityNoise(Entity source, double x, double y, double z, float volume) {
        noise(source.level(), GameEvent.ENTITY_ACTION, source, x, y, z, volume);
    }

    public static void entityLoop(Entity source, boolean starting) {
        if (starting || TickPhase.every(source, SharedConstants.TICKS_PER_SECOND)) {
            entityNoise(source, source.getX(), source.getY(), source.getZ(), 1F);
        }
    }

    public static void play(Entity emitter, SoundEvent sound, float volume, float pitch) {
        emitter.playSound(sound, volume, pitch);
        if (!emitter.isSilent())
            entityNoise(emitter, emitter.getX(), emitter.getY(), emitter.getZ(), volume);
    }

    public static void machineNoise(Level level, BlockPos pos) {
        noise(
                level,
                GameEvent.BLOCK_ACTIVATE,
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                1F);
    }

    private static void noise(
            Level level,
            Holder<GameEvent> event,
            @Nullable Entity source,
            double x,
            double y,
            double z,
            float volume) {
        if (volume > 0F && level instanceof ServerLevel server) {
            server.gameEvent(event, new Vec3(x, y, z), GameEvent.Context.of(source));
        }
    }

    public static void play(
            Level level,
            @Nullable Entity except,
            BlockPos pos,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, pos, sound, source, volume, pitch);
        noise(
                level,
                GameEvent.ENTITY_ACTION,
                except,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                volume);
    }

    public static void play(
            Level level,
            @Nullable Entity except,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, x, y, z, sound, source, volume, pitch);
        noise(level, GameEvent.ENTITY_ACTION, except, x, y, z, volume);
    }

    public static void play(
            Level level,
            @Nullable Entity except,
            double x,
            double y,
            double z,
            Holder<SoundEvent> sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, x, y, z, sound, source, volume, pitch);
        noise(level, GameEvent.ENTITY_ACTION, except, x, y, z, volume);
    }

    public static void play(
            Level level,
            @Nullable Entity except,
            Entity emitter,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, emitter, sound, source, volume, pitch);
        noise(
                level,
                GameEvent.ENTITY_ACTION,
                emitter,
                emitter.getX(),
                emitter.getY(),
                emitter.getZ(),
                volume);
    }

    public static void machine(
            Level level,
            @Nullable Entity except,
            BlockPos pos,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, pos, sound, source, volume, pitch);
        noise(
                level,
                GameEvent.BLOCK_ACTIVATE,
                except,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                volume);
    }

    public static void machine(
            Level level,
            @Nullable Entity except,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, x, y, z, sound, source, volume, pitch);
        noise(level, GameEvent.BLOCK_ACTIVATE, except, x, y, z, volume);
    }

    public static void machine(
            Level level,
            @Nullable Entity except,
            double x,
            double y,
            double z,
            Holder<SoundEvent> sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, x, y, z, sound, source, volume, pitch);
        noise(level, GameEvent.BLOCK_ACTIVATE, except, x, y, z, volume);
    }

    public static void machine(
            Level level,
            @Nullable Entity except,
            Entity emitter,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, emitter, sound, source, volume, pitch);
        noise(
                level,
                GameEvent.BLOCK_ACTIVATE,
                emitter,
                emitter.getX(),
                emitter.getY(),
                emitter.getZ(),
                volume);
    }

    public static void explosion(
            Level level,
            @Nullable Entity except,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        level.playSound(except, x, y, z, sound, source, volume, pitch);
        noise(level, GameEvent.EXPLODE, except, x, y, z, volume);
    }
}
