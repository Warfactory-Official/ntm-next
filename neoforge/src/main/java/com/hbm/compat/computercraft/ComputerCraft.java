// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.hbm.registration.RegistryHandle;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

public final class ComputerCraft {

    public static final String MOD_ID = "computercraft";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private ComputerCraft() {}

    public static void init() {
        if (LOADED) Peripherals.init();
    }

    public static void declare(List<RegistryHandle<? extends Block>> roster) {
        if (LOADED) Peripherals.declare(roster);
    }
}
