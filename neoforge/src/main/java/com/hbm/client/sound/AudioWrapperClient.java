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

package com.hbm.client.sound;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.sound.SoundHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class AudioWrapperClient implements SoundHandle {

    protected final @Nullable AudioDynamic sound;

    public AudioWrapperClient(@Nullable SoundEvent source, SoundSource category) {
        sound = source != null ? new AudioDynamic(source, category) : null;
    }

    @Override
    public void setTimeout(int keepAlive) {
        if (sound != null) {
            sound.setKeepAlive(keepAlive);
        }
    }

    @Override
    public void refresh() {
        if (sound != null) {
            sound.keepAlive();
        }
    }

    @Override
    public void moveTo(float x, float y, float z) {
        if (sound != null) {
            sound.setPosition(x, y, z);
        }
    }

    @Override
    public void follow(Entity entity) {
        if (sound != null) {
            sound.attachTo(entity);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (sound != null) {
            sound.setVolume(volume);
        }
    }

    @Override
    public void setRange(float range) {
        if (sound != null) {
            sound.setRange(range);
        }
    }

    @Override
    public void setPitch(float pitch) {
        if (sound != null) {
            sound.setPitch(pitch);
        }
    }

    @Override
    public float getPitch() {
        return sound != null ? sound.rawPitch() : 1F;
    }

    @Override
    public void setLooping(boolean repeats) {
        if (sound != null) {
            sound.setLooping(repeats);
        }
    }

    @Override
    public boolean isLooping() {
        return sound != null && sound.isLooping();
    }

    @Override
    public void start() {
        if (sound != null) {
            sound.machineCore = MultiblockSurface.NO_CORE;
            sound.start();
        }
    }

    @Override
    public void stop() {
        if (sound != null) {
            sound.stopSound();
            sound.setKeepAlive(0);
        }
    }

    @Override
    public void start(BlockPos owner) {
        if (sound != null) {
            sound.machineCore = owner.asLong();
            sound.start();
        }
    }

    @Override
    public boolean isPlaying() {
        return sound != null && sound.isPlaying();
    }
}
