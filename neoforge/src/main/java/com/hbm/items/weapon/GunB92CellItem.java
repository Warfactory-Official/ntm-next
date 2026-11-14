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

package com.hbm.items.weapon;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.GunB92Item;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class GunB92CellItem extends Item {

    public static final int CAPACITY = 25;

    public GunB92CellItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player) || GunB92Item.getPower(stack) >= CAPACITY) return;

        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack gun = player.getInventory().getItem(i);
            if (!gun.is(ModItems.GUN_B92.get())) continue;
            int power = GunB92Item.getPower(gun);
            if (power > 1) {
                GunB92Item.setPower(gun, power - 1);
                GunB92Item.setPower(stack, GunB92Item.getPower(stack) + 1);
                return;
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.gunB92Cell.drawsEnergyFrom"));
        adder.accept(Component.translatable("desc.item.gunB92Cell.reloadItAn"));
        adder.accept(Component.translatable("desc.item.gunB92Cell.theCellWill"));
        adder.accept(Component.translatable("desc.item.gunB92Cell.itIsNot"));
        adder.accept(Component.translatable("desc.item.gunB92Cell.forTheB92"));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("desc.item.gunB92Cell.charges", GunB92Item.getPower(stack)));
    }
}
