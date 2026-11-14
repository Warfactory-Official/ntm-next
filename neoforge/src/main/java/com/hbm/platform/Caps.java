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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyProbe;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlock;
import com.hbm.capability.*;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.InventoryUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class Caps {
    public static final List<Registration<?, ?, ?>> pending = new ArrayList<>();
    public static final List<BlockRegistration<?, ?>> pendingBlocks = new ArrayList<>();
    public static final List<Supplier<? extends BlockEntityType<?>>> itemExposures =
            new ArrayList<>();
    public static final List<Supplier<? extends Block>> itemBlockExposures = new ArrayList<>();
    public static final List<Supplier<? extends BlockEntityType<?>>> energyExposures =
            new ArrayList<>();
    public static final List<Supplier<? extends Block>> energyBlockExposures = new ArrayList<>();
    public static final List<Supplier<? extends Block>> fluidExposures = new ArrayList<>();
    public static final CapabilityProviderLedger ledger = new CapabilityProviderLedger();

    public static <T, C> BlockLookupCache<T> createCache(
            BlockCapability<T, C> token, ServerLevel level, BlockPos pos, @Nullable C context) {
        return new CapCache<>(token, level, pos, context);
    }

    public static void invalidateCaps(Level level, BlockPos pos) {
        level.invalidateCapabilities(pos);
    }

    public static @Nullable Object listenForCapChanges(
            ServerLevel level, Iterable<BlockPos> positions, BooleanSupplier onInvalidate) {
        ICapabilityInvalidationListener listener = onInvalidate::getAsBoolean;
        for (BlockPos pos : positions) level.registerCapabilityListener(pos.immutable(), listener);
        return listener;
    }

    public static @Nullable ResourceHandler<FluidResource> findFluidStore(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findFluidStore(new SimpleContainer(stack.copyWithCount(1)), 0);
    }

    public static @Nullable ResourceHandler<FluidResource> findFluidStore(
            Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        return held.getCapability(Capabilities.Fluid.ITEM, access);
    }

    @SuppressWarnings("deprecation")
    public static @Nullable ItemStack drainFluidContainer(
            ItemStack stack, Fluid fluid, int amount) {
        SimpleContainer scratch = new SimpleContainer(stack.copyWithCount(1));
        ResourceHandler<FluidResource> store = findFluidStore(scratch, 0);
        if (store == null) return null;
        try (Transaction tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
            if (store.extract(FluidResource.of(fluid), amount, tx) != amount) return null;
            tx.commit();
        }
        return scratch.getItem(0).copy();
    }

    public static BlockLookupCache<ResourceHandler<FluidResource>> createFluidStoreCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new NativeCache<>(
                BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos, side));
    }

    public static long extractable(ResourceHandler<FluidResource> store, Fluid fluid) {
        return extractable(store, fluid, DataComponentPatch.EMPTY);
    }

    public static long insertable(ResourceHandler<FluidResource> store, Fluid fluid) {
        return insertable(store, fluid, DataComponentPatch.EMPTY);
    }

    @SuppressWarnings("deprecation")
    public static long extractable(
            ResourceHandler<FluidResource> store, Fluid fluid, DataComponentPatch components) {
        try (Transaction tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
            return store.extract(FluidResource.of(fluid, components), Integer.MAX_VALUE, tx);
        }
    }

    public static long extract(
            ResourceHandler<FluidResource> store,
            Fluid fluid,
            DataComponentPatch components,
            long maxMb) {
        try (Transaction tx = Transaction.openRoot()) {
            int out = store.extract(FluidResource.of(fluid, components), clampInt(maxMb), tx);
            tx.commit();
            return out;
        }
    }

    @SuppressWarnings("deprecation")
    public static long insertable(
            ResourceHandler<FluidResource> store, Fluid fluid, DataComponentPatch components) {
        try (Transaction tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
            return store.insert(FluidResource.of(fluid, components), Integer.MAX_VALUE, tx);
        }
    }

    public static long insert(
            ResourceHandler<FluidResource> store,
            Fluid fluid,
            DataComponentPatch components,
            long maxMb) {
        try (Transaction tx = Transaction.openRoot()) {
            int in = store.insert(FluidResource.of(fluid, components), clampInt(maxMb), tx);
            tx.commit();
            return in;
        }
    }

    public static void forEachComponents(
            ResourceHandler<FluidResource> store,
            Fluid fluid,
            Consumer<DataComponentPatch> visitor) {
        for (int index = 0, size = store.size(); index < size; index++) {
            FluidResource resource = store.getResource(index);
            if (resource.getFluid() == fluid
                    && !resource.isComponentsPatchEmpty()
                    && store.getAmountAsLong(index) > 0) {
                visitor.accept(resource.getComponentsPatch());
            }
        }
    }

    private static int clampInt(long amount) {
        return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    public static @Nullable ResourceHandler<ItemResource> findItemStore(
            Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<ItemResource> handler =
                level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler != null ? handler : containerHandler(level, pos, side);
    }

    public static BlockLookupCache<ResourceHandler<ItemResource>> createItemStoreCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new ItemCache(
                BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, pos, side));
    }

    public static ItemStack insert(ResourceHandler<ItemResource> store, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = store.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return moved >= stack.getCount()
                    ? ItemStack.EMPTY
                    : stack.copyWithCount(stack.getCount() - moved);
        }
    }

    public static @Nullable ItemStack insertIf(
            ResourceHandler<ItemResource> store,
            ItemStack stack,
            Predicate<ItemStack> acceptRemainder) {
        if (stack.isEmpty()) return acceptRemainder.test(ItemStack.EMPTY) ? ItemStack.EMPTY : null;
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = store.insert(ItemResource.of(stack), stack.getCount(), tx);
            int remaining = stack.getCount() - accepted;
            ItemStack remainder = remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
            if (!acceptRemainder.test(remainder)) return null;
            tx.commit();
            return remainder;
        }
    }

    public static ItemStack extract(
            ResourceHandler<ItemResource> store, ToIntFunction<ItemStack> request) {
        for (int index = 0; index < store.size(); index++) {
            ItemResource resource = store.getResource(index);
            int amount = store.getAmountAsInt(index);
            if (resource.isEmpty() || amount <= 0) continue;
            int wanted = request.applyAsInt(resource.toStack(amount));
            if (wanted == 0) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int moved = store.extract(index, resource, Math.min(wanted, amount), tx);
                tx.commit();
                if (moved > 0) return resource.toStack(moved);
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<ItemStack> contents(ResourceHandler<ItemResource> store) {
        List<ItemStack> out = new ArrayList<>();
        for (int index = 0; index < store.size(); index++) {
            ItemResource resource = store.getResource(index);
            int amount = store.getAmountAsInt(index);
            if (resource.isEmpty() || amount <= 0) continue;
            out.add(resource.toStack(amount));
        }
        return out;
    }

    public static int move(
            ResourceHandler<ItemResource> source,
            ResourceHandler<ItemResource> dest,
            ItemStack stack,
            int amount) {
        ItemResource resource = ItemResource.of(stack);
        return ResourceHandlerUtil.move(
                source, dest, candidate -> candidate.equals(resource), amount, null);
    }

    private static @Nullable ResourceHandler<ItemResource> containerHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        Container container = InventoryUtil.containerAt(level, pos);
        if (container == null) return null;
        return side != null && container instanceof WorldlyContainer worldly
                ? new WorldlyContainerWrapper(worldly, side)
                : VanillaContainerWrapper.of(container);
    }

    public static net.neoforged.neoforge.transfer.energy.@Nullable EnergyHandler findEnergyStore(
            Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        return held.getCapability(Capabilities.Energy.ITEM, access);
    }

    public static BlockLookupCache<net.neoforged.neoforge.transfer.energy.EnergyHandler>
            createEnergyStoreCache(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new NativeCache<>(
                BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos, side));
    }

    public static long insertable(net.neoforged.neoforge.transfer.energy.EnergyHandler store) {
        try (Transaction tx = Transaction.openRoot()) {
            return store.insert(Integer.MAX_VALUE, tx);
        }
    }

    public static long extractable(net.neoforged.neoforge.transfer.energy.EnergyHandler store) {
        try (Transaction tx = Transaction.openRoot()) {
            return store.extract(Integer.MAX_VALUE, tx);
        }
    }

    public static void probeEnergy(
            net.neoforged.neoforge.transfer.energy.EnergyHandler store, EnergyProbe result) {
        int input;
        int output;
        int directions = 0;
        try (Transaction transaction = Transaction.openRoot()) {
            try (Transaction probe = Transaction.open(transaction)) {
                input = store.insert(Integer.MAX_VALUE, probe);
                if (input > 0) directions |= EnergyProbe.RECEIVE;
                if (store.extract(Integer.MAX_VALUE, probe) > 0) directions |= EnergyProbe.SEND;
            }
            try (Transaction probe = Transaction.open(transaction)) {
                output = store.extract(Integer.MAX_VALUE, probe);
                if (output > 0) directions |= EnergyProbe.SEND;
                if (store.insert(Integer.MAX_VALUE, probe) > 0) directions |= EnergyProbe.RECEIVE;
            }
        }
        result.energyProbe(input, output, directions);
    }

    public static long transfer(
            net.neoforged.neoforge.transfer.energy.EnergyHandler store,
            long maxFe,
            long feQuantum,
            boolean simulate,
            boolean insert) {
        assert feQuantum > 0;
        long request = Math.min(maxFe, Integer.MAX_VALUE);
        request -= request % feQuantum;
        if (request <= 0) return 0;

        try (Transaction outer = Transaction.openRoot()) {
            long available;
            try (Transaction probe = Transaction.open(outer)) {
                available =
                        insert
                                ? store.insert((int) request, probe)
                                : store.extract((int) request, probe);
            }
            long exact = available - available % feQuantum;
            if (exact <= 0) return 0;

            long moved =
                    insert ? store.insert((int) exact, outer) : store.extract((int) exact, outer);
            if (moved != exact) return 0;
            if (!simulate) outer.commit();
            return exact;
        }
    }

    private static void registerConductorEnergy(RegisterCapabilitiesEvent event) {
        List<Block> conductors = new ArrayList<>();
        for (RegistryHandle<? extends Block> handle : Registrar.registeredBlocks) {
            Block block = handle.get();
            if (block instanceof CableBlock || block instanceof CableConductorBlock)
                conductors.add(block);
        }
        if (conductors.isEmpty()) return;
        registerBlock(
                event,
                Capabilities.Energy.BLOCK,
                (level, pos, state, be, side) ->
                        level instanceof ServerLevel sl
                                ? new ConductorEnergyHandler(sl, pos, side)
                                : null,
                conductors.toArray(new Block[0]));
    }

    public static <T, C> void registerBlock(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, C> cap,
            IBlockCapabilityProvider<T, C> provider,
            Block... blocks) {
        ledger.blocks(cap.name(), blocks);
        event.registerBlock(cap, provider, blocks);
    }

    public static <T, C, BE extends BlockEntity> void registerBlockEntity(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, C> cap,
            BlockEntityType<BE> type,
            ICapabilityProvider<? super BE, C, T> provider) {
        ledger.blockEntity(cap.name(), type);
        event.registerBlockEntity(cap, type, provider);
    }

    private static void registerItems(RegisterCapabilitiesEvent event) {
        Map<Block, BlockEntityType<?>> coreTypes = new IdentityHashMap<>();
        for (Supplier<? extends BlockEntityType<?>> type : itemExposures) {
            for (Block block : type.get().getValidBlocks()) coreTypes.put(block, type.get());
        }
        Set<Block> cellBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Supplier<? extends Block> block : itemBlockExposures) cellBlocks.add(block.get());
        for (Supplier<? extends BlockEntityType<?>> typeSupplier : itemExposures) {
            BlockEntityType<?> type = typeSupplier.get();
            Block[] coreOnly =
                    type.getValidBlocks().stream()
                            .filter(block -> !cellBlocks.contains(block))
                            .toArray(Block[]::new);
            if (coreOnly.length == 0) continue;
            registerBlock(
                    event,
                    Capabilities.Item.BLOCK,
                    (level, pos, state, be, side) -> coreItems(type, be, side),
                    coreOnly);
        }
        for (Supplier<? extends Block> blockSupplier : itemBlockExposures) {
            BlockEntityType<?> type = coreTypes.get(blockSupplier.get());
            registerBlock(
                    event,
                    Capabilities.Item.BLOCK,
                    (level, pos, state, be, side) -> {
                        MachineItemHandler core = type == null ? null : coreItems(type, be, side);
                        return core != null ? core : cellItems(level, pos, state, side);
                    },
                    blockSupplier.get());
        }
    }

    private static @Nullable MachineItemHandler coreItems(
            BlockEntityType<?> type, @Nullable BlockEntity be, @Nullable Direction side) {
        return be != null && be.getType() == type && be instanceof WorldlyContainer
                ? MachineItemHandler.of(be, side)
                : null;
    }

    private static @Nullable MachineItemHandler cellItems(
            Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        if (side == null) return null;
        BlockEntity owner = NtmCapabilities.itemOwnerAt(level, pos, state, side);
        if (!(owner instanceof WorldlyContainer)) return null;
        if (owner instanceof CapabilityPortProvider host) {
            ItemPort access = host.itemAccess(pos, side);
            if (access != null)
                return access.isEmpty() ? null : MachineItemHandler.of(owner, side, access);
        }
        return MachineItemHandler.of(owner, side);
    }

    @SuppressWarnings("unchecked")
    public static void registerEnergy0(
            RegisterCapabilitiesEvent event, Supplier<? extends BlockEntityType<?>> typeSupplier) {
        BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) typeSupplier.get();
        registerBlockEntity(
                event,
                Capabilities.Energy.BLOCK,
                type,
                (be, side) -> {
                    if (side == null || !(be instanceof EnergyHandler e)) return null;
                    int bits = MachineCaps.declaredDomains(be);
                    EnergyHandler in =
                            (bits & MachineCaps.POWER_IN) != 0
                                            && MachineCaps.accepts(
                                                    be, side, NtmCapabilities.CapRole.POWER_IN)
                                    ? e
                                    : null;
                    EnergyHandler out =
                            (bits & MachineCaps.POWER_OUT) != 0
                                            && MachineCaps.accepts(
                                                    be, side, NtmCapabilities.CapRole.POWER_OUT)
                                    ? e
                                    : null;
                    return in == null && out == null
                            ? null
                            : new MachineEnergyHandler(e, be, in, out, be.getBlockPos(), side);
                });
    }

    public static void flush(RegisterCapabilitiesEvent event) {
        for (Registration<?, ?, ?> r : pending) r.register(event);
        for (BlockRegistration<?, ?> r : pendingBlocks) r.register(event);
        registerItems(event);
        for (Supplier<? extends BlockEntityType<?>> t : energyExposures) registerEnergy0(event, t);
        for (Supplier<? extends Block> b : energyBlockExposures)
            registerBlock(
                    event,
                    Capabilities.Energy.BLOCK,
                    (level, pos, state, be, side) -> {
                        if (side == null || !MultiblockSurface.isSurface(state)) return null;
                        EnergyHandler receiver =
                                NtmCapabilities.feCap(
                                        level,
                                        pos,
                                        state,
                                        side,
                                        EnergyHandler.class,
                                        NtmCapabilities.CapRole.POWER_IN);
                        EnergyHandler provider =
                                NtmCapabilities.feCap(
                                        level,
                                        pos,
                                        state,
                                        side,
                                        EnergyHandler.class,
                                        NtmCapabilities.CapRole.POWER_OUT);
                        EnergyHandler machine = receiver != null ? receiver : provider;
                        if (machine == null) return null;
                        return new MachineEnergyHandler(
                                machine, (BlockEntity) machine, receiver, provider, pos, side);
                    },
                    ((Supplier<? extends Block>) b).get());
        registerConductorEnergy(event);
        registerFluidBlocks(event);
        registerFluidItems(event);
        FluidContainerRowHandler.register(event);
        registerEnergyItems(event);
    }

    private static void registerEnergyItems(RegisterCapabilitiesEvent event) {
        List<Item> batteries = ForeignEnergyItemAccess.batteryItems();
        if (batteries.isEmpty()) return;
        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, access) -> new BatteryEnergyHandler(access),
                batteries.toArray(new Item[0]));
    }

    public static void registerFluidBlocks(RegisterCapabilitiesEvent event) {
        if (fluidExposures.isEmpty()) return;
        Block[] blocks = fluidExposures.stream().map(Supplier::get).toArray(Block[]::new);
        registerBlock(
                event,
                Capabilities.Fluid.BLOCK,
                (level, pos, state, be, side) -> MachineFluidHandler.at(level, pos, side),
                blocks);
    }

    private static void registerFluidItems(RegisterCapabilitiesEvent event) {
        List<Item> items = ForeignFluidItemAccess.containerItems();
        if (items.isEmpty()) return;
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, access) -> new ItemFluidHandler(access),
                items.toArray(new Item[0]));
    }

    private static final class CapCache<T, C> extends ResidentLookup<T> {
        private final BlockCapability<T, C> cap;
        private final BlockPos pos;
        private final @Nullable C context;
        private final BlockCapabilityCache<T, C> cache;

        public CapCache(
                BlockCapability<T, C> cap, ServerLevel level, BlockPos pos, @Nullable C context) {
            super(level);
            this.cap = cap;
            this.pos = pos;
            this.context = context;
            this.cache =
                    BlockCapabilityCache.create(
                            cap, level, pos, context, () -> true, this::invalidate);
        }

        @Override
        protected @Nullable T lookup() {
            if (MultiblockCoreBlock.readableChunk(level, pos.getX(), pos.getZ()) == null)
                return null;
            return cache.getCapability();
        }

        @Override
        protected @Nullable T fresh(T found) {
            return level.getCapability(cap, pos, context);
        }
    }

    public record NativeCache<T>(BlockCapabilityCache<T, @Nullable Direction> cache)
            implements BlockLookupCache<T> {
        @Override
        public @Nullable T find() {
            if (MultiblockCoreBlock.readableChunk(
                            cache.level(), cache.pos().getX(), cache.pos().getZ())
                    == null) return null;
            return cache.getCapability();
        }
    }

    public record ItemCache(
            BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> cache)
            implements BlockLookupCache<ResourceHandler<ItemResource>> {
        @Override
        public @Nullable ResourceHandler<ItemResource> find() {
            if (MultiblockCoreBlock.readableChunk(
                            cache.level(), cache.pos().getX(), cache.pos().getZ())
                    == null) return null;
            ResourceHandler<ItemResource> handler = cache.getCapability();
            return handler != null
                    ? handler
                    : containerHandler(cache.level(), cache.pos(), cache.context());
        }
    }

    public record Registration<T, C, BE extends BlockEntity>(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            ICapabilityProvider<? super BE, C, T> provider) {
        public void register(RegisterCapabilitiesEvent event) {
            registerBlockEntity(event, token, beType.get(), provider);
        }
    }

    public record BlockRegistration<T, C>(
            BlockCapability<T, C> token,
            Supplier<? extends Block> block,
            IBlockCapabilityProvider<T, C> provider) {
        public void register(RegisterCapabilitiesEvent event) {
            registerBlock(event, token, provider, block.get());
        }
    }
}
