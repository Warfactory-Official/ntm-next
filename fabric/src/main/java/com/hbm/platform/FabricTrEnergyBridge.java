// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

final class FabricTrEnergyBridge {

    private FabricTrEnergyBridge() {}

    static @Nullable IEnergyHandlerView find(Level level, BlockPos pos, @Nullable Direction side) {
        EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FabricEnergyHandlerView(storage);
    }

    static BlockLookupCache<IEnergyHandlerView> createCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        BlockApiCache<EnergyStorage, @Nullable Direction> cache =
                BlockApiCache.create(EnergyStorage.SIDED, level, pos);
        return new BlockLookupCache<>() {
            private @Nullable EnergyStorage last;
            private @Nullable IEnergyHandlerView view;

            @Override
            public @Nullable IEnergyHandlerView find() {
                if (!level.isLoaded(pos)) return null;
                EnergyStorage storage = cache.find(side);
                if (storage == null) return null;
                if (storage != last) {
                    last = storage;
                    view = new FabricEnergyHandlerView(storage);
                }
                return view;
            }
        };
    }

    private record FabricEnergyHandlerView(EnergyStorage storage) implements IEnergyHandlerView {

        @Override
        public Object handlerIdentity() {
            return storage;
        }

        @Override
        public long amount() {
            return storage.getAmount();
        }

        @Override
        public long capacity() {
            return storage.getCapacity();
        }

        @Override
        public long insertable() {
            try (Transaction tx = Transaction.openOuter()) {
                return storage.insert(Integer.MAX_VALUE, tx);
            }
        }

        @Override
        public long extractable() {
            try (Transaction tx = Transaction.openOuter()) {
                return storage.extract(Integer.MAX_VALUE, tx);
            }
        }

        @Override
        public long insert(long maxFe, long feQuantum, boolean simulate) {
            return transfer(maxFe, feQuantum, simulate, true);
        }

        @Override
        public long extract(long maxFe, long feQuantum, boolean simulate) {
            return transfer(maxFe, feQuantum, simulate, false);
        }

        private long transfer(long maxFe, long feQuantum, boolean simulate, boolean insert) {
            assert feQuantum > 0;
            long request = Math.min(maxFe, Integer.MAX_VALUE);
            request -= request % feQuantum;
            if (request <= 0) return 0;

            try (Transaction outer = Transaction.openOuter()) {
                long available;
                try (Transaction probe = outer.openNested()) {
                    available =
                            insert
                                    ? storage.insert(request, probe)
                                    : storage.extract(request, probe);
                }
                long exact = available - available % feQuantum;
                if (exact <= 0) return 0;

                long moved = insert ? storage.insert(exact, outer) : storage.extract(exact, outer);
                if (moved != exact) return 0;
                if (!simulate) outer.commit();
                return exact;
            }
        }
    }

    static @Nullable IEnergyHandlerView findItem(Container container, int slot) {
        SingleSlotStorage<ItemVariant> backing = ContainerStorage.of(container, null).getSlot(slot);
        EnergyStorage storage = ContainerItemContext.ofSingleSlot(backing).find(EnergyStorage.ITEM);
        return storage == null ? null : new FabricEnergyHandlerView(storage);
    }
}
