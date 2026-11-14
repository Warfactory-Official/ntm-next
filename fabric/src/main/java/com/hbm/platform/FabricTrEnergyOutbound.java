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

import com.hbm.api.energymk2.*;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlock;
import com.hbm.capability.ForeignBridges;
import com.hbm.capability.ForeignEnergyStaging;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities;
import com.hbm.fabric.energy.TechRebornEnergy;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

public final class FabricTrEnergyOutbound {

    private FabricTrEnergyOutbound() {}

    public static void register(
            List<Supplier<? extends BlockEntityType<?>>> machineTypes,
            List<Supplier<? extends Block>> machineBlocks) {
        for (Supplier<? extends BlockEntityType<?>> type : machineTypes) {
            registerMachineType(type);
        }
        for (Supplier<? extends Block> block : machineBlocks) {
            registerMachineBlock(block);
        }
        registerConductors();
        FabricTrEnergyItem.register();
        if (FabricLoader.getInstance().isModLoaded("techreborn")) TechRebornEnergy.register();
    }

    private static int declaredOf(BlockEntity be, EnergyHandler machine) {
        return MachineCaps.declaredDomains(machine instanceof BlockEntity owner ? owner : be);
    }

    private static @Nullable EnergyHandler halfIn(BlockEntity be, EnergyHandler machine) {
        return (declaredOf(be, machine) & MachineCaps.POWER_IN) != 0 ? machine : null;
    }

    private static @Nullable EnergyHandler halfOut(BlockEntity be, EnergyHandler machine) {
        return (declaredOf(be, machine) & MachineCaps.POWER_OUT) != 0 ? machine : null;
    }

    private static void registerConductors() {
        List<Block> conductors = new ArrayList<>();
        for (RegistryHandle<? extends Block> handle : Registrar.registeredBlocks) {
            Block block = handle.get();
            if (block instanceof CableBlock || block instanceof CableConductorBlock)
                conductors.add(block);
        }
        if (conductors.isEmpty()) return;
        Caps.registerForBlocks(
                EnergyStorage.SIDED,
                (level, pos, state, be, side) -> {
                    if (!(level instanceof ServerLevel server)) return null;
                    ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(server, pos);
                    if (bridge == null) return null;
                    long peerKey = side == null ? pos.asLong() : pos.relative(side).asLong();
                    return new NetFace(server, pos.immutable(), side, peerKey);
                },
                conductors.toArray(new Block[0]));
    }

