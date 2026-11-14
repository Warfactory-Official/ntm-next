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

package com.hbm.lib;

import com.hbm.registration.Registrar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class CapeRoster {

    private static final Map<UUID, Identifier> ADDON = new HashMap<>();

    private CapeRoster() {}

    public static synchronized void addon(UUID player, Identifier texture) {
        Registrar.assertOpen("CapeRoster.addon");
        if (ADDON.putIfAbsent(player, texture) != null)
            throw new IllegalStateException(player + " already has a cape");
    }

    public static @Nullable Identifier addon(UUID player) {
        return ADDON.get(player);
    }
}
