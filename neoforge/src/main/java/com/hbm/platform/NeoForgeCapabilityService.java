// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.capability.*;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.lib.Library;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.FullChunkStatus;
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
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import org.jspecify.annotations.Nullable;

public final class NeoForgeCapabilityService implements ICapabilityService {

    private final List<Registration<?, ?, ?>> pending = new ArrayList<>();
    private final List<BlockRegistration<?, ?>> pendingBlocks = new ArrayList<>();
    private final List<Supplier<? extends BlockEntityType<?>>> itemExposures = new ArrayList<>();
    private final List<Supplier<? extends Block>> itemBlockExposures = new ArrayList<>();
    private final List<Supplier<? extends BlockEntityType<?>>> energyExposures = new ArrayList<>();
    private final List<Supplier<? extends Block>> energyBlockExposures = new ArrayList<>();
    private final List<Supplier<? extends Block>> fluidExposures = new ArrayList<>();
    private final CapabilityProviderLedger ledger = new CapabilityProviderLedger();

    private final Reference2ObjectOpenHashMap<
                    ServerLevel, Long2ObjectOpenHashMap<ICapabilityInvalidationListener>>
            foreignCapWatches = new Reference2ObjectOpenHashMap<>();

    private static boolean isRemoteCoreSurface(
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

    private void registerConductorEnergy(RegisterCapabilitiesEvent event) {
        List<Block> conductors = new ArrayList<>();
        for (RegistryHandle<? extends Block> handle : Services.REGISTRAR.blocks()) {
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

    private <T, C> void registerBlock(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, C> cap,
            IBlockCapabilityProvider<T, C> provider,
            Block... blocks) {
        ledger.blocks(cap.name(), blocks);
        event.registerBlock(cap, provider, blocks);
    }

    private <T, C, BE extends BlockEntity> void registerBlockEntity(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, C> cap,
            BlockEntityType<BE> type,
            ICapabilityProvider<? super BE, C, T> provider) {
        ledger.blockEntity(cap.name(), type);
        event.registerBlockEntity(cap, type, provider);
    }

    private <T, C> void registerBlock0(RegisterCapabilitiesEvent event, BlockRegistration<T, C> r) {
        registerBlock(
                event,
                r.token(),
                (level, pos, state, be, context) -> r.mapper().map(level, pos, state, be, context),
                r.block().get());
    }

    private <T, C, BE extends BlockEntity> void register0(
            RegisterCapabilitiesEvent event, Registration<T, C, BE> r) {
        registerBlockEntity(
                event, r.token(), r.beType().get(), (be, context) -> r.mapper().apply(be, context));
    }

    private void registerItems(RegisterCapabilitiesEvent event) {
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
        return be != null && be.getType() == type && be instanceof BlockEntityMachineBase m
                ? MachineItemHandler.of(m, side)
                : null;
    }

    private static @Nullable MachineItemHandler cellItems(
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
    private void registerEnergy0(
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

    private void registerEnergyBlock0(
            RegisterCapabilitiesEvent event, Supplier<? extends Block> blockSupplier) {
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
                blockSupplier.get());
    }

    @Override
    public <T, C> BlockCapability<T, C> createToken(
            Identifier id, Class<T> type, Class<C> context) {
        return BlockCapability.create(id, type, context);
    }

    @Override
    public <T, C> @Nullable T find(
            BlockCapability<T, C> token, Level level, BlockPos pos, @Nullable C context) {
        return level.getCapability(token, pos, context);
    }

    @Override
    public <T, C> BlockLookupCache<T> createCache(
            BlockCapability<T, C> cap, ServerLevel level, BlockPos pos, @Nullable C context) {
        var cache = BlockCapabilityCache.create(cap, level, pos, context);
        return new BlockLookupCache<>() {

            private boolean probing;

            @Override
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
                if (result == null
                        && isRemoteCoreSurface(level, pos, ICapabilityService.sideOf(context)))
                    probing = true;
                if (!probing) return result;
                level.invalidateCapabilities(pos);
                result = cache.getCapability();
                if (result == null || (result instanceof BlockEntity be && be.isRemoved()))
                    return null;
                probing = false;
                return result;
            }
        };
    }

    @Override
    public BlockLookupCache<IFluidHandlerView> createFluidHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        var cache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos, side);
        return new BlockLookupCache<>() {
            private @Nullable ResourceHandler<FluidResource> last;
            private @Nullable IFluidHandlerView view;

            @Override
            public @Nullable IFluidHandlerView find() {
                ResourceHandler<FluidResource> handler = cache.getCapability();
                if (handler == null) return null;
                if (handler != last) {
                    last = handler;
                    view = new NeoForgeFluidHandlerView(handler);
                }
                return view;
            }
        };
    }

    @Override
    public BlockLookupCache<IEnergyHandlerView> createEnergyHandlerCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        var cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos, side);
        return new BlockLookupCache<>() {
            private @Nullable EnergyHandler last;
            private @Nullable IEnergyHandlerView view;

            @Override
            public @Nullable IEnergyHandlerView find() {
                EnergyHandler handler = cache.getCapability();
                if (handler == null) return null;
                if (handler != last) {
                    last = handler;
                    view = new NeoForgeEnergyHandlerView(handler);
                }
                return view;
            }
        };
    }

    @Override
    public <T, C, BE extends BlockEntity> void registerProvider(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper) {
        pending.add(new Registration<>(token, beType, mapper));
    }

    @Override
    public <T, C> void registerBlockProvider(
            BlockCapability<T, C> token,
            Supplier<? extends Block> block,
            BlockCapMapper<T, C> mapper) {
        pendingBlocks.add(new BlockRegistration<>(token, block, mapper));
    }

    @Override
    public void invalidateCaps(Level level, BlockPos pos) {
        level.invalidateCapabilities(pos);
    }

    @Override
    public void watchForeignCaps(ServerLevel level, BlockPos pos) {
        Long2ObjectOpenHashMap<ICapabilityInvalidationListener> byPos =
                foreignCapWatches.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
        long key = pos.asLong();
        if (byPos.containsKey(key)) return;

        BlockPos at = pos.immutable();
        ICapabilityInvalidationListener listener =
                () -> {
                    LevelNodeGraph.invalidateEndpointsAround(level, at);

                    MinecraftServer server = level.getServer();
                    server.schedule(
                            new TickTask(
                                    server.getTickCount(),
                                    () -> updateLoadedNeighbourShapes(level, at)));

                    if (LevelNodeGraph.anyEndpointLookingAt(level, at)
                            || Library.armDrawerBeside(level, at)) return true;
                    unwatchForeignCaps(level, at);
                    return false;
                };
        byPos.put(key, listener);
        level.registerCapabilityListener(at, listener);
    }

    @Override
    public ICapabilityInvalidationListener listenForCapChanges(
            ServerLevel level, Iterable<BlockPos> positions, BooleanSupplier onInvalidate) {
        ICapabilityInvalidationListener listener = onInvalidate::getAsBoolean;
        for (BlockPos pos : positions) level.registerCapabilityListener(pos.immutable(), listener);
        return listener;
    }

    private static final Direction[] UPDATE_SHAPE_ORDER = {
        Direction.WEST,
        Direction.EAST,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.DOWN,
        Direction.UP
    };

    private static void updateLoadedNeighbourShapes(ServerLevel level, BlockPos at) {
        if (!level.isLoaded(at)) return;
        BlockState state = level.getBlockState(at);
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : UPDATE_SHAPE_ORDER) {
            neighbour.setWithOffset(at, direction);
            if (level.isLoaded(neighbour)) {
                level.neighborShapeChanged(
                        direction.getOpposite(), neighbour, at, state, Block.UPDATE_CLIENTS, 512);
            }
        }
    }

    @Override
    public void unwatchForeignCaps(ServerLevel level, BlockPos pos) {
        Long2ObjectOpenHashMap<ICapabilityInvalidationListener> byPos =
                foreignCapWatches.get(level);
        if (byPos == null) return;
        byPos.remove(pos.asLong());
        if (byPos.isEmpty()) foreignCapWatches.remove(level);
    }

    @Override
    public void clearForeignCapWatches() {
        foreignCapWatches.clear();
    }

    boolean hasForeignCapWatch(ServerLevel level, BlockPos pos) {
        Long2ObjectOpenHashMap<ICapabilityInvalidationListener> byPos =
                foreignCapWatches.get(level);
        return byPos != null && byPos.containsKey(pos.asLong());
    }

    @Override
    public @Nullable IFluidHandlerView findFluidHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<FluidResource> handler =
                level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler == null ? null : new NeoForgeFluidHandlerView(handler);
    }

    @Override
    public @Nullable IFluidHandlerView findFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;

        return findFluidHandler(new SimpleContainer(stack.copyWithCount(1)), 0);
    }

    @Override
    public @Nullable IFluidHandlerView findFluidHandler(Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        ResourceHandler<FluidResource> handler =
                held.getCapability(Capabilities.Fluid.ITEM, access);
        return handler == null ? null : new NeoForgeFluidHandlerView(handler);
    }

    @Override
    public @Nullable IEnergyHandlerView findEnergyHandler(
            Level level, BlockPos pos, @Nullable Direction side) {
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler == null ? null : new NeoForgeEnergyHandlerView(handler);
    }

    @Override
    public @Nullable IEnergyHandlerView findEnergyHandler(Container container, int slot) {
        ItemStack held = container.getItem(slot);
        if (held.isEmpty()) return null;
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), slot);
        EnergyHandler handler = held.getCapability(Capabilities.Energy.ITEM, access);
        return handler == null ? null : new NeoForgeEnergyHandlerView(handler);
    }

    @Override
    public void exposeItem(Supplier<? extends BlockEntityType<?>> beType) {
        itemExposures.add(beType);
    }

    @Override
    public void exposeItemAtBlock(Supplier<? extends Block> block) {
        itemBlockExposures.add(block);
    }

    @Override
    public void exposeEnergy(Supplier<? extends BlockEntityType<?>> beType) {
        energyExposures.add(beType);
    }

    @Override
    public void exposeEnergyAtBlock(Supplier<? extends Block> block) {
        energyBlockExposures.add(block);
    }

    @Override
    public void exposeFluid(Supplier<? extends Block> block) {
        fluidExposures.add(block);
    }

    @Override
    public CapabilityProviderLedger providerLedger() {
        return ledger;
    }

    public void flush(RegisterCapabilitiesEvent event) {
        for (Registration<?, ?, ?> r : pending) register0(event, r);
        for (BlockRegistration<?, ?> r : pendingBlocks) registerBlock0(event, r);
        registerItems(event);
        for (Supplier<? extends BlockEntityType<?>> t : energyExposures) registerEnergy0(event, t);
        for (Supplier<? extends Block> b : energyBlockExposures) registerEnergyBlock0(event, b);
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

    private void registerFluidBlocks(RegisterCapabilitiesEvent event) {
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

    private record Registration<T, C, BE extends BlockEntity>(
            BlockCapability<T, C> token,
            Supplier<? extends BlockEntityType<? extends BE>> beType,
            BiFunction<? super BE, C, T> mapper) {}

    private record BlockRegistration<T, C>(
            BlockCapability<T, C> token,
            Supplier<? extends Block> block,
            ICapabilityService.BlockCapMapper<T, C> mapper) {}
}
