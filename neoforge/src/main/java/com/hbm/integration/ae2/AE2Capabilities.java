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

package com.hbm.integration.ae2;

import appeng.api.AECapabilities;
import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.Caps;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class AE2Capabilities {
    private AE2Capabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        Caps.registerBlockEntity(
                event,
                AECapabilities.ME_STORAGE,
                ModBlockEntities.MASS_STORAGE.get(),
                (owner, side) -> new MassStorageMEInventory(owner));
        Caps.registerBlockEntity(
                event,
                AECapabilities.ME_STORAGE,
                ModBlockEntities.ARC_FURNACE_LARGE.get(),
                (owner, side) -> new ArcFurnaceMEInventory(owner));
        Caps.registerBlock(
                event,
                AECapabilities.ME_STORAGE,
                (level, pos, state, entity, side) -> {
                    if (side == null) return null;
                    var owner = NtmCapabilities.itemOwnerAt(level, pos, state, side);
                    return owner instanceof ArcFurnaceBlockEntity furnace
                            ? new ArcFurnaceMEInventory(furnace)
                            : null;
                },
                ModBlocks.cellHandles().stream()
                        .map(handle -> (Block) handle.get())
                        .toArray(Block[]::new));
    }
}