    private static void registerMachineType(Supplier<? extends BlockEntityType<?>> typeSupplier) {
        @SuppressWarnings("unchecked")
        BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) typeSupplier.get();
        Caps.registerForBlockEntity(
                EnergyStorage.SIDED,
                (be, side) -> {
                    if (side == null || !(be instanceof EnergyHandler e)) return null;
                    EnergyHandler in =
                            MachineCaps.accepts(be, side, NtmCapabilities.CapRole.POWER_IN)
                                    ? halfIn(be, e)
                                    : null;
                    EnergyHandler out =
                            MachineCaps.accepts(be, side, NtmCapabilities.CapRole.POWER_OUT)
                                    ? halfOut(be, e)
                                    : null;
                    return in == null && out == null
                            ? null
                            : MachineStaging.of(be, e).face(be.getBlockPos(), side, in, out);
                },
                type);
    }

    private static void registerMachineBlock(Supplier<? extends Block> blockSupplier) {
        Caps.registerForBlocks(
                EnergyStorage.SIDED,
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
                    return MachineStaging.of((BlockEntity) machine, machine)
                            .face(pos, side, receiver, provider);
                },
                blockSupplier.get());
    }

    private static final class NetStaging extends SnapshotParticipant<NetStaging.Staged> {

        private final ForeignEnergyBridge bridge;
        private final ServerLevel level;
        private final Long2LongOpenHashMap insertsByPeer = new Long2LongOpenHashMap();
        private final LongOpenHashSet plannedPeers = new LongOpenHashSet();
        private long insertTotalFe;
        private long extractFe;
        private long plannedInsertFe;
        private long plannedExtractFe;
        private long tick;

        private NetStaging(ForeignEnergyBridge bridge, ServerLevel level) {
            this.bridge = bridge;
            this.level = level;
        }

        static NetStaging of(ForeignEnergyBridge bridge, ServerLevel level) {
            NetStaging staging = bridge.loaderStaging instanceof NetStaging held ? held : null;
            if (staging == null) bridge.loaderStaging = staging = new NetStaging(bridge, level);
            return staging;
        }

        @Override
        protected Staged createSnapshot() {
            return new Staged(
                    new Long2LongOpenHashMap(insertsByPeer),
                    new LongOpenHashSet(plannedPeers),
                    insertTotalFe,
                    extractFe,
                    plannedInsertFe,
                    plannedExtractFe,
                    tick);
        }

        @Override
        protected void readSnapshot(Staged snapshot) {
            insertsByPeer.clear();
            insertsByPeer.putAll(snapshot.insertsByPeer());
            plannedPeers.clear();
            plannedPeers.addAll(snapshot.plannedPeers());
            insertTotalFe = snapshot.insertTotalFe();
            extractFe = snapshot.extractFe();
            plannedInsertFe = snapshot.plannedInsertFe();
            plannedExtractFe = snapshot.plannedExtractFe();
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
            bridge.insertPlannedFe(plannedInsertFe);
            if (plannedExtractFe > 0) bridge.extractPlannedFe(plannedExtractFe);
            for (LongIterator it = insertsByPeer.keySet().iterator(); it.hasNext(); )
                ForeignEnergyAdapters.committed(level, BlockPos.of(it.nextLong()));
            for (LongIterator it = plannedPeers.iterator(); it.hasNext(); )
                ForeignEnergyAdapters.committed(level, BlockPos.of(it.nextLong()));
            insertsByPeer.clear();
            plannedPeers.clear();
            insertTotalFe = 0;
            extractFe = 0;
            plannedInsertFe = 0;
            plannedExtractFe = 0;
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

        long stagePlannedInsert(
                MixedEnergyPlan plan,
                int slot,
                long maxFe,
                long peerKey,
                TransactionContext transaction) {
            long[] reserved = {0};
            long accepted =
                    MixedEnergyQuota.reserve(
                            plan,
                            slot,
                            Math.min(maxFe, bridge.plannedInsertableFe() - plannedInsertFe),
                            1,
                            transaction,
                            reserved);
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            plannedInsertFe += accepted;
            plannedPeers.add(peerKey);
            return accepted;
        }

        long stagePlannedExtract(
                MixedEnergyPlan plan, int slot, long maxFe, TransactionContext transaction) {
            long[] reserved = {0};
            long given =
                    MixedEnergyQuota.reserve(
                            plan,
                            slot,
                            Math.min(maxFe, bridge.plannedExtractableFe() - plannedExtractFe),
                            1,
                            transaction,
                            reserved);
            if (given <= 0) return 0;
            updateSnapshots(transaction);
            plannedExtractFe += given;
            return given;
        }

        long amountFe() {
            return Math.max(0, bridge.amountFe() - extractFe);
        }

        long plannedAmountFe(MixedEnergyPlan plan, int slot) {
            return Math.max(
                    0,
                    Math.min(
                            bridge.plannedExtractableFe() - plannedExtractFe,
                            plan.availableFe(slot)));
        }

        long capacityFe() {
            return bridge.capacityFe();
        }

        long plannedRoomFe(MixedEnergyPlan plan, int slot) {
            return Math.max(
                    0,
                    Math.min(
                            bridge.plannedInsertableFe() - plannedInsertFe,
                            plan.availableFe(slot)));
        }

        record Staged(
                Long2LongMap insertsByPeer,
                LongOpenHashSet plannedPeers,
                long insertTotalFe,
                long extractFe,
                long plannedInsertFe,
                long plannedExtractFe,
                long tick) {}
    }

    private record NetFace(
            ServerLevel level, BlockPos position, @Nullable Direction side, long peerKey)
            implements EnergyStorage, NtmEnergyFace {

        private @Nullable NetStaging staging() {
            ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, position);
            return bridge == null ? null : NetStaging.of(bridge, level);
        }

        private int slot(@Nullable MixedEnergyPlan plan, boolean toForeign) {
            return plan == null || side == null
                    ? -1
                    : plan.slot(position.asLong(), side, toForeign);
        }

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
            NetStaging staging = maxAmount <= 0 ? null : staging();
            if (staging == null) return 0;
            MixedEnergyPlan plan = staging.bridge.plan();
            int slot = slot(plan, false);
            return slot < 0
                    ? staging.stageInsert(maxAmount, peerKey, transaction)
                    : staging.stagePlannedInsert(plan, slot, maxAmount, peerKey, transaction);
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            NetStaging staging = maxAmount <= 0 ? null : staging();
            if (staging == null) return 0;
            MixedEnergyPlan plan = staging.bridge.plan();
            int slot = slot(plan, true);
            return slot < 0
                    ? staging.stageExtract(maxAmount, transaction)
                    : staging.stagePlannedExtract(plan, slot, maxAmount, transaction);
        }

        @Override
        public long getAmount() {
            NetStaging staging = staging();
            if (staging == null) return 0;
            MixedEnergyPlan plan = staging.bridge.plan();
            int slot = slot(plan, true);
            return slot < 0 ? staging.amountFe() : staging.plannedAmountFe(plan, slot);
        }

        @Override
        public long getCapacity() {
            NetStaging staging = staging();
            if (staging == null) return 0;
            MixedEnergyPlan plan = staging.bridge.plan();
            int slot = slot(plan, false);
            if (slot < 0) return staging.capacityFe();
            long total = getAmount() + staging.plannedRoomFe(plan, slot);
            return total < 0 ? Long.MAX_VALUE : total;
        }
    }

    private static final class MachineStaging extends SnapshotParticipant<long[]> {

        private final ForeignEnergyStaging staging;
        private final BlockEntity be;
        private final Map<Direction, MachineFace> faces = new EnumMap<>(Direction.class);

        private MachineStaging(BlockEntity be, EnergyHandler machine) {
            this.staging = new ForeignEnergyStaging(be, machine);
            this.be = be;
        }

        static MachineStaging of(BlockEntity be, EnergyHandler machine) {
            ForeignBridges bridges = ForeignBridges.of(be);
            MachineStaging staging =
                    bridges.energy(machine) instanceof MachineStaging held ? held : null;
            if (staging == null) bridges.energy(machine, staging = new MachineStaging(be, machine));
            return staging;
        }

        MachineFace face(
                BlockPos pos,
                Direction side,
                @Nullable EnergyHandler receiver,
                @Nullable EnergyHandler provider) {
            MachineFace face = faces.get(side);
            if (face == null
                    || face.receiver != receiver
                    || face.provider != provider
                    || !face.pos.equals(pos)) {
                faces.put(
                        side,
                        face = new MachineFace(this, pos.immutable(), side, receiver, provider));
            }
            return face;
        }

        private int slot(
                @Nullable MixedEnergyPlan plan, BlockPos pos, Direction side, boolean toForeign) {
            return plan == null ? -1 : plan.slot(pos.asLong(), side, toForeign);
        }

        private @Nullable MixedEnergyPlan plan(BlockPos pos) {
            return be.getLevel() instanceof ServerLevel level
                    ? PowerNetwork.planAt(level, pos)
                    : null;
        }

        @Override
        protected long[] createSnapshot() {
            return staging.save();
        }

        @Override
        protected void readSnapshot(long[] snapshot) {
            staging.restore(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            staging.commit();
        }

        long stageInsert(
                long maxFe,
                EnergyHandler receiver,
                BlockPos pos,
                Direction side,
                TransactionContext transaction) {
            long fits = staging.insertableFe(maxFe, receiver);
            MixedEnergyPlan plan = fits <= 0 ? null : plan(pos);
            int slot = slot(plan, pos, side, false);
            long[] reserved = {0};
            if (slot >= 0)
                fits =
                        MixedEnergyQuota.reserve(
                                plan,
                                slot,
                                fits,
                                EnergyConversion.feQuantum(),
                                transaction,
                                reserved);
            if (fits <= 0) return 0;
            updateSnapshots(transaction);
            staging.stageInsert(fits);
            return fits;
        }

        long stageExtract(
                long maxFe,
                EnergyHandler provider,
                BlockPos pos,
                Direction side,
                TransactionContext transaction) {
            long gives = staging.extractableFe(maxFe, provider);
            MixedEnergyPlan plan = gives <= 0 ? null : plan(pos);
            int slot = slot(plan, pos, side, true);
            long[] reserved = {0};
            if (slot >= 0)
                gives =
                        MixedEnergyQuota.reserve(
                                plan,
                                slot,
                                gives,
                                EnergyConversion.feQuantum(),
                                transaction,
                                reserved);
            if (gives <= 0) return 0;
            updateSnapshots(transaction);
            staging.stageExtract(gives);
            return gives;
        }

        long amountFe() {
            return staging.amountFe();
        }

        long capacityFe() {
            return staging.capacityFe();
        }
    }

    private record MachineFace(
            MachineStaging staging,
            BlockPos pos,
            Direction side,
            @Nullable EnergyHandler receiver,
            @Nullable EnergyHandler provider)
            implements EnergyStorage, NtmEnergyFace {
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
            return staging.stageInsert(maxAmount, receiver, pos, side, transaction);
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || provider == null) return 0;
            return staging.stageExtract(maxAmount, provider, pos, side, transaction);
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
