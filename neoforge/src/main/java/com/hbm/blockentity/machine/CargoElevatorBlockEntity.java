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

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CargoElevatorBlock;
import com.hbm.blocks.machine.CargoElevatorShape;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.ClientSyncRecovery;
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
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class CargoElevatorBlockEntity extends BlockEntity
        implements BlockEntityTicks, Synced, SyncUnitSchema, RadioControlHandler {
    private static final String[] RADIO_COMMANDS = {"VAL:extension", "FUN:setextension"};
    private static final Vec3 DOWNWARD_SETTLE = new Vec3(0D, -0.125D, 0D);
    private boolean platformVisible;
    private int height;
    private double extension;
    private int target;
    private double previousExtension;
    private double receivedExtension;
    private int correctionTicks;
    private boolean lowerCheckPending = true;
    private int shapeHeight = -1;
    private double shapeExtension = Double.NaN;
    private VoxelShape preparedShape = Shapes.empty();
    private AABB[] preparedBoxes = new AABB[0];

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_ELEVATOR.get(), pos, state);
    }

    public static @Nullable CargoElevatorBlockEntity create(BlockPos pos, BlockState state) {
        var block = (CargoElevatorBlock) state.getBlock();
        if (block.isCellState(state)) return null;
        return new CargoElevatorBlockEntity(pos, state);
    }

    public int height() {
        return height;
    }

    public double extension() {
        return extension;
    }

    public double previousExtension() {
        return previousExtension;
    }

    public double receivedExtension() {
        return receivedExtension;
    }

    public boolean platformVisible() {
        return platformVisible;
    }

    public int target() {
        return target;
    }

    public double renderExtension(float partialTick) {
        return previousExtension + (extension - previousExtension) * partialTick;
    }

    public void requestLowerCheck() {
        lowerCheckPending = true;
    }

    @Override
    public void tickServer() {
        previousExtension = extension;
        if (lowerCheckPending) {
            lowerCheckPending = false;
            var block = (CargoElevatorBlock) getBlockState().getBlock();
            var lower = block.elevator(level, worldPosition.below());
            if (lower != null
                    && lower != this
                    && lower.worldPosition.getX() == worldPosition.getX()
                    && lower.worldPosition.getZ() == worldPosition.getZ()) {
                lower.hbm$syncSet$CargoElevatorBlockEntity$height(
                        (int) (lower.height + (height + 1)));
                block.fillSpace(
                        level,
                        lower.worldPosition,
                        lower.getBlockState().getValue(MultiblockCoreBlock.FACING));
                level.removeBlockEntity(worldPosition);
                lower.setChanged();
                return;
            }
        }
        if (!platformVisible) {
            hbm$syncSet$CargoElevatorBlockEntity$platformVisible(true);
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(CargoElevatorBlock.PART, CargoElevatorBlock.Part.CORE),
                    Block.UPDATE_CLIENTS);
        }
        if (extension < target)
            hbm$syncSet$CargoElevatorBlockEntity$extension(
                    Math.clamp(extension + 0.1D, 0D, target));
        else if (extension > target)
            hbm$syncSet$CargoElevatorBlockEntity$extension(
                    Math.clamp(extension - 0.1D, target, height));
        hbm$syncSet$CargoElevatorBlockEntity$extension(Math.clamp(extension, 0D, height));
        if (extension != previousExtension) {
            setChanged();
            moveRiders();
        }
        networkPackNT(300);
    }

    @Override
    public void tickClient() {
        previousExtension = extension;
        if (correctionTicks > 0) {
            hbm$syncSet$CargoElevatorBlockEntity$extension(
                    (double) (extension + ((receivedExtension - extension) / correctionTicks)));
            correctionTicks--;
        } else {
            hbm$syncSet$CargoElevatorBlockEntity$extension(receivedExtension);
        }
        if (extension != previousExtension) moveRiders();
    }

    private void moveRiders() {
        double oldTop = worldPosition.getY() + 1D + previousExtension;
        double newTop = worldPosition.getY() + 1D + extension;
        double low = Math.min(oldTop, newTop);
        double high = Math.max(oldTop, newTop);
        var sweep =
                new AABB(
                        worldPosition.getX() - 0.99D,
                        low,
                        worldPosition.getZ() - 0.99D,
                        worldPosition.getX() + 1.99D,
                        high,
                        worldPosition.getZ() + 1.99D);
        var riders = level.getEntitiesOfClass(Entity.class, sweep);
        for (var rider : riders) {
            double feet = rider.getBoundingBox().minY;
            if (feet < low || feet > high || rider instanceof ServerPlayer) continue;
            rider.move(MoverType.SHULKER_BOX, new Vec3(0D, newTop - feet, 0D));
            rider.setOnGround(true);
            rider.move(MoverType.SHULKER_BOX, DOWNWARD_SETTLE);
        }
    }

    public boolean addSection() {
        var next = new BlockPos.MutableBlockPos();
        int y = worldPosition.getY() + height + 1;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                next.set(worldPosition.getX() + x, y, worldPosition.getZ() + z);
                if (level.isOutsideBuildHeight(next)
                        || !level.hasChunkAt(next)
                        || !level.getBlockState(next).canBeReplaced()) return false;
            }
        }
        hbm$syncSet$CargoElevatorBlockEntity$height((int) (height + 1));
        ((CargoElevatorBlock) getBlockState().getBlock())
                .fillSpace(
                        level, worldPosition, getBlockState().getValue(MultiblockCoreBlock.FACING));
        setChanged();
        return true;
    }

    public void toggleTarget() {
        setTarget(target == 0 ? height : 0);
    }

    public void setTarget(int selected) {
        if (target == selected) return;
        target = selected;
        setChanged();
    }

    public void receiveExtension(double received) {
        receivedExtension = received;
        if (received > 0D && received < height) correctionTicks = 3;
    }

    public VoxelShape collisionShape() {
        if (height != shapeHeight || extension != shapeExtension) {
            preparedBoxes =
                    new AABB[] {
                        new AABB(-1D, 0D, -1D, -0.75D, height + 1D, -0.75D),
                        new AABB(-1D, 0D, 1.75D, -0.75D, height + 1D, 2D),
                        new AABB(1.75D, 0D, -1D, 2D, height + 1D, -0.75D),
                        new AABB(1.75D, 0D, 1.75D, 2D, height + 1D, 2D),
                        new AABB(-1D, 0.75D + extension, -1D, 2D, 1D + extension, 2D)
                    };
            var geometry = Shapes.empty();
            for (var box : preparedBoxes) geometry = Shapes.or(geometry, Shapes.create(box));
            preparedShape = new CargoElevatorShape(geometry, preparedBoxes, height);
            shapeHeight = height;
            shapeExtension = extension;
        }
        return preparedShape;
    }

    public AABB[] collisionBoxes() {
        collisionShape();
        return preparedBoxes;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CargoElevatorBlockEntity$extension(input.getDoubleOr("extension", 0D));
        target = input.getIntOr("targetExtension", 0);
        hbm$syncSet$CargoElevatorBlockEntity$height(input.getIntOr("height", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("extension", extension);
        output.putInt("targetExtension", target);
        output.putInt("height", height);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(platformVisible);
            case 1 -> output.writeShort(height);
            case 2 -> output.writeDouble(extension);
            default -> throw new IndexOutOfBoundsException(unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$CargoElevatorBlockEntity$platformVisible(input.readBoolean());
            case 1 -> hbm$syncSet$CargoElevatorBlockEntity$height(input.readShort());
            case 2 -> receiveExtension(input.readDouble());
            default -> throw new IndexOutOfBoundsException(unit);
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return RADIO_COMMANDS;
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if ("FUN:setextension".equals(key) && params.length > 0) {
            setTarget(RadioCommandParser.intParam(params[0], 0, height));
        }
    }

    public final void hbm$syncSet$CargoElevatorBlockEntity$platformVisible(boolean next) {
        if (this.platformVisible != next) {
            this.platformVisible = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$CargoElevatorBlockEntity$height(int next) {
        if (this.height != next) {
            this.height = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$CargoElevatorBlockEntity$extension(double next) {
        if (Double.doubleToRawLongBits(this.extension) != Double.doubleToRawLongBits(next)) {
            this.extension = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
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
