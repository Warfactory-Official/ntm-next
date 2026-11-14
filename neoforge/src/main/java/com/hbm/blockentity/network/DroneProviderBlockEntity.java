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

package com.hbm.blockentity.network;

import com.hbm.api.block.DroneInventoryProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.DroneProviderMenu;
import com.hbm.logistics.courier.CourierEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DroneProviderBlockEntity extends RequestNetworkBlockEntity
        implements DroneInventoryProvider.Provider, MenuProvider {

    public static final int SLOT_COUNT = 9;
    private static final int[] STOCK_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    public DroneProviderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_PROVIDER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected CourierEndpoint createNode(BlockPos pos) {
        var node = new CourierEndpoint.Stock(pos, reachable);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var stock = inventory.get(slot);
            if (!stock.isEmpty()) node.items().add(stock.copy());
        }
        return node;
    }

    @Override
    public Container droneOfferInventory() {
        return this;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STOCK_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneProvider");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneProviderMenu(containerId, inventory, this);
    }
}
