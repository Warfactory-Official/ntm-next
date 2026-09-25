// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.sound.AudioWrapper;

public interface Audible {

    default AudioWrapper createAudioLoop() {
        return null;
    }

    default AudioWrapper rebootAudio(AudioWrapper wrapper) {
        wrapper.stopSound();
        AudioWrapper audio = createAudioLoop();
        audio.startSound();
        return audio;
    }

    default boolean isMuffled() {
        return false;
    }

    default float getVolume(float base) {
        return isMuffled() ? base * 0.1F : base;
    }
}
