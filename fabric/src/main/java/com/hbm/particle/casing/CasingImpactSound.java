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

package com.hbm.particle.casing;

import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

public enum CasingImpactSound {
    SHELL(0.5F),
    SMALL(0.5F),
    MEDIUM(0.5F),
    LARGE(1F);

    private static final float MEDIUM_SIZE = 3F;
    private static final float LARGE_SIZE = 100F;

    private final float volume;
    private @Nullable SoundEvent event;

    CasingImpactSound(float volume) {
        this.volume = volume;
    }

    static CasingImpactSound of(CasingShape shape, float scaleX, float scaleY, float scaleZ) {
        if (shape == CasingShape.SHOTGUN) return SHELL;
        float size = scaleX * scaleY * scaleZ;
        if (size >= LARGE_SIZE) return LARGE;
        return size >= MEDIUM_SIZE ? MEDIUM : SMALL;
    }

    public RegistryHandle<SoundEvent> getSound() {
        return switch (this) {
            case SHELL -> ModSounds.WEAPON_CASING_SHELL;
            case SMALL -> ModSounds.WEAPON_CASING_SMALL;
            case MEDIUM -> ModSounds.WEAPON_CASING_MEDIUM;
            case LARGE -> ModSounds.WEAPON_CASING_LARGE;
        };
    }

    public SoundEvent getSoundEvent() {
        var resolved = event;
        if (resolved == null) event = resolved = getSound().get();
        return resolved;
    }

    public float getVolume() {
        return volume;
    }
}
