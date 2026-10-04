// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.hbm.registration.RegistryHandle;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;

public final class ComputerCraft {

    public static final String MOD_ID = "computercraft";
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

    private ComputerCraft() {}

    public static void init() {
        if (LOADED) Peripherals.init();
    }

    public static void declare(List<RegistryHandle<? extends Block>> roster) {
        if (LOADED) Peripherals.declare(roster);
    }
}
