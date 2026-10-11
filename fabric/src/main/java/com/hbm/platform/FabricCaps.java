// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.capability.port.PortContainerView;
import com.hbm.uninos.graph.LevelNodeGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

public final class FabricCaps {
    private static final long DROPLETS_PER_MB = 81L;
    private static final long MAX_MB = Long.MAX_VALUE / DROPLETS_PER_MB;

    public static final List<Registration<?, ?, ?>> pending = new ArrayList<>();
    public static final List<Supplier<? extends Block>> pendingItemBlocks = new ArrayList<>();
    public static final List<BlockRegistration<?, ?>> pendingBlocks = new ArrayList<>();
    public static final List<Supplier<? extends BlockEntityType<?>>> energyTypes =
            new ArrayList<>();
    public static final List<Supplier<? extends Block>> energyBlocks = new ArrayList<>();
    public static final List<Supplier<? extends Block>> fluidBlocks = new ArrayList<>();
    public static final CapabilityProviderLedger ledger = new CapabilityProviderLedger();
    public static boolean flushed;

    public static void onChunkLoaded(ServerLevel level, long chunkKey) {
        level.hbm$capCaches().invalidateChunk(chunkKey);
    }

    public static <A, C> void registerForBlocks(
            BlockApiLookup<A, C> lookup,
            BlockApiLookup.BlockApiProvider<A, C> provider,
            Block... blocks) {
        ledger.blocks(lookup.getId(), blocks);
        lookup.registerForBlocks(provider, blocks);
    }

    public static <A, C, BE extends BlockEntity> void registerForBlockEntity(
            BlockApiLookup<A, C> lookup,
            BiFunction<? super BE, C, @Nullable A> provider,
            BlockEntityType<BE> type) {
        ledger.blockEntity(lookup.getId(), type);
        lookup.registerForBlockEntity(provider, type);
    }

    public static <T, C> BlockLookupCache<T> createCache(
            BlockApiLookup<T, C> token, ServerLevel level, BlockPos pos, @Nullable C context) {
        MemoCache<T> memo =
                new MemoCache<>(
                        new ApiCache<>(
                                level, pos, context, BlockApiCache.create(token, level, pos)));
        level.hbm$capCaches().register(pos, memo);
        return memo;
    }

    public static <T, C, BE extends BlockEntity> void registerProvider(
            BlockApiLookup<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> provider) {
        pending.add(new Registration<>(token, beType, provider));
    }

