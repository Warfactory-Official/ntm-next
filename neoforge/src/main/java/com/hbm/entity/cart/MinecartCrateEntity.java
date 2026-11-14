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

package com.hbm.entity.cart;

import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.container.CrateMenu;
import com.hbm.items.tool.ModMinecartItem.EnumCartBase;
import com.hbm.items.tool.ModMinecartItem.EnumMinecart;
import com.hbm.items.tool.ModMinecartItem;
import com.hbm.platform.Platform;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public class MinecartCrateEntity extends MinecartContainerBaseEntity {

    private static final CrateType CRATE = CrateType.STEEL;

    public MinecartCrateEntity(EntityType<? extends MinecartCrateEntity> type, Level level) {
        super(type, level);
    }

    public MinecartCrateEntity(
            Level level, double x, double y, double z, EnumCartBase base, ItemStack stack) {
        super(ModEntities.CART_CRATE.get(), level, x, y, z, base);
        if (stack != null && !stack.isEmpty()) {
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .copyInto(getItemStacks());
        }
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        List<ItemStack> held = List.copyOf(getItemStacks());
        ItemContainerContents contents = ItemContainerContents.fromItems(held);
        clearContent();
        kill(level);

        if (!level.getGameRules().get(GameRules.ENTITY_DROPS)) return;

        ItemStack stack = getCartItem();
        if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());

        if (contents != ItemContainerContents.EMPTY) stack.set(DataComponents.CONTAINER, contents);

        spawnAtLocation(level, stack);
    }

    @Override
    public int getContainerSize() {
        return CRATE.slots;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CrateMenu(containerId, inventory, CRATE, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.CRATE_STEEL.get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 6;
    }

    @Override
    public ItemStack getCartItem() {
        return ModMinecartItem.createCartItem(getBase(), EnumMinecart.CRATE);
    }
}
