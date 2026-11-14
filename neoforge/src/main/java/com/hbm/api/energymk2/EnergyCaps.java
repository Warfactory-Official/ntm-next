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

package com.hbm.api.energymk2;

import com.hbm.util.RegistryUtil;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class EnergyCaps {

    public static BlockCapability<EnergyHandler, Direction> PROVIDER;
    public static BlockCapability<EnergyHandler, Direction> RECEIVER;

    private EnergyCaps() {}

    public static void register() {
        PROVIDER =
                BlockCapability.create(
                        RegistryUtil.id("energy_provider"), EnergyHandler.class, Direction.class);
        RECEIVER =
                BlockCapability.create(
                        RegistryUtil.id("energy_receiver"), EnergyHandler.class, Direction.class);
    }
}
