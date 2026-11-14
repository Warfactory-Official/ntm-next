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
import com.hbm.blockentity.GhostFilters;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.DroneRequesterMenu;
import com.hbm.logistics.courier.CourierEndpoint;
import com.hbm.logistics.courier.CourierItemRequest;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class DroneRequesterBlockEntity extends RequestNetworkBlockEntity
        implements DroneInventoryProvider.Requester, GhostFilters, MenuProvider, SyncUnitSchema {

    public static final int FILTER_COUNT = 9;
    public static final int STOCK_START = FILTER_COUNT;
    public static final int SLOT_COUNT = FILTER_COUNT * 2;
    private static final int[] STOCK_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final Window FILTERS = new Window(0, FILTER_COUNT);
    private static final long MATCHER_MASK = 1;

    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_COUNT);

    private final Container deliveryStock =
            new Container() {
                @Override
                public int getContainerSize() {
                    return FILTER_COUNT;
                }

                @Override
                public boolean isEmpty() {
                    for (int slot = 0; slot < FILTER_COUNT; slot++) {
                        if (!getItem(slot).isEmpty()) return false;
                    }
                    return true;
                }

                @Override
                public ItemStack getItem(int slot) {
                    return DroneRequesterBlockEntity.this.getItem(STOCK_START + slot);
                }

                @Override
                public ItemStack removeItem(int slot, int count) {
                    return DroneRequesterBlockEntity.this.removeItem(STOCK_START + slot, count);
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    return DroneRequesterBlockEntity.this.removeItemNoUpdate(STOCK_START + slot);
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    DroneRequesterBlockEntity.this.setItem(STOCK_START + slot, stack);
                }

                @Override
                public void setChanged() {
                    DroneRequesterBlockEntity.this.setChanged();
                }

                @Override
                public boolean stillValid(Player player) {
                    return DroneRequesterBlockEntity.this.stillValid(player);
                }

                @Override
                public void clearContent() {
                    for (int slot = 0; slot < FILTER_COUNT; slot++) {
                        if (!getItem(slot).isEmpty()) setItem(slot, ItemStack.EMPTY);
                    }
                }
            };

    public DroneRequesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_REQUESTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        networkPackNT(15);
    }

    @Override
    protected CourierEndpoint createNode(BlockPos pos) {
        var node = new CourierEndpoint.Requests(pos, reachable);
        for (int slot = 0; slot < FILTER_COUNT; slot++) {
            var filter = inventory.get(slot);
            var mode = matcher.mode(slot);
            if (filter.isEmpty() || mode == null) continue;
            var stock = inventory.get(STOCK_START + slot);
            if (stock.isEmpty() || !ModulePatternMatcher.matches(mode, filter, stock)) {
                node.demands().add(new CourierItemRequest(filter.copy(), mode));
            }
        }
        return node;
    }

    @Override
    public Window filterWindow() {
        return FILTERS;
    }

    @Override
    public void advanceFilterMode(int slot) {
        matcher.nextMode(level, inventory.get(slot), slot);
    }

    @Override
    public Container droneDeliveryInventory() {
        return deliveryStock;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STOCK_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneRequester");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneRequesterMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        matcher.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        matcher.save(output);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | MATCHER_MASK;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) matcher.serialize(output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 0) matcher.deserialize(input);
        else super.readSyncUnit(unit, input);
    }

    static {
        com.hbm.packet.SyncBindings.field(
                DroneRequesterBlockEntity.class, "matcher", 3, MATCHER_MASK);
    }
}
