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

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class EntityAudioLoop {

    private static final int KEEP_ALIVE = 20;

    private final Entity owner;
    private final Supplier<SoundEvent> sound;
    private final SoundSource category;
    private final float volume;
    private final float range;
    private @Nullable SoundHandle audio;

    public EntityAudioLoop(
            Entity owner,
            Supplier<SoundEvent> sound,
            SoundSource category,
            float volume,
            float range) {
        this.owner = owner;
        this.sound = sound;
        this.category = category;
        this.volume = volume;
        this.range = range;
    }

    public void tick(boolean wanted) {
        if (!wanted) {
            stop();
            return;
        }
        float x = (float) owner.getX();
        float y = (float) owner.getY();
        float z = (float) owner.getZ();
        if (audio == null || !audio.isPlaying()) {
            audio =
                    AudioSystem.getLoopedSound(
                            sound.get(), category, x, y, z, volume, range, 1F, KEEP_ALIVE);
            if (audio == null) return;
            audio.start();
        }
        audio.refresh();
        audio.moveTo(x, y, z);
    }

    public void stop() {
        if (audio != null) audio.stop();
        audio = null;
    }
}
