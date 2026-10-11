// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.inventory.StorageLayouts;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
        targets = {
            StorageLayouts.BASE, StorageLayouts.CHEST, StorageLayouts.BARREL, StorageLayouts.HOPPER,
            StorageLayouts.DISPENSER, StorageLayouts.SHULKER, StorageLayouts.BREWING,
                    StorageLayouts.CRAFTER,
            StorageLayouts.CAMPFIRE, StorageLayouts.SHELF, StorageLayouts.BOOKSHELF,
                    StorageLayouts.LECTERN,
            StorageLayouts.LECTERN_MENU, StorageLayouts.BRUSHABLE, StorageLayouts.JUKEBOX,
                    StorageLayouts.POT,
            StorageLayouts.VAULT, StorageLayouts.CRAFTING, StorageLayouts.TOM_MATRIX,
                    StorageLayouts.COMPACT,
            StorageLayouts.DRUM, StorageLayouts.REBORN_TRANSFER, StorageLayouts.STACK_HANDLER,
                    StorageLayouts.AE_COMBINED,
            StorageLayouts.AE_RETURN, StorageLayouts.AE_CABLE_BUS,
                    StorageLayouts.SOPH_INVENTORY_DATA, StorageLayouts.SOPH_UPGRADE_DATA
        },
        priority = 500,
        remap = false)
public abstract class MixinStorageTargets {}
