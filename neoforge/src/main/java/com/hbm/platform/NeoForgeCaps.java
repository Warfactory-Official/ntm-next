// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.capability.*;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.InventoryUtil;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class NeoForgeCaps {
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

    public static void registerConductorEnergy(RegisterCapabilitiesEvent event) {
        List<Block> conductors = new ArrayList<>();
        for (RegistryHandle<? extends Block> handle : NeoForgeRegistrar.registeredBlocks) {
            Block block = handle.get();
            if (block instanceof CableBlock || block instanceof CableConductorBlockBase)
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

    public static void registerItems(RegisterCapabilitiesEvent event) {
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

    public static @Nullable MachineItemHandler coreItems(
            BlockEntityType<?> type, @Nullable BlockEntity be, @Nullable Direction side) {
        return be != null && be.getType() == type && be instanceof BlockEntityMachineBase m
                ? MachineItemHandler.of(m, side)
                : null;
    }

    public static @Nullable MachineItemHandler cellItems(
            Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        if (side == null) return null;
        BlockEntity owner = NtmCapabilities.itemOwnerAt(level, pos, state, side);
        if (!(owner instanceof BlockEntityMachineBase m)) return null;
        if (m instanceof IPortHost host) {
            ItemPort access = host.itemAccess(pos, side);
            if (access != null)
                return access.isEmpty() ? null : MachineItemHandler.of(m, side, access);
        }
        return MachineItemHandler.of(m, side);
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
                    if (side == null || !(be instanceof IEnergyHandlerMK2 e)) return null;
                    int bits = MachineCaps.declaredDomains(be);
                    IEnergyHandlerMK2 in =
                            (bits & MachineCaps.POWER_IN) != 0
                                            && MachineCaps.accepts(
                                                    be, side, NtmCapabilities.CapRole.POWER_IN)
                                    ? e
                                    : null;
                    IEnergyHandlerMK2 out =
                            (bits & MachineCaps.POWER_OUT) != 0
                                            && MachineCaps.accepts(
                                                    be, side, NtmCapabilities.CapRole.POWER_OUT)
                                    ? e
                                    : null;
                    return in == null && out == null
                            ? null
                            : new MachineEnergyHandler(e, be, in, out);
                });
    }

    public static <T, C> BlockLookupCache<T> createCache(
            BlockCapability<T, C> cap, ServerLevel level, BlockPos pos, @Nullable C context) {
        return new CapCache<>(cap, level, pos, context);
    }

    public static <T, C, BE extends BlockEntity> void registerProvider(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            ICapabilityProvider<? super BE, C, T> provider) {
        pending.add(new Registration<>(token, beType, provider));
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

    public static long extractable(ResourceHandler<FluidResource> store, Fluid fluid) {
        try (Transaction tx = Transaction.openRoot()) {
            return store.extract(FluidResource.of(fluid), Integer.MAX_VALUE, tx);
        }
    }

    public static long extract(ResourceHandler<FluidResource> store, Fluid fluid, long maxMb) {
        try (Transaction tx = Transaction.openRoot()) {
            int out = store.extract(FluidResource.of(fluid), clampInt(maxMb), tx);
            tx.commit();
            return out;
        }
    }

    public static long insertable(ResourceHandler<FluidResource> store, Fluid fluid) {
        try (Transaction tx = Transaction.openRoot()) {
            return store.insert(FluidResource.of(fluid), Integer.MAX_VALUE, tx);
        }
    }

    public static long insert(ResourceHandler<FluidResource> store, Fluid fluid, long maxMb) {
        try (Transaction tx = Transaction.openRoot()) {
            int in = store.insert(FluidResource.of(fluid), clampInt(maxMb), tx);
            tx.commit();
            return in;
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

    public static @Nullable EnergyHandler findEnergyStore(Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        return held.getCapability(Capabilities.Energy.ITEM, access);
    }

    public static long insertable(EnergyHandler store) {
        try (Transaction tx = Transaction.openRoot()) {
            return store.insert(Integer.MAX_VALUE, tx);
        }
    }

    public static long extractable(EnergyHandler store) {
        try (Transaction tx = Transaction.openRoot()) {
            return store.extract(Integer.MAX_VALUE, tx);
        }
    }

    public static long transfer(
            EnergyHandler store, long maxFe, long feQuantum, boolean simulate, boolean insert) {
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

    public static void exposeItem(Supplier<? extends BlockEntityType<?>> beType) {
        itemExposures.add(beType);
    }

    public static void exposeEnergy(Supplier<? extends BlockEntityType<?>> beType) {
        energyExposures.add(beType);
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
                        IEnergyHandlerMK2 receiver =
                                NtmCapabilities.feCap(
                                        level,
                                        pos,
                                        state,
                                        side,
                                        IEnergyHandlerMK2.class,
                                        NtmCapabilities.CapRole.POWER_IN);
                        IEnergyHandlerMK2 provider =
                                NtmCapabilities.feCap(
                                        level,
                                        pos,
                                        state,
                                        side,
                                        IEnergyHandlerMK2.class,
                                        NtmCapabilities.CapRole.POWER_OUT);
                        IEnergyHandlerMK2 machine = receiver != null ? receiver : provider;
                        if (machine == null) return null;
                        return new MachineEnergyHandler(
                                machine, (BlockEntity) machine, receiver, provider);
                    },
                    ((Supplier<? extends Block>) b).get());
        registerConductorEnergy(event);
        registerFluidBlocks(event);
        registerFluidItems(event);
        FluidContainerRowHandler.register(event);
        registerEnergyItems(event);
    }

    public static void registerEnergyItems(RegisterCapabilitiesEvent event) {
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

    public static void registerFluidItems(RegisterCapabilitiesEvent event) {
        List<Item> items = ForeignFluidItemAccess.containerItems();
        if (items.isEmpty()) return;
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, access) -> new ItemFluidHandler(access),
                items.toArray(new Item[0]));
    }

    public static final class CapCache<T, C> extends ResidentLookup<T> {
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
            return cache.getCapability();
        }
    }

    public record ItemCache(
            BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> cache)
            implements BlockLookupCache<ResourceHandler<ItemResource>> {
        @Override
        public @Nullable ResourceHandler<ItemResource> find() {
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
