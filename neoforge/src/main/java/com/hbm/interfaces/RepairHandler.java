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

package com.hbm.interfaces;

import com.hbm.capability.NtmContracts;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.util.InventoryUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface RepairHandler {
    boolean isDamaged();

    List<CountIngredient> getRepairMaterials();

    void repair(Player player);

    static boolean tryRepair(Level level, BlockPos pos, Player player) {
        RepairHandler machine = NtmContracts.REPAIRABLE.at(level, pos);
        if (machine == null || !machine.isDamaged()) return false;
        List<CountIngredient> cost = machine.getRepairMaterials();
        if (cost.isEmpty()) {
            if (!level.isClientSide()) machine.repair(player);
            return true;
        }
        if (level.isClientSide()) return InventoryUtil.hasIngredients(player, cost);
        if (!InventoryUtil.consumeIngredients(player, cost)) return false;
        machine.repair(player);
        return true;
    }
}
