// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.AECapabilities;
import com.hbm.blockentity.machine.BlockEntityMachineArcFurnace;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.NeoForgeCaps;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class AE2Capabilities {
    private AE2Capabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        NeoForgeCaps.registerBlockEntity(
                event,
                AECapabilities.ME_STORAGE,
                ModBlockEntities.MASS_STORAGE.get(),
                (owner, side) -> new MassStorageMEInventory(owner));
        NeoForgeCaps.registerBlockEntity(
                event,
                AECapabilities.ME_STORAGE,
                ModBlockEntities.ARC_FURNACE_LARGE.get(),
                (owner, side) -> new ArcFurnaceMEInventory(owner));
        NeoForgeCaps.registerBlock(
                event,
                AECapabilities.ME_STORAGE,
                (level, pos, state, entity, side) -> {
                    if (side == null) return null;
                    var owner = NtmCapabilities.itemOwnerAt(level, pos, state, side);
                    return owner instanceof BlockEntityMachineArcFurnace furnace
                            ? new ArcFurnaceMEInventory(furnace)
                            : null;
                },
                ModBlocks.cellHandles().stream()
                        .map(handle -> (Block) handle.get())
                        .toArray(Block[]::new));
    }
}
