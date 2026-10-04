// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.capability.*;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
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

    public static boolean isRemoteCoreSurface(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        if (side == null || !level.isLoaded(pos)) return false;
        var state = level.getBlockState(pos);
        if (MultiblockSurface.isSurface(state)) {
            return MultiblockSurface.isOpen(
                    level, pos, state, side, BlockMultiblockCore.PASSIVE_ANY);
        }
        return state.getBlock() instanceof BlockPWR
                        && state.getValue(BlockPWR.PART) == BlockPWR.Part.PORT
                || state.getBlock() instanceof BlockICF
                        && state.getValue(BlockICF.PART) == BlockICF.Part.PORT;
    }

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
                (be, side) ->
                        side != null
                                        && be instanceof IEnergyHandlerMK2 e
                                        && MachineCaps.acceptsFace(be, side)
                                ? new MachineEnergyHandler(e, be, side)
                                : null);
    }

    public static <T, C> @Nullable T find(
            BlockCapability<T, C> token, Level level, BlockPos pos, @Nullable C context) {
        return level.getCapability(token, pos, context);
    }

    public static <T, C> BlockLookupCache<T> createCache(
            BlockCapability<T, C> cap, ServerLevel level, BlockPos pos, @Nullable C context) {
        return new CapCache<>(
                cap, level, pos, context, BlockCapabilityCache.create(cap, level, pos, context));
    }

    public static BlockLookupCache<IFluidHandlerView> createFluidHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new FluidViewCache(
                BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos, side));
    }

    public static BlockLookupCache<IEnergyHandlerView> createEnergyHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new EnergyViewCache(
                BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos, side));
    }

    public static <T, C, BE extends BlockEntity> void registerProvider(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper) {
        pending.add(new Registration<>(token, beType, mapper));
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

    public static @Nullable IFluidHandlerView findFluidHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<FluidResource> handler =
                level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler == null ? null : new NeoForgeFluidHandlerView(handler);
    }

    public static @Nullable IFluidHandlerView findFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findFluidHandler(new SimpleContainer(stack.copyWithCount(1)), 0);
    }

    public static @Nullable IFluidHandlerView findFluidHandler(Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        ResourceHandler<FluidResource> handler =
                held.getCapability(Capabilities.Fluid.ITEM, access);
        return handler == null ? null : new NeoForgeFluidHandlerView(handler);
    }

    public static @Nullable IEnergyHandlerView findEnergyHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler == null ? null : new NeoForgeEnergyHandlerView(handler);
    }

    public static @Nullable IEnergyHandlerView findEnergyHandler(Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        EnergyHandler handler = held.getCapability(Capabilities.Energy.ITEM, access);
        return handler == null ? null : new NeoForgeEnergyHandlerView(handler);
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
                                machine, (BlockEntity) machine, side, receiver, provider);
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

    public static final class CapCache<T, C> implements BlockLookupCache<T> {
        private final BlockCapability<T, C> cap;
        private final ServerLevel level;
        private final BlockPos pos;
        private final @Nullable C context;
        private final BlockCapabilityCache<T, C> cache;
        private boolean probing;

        public CapCache(
                BlockCapability<T, C> cap,
                ServerLevel level,
                BlockPos pos,
                @Nullable C context,
                BlockCapabilityCache<T, C> cache) {
            this.cap = cap;
            this.level = level;
            this.pos = pos;
            this.context = context;
            this.cache = cache;
        }

        public @Nullable T find() {
            T result = cache.getCapability();
            if (result instanceof BlockEntity be) {
                var chunk =
                        BlockMultiblockCore.readableChunk(
                                level, be.getBlockPos().getX(), be.getBlockPos().getZ());
                if (chunk == null || !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) {
                    probing = true;
                    return null;
                }
            }
            if (result != null && !(result instanceof BlockEntity)) {
                return level.getCapability(cap, pos, context);
            }
            if (result instanceof BlockEntity be && be.isRemoved()) probing = true;
            if (result == null && isRemoteCoreSurface(level, pos, FacedContext.sideOf(context)))
                probing = true;
            if (!probing) return result;
            level.invalidateCapabilities(pos);
            result = cache.getCapability();
            if (result == null || (result instanceof BlockEntity be && be.isRemoved())) return null;
            probing = false;
            return result;
        }
    }

    public static final class FluidViewCache implements BlockLookupCache<IFluidHandlerView> {
        private final BlockCapabilityCache<ResourceHandler<FluidResource>, @Nullable Direction>
                cache;
        private @Nullable ResourceHandler<FluidResource> last;
        private @Nullable IFluidHandlerView view;

        public FluidViewCache(
                BlockCapabilityCache<ResourceHandler<FluidResource>, @Nullable Direction> cache) {
            this.cache = cache;
        }

        public @Nullable IFluidHandlerView find() {
            ResourceHandler<FluidResource> handler = cache.getCapability();
            if (handler == null) return null;
            if (handler != last) {
                last = handler;
                view = new NeoForgeFluidHandlerView(handler);
            }
            return view;
        }
    }

    public static final class EnergyViewCache implements BlockLookupCache<IEnergyHandlerView> {
        private final BlockCapabilityCache<EnergyHandler, @Nullable Direction> cache;
        private @Nullable EnergyHandler last;
        private @Nullable IEnergyHandlerView view;

        public EnergyViewCache(BlockCapabilityCache<EnergyHandler, @Nullable Direction> cache) {
            this.cache = cache;
        }

        public @Nullable IEnergyHandlerView find() {
            EnergyHandler handler = cache.getCapability();
            if (handler == null) return null;
            if (handler != last) {
                last = handler;
                view = new NeoForgeEnergyHandlerView(handler);
            }
            return view;
        }
    }

    public record Registration<T, C, BE extends BlockEntity>(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper) {
        public void register(RegisterCapabilitiesEvent event) {
            registerBlockEntity(
                    event, token, beType.get(), (be, context) -> mapper.apply(be, context));
        }
    }

    public record BlockRegistration<T, C>(
            BlockCapability<T, C> token,
            Supplier<? extends Block> block,
            BlockCapMapper<T, C> mapper) {
        public void register(RegisterCapabilitiesEvent event) {
            registerBlock(
                    event,
                    token,
                    (level, pos, state, be, context) -> mapper.map(level, pos, state, be, context),
                    block.get());
        }
    }
}
