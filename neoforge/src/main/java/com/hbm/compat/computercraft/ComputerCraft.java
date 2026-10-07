// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @SafeVarargs
    public static <BE extends BlockEntity> void addon(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            boolean proxied,
            RegistryHandle<? extends Block>... blocks) {
        NeoForgeRegistrar.assertOpen("ComputerCraft.addon");
        if (LOADED) Peripherals.addon(type, factory, proxied, List.of(blocks));
    }

    @SafeVarargs
    public static void extend(
            Class<? extends BlockEntity> type, RegistryHandle<? extends Block>... blocks) {
        NeoForgeRegistrar.assertOpen("ComputerCraft.extend");
        if (LOADED) Peripherals.extend(type, List.of(blocks));
    }
}
