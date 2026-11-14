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

package com.hbm.hazard;

import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;

public final class TrinketsHazards {
    private TrinketsHazards() {}

    public static float apply(Player player) {
        float activation = 0F;
        for (TrinketInventory inventory :
                TrinketsApi.getAttachment(player).getInventories().values()) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                activation += HazardSystem.applyExternalEquipment(inventory.getItem(slot), player);
                if (inventory.hasCosmeticItems()) {
                    activation +=
                            HazardSystem.applyExternalEquipment(
                                    inventory.getCosmeticItem(slot), player);
                }
            }
        }
        return activation;
    }
}
