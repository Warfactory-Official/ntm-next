// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.api.energymk2.*;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities;
import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

final class FabricTrEnergyOutbound {

    private FabricTrEnergyOutbound() {}

    static void register(
            FabricCapabilityService caps,
            List<Supplier<? extends BlockEntityType<?>>> machineTypes,
            List<Supplier<? extends Block>> machineBlocks) {
        for (Supplier<? extends BlockEntityType<?>> type : machineTypes) {
            registerMachineType(caps, type);
        }
        for (Supplier<? extends Block> block : machineBlocks) {
            registerMachineBlock(caps, block);
        }
        registerConductors(caps);
        FabricTrEnergyItem.register();
    }

    private static int declaredOf(BlockEntity be, IEnergyHandlerMK2 machine) {
        return MachineCaps.declaredDomains(machine instanceof BlockEntity owner ? owner : be);
    }

    private static @Nullable IEnergyHandlerMK2 halfIn(BlockEntity be, IEnergyHandlerMK2 machine) {
        return (declaredOf(be, machine) & MachineCaps.POWER_IN) != 0 ? machine : null;
    }

    private static @Nullable IEnergyHandlerMK2 halfOut(BlockEntity be, IEnergyHandlerMK2 machine) {
        return (declaredOf(be, machine) & MachineCaps.POWER_OUT) != 0 ? machine : null;
    }

    private static void registerConductors(FabricCapabilityService caps) {
        List<Block> conductors = new ArrayList<>();
        for (RegistryHandle<? extends Block> handle : Services.REGISTRAR.blocks()) {
            Block block = handle.get();
            if (block instanceof CableBlock || block instanceof CableConductorBlockBase)
                conductors.add(block);
        }
        if (conductors.isEmpty()) return;
        caps.registerForBlocks(
                EnergyStorage.SIDED,
                (level, pos, state, be, side) -> {
                    if (!(level instanceof ServerLevel server)) return null;
                    ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(server, pos);
                    if (bridge == null) return null;

                    long peerKey = side == null ? pos.asLong() : pos.relative(side).asLong();
                    return NetStaging.of(bridge, server).face(peerKey);
                },
                conductors.toArray(new Block[0]));
    }

