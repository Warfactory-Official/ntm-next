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

package com.hbm.blockentity.network;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.module.ParseMSES1Ext1;
import com.hbm.module.StatementParser;
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
import com.hbm.util.TickPhase;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
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

public final class RadioAutocalBlockEntity extends BlockEntity
        implements BlockEntityTicks,
                Synced,
                SyncUnitSchema,
                GraphResident,
                MenuProvider,
                ScreenMenu.Host {

    public static final int MAX_SCRIPT_BYTES = 32767;

    private static final int HISTORY_SIZE = 6;
    private static final int ATTEMPT_LIMIT = 100;
    private static final StatementParser INTERPRETER = new ParseMSES1Ext1();
    private static final Component TITLE = Component.translatable("block.hbm.radio_autocal");
    private static final Component NATURAL_END =
            Component.translatable("desc.gui.radioAUTOCAL.programHasTerminated");
    private static final Component INVALID_POSITION =
            Component.translatable("desc.gui.radioAUTOCAL.programIndexIsOut");
    private static final Component PROGRAM_SHUTDOWN =
            Component.translatable("desc.gui.radioAUTOCAL.programRequestedShutdown");
    private static final Component UNKNOWN_COMMAND =
            Component.translatable("desc.gui.radioAUTOCAL.unrecognizedCommand");
    private static final Component INVALID_PARAMETER =
            Component.translatable("desc.gui.radioAUTOCAL.parameterError");
    private static final Component UNDEFINED_RESULT =
            Component.translatable("desc.gui.radioAUTOCAL.undefinedBehavior");
    private static final Component STACK_CAPACITY =
            Component.translatable("desc.gui.radioAUTOCAL.stackExceededCapacity");
    private static final Component EVALUATION_FAILED =
            Component.translatable("desc.gui.radioAUTOCAL.evaluationUnsuccessful");
    private static final Component USER_SHUTDOWN =
            Component.translatable("desc.gui.radioAUTOCAL.userRequestedShutdown");
    private static final Component PROGRAM_CHANGED =
            Component.translatable("desc.gui.radioAUTOCAL.scriptHasChanged");

    private boolean running;

    private boolean ignoreErrors;

    private boolean automaticReboot;

    private final Component[] history = new Component[HISTORY_SIZE];

    private StatementParser.ParseContext context = new StatementParser.ParseContext();
    private String[] program = new String[0];
    private String projectedBuffer;

    public RadioAutocalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_AUTOCAL.get(), pos, state);
        Arrays.fill(history, CommonComponents.EMPTY);
    }

    public boolean running() {
        return running;
    }

    public boolean ignoresErrors() {
        return ignoreErrors;
    }

    public boolean rebootsAutomatically() {
        return automaticReboot;
    }

    public String[] programLines() {
        return program;
    }

    public StatementParser.ParseContext executionContext() {
        return context;
    }

    public int historySize() {
        return HISTORY_SIZE;
    }

    public Component historyLine(int index) {
        return history[index];
    }

    @Override
    public void tickServer() {
        context.world = level;
        if (TickPhase.every(level, worldPosition, 60)) {
            markChanged();
        }
        if (!running && automaticReboot) {
            hbm$syncSet$RadioAutocalBlockEntity$running(true);
        }
        if (running) {
            executeProgram();
            markChanged();
        }
        networkPackNT(15);
    }

    private void executeProgram() {
        var cyclesUsed = 0;
        var attempts = 0;
        execution:
        while (running && cyclesUsed < context.clockSpeed && attempts < ATTEMPT_LIMIT) {
            var position = context.current;
            if (position == program.length) {
                stop(NATURAL_END);
                break;
            }
            if (position < 0 || position > program.length) {
                stop(INVALID_POSITION);
                break;
            }
            attempts++;
            try {
                var line = program[position];
                context.current = position + 1;
                var result = INTERPRETER.eval(context, line);
                if (result != StatementParser.StatementResult.SKIP) {
                    cyclesUsed++;
                    appendHistory(Component.literal(position + ": " + line));
                }
                refreshBufferProjection();
                switch (result) {
                    case END_TICK -> {
                        break execution;
                    }
                    case SHUTDOWN -> stop(PROGRAM_SHUTDOWN);
                    case UNRECOGNIZED_COMMAND -> stopOnError(UNKNOWN_COMMAND);
                    case PARAMETER_ERROR -> stopOnError(INVALID_PARAMETER);
                    case UNDEFINED -> stopOnError(UNDEFINED_RESULT);
                    case STACK_EXCEEDED -> stopOnError(STACK_CAPACITY);
                    case OK, SKIP -> {}
                }
            } catch (Exception _) {
                stop(EVALUATION_FAILED);
            }
        }
    }

    private void refreshBufferProjection() {
        var buffer = context.readBuffer();
        if (!buffer.equals(projectedBuffer)) {
            projectedBuffer = buffer;
            hbm$syncArraySet$RadioAutocalBlockEntity$history(
                    history, 0, Component.translatable("desc.gui.radioAUTOCAL.buffer", buffer));
        }
    }

    private void appendHistory(Component message) {
        hbm$syncArraySet$RadioAutocalBlockEntity$history(history, 1, history[2]);
        hbm$syncArraySet$RadioAutocalBlockEntity$history(history, 2, history[3]);
        hbm$syncArraySet$RadioAutocalBlockEntity$history(history, 3, history[4]);
        hbm$syncArraySet$RadioAutocalBlockEntity$history(history, 4, history[5]);
        hbm$syncArraySet$RadioAutocalBlockEntity$history(history, HISTORY_SIZE - 1, message);
    }

    private void stopOnError(Component reason) {
        if (!ignoreErrors) {
            stop(reason);
        }
    }

    private void stop(Component reason) {
        hbm$syncSet$RadioAutocalBlockEntity$running(false);
        context.turnOff();
        appendHistory(reason);
        markChanged();
    }

    public void toggleRunning() {
        setRunning(!running);
    }

    public void setRunning(boolean enabled) {
        if (!enabled) {
            stop(USER_SHUTDOWN);
        } else if (!running) {
            hbm$syncSet$RadioAutocalBlockEntity$running(true);
            markChanged();
        }
    }

    public void toggleErrorPolicy() {
        setErrorPolicy(!ignoreErrors);
    }

    public void setErrorPolicy(boolean enabled) {
        if (ignoreErrors != enabled) {
            hbm$syncSet$RadioAutocalBlockEntity$ignoreErrors(enabled);
            markChanged();
        }
    }

    public void toggleRebootPolicy() {
        setRebootPolicy(!automaticReboot);
    }

    public void setRebootPolicy(boolean enabled) {
        if (automaticReboot != enabled) {
            hbm$syncSet$RadioAutocalBlockEntity$automaticReboot(enabled);
            markChanged();
        }
    }

    public void replaceProgram(String source) {
        var lines = source.split("\n");
        for (var index = 0; index < lines.length; index++) {
            lines[index] = lines[index].trim();
        }
        program = lines;
        context.jmp.clear();
        for (var index = 0; index < program.length; index++) {
            INTERPRETER.generateJumpPoints(context, program[index], index);
        }
        if (running) {
            stop(PROGRAM_CHANGED);
        }
        markChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5,
                                worldPosition.getY() + 1,
                                worldPosition.getZ() + 0.5)
                <= 225;
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScreenMenu<>(ModMenus.RADIO_AUTOCAL.get(), containerId, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RadioAutocalBlockEntity$running(input.getBooleanOr("isOn", false));
        hbm$syncSet$RadioAutocalBlockEntity$ignoreErrors(input.getBooleanOr("ignoreError", false));
        hbm$syncSet$RadioAutocalBlockEntity$automaticReboot(
                input.getBooleanOr("autoReboot", false));
        program =
                input.read("script", Codec.STRING.listOf())
                        .orElse(List.of())
                        .toArray(String[]::new);
        context = new StatementParser.ParseContext();
        context.load(input, program, INTERPRETER);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", running);
        output.putBoolean("ignoreError", ignoreErrors);
        output.putBoolean("autoReboot", automaticReboot);
        output.store("script", Codec.STRING.listOf(), Arrays.asList(program));
        context.save(output);
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                output.writeBoolean(running);
                output.writeBoolean(ignoreErrors);
                output.writeBoolean(automaticReboot);
            }
            case 1 -> {
                for (var line : history) {
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(output, line);
                }
            }
            default -> throw new IllegalArgumentException("Unknown radio sync unit: " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                hbm$syncSet$RadioAutocalBlockEntity$running(input.readBoolean());
                hbm$syncSet$RadioAutocalBlockEntity$ignoreErrors(input.readBoolean());
                hbm$syncSet$RadioAutocalBlockEntity$automaticReboot(input.readBoolean());
            }
            case 1 -> {
                for (var index = 0; index < history.length; index++) {
                    hbm$syncArraySet$RadioAutocalBlockEntity$history(
                            history,
                            index,
                            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(input));
                }
            }
            default -> throw new IllegalArgumentException("Unknown radio sync unit: " + unit);
        }
    }

    public final void hbm$syncSet$RadioAutocalBlockEntity$running(boolean next) {
        if (this.running != next) {
            this.running = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$RadioAutocalBlockEntity$ignoreErrors(boolean next) {
        if (this.ignoreErrors != next) {
            this.ignoreErrors = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$RadioAutocalBlockEntity$automaticReboot(boolean next) {
        if (this.automaticReboot != next) {
            this.automaticReboot = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final Component hbm$syncArraySet$RadioAutocalBlockEntity$history(
            Component[] array, int index, Component next) {
        if (array[index] != next) {
            if (array == this.history && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.history) {
                syncArrayChanged(array, index, 3, 2);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(RadioAutocalBlockEntity.class, "history", 3, 2);
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