    public static void invalidateCaps(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.hbm$capCaches().invalidate(pos.asLong());
            LevelNodeGraph.invalidateTransportAround(server, pos);
        }
    }

    public static @Nullable Object listenForCapChanges(
            ServerLevel level, Iterable<BlockPos> positions, BooleanSupplier onInvalidate) {
        return null;
    }

    public static @Nullable Storage<FluidVariant> findFluidStore(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findFluidStore(new SimpleContainer(stack.copyWithCount(1)), 0);
    }

    public static @Nullable Storage<FluidVariant> findFluidStore(Container container, int slot) {
        if (container.getItem(slot).isEmpty()) return null;
        SingleSlotStorage<ItemVariant> backing = ContainerStorage.of(container, null).getSlot(slot);
        return ContainerItemContext.ofSingleSlot(backing).find(FluidStorage.ITEM);
    }

    public static long extractable(Storage<FluidVariant> store, Fluid fluid) {
        try (Transaction tx = Transaction.openOuter()) {
            return store.extract(FluidVariant.of(fluid), Long.MAX_VALUE, tx) / DROPLETS_PER_MB;
        }
    }

    public static long extract(Storage<FluidVariant> store, Fluid fluid, long maxMb) {
        return moveWholeMb(store, FluidVariant.of(fluid), maxMb, false);
    }

    public static long insertable(Storage<FluidVariant> store, Fluid fluid) {
        try (Transaction tx = Transaction.openOuter()) {
            return store.insert(FluidVariant.of(fluid), Long.MAX_VALUE, tx) / DROPLETS_PER_MB;
        }
    }

    public static long insert(Storage<FluidVariant> store, Fluid fluid, long maxMb) {
        return moveWholeMb(store, FluidVariant.of(fluid), maxMb, true);
    }

    public static long moveWholeMb(
            Storage<FluidVariant> store, FluidVariant variant, long maxMb, boolean insert) {
        long offered = Math.min(maxMb, MAX_MB) * DROPLETS_PER_MB;
        if (offered <= 0) return 0L;
        try (Transaction outer = Transaction.openOuter()) {
            long probed;
            try (Transaction probe = outer.openNested()) {
                probed =
                        insert
                                ? store.insert(variant, offered, probe)
                                : store.extract(variant, offered, probe);
            }
            long whole = probed / DROPLETS_PER_MB * DROPLETS_PER_MB;
            if (whole <= 0) return 0L;
            long moved =
                    insert
                            ? store.insert(variant, whole, outer)
                            : store.extract(variant, whole, outer);
            if (moved != whole) return 0L;
            outer.commit();
            return moved / DROPLETS_PER_MB;
        }
    }

    public static BlockLookupCache<Storage<ItemVariant>> createItemStoreCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new ApiCache<>(
                level, pos, side, BlockApiCache.create(ItemStorage.SIDED, level, pos));
    }

    public static ItemStack insert(Storage<ItemVariant> store, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = store.insert(ItemVariant.of(stack), stack.getCount(), tx);
            tx.commit();
            return moved >= stack.getCount()
                    ? ItemStack.EMPTY
                    : stack.copyWithCount(stack.getCount() - (int) moved);
        }
    }

    public static ItemStack extract(Storage<ItemVariant> store, ToIntFunction<ItemStack> request) {
        for (StorageView<ItemVariant> view : store) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            ItemVariant resource = view.getResource();
            ItemStack held = resource.toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
            int wanted = request.applyAsInt(held);
            if (wanted == 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long moved = view.extract(resource, Math.min(wanted, held.getCount()), tx);
                if (moved > 0) {
                    tx.commit();
                    return resource.toStack((int) moved);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<ItemStack> contents(Storage<ItemVariant> store) {
        List<ItemStack> out = new ArrayList<>();
        for (StorageView<ItemVariant> view : store) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            out.add(
                    view.getResource()
                            .toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE)));
        }
        return out;
    }

    public static int move(
            Storage<ItemVariant> source, Storage<ItemVariant> dest, ItemStack stack, int amount) {
        ItemVariant resource = ItemVariant.of(stack);
        return (int)
                StorageUtil.move(
                        source, dest, candidate -> candidate.equals(resource), amount, null);
    }

    public static @Nullable EnergyStorage findEnergyStore(Container container, int slot) {
        if (!FabricTrEnergy.AVAILABLE || container.getItem(slot).isEmpty()) return null;
        return FabricTrEnergyBridge.findItem(container, slot);
    }

    public static void exposeItem(Supplier<? extends BlockEntityType<?>> beType) {}

    public static void exposeEnergy(Supplier<? extends BlockEntityType<?>> beType) {
        energyTypes.add(beType);
    }

    public static void flush() {
        if (flushed) return;
        flushed = true;
        FabricServerEvents.onChunkLoad(
                (level, chunk) -> onChunkLoaded(level, chunk.getPos().pack()));
        FabricServerEvents.onChunkUnload(
                (level, chunk) -> onChunkLoaded(level, chunk.getPos().pack()));
        for (Registration<?, ?, ?> r : pending) r.register();
        for (BlockRegistration<?, ?> r : pendingBlocks) r.register();
        for (Supplier<? extends Block> b : pendingItemBlocks) {
            registerForBlocks(
                    ItemStorage.SIDED,
                    (level, pos, state, be, side) -> {
                        if (side == null) return null;
                        BlockEntity core = NtmCapabilities.itemOwnerAt(level, pos, state, side);
                        if (!(core instanceof WorldlyContainer wc)) return null;
                        if (core instanceof IPortHost host) {
                            ItemPort access = host.itemAccess(pos, side);
                            if (access != null) {
                                return access.isEmpty()
                                        ? null
                                        : ContainerStorage.of(
                                                new PortContainerView(wc, access), side);
                            }
                        }
                        return ContainerStorage.of(wc, side);
                    },
                    b.get());
        }
        if (FabricTrEnergy.AVAILABLE) FabricTrEnergyOutbound.register(energyTypes, energyBlocks);
        FabricFluidOutbound.register(fluidBlocks);
        FabricFluidItemStorage.register();
        FabricFluidContainerRows.register();
    }

    public static final class MemoCache<T> extends ResidentLookup<T> {
        private final ApiCache<T, ?> api;

        public MemoCache(ApiCache<T, ?> api) {
            super(api.level());
            this.api = api;
        }

        @Override
        protected @Nullable T lookup() {
            return api.find();
        }

        @Override
        protected T fresh(T found) {
            return found;
        }
    }

    public record ApiCache<T, C>(
            ServerLevel level, BlockPos pos, @Nullable C context, BlockApiCache<T, C> cache)
            implements BlockLookupCache<T> {
        @Override
        public @Nullable T find() {
            return level.isLoaded(pos) ? cache.find(context) : null;
        }
    }

    public static final class Absent<T> implements BlockLookupCache<T> {
        @Override
        public @Nullable T find() {
            return null;
        }
    }

    public record Registration<T, C, BE extends BlockEntity>(
            BlockApiLookup<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> provider) {
        public void register() {
            registerForBlockEntity(token, provider, beType.get());
        }
    }

    public record BlockRegistration<T, C>(
            BlockApiLookup<T, C> token,
            Supplier<? extends Block> block,
            BlockApiLookup.BlockApiProvider<T, C> provider) {
        public void register() {
            registerForBlocks(token, provider, block.get());
        }
    }
}
