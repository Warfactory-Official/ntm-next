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

package com.hbm.api.environment;

import java.util.Arrays;
import net.minecraft.server.level.ServerLevel;

public final class LevelWeather {

    public interface Owner {
        boolean advance(ServerLevel level);

        default void sleptThrough(ServerLevel level) {}
    }

    private static Owner[] owners = new Owner[0];

    private LevelWeather() {}

    public static void register(Owner owner) {
        owners = Arrays.copyOf(owners, owners.length + 1);
        owners[owners.length - 1] = owner;
    }

    public static boolean advance(ServerLevel level) {
        for (Owner owner : owners) {
            if (owner.advance(level)) return true;
        }
        return false;
    }

    public static void sleptThrough(ServerLevel level) {
        for (Owner owner : owners) owner.sleptThrough(level);
    }
}
