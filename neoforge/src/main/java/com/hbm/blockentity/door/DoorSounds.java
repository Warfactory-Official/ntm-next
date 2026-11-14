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

package com.hbm.blockentity.door;

import com.hbm.registration.RegistryHandle;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

public record DoorSounds(
        @Nullable RegistryHandle<SoundEvent> openStart,
        @Nullable RegistryHandle<SoundEvent> openLoop,
        @Nullable RegistryHandle<SoundEvent> openEnd,
        @Nullable RegistryHandle<SoundEvent> closeStart,
        @Nullable RegistryHandle<SoundEvent> closeLoop,
        @Nullable RegistryHandle<SoundEvent> closeEnd,
        @Nullable RegistryHandle<SoundEvent> secondLoop) {

    public static final DoorSounds NONE = new DoorSounds(null, null, null, null, null, null, null);

    public static DoorSounds of(
            @Nullable RegistryHandle<SoundEvent> start,
            @Nullable RegistryHandle<SoundEvent> loop,
            @Nullable RegistryHandle<SoundEvent> end) {
        return new DoorSounds(start, loop, end, start, loop, end, null);
    }

    public DoorSounds withCloseStart(@Nullable RegistryHandle<SoundEvent> sound) {
        return new DoorSounds(openStart, openLoop, openEnd, sound, closeLoop, closeEnd, secondLoop);
    }

    public DoorSounds withCloseLoop(@Nullable RegistryHandle<SoundEvent> sound) {
        return new DoorSounds(
                openStart, openLoop, openEnd, closeStart, sound, closeEnd, secondLoop);
    }

    public DoorSounds withCloseEnd(@Nullable RegistryHandle<SoundEvent> sound) {
        return new DoorSounds(
                openStart, openLoop, openEnd, closeStart, closeLoop, sound, secondLoop);
    }

    public DoorSounds withSecondLoop(@Nullable RegistryHandle<SoundEvent> sound) {
        return new DoorSounds(openStart, openLoop, openEnd, closeStart, closeLoop, closeEnd, sound);
    }

    public @Nullable RegistryHandle<SoundEvent> start(boolean opening) {
        return opening ? openStart : closeStart;
    }

    public @Nullable RegistryHandle<SoundEvent> loop(boolean opening) {
        return opening ? openLoop : closeLoop;
    }

    public @Nullable RegistryHandle<SoundEvent> end(boolean opening) {
        return opening ? openEnd : closeEnd;
    }

    public boolean loops(boolean opening) {
        return loop(opening) != null || secondLoop != null;
    }
}
