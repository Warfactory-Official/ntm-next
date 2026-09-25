// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.sound.AudioWrapper;
import org.jspecify.annotations.Nullable;

public interface AudioLoop extends Audible {

    void audioLoop(boolean running, float volume);

    void audioLoop(boolean running, float volume, float pitch);

    static @Nullable AudioWrapper loop(
            Audible machine,
            @Nullable AudioWrapper audio,
            boolean running,
            float volume,
            float pitch,
            boolean pitched) {
        if (!running) {
            if (audio != null) audio.stopSound();
            return null;
        }
        if (audio == null) {
            audio = machine.createAudioLoop();
            if (audio == null) return null;
            audio.startSound();
        } else if (!audio.isPlaying()) {
            audio = machine.rebootAudio(audio);
        }
        audio.updateVolume(machine.getVolume(volume));
        if (pitched) audio.updatePitch(pitch);
        audio.keepAlive();
        return audio;
    }
}
