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

package com.hbm.blockentity;

import com.hbm.sound.SoundHandle;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public interface MachineSoundProvider {

    float MUFFLED_VOLUME = 0.1F;

    BlockPos getBlockPos();

    default @Nullable SoundHandle createAudioLoop() {
        return null;
    }

    default SoundHandle rebootAudio(SoundHandle stopped) {
        stopped.stop();
        SoundHandle audio = createAudioLoop();
        audio.start(getBlockPos());
        return audio;
    }

    default boolean isMuffled() {
        return false;
    }

    default float getVolume(float base) {
        return isMuffled() ? base * MUFFLED_VOLUME : base;
    }
}
