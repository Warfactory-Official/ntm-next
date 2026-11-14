/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import com.hbm.handler.radio.RadioChannel;
import com.hbm.handler.radio.RadioSignal;
import com.hbm.handler.radio.RadioSystem;
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
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RadioTelexBlockEntity extends BlockEntity
        implements BlockEntityTicks,
                Synced,
                GraphResident,
                MenuProvider,
                ScreenMenu.Host,
                SyncUnitSchema {

    public static final int LINE_WIDTH = 33;
    public static final char EOL = '\n';
    public static final char EOT = '';
    public static final char BELL = '';
    public static final char PRINT = '\f';
    public static final char PAUSE = '';
    public static final char CLEAR = '';
    private static final int BUFFER_LINES = 5;
    public final String[] txBuffer = {"", "", "", "", ""};
    public final String[] rxBuffer = {"", "", "", "", ""};
    public String txChannel;

    {
        hbm$syncSet$RadioTelexBlockEntity$txChannel("");
    }

    public String rxChannel;

    {
        hbm$syncSet$RadioTelexBlockEntity$rxChannel("");
    }

    public int sendingLine;
    public int sendingIndex;
    public boolean isSending;
    public int sendingWait;
    public int writingLine;
    public boolean printAfterRx;
    public boolean deleteOnReceive = true;
    public char sendingChar;

    {
        hbm$syncSet$RadioTelexBlockEntity$sendingChar(' ');
    }

    public RadioTelexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_TELEX.get(), pos, state);
    }

    @Override
    public void tickServer() {
        hbm$syncSet$RadioTelexBlockEntity$sendingChar(' ');

        if (isSending && txChannel.isEmpty()) isSending = false;

        if (isSending) {
            if (sendingWait > 0) {
                sendingWait--;
            } else {
                String line = txBuffer[sendingLine];
                if (line.length() > sendingIndex) {
                    char c = line.charAt(sendingIndex++);
                    if (c == PAUSE) {
                        sendingWait = 20;
                    } else {
                        RadioSystem.send(level, txChannel, c);
                        hbm$syncSet$RadioTelexBlockEntity$sendingChar(c);
                    }
                } else if (sendingLine >= BUFFER_LINES - 1) {
                    isSending = false;
                    RadioSystem.send(level, txChannel, EOT);
                    sendingLine = 0;
                    sendingIndex = 0;
                } else {
                    RadioSystem.send(level, txChannel, EOL);
                    sendingLine++;
                    sendingIndex = 0;
                }
            }
        }

        if (!rxChannel.isEmpty()) {
            RadioChannel channel = RadioSystem.read(level, rxChannel);
            if (channel != null
                    && channel.signal() instanceof RadioSignal.Char character
                    && channel.stamp() > level.getGameTime() - 2) {
                receiveCharacter(character.value());
            }
        }

        networkPackNT(16);
    }

    private void receiveCharacter(char c) {
        if (deleteOnReceive) {
            deleteOnReceive = false;
            clearReceiveBuffer();
        }

        if (c == EOT) {
            if (printAfterRx) {
                printAfterRx = false;
                print();
            }
            deleteOnReceive = true;
        } else if (c == EOL) {
            if (writingLine < BUFFER_LINES - 1) writingLine++;
            setChanged();
        } else if (c == BELL) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS,
                    2.0F,
                    0.5F);
        } else if (c == PRINT) {
            printAfterRx = true;
        } else if (c == CLEAR) {
            clearReceiveBuffer();
        } else {
            hbm$syncArraySet$RadioTelexBlockEntity$rxBuffer(
                    rxBuffer, writingLine, (java.lang.String) (rxBuffer[writingLine] + (c)));
            setChanged();
        }
    }

    public void clearReceiveBuffer() {
        for (int i = 0; i < BUFFER_LINES; i++)
            hbm$syncArraySet$RadioTelexBlockEntity$rxBuffer(rxBuffer, i, "");
        writingLine = 0;
        markChanged();
    }

    public void print() {
        List<Component> text = new ArrayList<>();
        for (String line : rxBuffer) {
            if (!line.isEmpty())
                text.add(Component.literal(ChatFormatting.RESET + "" + ChatFormatting.GRAY + line));
        }

        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.LORE, new ItemLore(text));
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable("chat.entityRadioTelex.message"));
        level.addFreshEntity(
                new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 1D,
                        worldPosition.getZ() + 0.5D,
                        stack));
    }

    public void startSending() {
        if (isSending) return;
        isSending = true;
        sendingLine = 0;
        sendingIndex = 0;
        setChanged();
    }

    public void setTransmitLine(int line, String text) {
        hbm$syncArraySet$RadioTelexBlockEntity$txBuffer(txBuffer, line, text);
    }

    public void setChannels(String txChannel, String rxChannel) {
        this.hbm$syncSet$RadioTelexBlockEntity$txChannel(txChannel);
        this.hbm$syncSet$RadioTelexBlockEntity$rxChannel(rxChannel);
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < BUFFER_LINES; i++) {
            hbm$syncArraySet$RadioTelexBlockEntity$txBuffer(
                    txBuffer, i, input.getStringOr("tx" + i, txBuffer[i]));
            hbm$syncArraySet$RadioTelexBlockEntity$rxBuffer(
                    rxBuffer, i, input.getStringOr("rx" + i, rxBuffer[i]));
        }
        hbm$syncSet$RadioTelexBlockEntity$txChannel(input.getStringOr("txChan", txChannel));
        hbm$syncSet$RadioTelexBlockEntity$rxChannel(input.getStringOr("rxChan", rxChannel));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < BUFFER_LINES; i++) {
            output.putString("tx" + i, txBuffer[i]);
            output.putString("rx" + i, rxBuffer[i]);
        }
        output.putString("txChan", txChannel);
        output.putString("rxChan", rxChannel);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.radio_telex");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScreenMenu<>(ModMenus.RADIO_TELEX.get(), containerId, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 16D * 16D;
    }

    private void writeTransmitBuffer(ByteBuf output) {
        for (String line : txBuffer) ByteBufCodecs.STRING_UTF8.encode(output, line);
    }

    private void readTransmitBuffer(ByteBuf input) {
        for (int i = 0; i < txBuffer.length; i++)
            hbm$syncArraySet$RadioTelexBlockEntity$txBuffer(
                    txBuffer, i, ByteBufCodecs.STRING_UTF8.decode(input));
    }

    private void writeReceiveBuffer(ByteBuf output) {
        for (String line : rxBuffer) ByteBufCodecs.STRING_UTF8.encode(output, line);
    }

    private void readReceiveBuffer(ByteBuf input) {
        for (int i = 0; i < rxBuffer.length; i++)
            hbm$syncArraySet$RadioTelexBlockEntity$rxBuffer(
                    rxBuffer, i, ByteBufCodecs.STRING_UTF8.decode(input));
    }

    private void writeTransmitChannel(ByteBuf output) {
        ByteBufCodecs.STRING_UTF8.encode(output, txChannel);
    }

    private void readTransmitChannel(ByteBuf input) {
        hbm$syncSet$RadioTelexBlockEntity$txChannel(ByteBufCodecs.STRING_UTF8.decode(input));
    }

    private void writeReceiveChannel(ByteBuf output) {
        ByteBufCodecs.STRING_UTF8.encode(output, rxChannel);
    }

    private void readReceiveChannel(ByteBuf input) {
        hbm$syncSet$RadioTelexBlockEntity$rxChannel(ByteBufCodecs.STRING_UTF8.decode(input));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTransmitBuffer(output);
            case 1 -> writeReceiveBuffer(output);
            case 2 -> writeTransmitChannel(output);
            case 3 -> writeReceiveChannel(output);
            case 4 -> output.writeChar(this.sendingChar);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTransmitBuffer(input);
            case 1 -> readReceiveBuffer(input);
            case 2 -> readTransmitChannel(input);
            case 3 -> readReceiveChannel(input);
            case 4 -> this.hbm$syncSet$RadioTelexBlockEntity$sendingChar(input.readChar());
            default -> throw new IllegalArgumentException();
        }
    }

    public final String hbm$syncArraySet$RadioTelexBlockEntity$txBuffer(
            String[] array, int index, String next) {
        if (array[index] != next) {
            boolean changed = !java.util.Objects.equals(array[index], next);
            array[index] = next;
            if (array == this.txBuffer && changed) {
                syncArrayChanged(array, index, 3, 1L);
            }
        }
        return next;
    }

    public final String hbm$syncArraySet$RadioTelexBlockEntity$rxBuffer(
            String[] array, int index, String next) {
        if (array[index] != next) {
            boolean changed = !java.util.Objects.equals(array[index], next);
            array[index] = next;
            if (array == this.rxBuffer && changed) {
                syncArrayChanged(array, index, 3, 1L << 1);
            }
        }
        return next;
    }

    public final void hbm$syncSet$RadioTelexBlockEntity$txChannel(String next) {
        if (this.txChannel != next) {
            boolean changed = !java.util.Objects.equals(this.txChannel, next);
            this.txChannel = next;
            if (changed)
                if ((1L << 2) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$RadioTelexBlockEntity$rxChannel(String next) {
        if (this.rxChannel != next) {
            boolean changed = !java.util.Objects.equals(this.rxChannel, next);
            this.rxChannel = next;
            if (changed)
                if ((1L << 3) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$RadioTelexBlockEntity$sendingChar(char next) {
        if (this.sendingChar != next) {
            this.sendingChar = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RadioTelexBlockEntity.class, "txBuffer", 3, 1L);
        com.hbm.packet.SyncBindings.field(RadioTelexBlockEntity.class, "rxBuffer", 3, 1L << 1);
        com.hbm.packet.SyncBindings.field(RadioTelexBlockEntity.class, "txChannel", 3, 1L << 2);
        com.hbm.packet.SyncBindings.field(RadioTelexBlockEntity.class, "rxChannel", 3, 1L << 3);
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
