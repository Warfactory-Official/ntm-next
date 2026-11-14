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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.handler.radio.RadioChannel;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncAudience;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitFrame;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.SyncUnitState;
import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import com.hbm.util.ChunkUtil;
import com.hbm.util.TickPhase;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RBMKGraphBlockEntity extends BlockEntity
        implements BlockEntityTicks,
                Synced,
                GraphResident,
                MenuProvider,
                ScreenMenu.Host,
                SyncUnitSchema {

    public static final int GRAPHS = 2;
    public static final int SAMPLES = 30;
    public static final int SAMPLE_PERIOD = 10;

    public final GraphUnit[] graphs = new GraphUnit[GRAPHS];

    public RBMKGraphBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_GRAPH.get(), pos, state);
        for (int i = 0; i < GRAPHS; i++)
            hbm$syncArraySet$RBMKGraphBlockEntity$graphs(graphs, i, new GraphUnit(i));
    }

    @Override
    public void tickServer() {
        if (TickPhase.every(this, SAMPLE_PERIOD)) {
            for (GraphUnit unit : this.graphs) {
                long[] values = unit.values.clone();
                unit.update(level);
                if (!Arrays.equals(values, unit.values)) this.markChanged();
            }
        }
        this.networkPackNT(50);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < GRAPHS; i++) graphs[i].load(input, i);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < GRAPHS; i++) graphs[i].save(output, i);
    }

    public void configureGraph(
            int index,
            boolean active,
            boolean polling,
            String label,
            String channel,
            boolean minimumPresent,
            long minimum,
            boolean maximumPresent,
            long maximum) {
        GraphUnit graph = graphs[index];
        graph.hbm$syncSet$GraphUnit$active(active);
        graph.hbm$syncSet$GraphUnit$polling(polling);
        graph.hbm$syncSet$GraphUnit$label(label);
        graph.hbm$syncSet$GraphUnit$rtty(channel);
        graph.hbm$syncSet$GraphUnit$minBound(minimumPresent);
        if (minimumPresent) graph.hbm$syncSet$GraphUnit$min(minimum);
        graph.hbm$syncSet$GraphUnit$maxBound(maximumPresent);
        if (maximumPresent) graph.hbm$syncSet$GraphUnit$max(maximum);
        if (graph.max < graph.min) {
            long temp = graph.max;
            graph.hbm$syncSet$GraphUnit$max(graph.min);
            graph.hbm$syncSet$GraphUnit$min(temp);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rbmkGraph");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScreenMenu<>(ModMenus.RBMK_GRAPH.get(), containerId, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 15 * 15;
    }

    public static class GraphUnit implements SyncSource {
        public boolean polling;
        public String label;

        {
            hbm$syncSet$GraphUnit$label("");
        }

        public String rtty;

        {
            hbm$syncSet$GraphUnit$rtty("");
        }

        public final long[] values = new long[SAMPLES];
        public boolean active;
        public long min;
        public boolean minBound;

        {
            hbm$syncSet$GraphUnit$minBound(false);
        }

        public long max;
        public boolean maxBound;

        {
            hbm$syncSet$GraphUnit$maxBound(false);
        }

        public GraphUnit(int initialIndex) {
            hbm$syncSet$GraphUnit$label("Graph " + (initialIndex + 1));
        }

        public void update(Level level) {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;

            RadioChannel chan = RadioSystem.read(level, rtty);
            long sigVal = 0;

            if (chan != null && chan.stamp() < level.getGameTime() - 1) chan = null;

            if (chan != null) {
                try {
                    sigVal = Long.parseLong(chan.text());
                } catch (Exception ex) {
                }
                pushValue(sigVal);
            } else {
                if (polling) pushValue(0);
            }
        }

        public void pushValue(long value) {
            SyncArrays.copy(this, 3, values, 1, values, 0, values.length - 1);
            hbm$syncArraySet$GraphUnit$values(values, values.length - 1, value);
        }

        public void serialize(ByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeBoolean(polling);
            ByteBufCodecs.STRING_UTF8.encode(buf, label);
            ByteBufCodecs.STRING_UTF8.encode(buf, rtty);
            buf.writeBoolean(minBound);
            if (minBound) buf.writeLong(min);
            buf.writeBoolean(maxBound);
            if (maxBound) buf.writeLong(max);
            if (active) for (long value : values) buf.writeLong(value);
        }

        public void deserialize(ByteBuf buf) {
            hbm$syncSet$GraphUnit$active(buf.readBoolean());
            hbm$syncSet$GraphUnit$polling(buf.readBoolean());
            hbm$syncSet$GraphUnit$label(ByteBufCodecs.STRING_UTF8.decode(buf));
            hbm$syncSet$GraphUnit$rtty(ByteBufCodecs.STRING_UTF8.decode(buf));
            hbm$syncSet$GraphUnit$minBound(buf.readBoolean());
            if (minBound) hbm$syncSet$GraphUnit$min(buf.readLong());
            hbm$syncSet$GraphUnit$maxBound(buf.readBoolean());
            if (maxBound) hbm$syncSet$GraphUnit$max(buf.readLong());
            if (active)
                for (int i = 0; i < values.length; i++)
                    hbm$syncArraySet$GraphUnit$values(values, i, buf.readLong());
        }

        public void load(ValueInput input, int index) {
            this.hbm$syncSet$GraphUnit$active(input.getBooleanOr("active" + index, false));
            this.hbm$syncSet$GraphUnit$polling(input.getBooleanOr("polling" + index, false));
            this.hbm$syncSet$GraphUnit$label(input.getStringOr("label" + index, ""));
            this.hbm$syncSet$GraphUnit$rtty(input.getStringOr("rtty" + index, ""));
            this.hbm$syncSet$GraphUnit$minBound(input.getBooleanOr("minBound" + index, false));
            this.hbm$syncSet$GraphUnit$min(input.getLongOr("min" + index, 0L));
            this.hbm$syncSet$GraphUnit$maxBound(input.getBooleanOr("maxBound" + index, false));
            this.hbm$syncSet$GraphUnit$max(input.getLongOr("max" + index, 0L));
            for (int i = 0; i < values.length; i++)
                hbm$syncArraySet$GraphUnit$values(
                        values, i, input.getLongOr("value" + index + "_" + i, 0L));
        }

        public void save(ValueOutput output, int index) {
            output.putBoolean("active" + index, active);
            output.putBoolean("polling" + index, polling);
            output.putString("label" + index, label);
            output.putString("rtty" + index, rtty);
            output.putBoolean("minBound" + index, minBound);
            output.putLong("min" + index, min);
            output.putBoolean("maxBound" + index, maxBound);
            output.putLong("max" + index, max);
            for (int i = 0; i < values.length; i++)
                output.putLong("value" + index + "_" + i, values[i]);
        }

        public final void hbm$syncSet$GraphUnit$polling(boolean next) {
            if (this.polling != next) {
                this.polling = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$GraphUnit$label(String next) {
            if (this.label != next) {
                boolean changed = !java.util.Objects.equals(this.label, next);
                this.label = next;
                if (changed)
                    if ((0L) == 0L) syncChanged(3);
                    else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$GraphUnit$rtty(String next) {
            if (this.rtty != next) {
                boolean changed = !java.util.Objects.equals(this.rtty, next);
                this.rtty = next;
                if (changed)
                    if ((0L) == 0L) syncChanged(3);
                    else syncUnitsChanged(3, 0L);
            }
        }

        public final long hbm$syncArraySet$GraphUnit$values(long[] array, int index, long next) {
            if (array[index] != next) {
                array[index] = next;
                if (array == this.values) {
                    syncArrayChanged(array, index, 3, 0L);
                }
            }
            return next;
        }

        public final void hbm$syncSet$GraphUnit$active(boolean next) {
            if (this.active != next) {
                this.active = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$GraphUnit$min(long next) {
            if (this.min != next) {
                this.min = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$GraphUnit$minBound(boolean next) {
            if (this.minBound != next) {
                this.minBound = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$GraphUnit$max(long next) {
            if (this.max != next) {
                this.max = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$GraphUnit$maxBound(boolean next) {
            if (this.maxBound != next) {
                this.maxBound = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        static {
            com.hbm.packet.SyncBindings.field(GraphUnit.class, "label", 3, 0L);
            com.hbm.packet.SyncBindings.field(GraphUnit.class, "rtty", 3, 0L);
            com.hbm.packet.SyncBindings.field(GraphUnit.class, "values", 3, 0L);
        }

        private SyncSource hbm$syncOwner;

        private int hbm$syncMask;

        private int hbm$syncBindings;

        private long hbm$syncUnits;

        public final boolean syncBound() {
            return hbm$syncOwner != null;
        }

        public final void syncChanged(int mask) {
            if (hbm$syncOwner == null) return;
            if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
            else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
        }

        public final void syncUnitsChanged(int mask, long units) {
            syncChanged(mask);
        }

        public final void bindSync(SyncSource owner, int mask) {
            bindSyncUnits(owner, mask, 0);
        }

        public final void bindSyncUnits(SyncSource owner, int mask, long units) {
            if (hbm$syncOwner != null && hbm$syncOwner != owner) {
                throw new IllegalStateException("Mutable sync state has two owners");
            }
            hbm$syncOwner = owner;
            hbm$syncMask |= mask;
            hbm$syncUnits |= units;
            if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
        }

        public final void unbindSync(SyncSource owner) {
            assert hbm$syncOwner == owner;
            if (--hbm$syncBindings != 0) return;
            SyncBindings.unbindFields(this, owner);
            hbm$syncOwner = null;
            hbm$syncMask = 0;
            hbm$syncUnits = 0;
        }
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= graphs.length) throw new IllegalArgumentException();
        graphs[unit].serialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= graphs.length) throw new IllegalArgumentException();
        graphs[unit].deserialize(input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.graphs) {
            SyncBindings.bindIndexed(this, value, flags, 0, 2, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == graphs) {
            long selected = index < 0 ? 0x3L : 1L << index;
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, graphs[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }

    public final GraphUnit hbm$syncArraySet$RBMKGraphBlockEntity$graphs(
            GraphUnit[] array, int index, GraphUnit next) {
        if (array[index] != next) {
            if (array == this.graphs && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.graphs) {
                syncArrayChanged(array, index, 3, 3L);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(RBMKGraphBlockEntity.class, "graphs", 3, 3L);
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
