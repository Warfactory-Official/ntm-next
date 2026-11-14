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

package com.hbm.capability;

import com.hbm.capability.port.ItemPort;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class MachineItemHandler implements ResourceHandler<ItemResource> {
    private static final int NULL_SIDE = 6;

    private static final int MAX_PORTS_PER_SIDE = 8;
    private final WeakReference<Binding> binding;
    private final @Nullable Direction side;
    private final boolean[] accessible;

    public MachineItemHandler(BlockEntity machine, @Nullable Direction side) {
        this(
                machine,
                side,
                side == null ? null : ((WorldlyContainer) machine).getSlotsForFace(side),
                null);
    }

    public MachineItemHandler(BlockEntity machine, @Nullable Direction side, ItemPort access) {
        this(machine, side, access.slots(), access);
    }

    public MachineItemHandler(
            BlockEntity machine, @Nullable Direction side, int @Nullable [] slots) {
        this(machine, side, slots, null);
    }

    private MachineItemHandler(
            BlockEntity machine,
            @Nullable Direction side,
            int @Nullable [] slots,
            @Nullable ItemPort access) {
        var binding = new Binding(machine, (WorldlyContainer) machine, access);
        this.binding = new WeakReference<>(binding);
        this.side = side;
        var n = binding.inventory.getContainerSize();
        this.accessible = new boolean[n];
        if (slots == null) {
            Arrays.fill(accessible, true);
        } else {
            for (int slot : slots) {
                if (slot >= 0 && slot < n) accessible[slot] = true;
            }
        }
        if (!machine.isRemoved()) PerMachine.of(machine).bindings.put(this, binding);
    }

    public static MachineItemHandler of(BlockEntity machine, @Nullable Direction side) {
        return handlerFor(machine, side, null);
    }

    public static MachineItemHandler of(
            BlockEntity machine, @Nullable Direction side, ItemPort access) {
        return handlerFor(machine, side, access);
    }

    private static MachineItemHandler handlerFor(
            BlockEntity machine, @Nullable Direction side, @Nullable ItemPort access) {
        if (machine.isRemoved()) {
            return access == null
                    ? new MachineItemHandler(machine, side)
                    : new MachineItemHandler(machine, side, access);
        }
        var perMachine = PerMachine.of(machine);
        int index = side == null ? NULL_SIDE : side.ordinal();
        Int2ObjectOpenHashMap<MachineItemHandler> byAccess = perMachine.bySide[index];
        if (byAccess == null) {
            byAccess = new Int2ObjectOpenHashMap<>();
            perMachine.bySide[index] = byAccess;
        }
        int key = access == null ? 0 : System.identityHashCode(access);
        MachineItemHandler handler = byAccess.get(key);
        var binding = handler == null ? null : handler.live();
        if (binding != null && binding.access == access) return handler;
        handler =
                access == null
                        ? new MachineItemHandler(machine, side)
                        : new MachineItemHandler(machine, side, access);
        if (access == null || byAccess.size() < MAX_PORTS_PER_SIDE) byAccess.put(key, handler);
        return handler;
    }

    private @Nullable Binding live() {
        var held = binding.get();
        return held != null && !held.owner.isRemoved() ? held : null;
    }

    @Override
    public int size() {
        return accessible.length;
    }

    @Override
    public ItemResource getResource(int index) {
        var binding = live();
        return binding == null ? ItemResource.EMPTY : binding.core.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        var binding = live();
        return binding == null ? 0 : binding.core.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        var binding = live();
        return binding == null ? 0 : binding.core.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        var binding = live();
        return binding != null && binding.core.isValid(index, resource);
    }

    @Override
    public int insert(
            int index, ItemResource resource, int amount, TransactionContext transaction) {
        var binding = live();
        if (binding == null || amount == 0 || !accessible[index]) return 0;
        return ItemPort.mayInsert(
                        binding.inventory, binding.access, index, resource.toStack(amount), side)
                ? binding.core.insert(index, resource, amount, transaction)
                : 0;
    }

    @Override
    public int extract(
            int index, ItemResource resource, int amount, TransactionContext transaction) {
        var binding = live();
        if (binding == null || !accessible[index]) return 0;
        return ItemPort.mayExtract(binding.inventory, binding.access, index, side)
                ? binding.core.extract(index, resource, amount, transaction)
                : 0;
    }

    private static final class Binding {
        final BlockEntity owner;
        final WorldlyContainer inventory;
        final ResourceHandler<ItemResource> core;
        final @Nullable ItemPort access;

        Binding(BlockEntity owner, WorldlyContainer inventory, @Nullable ItemPort access) {
            this.owner = owner;
            this.inventory = inventory;
            this.core = VanillaContainerWrapper.of(inventory);
            this.access = access;
        }
    }

    private static final class PerMachine {
        @SuppressWarnings("unchecked")
        final Int2ObjectOpenHashMap<MachineItemHandler>[] bySide =
                new Int2ObjectOpenHashMap[NULL_SIDE + 1];

        final Map<MachineItemHandler, Binding> bindings = new WeakHashMap<>();

        static PerMachine of(BlockEntity owner) {
            var bridges = ForeignBridges.of(owner);
            if (bridges.items() instanceof PerMachine held) return held;
            var created = new PerMachine();
            bridges.items(created);
            return created;
        }
    }
}
