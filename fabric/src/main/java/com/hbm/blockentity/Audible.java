// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity;

import com.hbm.sound.AudioWrapper;
import net.minecraft.core.BlockPos;

public interface Audible {

    BlockPos getBlockPos();

    default AudioWrapper createAudioLoop() {
        return null;
    }

    default AudioWrapper rebootAudio(AudioWrapper wrapper) {
        wrapper.stopSound();
        AudioWrapper audio = createAudioLoop();
        audio.startSound(getBlockPos());
        return audio;
    }

    default boolean isMuffled() {
        return false;
    }

    default float getVolume(float base) {
        return isMuffled() ? base * 0.1F : base;
    }
}
