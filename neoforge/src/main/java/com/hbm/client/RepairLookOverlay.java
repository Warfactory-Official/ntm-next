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

package com.hbm.client;

import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.RepairHandler;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.tool.BlowtorchItem;
import com.hbm.util.GameTime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RepairLookOverlay {
    private RepairLookOverlay() {}

    public static void build(Level level, BlockPos core, LookOverlay info) {
        if (!(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof BlowtorchItem))
            return;
        RepairHandler repairable = NtmContracts.REPAIRABLE.at(level, core);
        if (repairable == null || !repairable.isDamaged()) return;
        info.title(level.getBlockState(core).getBlock().getName());
        info.row(Component.translatable("desc.repair.with"), 0xFFAA00);
        for (CountIngredient ingredient : repairable.getRepairMaterials()) {
            ItemStack display = ingredient.extractForCyclingDisplay(GameTime.millis(level), 20);
            if (display.isEmpty()) continue;
            info.row(
                    Component.translatable(
                            "desc.repair.material", display.getHoverName(), display.getCount()));
        }
    }
}
