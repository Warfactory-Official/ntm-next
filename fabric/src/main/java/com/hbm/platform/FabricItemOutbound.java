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

package com.hbm.platform;

import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class FabricItemOutbound {

    private FabricItemOutbound() {}

    public static void register(
            List<Supplier<? extends BlockEntityType<?>>> types,
            List<Supplier<? extends Block>> cells) {
        Map<Block, BlockEntityType<?>> coreTypes = new IdentityHashMap<>();
        for (Supplier<? extends BlockEntityType<?>> type : types) {
            for (Block block : type.get().validBlocks) coreTypes.put(block, type.get());
        }
        Set<Block> cellBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Supplier<? extends Block> block : cells) cellBlocks.add(block.get());
        for (Supplier<? extends BlockEntityType<?>> typeSupplier : types) {
            BlockEntityType<?> type = typeSupplier.get();
            Block[] coreOnly =
                    type.validBlocks.stream()
                            .filter(block -> !cellBlocks.contains(block))
                            .toArray(Block[]::new);
            if (coreOnly.length == 0) continue;
            Caps.registerForBlocks(
                    ItemStorage.SIDED,
                    (level, pos, state, be, side) -> core(type, be, side),
                    coreOnly);
        }
        for (Supplier<? extends Block> blockSupplier : cells) {
            BlockEntityType<?> type = coreTypes.get(blockSupplier.get());
            Caps.registerForBlocks(
                    ItemStorage.SIDED,
                    (level, pos, state, be, side) -> {
                        Face core = type == null ? null : core(type, be, side);
                        return core != null ? core : cell(level, pos, state, side);
                    },
                    blockSupplier.get());
        }
    }

    private static @Nullable Face core(
            BlockEntityType<?> type, @Nullable BlockEntity be, @Nullable Direction side) {
        return be != null && be.getType() == type && be instanceof WorldlyContainer inventory
                ? new Face(inventory, null, side)
                : null;
    }

    private static @Nullable Face cell(
            Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        if (side == null) return null;
        BlockEntity owner = NtmCapabilities.itemOwnerAt(level, pos, state, side);
        if (!(owner instanceof WorldlyContainer inventory)) return null;
        if (owner instanceof CapabilityPortProvider host) {
            ItemPort access = host.itemAccess(pos, side);
            if (access != null) return access.isEmpty() ? null : new Face(inventory, access, side);
        }
        return new Face(inventory, null, side);
    }

    private static final class Face
            extends CombinedStorage<ItemVariant, SingleSlotStorage<ItemVariant>>
            implements SlottedStorage<ItemVariant> {

        Face(WorldlyContainer inventory, @Nullable ItemPort access, @Nullable Direction side) {
            super(slots(inventory, access, side));
        }

        private static List<SingleSlotStorage<ItemVariant>> slots(
                WorldlyContainer inventory, @Nullable ItemPort access, @Nullable Direction side) {
            int[] reach =
                    access != null
                            ? access.slots()
                            : side != null ? inventory.getSlotsForFace(side) : null;
            int count = reach != null ? reach.length : inventory.getContainerSize();
            SlottedStorage<ItemVariant> backing = ContainerStorage.of(inventory, null);
            List<SingleSlotStorage<ItemVariant>> slots = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int index = reach != null ? reach[i] : i;
                slots.add(new Slot(backing.getSlot(index), inventory, access, side, index));
            }
            return slots;
        }

        @Override
        public int getSlotCount() {
            return parts.size();
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(int slot) {
            return parts.get(slot);
        }
    }

    private record Slot(
            SingleSlotStorage<ItemVariant> backing,
            WorldlyContainer inventory,
            @Nullable ItemPort access,
            @Nullable Direction side,
            int index)
            implements SingleSlotStorage<ItemVariant> {

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (maxAmount == 0) return 0;
            var offered = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            return ItemPort.mayInsert(inventory, access, index, offered, side)
                    ? backing.insert(resource, maxAmount, transaction)
                    : 0;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return ItemPort.mayExtract(inventory, access, index, side)
                    ? backing.extract(resource, maxAmount, transaction)
                    : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return backing.isResourceBlank();
        }

        @Override
        public ItemVariant getResource() {
            return backing.getResource();
        }

        @Override
        public long getAmount() {
            return backing.getAmount();
        }

        @Override
        public long getCapacity() {
            return backing.getCapacity();
        }

        @Override
        public StorageView<ItemVariant> getUnderlyingView() {
            return backing.getUnderlyingView();
        }
    }
}
