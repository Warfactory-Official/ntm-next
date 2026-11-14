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

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public interface SoundHandle {
    default void setTimeout(int ticks) {}

    default void refresh() {}

    default void moveTo(float x, float y, float z) {}

    default void follow(Entity entity) {}

    default void setVolume(float volume) {}

    default void setRange(float range) {}

    default void setPitch(float pitch) {}

    default float getPitch() {
        return 0;
    }

    default void setLooping(boolean looping) {}

    default boolean isLooping() {
        return false;
    }

    default void start() {}

    default void start(BlockPos owner) {
        start();
    }

    default void stop() {}

    default boolean isPlaying() {
        return false;
    }
}
