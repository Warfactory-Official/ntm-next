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

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardSystem;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosHazards {
    private CuriosHazards() {}

    public static float apply(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return 0F;
        float activation = 0F;
        for (ICurioStacksHandler slots : inventory.getCurios().values()) {
            activation += apply(slots.getStacks(), player);
            activation += apply(slots.getCosmeticStacks(), player);
        }
        return activation;
    }

    private static float apply(IDynamicStackHandler slots, Player player) {
        float activation = 0F;
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            activation += HazardSystem.applyExternalEquipment(slots.getStackInSlot(slot), player);
        }
        return activation;
    }
}
