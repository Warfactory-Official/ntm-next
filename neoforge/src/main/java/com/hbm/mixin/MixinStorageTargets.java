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
