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
import org.jspecify.annotations.Nullable;

public interface AudioLoop extends MachineSoundProvider {
    void audioLoop(boolean running, float volume);

    void audioLoop(boolean running, float volume, float pitch);

    static @Nullable SoundHandle loop(
            MachineSoundProvider machine,
            @Nullable SoundHandle audio,
            boolean running,
            float volume,
            float pitch,
            boolean pitched) {
        if (!running) {
            if (audio != null) audio.stop();
            return null;
        }
        if (audio == null) {
            audio = machine.createAudioLoop();
            if (audio == null) return null;
            audio.start(machine.getBlockPos());
        } else if (!audio.isPlaying()) {
            audio = machine.rebootAudio(audio);
        }
        audio.setVolume(machine.getVolume(volume));
        if (pitched) audio.setPitch(pitch);
        audio.refresh();
        return audio;
    }
}
