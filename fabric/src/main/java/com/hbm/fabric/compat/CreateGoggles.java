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

package com.hbm.fabric.compat;

import com.hbm.handler.ArmorModHandler;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class CreateGoggles {

    private static final int SOCKETS =
            1 << ArmorModHandler.HELMET_ONLY | 1 << ArmorModHandler.EXTRA;

    private CreateGoggles() {}

    public static void register() {
        ArmorModHandler.registerForeignMod(AllItems.GOGGLES, SOCKETS, EquipmentSlot.HEAD);
        GogglesItem.addIsWearingPredicate(
                player -> {
                    ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
                    return ArmorModHandler.modItem(helmet, ArmorModHandler.HELMET_ONLY)
                                    == AllItems.GOGGLES
                            || ArmorModHandler.modItem(helmet, ArmorModHandler.EXTRA)
                                    == AllItems.GOGGLES;
                });
    }
}
