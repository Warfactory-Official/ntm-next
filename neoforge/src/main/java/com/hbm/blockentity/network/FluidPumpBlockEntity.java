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

import com.hbm.api.fluidmk2.*;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.ChunkDuctNodes;
import com.hbm.blocks.network.FluidPumpBlock;
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
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.ChunkUtil;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FluidPumpBlockEntity extends BlockEntity
        implements BlockEntityTicks,
                Synced,
                GraphResident,
                FluidForwarder,
                ComponentFluidReceiver,
                MenuProvider,
                ScreenMenu.Host,
                SyncUnitSchema {

    public static final int MAX_RATE = 10_000;
    public static final int DEFAULT_RATE = 100;

    public @Nullable Fluid type;

    public boolean setFluidType(@Nullable Fluid fluid) {
        if (fluid == Fluids.EMPTY) fluid = null;
        if (type == fluid) return false;
        hbm$syncSet$FluidPumpBlockEntity$type(fluid);
        ChunkDuctNodes.schedule(this);
        return true;
    }

    public boolean carries(@Nullable Fluid fluid) {
        return fluid == null || fluid == type;
    }

    public int pressure;
    public int rate;

    {
        hbm$syncSet$FluidPumpBlockEntity$rate(DEFAULT_RATE);
    }

    public ConnectionPriority priority;

    {
        hbm$syncSet$FluidPumpBlockEntity$priority(ConnectionPriority.NORMAL);
    }

    private long moved;
    private long allowanceTick = Long.MIN_VALUE;
    private long lastMoved;
    private boolean recursionBrake;
    private boolean redstone;

    public void refreshRedstone() {
        boolean was = redstone;
        redstone = level.hasNeighborSignal(worldPosition);
        if (redstone != was) markChanged();
    }

    private @Nullable BlockLookupCache<FluidHandler> outputReceiver;
    private @Nullable Fluid outputFluid;

    public FluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_PUMP.get(), pos, state);
    }

    @Override
    public void tickServer() {
        refreshAllowance();
        networkPackNT(15);
    }

    private void refreshAllowance() {
        long tick = level.getGameTime();
        if (allowanceTick == tick) return;
        allowanceTick = tick;
        hbm$syncSet$FluidPumpBlockEntity$lastMoved(moved);
        moved = 0;
    }

    public long lastMoved() {
        return lastMoved;
    }

    public Direction intake() {
        return getBlockState().getValue(FluidPumpBlock.FACING).getClockWise();
    }

    @Override
    public void setBlockState(BlockState state) {
        if (getBlockState().getValue(FluidPumpBlock.FACING)
                != state.getValue(FluidPumpBlock.FACING)) {
            outputReceiver = null;
        }
        super.setBlockState(state);
    }

    public Direction output() {
        return intake().getOpposite();
    }

    @Override
    public boolean canReceiveFluid(Fluid fluid) {
        return fluid != Fluids.EMPTY && fluid == type;
    }

    @Override
    public boolean canProvideFluid(Fluid fluid) {
        return fluid != Fluids.EMPTY && fluid == type;
    }

    @Override
    public long getDemand(Fluid fluid, DataComponentPatch components, int lane) {
        if (redstone || fluid != type || fluid == null || lane != pressure) return 0;
        return getReceiverSpeed(fluid, lane);
    }

    @Override
    public long getReceiverSpeed(Fluid fluid, int lane) {
        refreshAllowance();
        return Math.max(0, rate - moved);
    }

    @Override
    public int receivingPressureMask(Fluid fluid, DataComponentPatch components) {
        return 1 << pressure;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return priority;
    }

    @Override
    public long beginForwarding(Fluid fluid, int lane, int outputOrdinal) {
        if (isRemoved()
                || recursionBrake
                || redstone
                || fluid != type
                || lane != pressure
                || output().ordinal() != outputOrdinal) return 0;
        long available = getReceiverSpeed(fluid, lane);
        if (available > 0) recursionBrake = true;
        return available;
    }

    @Override
    public void finishForwarding(long amount) {
        moved += amount;
        recursionBrake = false;
    }

    @Override
    public @Nullable FluidHandler directOutput(Fluid fluid) {
        if (outputReceiver == null || outputFluid != fluid) {
            outputFluid = fluid;
            Direction out = output();
            outputReceiver =
                    Caps.createCache(
                            FluidCaps.RECEIVER,
                            (ServerLevel) level,
                            worldPosition.relative(out),
                            FluidFace.of(out.getOpposite(), fluid));
        }
        return outputReceiver.find();
    }

    @Override
    public long transferFluid(Fluid fluid, DataComponentPatch components, int lane, long amount) {
        if (amount <= 0) return amount;
        Direction out = output();
        long toMove = Math.min(amount, beginForwarding(fluid, lane, out.ordinal()));
        if (toMove <= 0) return amount;
        long transferred = 0;
        try {
            ServerLevel sl = (ServerLevel) level;
            long outKey = worldPosition.relative(out).asLong();
            LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(sl, outKey);
            GraphNode<PipeData> node = graph == null ? null : graph.getNode(outKey);
            if (node != null && node.isOpen(out.getOpposite()) && node.data.fluid() == fluid) {
                transferred =
                        FluidNetwork.forward(sl, graph, node.net, fluid, components, lane, toMove);
            } else {
                FluidHandler rec = directOutput(fluid);
                if (rec == null
                        || rec == this
                        || (rec.receivingPressureMask(fluid, components) & 1 << lane) == 0)
                    return amount;
                long grant =
                        Math.min(
                                toMove,
                                Math.min(
                                        rec.getDemand(fluid, components, lane),
                                        rec.getReceiverSpeed(fluid, lane)));
                transferred =
                        grant <= 0 ? 0 : grant - rec.transferFluid(fluid, components, lane, grant);
            }
            return amount - transferred;
        } finally {
            finishForwarding(transferred);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.fluid_pump");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScreenMenu<>(ModMenus.FLUID_PUMP.get(), containerId, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                <= 128;
    }

    public void setControlSettings(
            boolean ratePresent,
            int rate,
            boolean pressurePresent,
            int pressure,
            boolean priorityPresent,
            ConnectionPriority priority) {
        if (ratePresent) this.hbm$syncSet$FluidPumpBlockEntity$rate(Math.clamp(rate, 0, MAX_RATE));
        if (pressurePresent)
            this.hbm$syncSet$FluidPumpBlockEntity$pressure(Math.clamp(pressure, 0, 5));
        if (priorityPresent) this.hbm$syncSet$FluidPumpBlockEntity$priority(priority);
        if (ratePresent || pressurePresent || priorityPresent) setChanged();
    }

    private void writeType(ByteBuf output) {
        output.writeInt(type == null ? -1 : BuiltInRegistries.FLUID.getId(type));
    }

    private void readType(ByteBuf input) {
        int id = input.readInt();
        Fluid decoded = id < 0 ? null : BuiltInRegistries.FLUID.byId(id);
        hbm$syncSet$FluidPumpBlockEntity$type(
                (decoded == null || decoded == Fluids.EMPTY) ? null : decoded);
    }

    private void writePressure(ByteBuf output) {
        output.writeByte(pressure);
    }

    private void readPressure(ByteBuf input) {
        hbm$syncSet$FluidPumpBlockEntity$pressure(Math.clamp(input.readByte(), 0, 5));
    }

    private void readRate(ByteBuf input) {
        hbm$syncSet$FluidPumpBlockEntity$rate(Math.clamp(input.readInt(), 0, MAX_RATE));
    }

    private void writePriority(ByteBuf output) {
        output.writeByte(priority.ordinal());
    }

    private void readPriority(ByteBuf input) {
        hbm$syncSet$FluidPumpBlockEntity$priority(ConnectionPriority.VALUES[input.readByte()]);
    }

    private void readLastMoved(ByteBuf input) {
        hbm$syncSet$FluidPumpBlockEntity$lastMoved(Math.max(input.readLong(), 0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String id = input.getStringOr("type", "");
        if (!id.isEmpty()) {
            Fluid decoded = BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
            setFluidType(decoded);
        }
        hbm$syncSet$FluidPumpBlockEntity$pressure(
                Math.clamp(input.getIntOr("pressure", pressure), 0, 5));
        hbm$syncSet$FluidPumpBlockEntity$rate(
                Math.clamp(input.getIntOr("rate", rate), 0, MAX_RATE));
        hbm$syncSet$FluidPumpBlockEntity$priority(
                ConnectionPriority.VALUES[input.getByteOr("p", (byte) priority.ordinal())]);
        redstone = input.getBooleanOr("redstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (type != null) output.putString("type", BuiltInRegistries.FLUID.getKey(type).toString());
        output.putInt("pressure", pressure);
        output.putInt("rate", rate);
        output.putByte("p", (byte) priority.ordinal());
        output.putBoolean("redstone", redstone);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeType(output);
            case 1 -> writePressure(output);
            case 2 -> output.writeInt(this.rate);
            case 3 -> writePriority(output);
            case 4 -> output.writeLong(this.lastMoved);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readType(input);
            case 1 -> readPressure(input);
            case 2 -> readRate(input);
            case 3 -> readPriority(input);
            case 4 -> readLastMoved(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$FluidPumpBlockEntity$type(Fluid next) {
        if (this.type != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.type);
            this.type = next;
            if (bound) bindSyncValue(next, 3, 1L << 0);
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$FluidPumpBlockEntity$pressure(int next) {
        if (this.pressure != next) {
            this.pressure = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$FluidPumpBlockEntity$rate(int next) {
        if (this.rate != next) {
            this.rate = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$FluidPumpBlockEntity$priority(ConnectionPriority next) {
        if (this.priority != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.priority);
            this.priority = next;
            if (bound) bindSyncValue(next, 3, 1L << 3);
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$FluidPumpBlockEntity$lastMoved(long next) {
        if (this.lastMoved != next) {
            this.lastMoved = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FluidPumpBlockEntity.class, "type", 3, 1L << 0);
        com.hbm.packet.SyncBindings.field(FluidPumpBlockEntity.class, "priority", 3, 1L << 3);
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
