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

package com.hbm.util;

import java.util.function.LongSupplier;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class GameTime {

    public static @Nullable LongSupplier CLIENT_MILLIS;
    public static @Nullable LongSupplier CLIENT_NOW;

    private GameTime() {}

    public static long millis(Level level) {
        return level.isClientSide() && CLIENT_MILLIS != null
                ? CLIENT_MILLIS.getAsLong()
                : level.getGameTime() * 50L;
    }

    public static long millis() {
        return CLIENT_MILLIS == null ? 0L : CLIENT_MILLIS.getAsLong();
    }

    public static long now() {
        return CLIENT_NOW == null ? 0L : CLIENT_NOW.getAsLong();
    }

    public static long ticks() {
        return millis() / 50L;
    }
}
