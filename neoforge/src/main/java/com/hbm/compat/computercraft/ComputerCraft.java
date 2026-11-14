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

import com.hbm.platform.Platform;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ComputerCraft {

    public static final String MOD_ID = "computercraft";
    public static final boolean LOADED = Platform.isModLoaded(MOD_ID);

    private ComputerCraft() {}

    public static void init() {
        if (LOADED) Peripherals.init();
    }

    public static void declare(List<RegistryHandle<? extends Block>> roster) {
        if (LOADED) Peripherals.declare(roster);
    }

    @SafeVarargs
    public static <BE extends BlockEntity> void addon(
            Class<BE> type,
            Function<? super BE, ? extends BlockEntityPeripheral<?>> factory,
            boolean proxied,
            RegistryHandle<? extends Block>... blocks) {
        Registrar.assertOpen("ComputerCraft.addon");
        if (LOADED) Peripherals.addon(type, factory, proxied, List.of(blocks));
    }

    @SafeVarargs
    public static void extend(
            Class<? extends BlockEntity> type, RegistryHandle<? extends Block>... blocks) {
        Registrar.assertOpen("ComputerCraft.extend");
        if (LOADED) Peripherals.extend(type, List.of(blocks));
    }
}
