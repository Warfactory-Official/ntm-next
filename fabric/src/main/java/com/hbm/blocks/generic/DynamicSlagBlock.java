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

package com.hbm.blocks.generic;

import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.items.machine.ScrapsItem;
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
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DynamicSlagBlock extends Block implements EntityBlock {

    private static final Direction[] SIDES = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public DynamicSlagBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof BlockEntitySlag slag && slag.amount > 0) {
            return Shapes.box(
                    0D,
                    0D,
                    0D,
                    1D,
                    Math.min(1D, (double) slag.amount / BlockEntitySlag.MAX_AMOUNT),
                    1D);
        }
        return Shapes.box(0D, 0D, 0D, 1D, 0.0625D, 1D);
    }

    public static boolean spill(Level level, BlockPos pos, @Nullable Material mat, int amount) {
        DynamicSlagBlock block = ModBlocks.SLAG.get();
        if (!level.setBlockAndUpdate(pos, block.defaultBlockState())) return false;
        BlockEntitySlag pile = (BlockEntitySlag) level.getBlockEntity(pos);
        pile.hbm$syncSet$BlockEntitySlag$mat(mat);
        pile.hbm$syncSet$BlockEntitySlag$amount(amount);
        pile.setChanged();
        level.scheduleTick(pos, block, 1);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySlag self)) {
            level.removeBlock(pos, false);
            return;
        }
        if (!fall(level, pos, self)) spread(level, pos, self);
    }

    private boolean fall(ServerLevel level, BlockPos pos, BlockEntitySlag self) {
        BlockPos below = pos.below();
        if (below.getY() >= level.getMinY() && level.getBlockState(below).canBeReplaced()) {
            spill(level, below, self.mat, self.amount);
            level.removeBlock(pos, false);
            return true;
        }
        if (!(level.getBlockEntity(below) instanceof BlockEntitySlag pile)
                || pile.mat != self.mat
                || pile.amount >= BlockEntitySlag.MAX_AMOUNT) return false;
        self.hbm$syncSet$BlockEntitySlag$amount((int) (self.amount - (pile.absorb(self.amount))));
        level.scheduleTick(below, this, 1);
        if (self.amount <= 0) level.removeBlock(pos, false);
        else self.setChanged();
        return true;
    }

    private void spread(ServerLevel level, BlockPos pos, BlockEntitySlag self) {
        if (self.amount < BlockEntitySlag.MAX_AMOUNT / 5) return;
        int open = 0;
        for (int i = 0; i < SIDES.length; i++) {
            if (level.getBlockState(pos.relative(SIDES[i])).canBeReplaced()) open |= 1 << i;
        }
        if (open == 0) return;
        int share = Math.max(self.amount / (2 * Integer.bitCount(open)), 1);
        for (int i = 0; i < SIDES.length; i++) {
            if ((open & 1 << i) == 0) continue;
            spill(level, pos.relative(SIDES[i]), self.mat, share);
            self.hbm$syncSet$BlockEntitySlag$amount((int) (self.amount - (share)));
        }
        self.setChanged();
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.isCreative()
                && level.getBlockEntity(pos) instanceof BlockEntitySlag slag) {
            ItemStack scraps = slag.scraps();
            if (!scraps.isEmpty() && survives(level.getRandom(), 1F)) {
                level.addFreshEntity(
                        new ItemEntity(
                                level,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                scraps));
                slag.hbm$syncSet$BlockEntitySlag$amount(0);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof BlockEntitySlag slag)) {
            return List.of();
        }
        ItemStack scraps = slag.scraps();
        if (scraps.isEmpty()) return List.of();
        Float radius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        return survives(params.getLevel().getRandom(), radius == null ? 1F : 1F / radius)
                ? List.of(scraps)
                : List.of();
    }

    private static boolean survives(RandomSource random, float chance) {
        return random.nextFloat() <= chance;
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof BlockEntitySlag slag && slag.mat != null) {
            return ScrapsItem.create(new MaterialStack(slag.mat, slag.amount));
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySlag(pos, state);
    }

    public static class BlockEntitySlag extends BlockEntity
            implements Synced, SyncUnitSchema, GraphResident {

        public static final int MAX_AMOUNT = MaterialShape.BLOCK.amount(16);

        public @Nullable Material mat;
        public int amount;

        public BlockEntitySlag(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FOUNDRY_SLAG.get(), pos, state);
        }

        public int absorb(int offered) {
            int taken = Math.min(MAX_AMOUNT - amount, offered);
            hbm$syncSet$BlockEntitySlag$amount((int) (amount + (taken)));
            setChanged();
            return taken;
        }

        ItemStack scraps() {
            return mat != null && amount > 0
                    ? ScrapsItem.create(new MaterialStack(mat, amount))
                    : ItemStack.EMPTY;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            networkPackNT(50);
        }

        @Override
        public long syncUnitMask() {
            return 3L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> output.writeInt(mat == null ? -1 : mat.id());
                case 1 -> output.writeInt(amount);
                default -> throw new IllegalArgumentException();
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> hbm$syncSet$BlockEntitySlag$mat(MaterialRegistry.byId(input.readInt()));
                case 1 -> hbm$syncSet$BlockEntitySlag$amount(input.readInt());
                default -> throw new IllegalArgumentException();
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            this.hbm$syncSet$BlockEntitySlag$mat(MaterialRegistry.byId(input.getIntOr("mat", -1)));
            this.hbm$syncSet$BlockEntitySlag$amount(input.getIntOr("amount", 0));
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (this.mat != null) output.putInt("mat", this.mat.id());
            output.putInt("amount", this.amount);
        }

        public final void hbm$syncSet$BlockEntitySlag$mat(Material next) {
            if (this.mat != next) {
                boolean bound = syncBound();
                if (bound) com.hbm.packet.SyncBindings.unbind(this, this.mat);
                this.mat = next;
                if (bound) bindSyncValue(next, 3, 1L);
                if ((1L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 1L);
            }
        }

        public final void hbm$syncSet$BlockEntitySlag$amount(int next) {
            if (this.amount != next) {
                this.amount = next;
                if ((2L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 2L);
            }
        }

        static {
            com.hbm.packet.SyncBindings.field(BlockEntitySlag.class, "mat", 3, 1L);
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
            if (!hbm$syncWatched
                    || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion) return;
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
            if (!hbm$syncWatched
                    || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion) return;
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
                                    schema,
                                    bytes,
                                    full,
                                    hbm$syncUnitState,
                                    SyncWire.nextRevision());
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
                schema.afterSyncUnits(
                        SyncUnitFrame.readUnits(schema, body, false, capture, revision));
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
                ChunkTrackerIndex.initialSnapshot(
                        this, publication, schema.initialMatchesSyncUnits());
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
}