    private static void registerMachineType(
            FabricCapabilityService caps, Supplier<? extends BlockEntityType<?>> typeSupplier) {
        @SuppressWarnings("unchecked")
        BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) typeSupplier.get();
        caps.registerForBlockEntity(
                EnergyStorage.SIDED,
                (be, side) ->
                        side != null
                                        && be instanceof IEnergyHandlerMK2 e
                                        && MachineCaps.acceptsFace(be, side)
                                ? MachineStaging.of(be, e).face(side, halfIn(be, e), halfOut(be, e))
                                : null,
                type);
    }

    private static void registerMachineBlock(
            FabricCapabilityService caps, Supplier<? extends Block> blockSupplier) {
        caps.registerForBlocks(
                EnergyStorage.SIDED,
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

                    return MachineStaging.of((BlockEntity) machine, machine)
                            .face(side, receiver, provider);
                },
                blockSupplier.get());
    }

    private static final class NetStaging extends SnapshotParticipant<NetStaging.Staged> {

        private static final Map<ForeignEnergyBridge, WeakReference<NetStaging>> ALL =
                new WeakHashMap<>();
        private final ForeignEnergyBridge bridge;
        private final ServerLevel level;
        private final Long2LongOpenHashMap insertsByPeer = new Long2LongOpenHashMap();
        private final Long2ObjectOpenHashMap<NetFace> faces = new Long2ObjectOpenHashMap<>();
        private long insertTotalFe;
        private long extractFe;
        private long tick;

        private NetStaging(ForeignEnergyBridge bridge, ServerLevel level) {
            this.bridge = bridge;
            this.level = level;
        }

        static synchronized NetStaging of(ForeignEnergyBridge bridge, ServerLevel level) {
            WeakReference<NetStaging> ref = ALL.get(bridge);
            NetStaging staging = ref == null ? null : ref.get();
            if (staging == null)
                ALL.put(bridge, new WeakReference<>(staging = new NetStaging(bridge, level)));
            return staging;
        }

        NetFace face(long peerKey) {
            NetFace face = faces.get(peerKey);
            if (face == null) faces.put(peerKey, face = new NetFace(this, peerKey));
            return face;
        }

        @Override
        protected Staged createSnapshot() {
            return new Staged(
                    new Long2LongOpenHashMap(insertsByPeer), insertTotalFe, extractFe, tick);
        }

        @Override
        protected void readSnapshot(Staged snapshot) {
            insertsByPeer.clear();
            insertsByPeer.putAll(snapshot.insertsByPeer());
            insertTotalFe = snapshot.insertTotalFe();
            extractFe = snapshot.extractFe();
            tick = snapshot.tick();
        }

        @Override
        protected void onFinalCommit() {

            if (!insertsByPeer.isEmpty()) {
                for (ObjectIterator<Long2LongMap.Entry> it =
                                insertsByPeer.long2LongEntrySet().fastIterator();
                        it.hasNext(); ) {
                    Long2LongMap.Entry e = it.next();
                    bridge.insertFe(e.getLongValue(), e.getLongKey(), tick);
                }
            }
            if (extractFe > 0) bridge.extractFe(extractFe);
            insertsByPeer.clear();
            insertTotalFe = 0;
            extractFe = 0;
        }

        long stageInsert(long maxFe, long peerKey, TransactionContext transaction) {
            long accepted = Math.min(maxFe, bridge.insertableFe() - insertTotalFe);
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            insertsByPeer.addTo(peerKey, accepted);
            insertTotalFe += accepted;

            tick = level.getGameTime();
            return accepted;
        }

        long stageExtract(long maxFe, TransactionContext transaction) {
            long given = Math.min(maxFe, bridge.extractableFe() - extractFe);
            if (given <= 0) return 0;
            updateSnapshots(transaction);
            extractFe += given;
            return given;
        }

        long amountFe() {
            return Math.max(0, bridge.amountFe() - extractFe);
        }

        long capacityFe() {
            return bridge.capacityFe();
        }

        record Staged(Long2LongMap insertsByPeer, long insertTotalFe, long extractFe, long tick) {}
    }

    private record NetFace(NetStaging staging, long peerKey) implements EnergyStorage {

        @Override
        public boolean supportsInsertion() {
            return true;
        }

        @Override
        public boolean supportsExtraction() {
            return true;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0) return 0;
            return staging.stageInsert(maxAmount, peerKey, transaction);
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0) return 0;
            return staging.stageExtract(maxAmount, transaction);
        }

        @Override
        public long getAmount() {
            return staging.amountFe();
        }

        @Override
        public long getCapacity() {
            return staging.capacityFe();
        }
    }

    private static final class MachineStaging extends SnapshotParticipant<long[]> {

        private static final Map<BlockEntity, WeakReference<MachineStaging>> ALL =
                new WeakHashMap<>();

        private final BlockEntity be;
        private final IEnergyHandlerMK2 machine;
        private final Map<Direction, MachineFace> faces = new EnumMap<>(Direction.class);
        private long insertFe;
        private long extractFe;

        private MachineStaging(BlockEntity be, IEnergyHandlerMK2 machine) {
            this.be = be;
            this.machine = machine;
        }

        static synchronized MachineStaging of(BlockEntity be, IEnergyHandlerMK2 machine) {

            WeakReference<MachineStaging> ref = ALL.get(be);
            MachineStaging staging = ref == null ? null : ref.get();
            if (staging == null)
                ALL.put(be, new WeakReference<>(staging = new MachineStaging(be, machine)));
            return staging;
        }

        MachineFace face(
                Direction side,
                @Nullable IEnergyHandlerMK2 receiver,
                @Nullable IEnergyHandlerMK2 provider) {
            MachineFace face = faces.get(side);
            if (face == null || face.receiver != receiver || face.provider != provider) {
                faces.put(side, face = new MachineFace(this, side, receiver, provider));
            }
            return face;
        }

        @Override
        protected long[] createSnapshot() {
            return new long[] {insertFe, extractFe};
        }

        @Override
        protected void readSnapshot(long[] snapshot) {
            insertFe = snapshot[0];
            extractFe = snapshot[1];
        }

        @Override
        protected void onFinalCommit() {

            if (insertFe > 0) machine.transferPower(EnergyConversion.heFromFe(insertFe), false);
            if (extractFe > 0) machine.usePower(EnergyConversion.heFromFe(extractFe));
            insertFe = 0;
            extractFe = 0;
            be.setChanged();
        }

        long stageInsert(long maxFe, IEnergyHandlerMK2 receiver, TransactionContext transaction) {
            if (be.isRemoved()) return 0;
            long room = receiver.getMaxPower() - receiver.getPower();

            long offered = Math.min(room, receiver.getReceiverSpeed());
            long quantum = EnergyConversion.feQuantum();
            long fits =
                    Math.min(maxFe, EnergyConversion.feFromHe(offered) - insertFe)
                            / quantum
                            * quantum;
            if (fits <= 0) return 0;
            updateSnapshots(transaction);
            insertFe += fits;
            return fits;
        }

        long stageExtract(long maxFe, IEnergyHandlerMK2 provider, TransactionContext transaction) {
            if (be.isRemoved()) return 0;
            long available = Math.min(provider.getPower(), provider.getProviderSpeed());
            long quantum = EnergyConversion.feQuantum();
            long gives =
                    Math.min(maxFe, EnergyConversion.feFromHe(available) - extractFe)
                            / quantum
                            * quantum;
            if (gives <= 0) return 0;
            updateSnapshots(transaction);
            extractFe += gives;
            return gives;
        }

        long amountFe() {
            if (be.isRemoved()) return 0;
            return Math.max(
                    0, EnergyConversion.feFromHe(machine.getPower()) + insertFe - extractFe);
        }

        long capacityFe() {
            return be.isRemoved() ? 0 : EnergyConversion.feFromHe(machine.getMaxPower());
        }
    }

    private record MachineFace(
            MachineStaging staging,
            Direction side,
            @Nullable IEnergyHandlerMK2 receiver,
            @Nullable IEnergyHandlerMK2 provider)
            implements EnergyStorage {

        @Override
        public boolean supportsInsertion() {
            return receiver != null;
        }

        @Override
        public boolean supportsExtraction() {
            return provider != null;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || receiver == null) return 0;
            return staging.stageInsert(maxAmount, receiver, transaction);
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || provider == null) return 0;
            return staging.stageExtract(maxAmount, provider, transaction);
        }

        @Override
        public long getAmount() {
            return staging.amountFe();
        }

        @Override
        public long getCapacity() {
            return staging.capacityFe();
        }
    }
}
