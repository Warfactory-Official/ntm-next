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

package com.hbm.compat.computercraft;

import com.hbm.inventory.fluid.NTMFluids;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidKeys {

    private static final String NONE = key(null);
    private static final ConcurrentHashMap<Fluid, String> KEYS = new ConcurrentHashMap<>();

    private FluidKeys() {}

    public static String of(@Nullable Fluid fluid) {
        if (fluid == null) return NONE;
        var key = KEYS.get(fluid);
        return key != null ? key : KEYS.computeIfAbsent(fluid, FluidKeys::key);
    }

    private static String key(@Nullable Fluid fluid) {
        return "hbmfluid." + NTMFluids.legacyName(fluid).toLowerCase(Locale.ROOT);
    }
}
