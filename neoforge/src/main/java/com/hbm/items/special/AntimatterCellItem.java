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

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.interfaces.DroppedItemUpdateHandler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidCellItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class AntimatterCellItem extends FluidCellItem implements DroppedItemUpdateHandler {

    public AntimatterCellItem(Properties properties) {
        super(properties, () -> NTMFluids.AMAT, ModItems.CELL_EMPTY);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_ANTIMATTER_CELL.get()) {
            new ComposableExplosion(entity.level(), entity.getX(), entity.getY(), entity.getZ(), 3F)
                    .antimatter()
                    .explode();
        }
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.shared.warningExposureToMatter"));
        adder.accept(Component.translatable("desc.item.antimatterCell.leadToViolentAnnihilation"));
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
