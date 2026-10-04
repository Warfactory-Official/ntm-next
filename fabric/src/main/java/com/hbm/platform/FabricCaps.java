// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.capability.port.PortContainerView;
import com.hbm.fabric.platform.CapCacheIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class FabricCaps {
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

    public static <T, C> @Nullable T find(
            BlockApiLookup<T, C> token, Level level, BlockPos pos, @Nullable C context) {
        return token.find(level, pos, context);
    }

    public static <T, C> BlockLookupCache<T> createCache(
            BlockApiLookup<T, C> token, ServerLevel level, BlockPos pos, @Nullable C context) {
        BlockApiCache<T, C> cache = BlockApiCache.create(token, level, pos);
        MemoCache<T, C> memo = new MemoCache<>(level, pos, context, cache);
        level.hbm$capCaches().register(pos, memo);
        return memo;
    }

    public static BlockLookupCache<IFluidHandlerView> createFluidHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new FluidViewCache(
                level, pos, side, BlockApiCache.create(FluidStorage.SIDED, level, pos));
    }

    public static BlockLookupCache<IEnergyHandlerView> createEnergyHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return FabricTrEnergy.AVAILABLE
                ? FabricTrEnergyBridge.createCache(level, pos, side)
                : () -> null;
    }

    public static <T, C, BE extends BlockEntity> void registerProvider(
            BlockApiLookup<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper) {
        pending.add(new Registration<>(token, beType, mapper));
    }

    public static void invalidateCaps(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) server.hbm$capCaches().invalidate(pos.asLong());
    }

    public static @Nullable Object listenForCapChanges(
            ServerLevel level, Iterable<BlockPos> positions, BooleanSupplier onInvalidate) {
        return null;
    }

    public static @Nullable IFluidHandlerView findFluidHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FabricFluidHandlerView(storage);
    }

    public static @Nullable IFluidHandlerView findFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findFluidHandler(new SimpleContainer(stack.copyWithCount(1)), 0);
    }

    public static @Nullable IFluidHandlerView findFluidHandler(Container container, int slot) {
        if (container.getItem(slot).isEmpty()) return null;
        SingleSlotStorage<ItemVariant> backing = ContainerStorage.of(container, null).getSlot(slot);
        Storage<FluidVariant> storage =
                ContainerItemContext.ofSingleSlot(backing).find(FluidStorage.ITEM);
        return storage == null ? null : new FabricFluidHandlerView(storage);
    }

    public static @Nullable IEnergyHandlerView findEnergyHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        return FabricTrEnergy.AVAILABLE ? FabricTrEnergyBridge.find(level, pos, side) : null;
    }

    public static @Nullable IEnergyHandlerView findEnergyHandler(Container container, int slot) {
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

    public static final class MemoCache<T, C>
            implements BlockLookupCache<T>, CapCacheIndex.Invalidatable {

        private final ServerLevel level;
        private final BlockPos pos;
        private final @Nullable C side;
        private final BlockApiCache<T, C> cache;
        private @Nullable T memo;
        private @Nullable LevelChunk memoChunk;

        public MemoCache(
                ServerLevel level, BlockPos pos, @Nullable C side, BlockApiCache<T, C> cache) {
            this.level = level;
            this.pos = pos;
            this.side = side;
            this.cache = cache;
        }

        public @Nullable T find() {
            T cached = memo;
            if (cached != null
                    && !((BlockEntity) cached).isRemoved()
                    && memoChunk != null
                    && memoChunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) {
                return cached;
            }
            T found = level.isLoaded(pos) ? cache.find(side) : null;
            if (found instanceof BlockEntity be) {
                memo = found;
                memoChunk =
                        BlockMultiblockCore.readableChunk(
                                level, be.getBlockPos().getX(), be.getBlockPos().getZ());
                if (memoChunk == null) memo = null;
            } else {
                memo = null;
                memoChunk = null;
            }
            return found;
        }

        public void hbm$invalidate() {
            memo = null;
            memoChunk = null;
        }
    }

    public static final class FluidViewCache implements BlockLookupCache<IFluidHandlerView> {
        private final ServerLevel level;
        private final BlockPos pos;
        private final @Nullable Direction side;
        private final BlockApiCache<Storage<FluidVariant>, @Nullable Direction> cache;
        private @Nullable Storage<FluidVariant> last;
        private @Nullable IFluidHandlerView view;

        public FluidViewCache(
                ServerLevel level,
                BlockPos pos,
                @Nullable Direction side,
                BlockApiCache<Storage<FluidVariant>, @Nullable Direction> cache) {
            this.level = level;
            this.pos = pos;
            this.side = side;
            this.cache = cache;
        }

        public @Nullable IFluidHandlerView find() {
            if (!level.isLoaded(pos)) return null;
            Storage<FluidVariant> storage = cache.find(side);
            if (storage == null) return null;
            if (storage != last) {
                last = storage;
                view = new FabricFluidHandlerView(storage);
            }
            return view;
        }
    }

    public record Registration<T, C, BE extends BlockEntity>(
            BlockApiLookup<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper) {
        public void register() {
            registerForBlockEntity(token, (be, context) -> mapper.apply(be, context), beType.get());
        }
    }

    public record BlockRegistration<T, C>(
            BlockApiLookup<T, C> token,
            Supplier<? extends Block> block,
            BlockCapMapper<T, C> mapper) {
        public void register() {
            registerForBlocks(
                    token,
                    (level, pos, state, be, context) -> mapper.map(level, pos, state, be, context),
                    block.get());
        }
    }
}
