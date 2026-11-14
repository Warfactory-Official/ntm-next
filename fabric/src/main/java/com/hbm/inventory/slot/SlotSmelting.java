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

package com.hbm.inventory.slot;

import com.hbm.advancement.HbmCriteria;
import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SlotSmelting extends FurnaceResultSlot {

    private final Player player;
    private final @Nullable SmeltingFurnaceBlockEntity furnace;

    private int takenCount;

    public SlotSmelting(Player player, SmeltingFurnaceBlockEntity furnace, int slot, int x, int y) {
        this(player, furnace, furnace, slot, x, y);
    }

    public SlotSmelting(Player player, Container container, int slot, int x, int y) {
        this(player, container, null, slot, x, y);
    }

    private SlotSmelting(
            Player player,
            Container container,
            @Nullable SmeltingFurnaceBlockEntity furnace,
            int slot,
            int x,
            int y) {
        super(player, container, slot, x, y);
        this.player = player;
        this.furnace = furnace;
    }

    private static void popResultKeyedExperience(
            ServerPlayer player, ItemStack carried, int taken) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        float experience = 0.0F;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof SmeltingRecipe smelting)) continue;
            if (ItemStack.isSameItem(
                    smelting.assemble(new SingleRecipeInput(ItemStack.EMPTY)), carried)) {
                experience = smelting.experience();
                break;
            }
        }
        if (experience == 0.0F) return;

        int count = taken;
        if (experience < 1.0F) {
            int floor = Mth.floor((float) count * experience);
            if (floor < Mth.ceil((float) count * experience)
                    && (float) Math.random() < (float) count * experience - (float) floor) {
                ++floor;
            }
            count = floor;
        }
        if (count > 0) {
            ExperienceOrb.award(
                    player.level(),
                    new Vec3(player.getX(), player.getY() + 0.5D, player.getZ()),
                    count);
        }
    }

    @Override
    public ItemStack remove(int amount) {
        if (hasItem()) takenCount += Math.min(amount, getItem().getCount());
        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack picked, int count) {
        takenCount += count;
        super.onQuickCraft(picked, count);
    }

    @Override
    protected void checkTakeAchievements(ItemStack carried) {
        if (player instanceof ServerPlayer serverPlayer) {
            HbmCriteria.itemCrafted(serverPlayer, carried);
            if (furnace != null)
                furnace.awardUsedRecipesAndPopExperience(serverPlayer, getContainerSlot());
            else popResultKeyedExperience(serverPlayer, carried, takenCount);
        }
        takenCount = 0;
        super.checkTakeAchievements(carried);
    }
}
