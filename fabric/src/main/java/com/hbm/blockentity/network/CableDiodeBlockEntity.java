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

package com.hbm.blockentity.network;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.container.ScreenMenu;
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
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.util.ChunkUtil;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class CableDiodeBlockEntity extends BlockEntity
        implements EnergyHandler,
                GraphResident,
                Synced,
                SyncUnitSchema,
                MenuProvider,
                ScreenMenu.Host,
                RenderDataBlockEntity {
    public static final long MAX_THROUGHPUT = 10_000_000_000L;

    private long throughput;

    {
        hbm$syncSet$CableDiodeBlockEntity$throughput(1_000);
    }

    private ConnectionPriority receiverPriority;

    {
        hbm$syncSet$CableDiodeBlockEntity$receiverPriority(ConnectionPriority.NORMAL);
    }

    private int contacts;
    private long ready;
    private long arriving;
    private long directProjection;
    private long accepted;
    private long operationTick = Long.MIN_VALUE;
    private int directAttempts;
    private boolean forwarding;
    private @Nullable Direction cachedOutput;
    private @Nullable BlockPos outputPosition;
    private @Nullable BlockLookupCache<EnergyHandler> outputReceiver;

    public CableDiodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    private void beginOperation() {
        long tick = level.getGameTime();
        if (operationTick == tick) return;
        operationTick = tick;
        ready += arriving;
        arriving = 0;
        directProjection = 0;
        accepted = 0;
        directAttempts = 0;
    }

    private Direction output() {
        return getBlockState().getValue(CableDiodeBlock.FACING).getOpposite();
    }

    private void prepareOutput() {
        Direction direction = output();
        if (direction == cachedOutput) return;
        cachedOutput = direction;
        outputPosition = worldPosition.relative(direction);
        outputReceiver = null;
    }

    public boolean acceptsFace(Direction side, CapRole role) {
        return role == CapRole.POWER_OUT ? side == output() : side != output();
    }

    @Override
    public long transferPower(long amount) {
        if (forwarding) return amount;
        beginOperation();
        long offered = Math.min(amount, Math.max(0, throughput - accepted));
        if (offered == 0) return amount;
        prepareOutput();
        var server = (ServerLevel) level;
        var graph = PowerGraph.TYPE.loaded(server);
        long outputKey = outputPosition.asLong();
        if (graph != null && graph.containsCell(outputKey)) {
            if (!graph.isOpenAt(outputKey, cachedOutput.getOpposite())) return amount;
            long received = Math.min(offered, Math.max(0, throughput * 2 - ready - arriving));
            arriving += received;
            accepted += received;
            if (received != 0) onEnergyChanged();
            return amount - received;
        }
        if (directAttempts == 10) return amount;
        directAttempts++;
        if (outputReceiver == null) {
            outputReceiver =
                    Caps.createCache(
                            EnergyCaps.RECEIVER,
                            server,
                            outputPosition,
                            cachedOutput.getOpposite());
        }
        var receiver = outputReceiver.find();
        if (receiver == null || !receiver.canReceiveDirectly()) return amount;
        forwarding = true;
        try {
            long received = offered - receiver.transferPower(offered);
            if (received <= 0) return amount;
            directProjection += received;
            accepted += received;
            return amount - received;
        } finally {
            forwarding = false;
        }
    }

    @Override
    public long getPower() {
        beginOperation();
        return Math.min(throughput, arriving + directProjection);
    }

    @Override
    public void setPower(long stored) {
        beginOperation();
        directProjection = stored;
    }

    @Override
    public long getMaxPower() {
        return throughput;
    }

    @Override
    public long getSendRate() {
        beginOperation();
        return Math.min(ready, throughput);
    }

    @Override
    public void usePower(long amount) {
        beginOperation();
        ready -= amount;
        if (amount != 0) onEnergyChanged();
    }

    @Override
    public ConnectionPriority getEnergyTier() {
        return receiverPriority;
    }

    @Override
    public boolean passThrough() {
        return true;
    }

    public long throughputLimit() {
        return throughput;
    }

    public ConnectionPriority receiverPriority() {
        return receiverPriority;
    }

    public void applySettings(
            boolean limitPresent,
            long limit,
            boolean priorityPresent,
            ConnectionPriority priority) {
        long nextLimit = limitPresent ? Math.clamp(limit, 0, MAX_THROUGHPUT) : throughput;
        ConnectionPriority nextPriority = priorityPresent ? priority : receiverPriority;
        if (nextLimit == throughput && nextPriority == receiverPriority) return;
        hbm$syncSet$CableDiodeBlockEntity$throughput(nextLimit);
        hbm$syncSet$CableDiodeBlockEntity$receiverPriority(nextPriority);
        setChanged();
        networkPackNT(15);
    }

    public void increaseLimit() {
        long next = 10;
        while (next <= throughput && next < MAX_THROUGHPUT) next *= 10;
        applySettings(true, next, false, receiverPriority);
    }

    public void decreaseLimit() {
        if (throughput <= 10) return;
        long next = 10;
        while (next * 10 < throughput) next *= 10;
        applySettings(true, next, false, receiverPriority);
    }

    public void cyclePriority() {
        int next = (receiverPriority.ordinal() + 1) % ConnectionPriority.VALUES.length;
        applySettings(false, throughput, true, ConnectionPriority.VALUES[next]);
    }

    public int connectionMask() {
        return contacts;
    }

    public void refreshContacts(ServerLevel server) {
        int next = 0;
        var neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.VALUES) {
            neighbour.setWithOffset(worldPosition, direction);
            if (PhysicalGraph.powerContact(server, neighbour, direction.getOpposite())) {
                next |= 1 << direction.ordinal();
            }
        }
        if (next == contacts) return;
        hbm$syncSet$CableDiodeBlockEntity$contacts(next);
        networkPackNTTracking();
    }

    public void updateContact(Direction direction, boolean joins, boolean knownAir) {
        int bit = 1 << direction.ordinal();
        int next =
                joins
                        ? contacts | bit
                        : level.isClientSide() && !knownAir ? contacts : contacts & ~bit;
        if (next == contacts) return;
        hbm$syncSet$CableDiodeBlockEntity$contacts(next);
        if (level.isClientSide()) refreshModel();
        else networkPackNTTracking();
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshContacts(server);
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        if (!world.isClientSide()) return;
        var neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.VALUES) {
            neighbour.setWithOffset(worldPosition, direction);
            var state = ChunkUtil.blockStateIfLoaded(world, neighbour);
            if (state == null) continue;
            int bit = 1 << direction.ordinal();
            if (PhysicalGraph.powerContact(world, neighbour, direction.getOpposite()))
                hbm$syncSet$CableDiodeBlockEntity$contacts((int) (contacts | (bit)));
            else if (state.isAir())
                hbm$syncSet$CableDiodeBlockEntity$contacts((int) (contacts & (~bit)));
        }
        refreshModel();
    }

    private void refreshModel() {
        var state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public Object getRenderData() {
        return contacts;
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScreenMenu<>(ModMenus.CABLE_DIODE.get(), containerId, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5)
                <= 128;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("limit", throughput);
        output.putByte("p", (byte) receiverPriority.ordinal());
        long stock = ready + arriving;
        if (stock > 0) output.putLong("stock", stock);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CableDiodeBlockEntity$throughput(
                Math.clamp(input.getLongOr("limit", 1_000), 0, MAX_THROUGHPUT));
        hbm$syncSet$CableDiodeBlockEntity$receiverPriority(
                ConnectionPriority.VALUES[input.getByteOr("p", (byte) 2)]);
        ready = Math.max(0, input.getLongOr("stock", 0));
        arriving = 0;
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(throughput);
            case 1 -> output.writeByte(receiverPriority.ordinal());
            case 2 -> output.writeByte(contacts);
            default -> throw new IllegalArgumentException("Unknown power limiter unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$CableDiodeBlockEntity$throughput(input.readLong());
            case 1 ->
                    hbm$syncSet$CableDiodeBlockEntity$receiverPriority(
                            ConnectionPriority.VALUES[input.readUnsignedByte()]);
            case 2 -> hbm$syncSet$CableDiodeBlockEntity$contacts(input.readUnsignedByte());
            default -> throw new IllegalArgumentException("Unknown power limiter unit " + unit);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 4) != 0) refreshModel();
    }

    public final void hbm$syncSet$CableDiodeBlockEntity$throughput(long next) {
        if (this.throughput != next) {
            this.throughput = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$CableDiodeBlockEntity$receiverPriority(ConnectionPriority next) {
        if (this.receiverPriority != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.receiverPriority);
            this.receiverPriority = next;
            if (bound) bindSyncValue(next, 3, 2);
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$CableDiodeBlockEntity$contacts(int next) {
        if (this.contacts != next) {
            this.contacts = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CableDiodeBlockEntity.class, "receiverPriority", 3, 2);
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
