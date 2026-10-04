// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.inventory.StorageLayouts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
        targets = {
            StorageLayouts.BASE,
            StorageLayouts.CHEST,
            StorageLayouts.BARREL,
            StorageLayouts.HOPPER,
            StorageLayouts.DISPENSER,
            StorageLayouts.SHULKER,
            StorageLayouts.BREWING,
            StorageLayouts.CRAFTER,
            StorageLayouts.FURNACE,
            StorageLayouts.CAMPFIRE,
            StorageLayouts.SHELF,
            StorageLayouts.BOOKSHELF,
            StorageLayouts.LECTERN,
            StorageLayouts.LECTERN_MENU,
            StorageLayouts.BRUSHABLE,
            StorageLayouts.JUKEBOX,
            StorageLayouts.POT,
            StorageLayouts.VAULT,
            StorageLayouts.CRAFTING,
            StorageLayouts.TOM_MATRIX,
            StorageLayouts.COMPACT,
            StorageLayouts.DRUM,
            StorageLayouts.REBORN_INVENTORY,
            StorageLayouts.REBORN_BULK,
            StorageLayouts.REBORN_TRANSFER,
            StorageLayouts.STACK_HANDLER,
            StorageLayouts.ITEM_HANDLER,
            StorageLayouts.UPGRADES,
            StorageLayouts.DRAWER,
            StorageLayouts.SOPH_STORAGE,
            StorageLayouts.SOPH_BACKPACK,
            StorageLayouts.AE_INVENTORY,
            StorageLayouts.AE_CELL_INVENTORY,
            StorageLayouts.AE_COMBINED,
            StorageLayouts.AE_GENERIC,
            StorageLayouts.AE_RETURN,
            StorageLayouts.AE_PATTERN,
            StorageLayouts.AE_CABLE_BUS,
            StorageLayouts.AE_ITEM_GEN,
            StorageLayouts.AE_CPU_LOGIC,
            StorageLayouts.AE_CPU_BLOCK,
            StorageLayouts.SOPH_INVENTORY_DATA,
            StorageLayouts.SOPH_UPGRADE_DATA
        },
        priority = 500,
        remap = false)
public abstract class MixinStorageTargets {}
