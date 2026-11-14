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

package com.hbm.neoforge.compat.computercraft.machine;

import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SmeltingFurnacePeripheral<T extends SmeltingFurnaceBlockEntity>
        extends BlockEntityPeripheral<T> {
    public SmeltingFurnacePeripheral(T machine) {
        super(machine, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType()).toString());
    }

    @Override
    public @NonNull Set<String> getAdditionalTypes() {
        return Set.of("inventory");
    }

    @LuaFunction(mainThread = true)
    public int size() {
        return machine().getContainerSize();
    }

    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, Object>> list() {
        T furnace = machine();
        Map<Integer, Map<String, Object>> items = new HashMap<>();
        for (int slot = 0; slot < furnace.getContainerSize(); slot++) {
            ItemStack stack = furnace.getItem(slot);
            if (!stack.isEmpty()) {
                items.put(
                        slot + 1,
                        VanillaDetailRegistries.ITEM_STACK.getBasicDetails(
                                furnace.getLevel().registryAccess(), stack));
            }
        }
        return items;
    }

    @LuaFunction(mainThread = true)
    public @Nullable Map<String, Object> getItemDetail(int slot) throws LuaException {
        T furnace = machine();
        int index = index(slot, furnace.getContainerSize());
        ItemStack stack = furnace.getItem(index);
        return stack.isEmpty()
                ? null
                : VanillaDetailRegistries.ITEM_STACK.getDetails(
                        furnace.getLevel().registryAccess(), stack);
    }

    @LuaFunction(mainThread = true)
    public int getItemLimit(int slot) throws LuaException {
        T furnace = machine();
        ItemStack stack = furnace.getItem(index(slot, furnace.getContainerSize()));
        return stack.isEmpty() ? furnace.getMaxStackSize() : stack.getMaxStackSize();
    }

    @LuaFunction(mainThread = true)
    public int pushItems(IComputerAccess computer, IArguments args) throws LuaException {
        T furnace = machine();
        ResourceHandler<ItemResource> from = VanillaContainerWrapper.of(furnace);
        ResourceHandler<ItemResource> to = connectedInventory(computer, args.getString(0));
        return transfer(from, to, args.getInt(1), args.optInt(2, 64), args.optInt(3, 0));
    }

    @LuaFunction(mainThread = true)
    public int pullItems(IComputerAccess computer, IArguments args) throws LuaException {
        T furnace = machine();
        ResourceHandler<ItemResource> from = connectedInventory(computer, args.getString(0));
        ResourceHandler<ItemResource> to = VanillaContainerWrapper.of(furnace);
        return transfer(from, to, args.getInt(1), args.optInt(2, 64), args.optInt(3, 0));
    }

    private static int index(int slot, int size) throws LuaException {
        if (slot < 1 || slot > size) throw new LuaException("Slot out of range (" + slot + ")");
        return slot - 1;
    }

    private static ResourceHandler<ItemResource> connectedInventory(
            IComputerAccess computer, String name) throws LuaException {
        IPeripheral peripheral = computer.getAvailablePeripheral(name);
        if (peripheral == null
                || !"inventory".equals(peripheral.getType())
                        && !peripheral.getAdditionalTypes().contains("inventory")) {
            throw new LuaException("Target is not an inventory");
        }
        Object target = peripheral.getTarget();
        if (target instanceof BlockEntity be && be.getLevel() != null) {
            ResourceHandler<ItemResource> handler =
                    be.getLevel().getCapability(Capabilities.Item.BLOCK, be.getBlockPos(), null);
            if (handler != null) return handler;
            for (Direction side : Direction.values()) {
                handler =
                        be.getLevel()
                                .getCapability(Capabilities.Item.BLOCK, be.getBlockPos(), side);
                if (handler != null) return handler;
            }
        }
        if (target instanceof Container container) return VanillaContainerWrapper.of(container);
        throw new LuaException("Target has no accessible item storage");
    }

    private static int transfer(
            ResourceHandler<ItemResource> from,
            ResourceHandler<ItemResource> to,
            int fromSlot,
            int limit,
            int toSlot)
            throws LuaException {
        int source = index(fromSlot, from.size());
        if (toSlot != 0) index(toSlot, to.size());
        if (limit < 0) throw new LuaException("Limit must be non-negative");
        if (limit == 0 || from == to) return 0;

        ItemResource resource = from.getResource(source);
        if (resource.isEmpty()) return 0;
        int remaining = (int) Math.min(limit, from.getAmountAsLong(source));
        int moved = 0;
        for (int destination = toSlot == 0 ? 0 : toSlot - 1;
                destination < (toSlot == 0 ? to.size() : toSlot) && remaining > 0;
                destination++) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = to.insert(destination, resource, remaining, transaction);
                if (inserted == 0) continue;
                int extracted = from.extract(source, resource, inserted, transaction);
                if (extracted != inserted) continue;
                transaction.commit();
                moved += extracted;
                remaining -= extracted;
            }
        }
        return moved;
    }
}
