/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.ICFBlock;
import com.hbm.blocks.machine.ICFControllerBlock;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.data.MachineData;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncAudience;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitFrame;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.SyncUnitState;
import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import com.hbm.util.ChunkUtil;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkDataFields;
import com.hbm.world.bulk.BulkTransform;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class ICFControllerBlockEntity extends BlockEntity
        implements BlockEntityTicks, EnergyHandler, GraphResident, Synced, SyncUnitSchema {
    private long energy;
    private int capacitors;
    private int turbochargers;
    private int beamLength;
    private int cells;
    private int emitters;
    private boolean assembled;
    private List<BlockPos> ports = List.of();
    private BoundingBox memberBounds;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private boolean capacityValid;
    private int cachedCapacitorCoefficient;
    private int cachedTurboCoefficient;
    private long capacity;

    public ICFControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    public boolean isAssembled() {
        return assembled;
    }

    public int cellCount() {
        return cells;
    }

    public int emitterCount() {
        return emitters;
    }

    public int capacitorCount() {
        return capacitors;
    }

    public int turbochargerCount() {
        return turbochargers;
    }

    public int beamLength() {
        return beamLength;
    }

    public void configureAssembly(
            Set<BlockPos> portPositions,
            Set<BlockPos> cellPositions,
            Set<BlockPos> emitterPositions,
            Set<BlockPos> capacitorPositions,
            Set<BlockPos> turbochargerPositions) {
        var countedCells = new LongOpenHashSet(cellPositions.size());
        var backward = getBlockState().getValue(ICFControllerBlock.FACING).getOpposite();
        cursor.set(worldPosition).move(backward);
        while (cellPositions.contains(cursor)) {
            countedCells.add(cursor.asLong());
            cursor.move(backward);
        }
        var countedEmitters = adjoining(emitterPositions, countedCells);
        var countedCapacitors = adjoining(capacitorPositions, countedEmitters);
        var countedTurbochargers = adjoining(turbochargerPositions, countedCapacitors);
        cells = countedCells.size();
        emitters = countedEmitters.size();
        hbm$syncSet$ICFControllerBlockEntity$capacitors(countedCapacitors.size());
        hbm$syncSet$ICFControllerBlockEntity$turbochargers(countedTurbochargers.size());
        ports = List.copyOf(portPositions);
        capacityValid = false;
        setChanged();
    }

    private static LongSet adjoining(Set<BlockPos> candidates, LongSet previous) {
        var counted = new LongOpenHashSet(candidates.size());
        for (var pos : candidates) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            if (previous.contains(BlockPos.asLong(x - 1, y, z))
                    || previous.contains(BlockPos.asLong(x + 1, y, z))
                    || previous.contains(BlockPos.asLong(x, y - 1, z))
                    || previous.contains(BlockPos.asLong(x, y + 1, z))
                    || previous.contains(BlockPos.asLong(x, y, z - 1))
                    || previous.contains(BlockPos.asLong(x, y, z + 1))) counted.add(pos.asLong());
        }
        return counted;
    }

    public void assemble(BoundingBox bounds) {
        assembled = true;
        memberBounds = bounds;
        setChanged();
    }

    public void disassemble() {
        if (!assembled) return;
        assembled = false;
        setChanged();
        if (memberBounds == null) return;
        var server = (ServerLevel) level;
        for (var member : AssembledMembers.members(server, worldPosition, memberBounds)) {
            var original = server.getBlockState(member).getValue(ICFBlock.PART).original();
            AssembledMembers.release(server, member);
            server.setBlock(member, original, 3);
        }
        memberBounds = null;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!BulkAction.active()) disassemble();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public long getPower() {
        return Math.min(energy, getMaxPower());
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$ICFControllerBlockEntity$energy(stored);
    }

    @Override
    public long getMaxPower() {
        int capacitorCoefficient = MachineData.ICF_CAPACITOR_POWER.get();
        int turboCoefficient = MachineData.ICF_TURBO_POWER.get();
        if (!capacityValid
                || cachedCapacitorCoefficient != capacitorCoefficient
                || cachedTurboCoefficient != turboCoefficient) {
            capacity =
                    (long)
                            (Math.sqrt(capacitors) * capacitorCoefficient
                                    + Math.sqrt(Math.min(turbochargers, capacitors))
                                            * turboCoefficient);
            cachedCapacitorCoefficient = capacitorCoefficient;
            cachedTurboCoefficient = turboCoefficient;
            capacityValid = true;
        }
        return capacity;
    }

    @Override
    public void tickServer() {
        hbm$syncSet$ICFControllerBlockEntity$beamLength(0);
        if (assembled && energy > 0) {
            pulse((ServerLevel) level);
            hbm$syncSet$ICFControllerBlockEntity$energy(0);
            setChanged();
        }
        networkPackNT(50);
    }

    private void pulse(ServerLevel server) {
        var forward = getBlockState().getValue(ICFControllerBlock.FACING);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        cursor.set(worldPosition);
        for (int distance = 1; distance <= 49; distance++) {
            cursor.move(forward);
            hbm$syncSet$ICFControllerBlockEntity$beamLength(distance);
            var state = server.getBlockState(cursor);
            if (state.isAir()) continue;
            int coreX = cursor.getX() + 8 * forward.getStepX();
            int coreY = y - 3;
            int coreZ = cursor.getZ() + 8 * forward.getStepZ();
            long owner =
                    MultiblockSurface.recordedCorePacked(server, cursor.getX(), y, cursor.getZ());
            boolean coupled = false;
            if (MultiblockSurface.hasCore(owner) && owner == BlockPos.asLong(coreX, coreY, coreZ)) {
                var reactor =
                        ChunkUtil.blockEntityIfLoaded(
                                ICFBlockEntity.class, server, new BlockPos(coreX, coreY, coreZ));
                if (reactor != null) {
                    reactor.hbm$syncSet$ICFBlockEntity$laser((long) (reactor.laser + (getPower())));
                    reactor.hbm$syncSet$ICFBlockEntity$maxLaser(
                            (long) (reactor.maxLaser + (getMaxPower())));
                    coupled = true;
                }
            }
            if (!coupled && state.getBlock().getExplosionResistance() < 6_000.0F) {
                server.destroyBlock(cursor, false);
            }
            break;
        }
        int tipX = x + beamLength * forward.getStepX(), tipZ = z + beamLength * forward.getStepZ();
        var bounds =
                new AABB(
                        Math.min(x, tipX) + 0.2,
                        y + 0.2,
                        Math.min(z, tipZ) + 0.2,
                        Math.max(x, tipX) + 0.8,
                        y + 0.8,
                        Math.max(z, tipZ) + 0.8);
        for (var entity : server.getEntities(null, bounds)) {
            entity.hurtServer(server, server.damageSources().inFire(), 50.0F);
            entity.igniteForSeconds(5);
        }
    }

    @Override
    public void tickClient() {
        if (beamLength <= 0 || level.getRandom().nextInt(5) != 0) return;
        var forward = getBlockState().getValue(ICFControllerBlock.FACING);
        var across = forward.getClockWise();
        double lateral = level.getRandom().nextDouble() * 0.25 - 0.125;
        double vertical = level.getRandom().nextDouble() * 0.25 - 0.125;
        level.addParticle(
                DustParticleOptions.REDSTONE,
                worldPosition.getX()
                        + 0.5
                        + 0.55 * forward.getStepX()
                        + lateral * across.getStepX(),
                worldPosition.getY() + 0.5 + vertical,
                worldPosition.getZ()
                        + 0.5
                        + 0.55 * forward.getStepZ()
                        + lateral * across.getStepZ(),
                0,
                0,
                0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ICFControllerBlockEntity$energy(input.getLongOr("power", 0));
        assembled = input.getBooleanOr("assembled", false);
        cells = input.getIntOr("cellCount", 0);
        emitters = input.getIntOr("emitterCount", 0);
        hbm$syncSet$ICFControllerBlockEntity$capacitors(input.getIntOr("capacitorCount", 0));
        hbm$syncSet$ICFControllerBlockEntity$turbochargers(input.getIntOr("turbochargerCount", 0));
        ports = input.read("ports", BlockPos.CODEC.listOf()).orElse(List.of());
        memberBounds = input.read("members", BoundingBox.CODEC).orElse(null);
        hbm$syncSet$ICFControllerBlockEntity$beamLength(0);
        capacityValid = false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", energy);
        output.putBoolean("assembled", assembled);
        output.putInt("cellCount", cells);
        output.putInt("emitterCount", emitters);
        output.putInt("capacitorCount", capacitors);
        output.putInt("turbochargerCount", turbochargers);
        output.store("ports", BlockPos.CODEC.listOf(), ports);
        if (memberBounds != null) output.store("members", BoundingBox.CODEC, memberBounds);
    }

    @Override
    public long syncUnitMask() {
        return 15;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(energy);
            case 1 -> output.writeInt(capacitors);
            case 2 -> output.writeInt(turbochargers);
            case 3 -> output.writeInt(beamLength);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ICFControllerBlockEntity$energy(input.readLong());
            case 1 -> {
                hbm$syncSet$ICFControllerBlockEntity$capacitors(input.readInt());
                capacityValid = false;
            }
            case 2 -> {
                hbm$syncSet$ICFControllerBlockEntity$turbochargers(input.readInt());
                capacityValid = false;
            }
            case 3 -> hbm$syncSet$ICFControllerBlockEntity$beamLength(input.readInt());
            default -> throw new IllegalArgumentException();
        }
    }

    public boolean captureBulkCells(ServerLevel server, Consumer<BlockPos> visitor) {
        if (memberBounds != null)
            AssembledMembers.members(server, worldPosition, memberBounds).forEach(visitor);
        return true;
    }

    public static void relocateBulkData(CompoundTag tag, BulkTransform transform) {
        BulkDataFields.positions(tag, "ports", transform);
        BulkDataFields.bounds(tag, "members", transform);
    }

    public static CompoundTag publicBulkData(CompoundTag tag) {
        return BulkDataFields.copy(tag, "ports", "members", "assembled");
    }

    public final void hbm$syncSet$ICFControllerBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ICFControllerBlockEntity$capacitors(int next) {
        if (this.capacitors != next) {
            this.capacitors = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$ICFControllerBlockEntity$turbochargers(int next) {
        if (this.turbochargers != next) {
            this.turbochargers = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$ICFControllerBlockEntity$beamLength(int next) {
        if (this.beamLength != next) {
            this.beamLength = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    private int hbm$syncDirty = 3;

    private int hbm$syncVersion;

    private int hbm$syncSentVersion;

    private boolean hbm$syncBound;

    private boolean hbm$syncWatched = true;

    private boolean hbm$syncedOnce;

    private CompoundTag hbm$syncInitial;

    private int hbm$syncInitialVersion;

    private long hbm$syncUnits;

    private long hbm$syncClientUnitRevision;

    private SyncUnitState hbm$syncUnitState;

    private SyncUnitState.Snapshot hbm$syncPublishedUnits;

    public final boolean syncBound() {
        return hbm$syncBound;
    }

    public final void syncChanged(int mask) {
        hbm$syncDirty |= mask;
        if ((mask & 1) != 0 && (Object) this instanceof SyncUnitSchema schema)
            hbm$syncUnits |= schema.syncUnitMask();
    }

    public final void syncUnitsChanged(int mask, long units) {
        if ((mask & 1) != 0 && (Object) this instanceof SyncUnitSchema schema) {
            hbm$syncUnits |= units & schema.syncUnitMask();
        }
        hbm$syncDirty |= mask;
    }

    public final void markSyncEvent() {
        hbm$syncDirty |= 7;
    }

    public final void bindSync(SyncSource owner, int mask) {
        throw new IllegalStateException("A block entity cannot be another snapshot's child");
    }

    public final void unbindSync(SyncSource owner) {
        throw new IllegalStateException("A block entity cannot be another snapshot's child");
    }

    private void hbm$syncBind() {
        if (hbm$syncBound) return;
        SyncBindings.bindFields(this);
        hbm$syncBound = true;
    }

    public void networkPackNT(int range) {
        if (!hbm$syncWatched || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion)
            return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        SyncUnitState.Snapshot publication = hbm$syncGated(server);
        if (publication == null) return;
        hbm$syncedOnce = true;
        BeSyncTable.refresh(
                publication,
                (BlockEntity) this,
                server,
                getBlockPos(),
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType()),
                range,
                (Object) this instanceof SyncAudience audience ? audience : null);
    }

    public void networkPackNTTracking() {
        if (!hbm$syncWatched || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion)
            return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        SyncUnitState.Snapshot publication = hbm$syncGated(server);
        if (publication != null) {
            PacketWire.sendSyncTracking(
                    publication,
                    server,
                    getBlockPos(),
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType()));
        }
    }

    public final void syncToTracking() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        SyncUnitState.Snapshot publication = hbm$syncGated(server, true);
        if (publication != null)
            PacketWire.sendSyncTracking(
                    publication,
                    server,
                    getBlockPos(),
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType()));
    }

    public void markChanged() {
        ChunkUtil.markUnsaved(this);
    }

    private SyncUnitState.Snapshot hbm$syncUnitSnapshot(SyncUnitSchema schema) {
        hbm$syncBind();
        if (!hbm$syncRelays()) {
            if ((hbm$syncDirty & 1) == 0 && hbm$syncPublishedUnits != null)
                return hbm$syncPublishedUnits;
            if (hbm$syncUnitState == null) {
                hbm$syncUnitState = new SyncUnitState(schema);
            }
            hbm$syncUnitState.publish(hbm$syncUnits, (hbm$syncDirty & 4) != 0);
        }
        hbm$syncUnits = 0;
        hbm$syncDirty &= ~5;
        SyncUnitState.Snapshot current = hbm$syncUnitState.snapshot();
        if (hbm$syncPublishedUnits == current) return current;
        if (hbm$syncPublishedUnits == null) SyncWire.retain(this);
        hbm$syncPublishedUnits = current;
        hbm$syncVersion++;
        return current;
    }

    private SyncUnitState hbm$syncCapture(SyncUnitSchema schema) {
        if (!SyncWire.replayCompat()) return null;
        Level level = getLevel();
        if (level == null
                || !level.isClientSide() && !(level.getServer() instanceof WireReplayServer))
            return null;
        if (hbm$syncUnitState == null) hbm$syncUnitState = new SyncUnitState(schema);
        return hbm$syncUnitState;
    }

    private boolean hbm$syncRelays() {
        return SyncWire.replayCompat()
                && hbm$syncUnitState != null
                && hbm$syncUnitState.captured()
                && !getLevel().isClientSide();
    }

    private SyncUnitState.Snapshot hbm$syncGated(ServerLevel server) {
        return hbm$syncGated(server, false);
    }

    private SyncUnitState.Snapshot hbm$syncGated(ServerLevel server, boolean includePublished) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return null;
        if (!PacketWire.hasPlayersTracking(server, getBlockPos())) {
            syncWatching(false);
            if (hbm$syncedOnce) {
                hbm$syncedOnce = false;
                BeSyncTable.evict(server, getBlockPos(), (BlockEntity) this);
            }
            return null;
        }
        SyncUnitState.Snapshot publication = hbm$syncUnitSnapshot(schema);
        if (!includePublished && hbm$syncSentVersion == hbm$syncVersion) return null;
        hbm$syncSentVersion = hbm$syncVersion;
        return publication;
    }

    private void hbm$syncRemove() {
        releaseSync();
    }

    public final void releaseSync() {
        if ((Object) this instanceof BlobSynced source) source.syncBlob().release();
        hbm$syncClientUnitRevision = 0;
        if (getLevel() instanceof ServerLevel server) {
            PacketWire.dropSyncPosition(server, getBlockPos(), (BlockEntity) this);
        }
        if (getLevel() instanceof ServerLevel server) {
            hbm$syncedOnce = false;
            BeSyncTable.evict(server, getBlockPos(), (BlockEntity) this);
        }
        if (hbm$syncPublishedUnits != null) {
            hbm$syncPublishedUnits = null;
            SyncWire.release(this);
        }
        if (hbm$syncUnitState != null) hbm$syncUnitState.release();
        hbm$syncUnitState = null;
        hbm$syncUnits = 0;
        hbm$syncInitial = null;
        hbm$syncDirty = 3;
        hbm$syncWatched = true;
        if (hbm$syncBound) {
            SyncBindings.unbindFields(this);
            hbm$syncBound = false;
        }
    }

    public final void syncWatching(boolean watched) {
        hbm$syncWatched = watched;
        if ((Object) this instanceof BlobSynced source) source.syncBlob().watching(watched);
    }

    private void hbm$syncLoad(ValueInput input) {
        Optional<ByteBuffer> stored = input.read(SyncWire.KEY, Codec.BYTE_BUFFER);
        if (stored.isPresent()) {
            ByteBuf bytes = Unpooled.wrappedBuffer(stored.get());
            try {
                long revision = VarLong.read(bytes);
                SyncUnitSchema schema = (SyncUnitSchema) (Object) this;
                SyncUnitState capture = hbm$syncCapture(schema);
                if (capture == null) SyncUnitFrame.readUnits(schema, bytes, true);
                else
                    SyncUnitFrame.readUnits(
                            schema,
                            bytes,
                            true,
                            capture,
                            level.isClientSide() ? revision : SyncWire.nextRevision());
                schema.readInitialExtras(bytes);
                if (capture == null || level.isClientSide()) schema.afterInitialSyncUnits();
                else schema.afterRelayedSyncUnits(schema.syncUnitMask(), true);
                if (bytes.isReadable())
                    throw new DecoderException("Trailing machine sync unit data");
                hbm$syncClientUnitRevision =
                        schema.initialMatchesSyncUnits() ? revision : -revision;
            } finally {
                bytes.release();
            }
            if (level != null && level.isClientSide()) {
                int parts = (Object) this instanceof BlobSynced ? 3 : 1;
                ClientSyncRecovery.received(getBlockPos(), parts);
            }
        }
    }

    private boolean hbm$syncRecorded(ValueInput input) {
        if (!(getLevel() instanceof ServerLevel server)
                || !(server.getServer() instanceof WireReplayServer replay)) {
            return false;
        }
        Optional<ByteBuffer> frame = input.read(SyncWire.FRAME_KEY, Codec.BYTE_BUFFER);
        Optional<ByteBuffer> blob = input.read(SyncWire.BLOB_KEY, Codec.BYTE_BUFFER);
        if (frame.isEmpty() && blob.isEmpty()) return false;
        if (frame.isPresent()) {
            if (!((Object) this instanceof SyncUnitSchema schema)) {
                throw new DecoderException("Recorded unit state without a schema");
            }
            if (hbm$syncUnitState == null) hbm$syncUnitState = new SyncUnitState(schema);
            if (hbm$syncUnitState.snapshot() == null)
                hbm$syncUnitState.publish(schema.syncUnitMask());
            ByteBuf bytes = Unpooled.wrappedBuffer(frame.get());
            try {
                boolean full = bytes.readBoolean();
                long units =
                        SyncUnitFrame.readUnits(
                                schema, bytes, full, hbm$syncUnitState, SyncWire.nextRevision());
                if (bytes.isReadable())
                    throw new DecoderException("Trailing machine sync unit data");
                if (schema.afterRelayedSyncUnits(units, false) && replay.skipsViewerTicks()) {
                    replay.resendWhenCaughtUp(this);
                }
            } finally {
                bytes.release();
            }
        }
        if (blob.isPresent()) {
            if (!((Object) this instanceof BlobSynced source)) {
                throw new DecoderException("Recorded blob without a carrier");
            }
            ByteBuf bytes = Unpooled.wrappedBuffer(blob.get());
            try {
                source.syncBlob().ingest(bytes);
            } finally {
                bytes.release();
            }
            source.flushSyncBlob();
        }
        return true;
    }

    public void loadWithComponents(ValueInput input) {
        if (SyncWire.replayCompat() && hbm$syncRecorded(input)) return;
        super.loadWithComponents(input);
        hbm$syncLoad(input);
    }

    public void loadCustomOnly(ValueInput input) {
        super.loadCustomOnly(input);
        hbm$syncLoad(input);
    }

    public final boolean applyUnitSync(long revision, long base, ByteBuf body) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return false;
        if (revision <= Math.abs(hbm$syncClientUnitRevision)) return true;
        if (base != 0 && base != hbm$syncClientUnitRevision) return false;
        SyncUnitState capture = hbm$syncCapture(schema);
        if (capture == null) {
            if (base == 0) SyncUnitFrame.readInitial(schema, body);
            else SyncUnitFrame.read(schema, body);
        } else if (base == 0) {
            SyncUnitFrame.readUnits(schema, body, true, capture, revision);
            schema.afterInitialSyncUnits();
        } else {
            schema.afterSyncUnits(SyncUnitFrame.readUnits(schema, body, false, capture, revision));
        }
        if (body.isReadable()) throw new DecoderException("Trailing machine sync unit data");
        hbm$syncClientUnitRevision = revision;
        return true;
    }

    public final void syncTo(ServerPlayer player) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return;
        if ((SyncWire.initialLayout(getClass()) & SyncWire.INITIAL_EXTRAS) != 0
                && schema.initialMatchesSyncUnits()) {
            ChunkTrackerIndex.beginInitial(player);
            try {
                player.connection.send(ClientboundBlockEntityDataPacket.create(this));
            } finally {
                ChunkTrackerIndex.endInitial();
            }
            return;
        }
        SyncUnitState.Snapshot publication = hbm$syncUnitSnapshot(schema);
        int type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType());
        long base = publication.base(0);
        long units = publication.selectedUnits(base);
        PacketWire.sendTo(
                publication.retainFrame(getBlockPos().asLong(), type, base, units), player);
        ChunkTrackerIndex.snapshotSent(player, this, publication.revision());
        if ((Object) this instanceof BlobSynced source) source.syncBlob().sendFull(player);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        if (!((Object) this instanceof SyncUnitSchema schema))
            return super.getUpdateTag(registries);
        if (SyncWire.replayCompat() && getLevel() != null && getLevel().isClientSide()) {
            return hbm$syncReceivedTag(registries, schema);
        }
        return hbm$syncUnitUpdateTag(registries, schema);
    }

    private CompoundTag hbm$syncReceivedTag(
            HolderLookup.Provider registries, SyncUnitSchema schema) {
        CompoundTag tag = super.getUpdateTag(registries);
        SyncUnitState state = hbm$syncUnitState;
        SyncUnitState.Snapshot received = state == null ? null : state.snapshot();
        if (received == null) return tag;
        ByteBuf scratch = SyncWire.SCRATCH.get();
        scratch.clear();
        VarLong.write(scratch, received.revision());
        received.writeBody(schema.syncUnitMask(), scratch);
        schema.writeInitialExtras(scratch);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.getBytes(scratch.readerIndex(), bytes);
        tag.putByteArray(SyncWire.KEY, bytes);
        return tag;
    }

    private CompoundTag hbm$syncUnitUpdateTag(
            HolderLookup.Provider registries, SyncUnitSchema schema) {
        hbm$syncBind();
        SyncUnitState.Snapshot publication = hbm$syncUnitSnapshot(schema);
        int layout = SyncWire.initialLayout(getClass());
        boolean published = (layout & SyncWire.INITIAL_REPUBLISHES) != 0 || hbm$syncRelays();
        if (hbm$syncInitial != null
                && hbm$syncInitialVersion == hbm$syncVersion
                && (published && (layout & SyncWire.INITIAL_EXTRAS) == 0
                        || (hbm$syncDirty & 2) == 0 && !hbm$syncRelays())) {
            hbm$syncDirty &= ~2;
            ChunkTrackerIndex.initialSnapshot(this, publication, schema.initialMatchesSyncUnits());
            return hbm$syncInitial;
        }
        ByteBuf scratch = SyncWire.SCRATCH.get();
        scratch.clear();
        VarLong.write(scratch, publication.revision());
        if (published) publication.writeBody(schema.syncUnitMask(), scratch);
        else SyncUnitFrame.writeInitial(schema, scratch);
        schema.writeInitialExtras(scratch);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.getBytes(scratch.readerIndex(), bytes);
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByteArray(SyncWire.KEY, bytes);
        hbm$syncDirty &= ~2;
        hbm$syncInitialVersion = hbm$syncVersion;
        ChunkTrackerIndex.initialSnapshot(this, publication, schema.initialMatchesSyncUnits());
        return hbm$syncInitial = tag;
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        if ((Object) this instanceof SyncUnitSchema) syncToTracking();
        return null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$syncRemove();
    }
}
