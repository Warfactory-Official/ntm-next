// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.sound;

import net.minecraft.world.entity.Entity;

public class AudioWrapper {

    public void setKeepAlive(int keepAlive) {}

    public void keepAlive() {}

    public void updatePosition(float x, float y, float z) {}

    public void attachTo(Entity entity) {}

    public void updateVolume(float volume) {}

    public void updateRange(float range) {}

    public void updatePitch(float pitch) {}

    public float getVolume() {
        return 0F;
    }

    public float getRange() {
        return 0F;
    }

    public float getPitch() {
        return 0F;
    }

    public void setDoesRepeat(boolean repeats) {}

    public boolean isRepeating() {
        return false;
    }

    public void startSound() {}

    public void stopSound() {}

    public boolean isPlaying() {
        return false;
    }
}
