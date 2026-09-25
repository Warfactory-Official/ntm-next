// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientSyncRecovery;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.SyncUnitState;
import com.hbm.packet.SyncWire;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.tileentity.Synced;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class UnitPayload extends ThreadedPayload {
    public static final Type<UnitPayload> TYPE = new Type<>(Library.id("unit"));
    public static final StreamCodec<ByteBuf, UnitPayload> STREAM_CODEC =
            streamCodec(UnitPayload::decode);

    private final BlockPos pos;
    private final int blockEntityType;
    private final long revision;
    private final long base;
    private SyncUnitState.Snapshot snapshot;
    private long units;
    private int unitBytes;
    private boolean resolved;
    private ByteBuf received;
    private long cachedBase = Long.MIN_VALUE;
    private long cachedRequest = Long.MIN_VALUE;
    private UnitPayload cached;
    private Long2ObjectOpenHashMap<UnitPayload> additional;

    private UnitPayload(BlockPos pos, int type, long revision, long base) {
        this.pos = pos;
        blockEntityType = type;
        this.revision = revision;
        this.base = base;
    }

    public static UnitPayload snapshot(BlockEntity be, SyncUnitState.Snapshot snapshot) {
        UnitPayload payload =
                new UnitPayload(
                        be.getBlockPos(),
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(be.getType()),
                        snapshot.revision(),
                        0);
        payload.snapshot = snapshot;
        return payload;
    }

    private static UnitPayload decode(ByteBuf input) {
        BlockPos pos = BlockPos.of(input.readLong());
        int type = VarInt.read(input);
        long revision = VarLong.read(input), base = VarLong.read(input);
        if (type < 0 || revision <= 0 || base < 0 || base >= revision)
            throw new DecoderException("Invalid machine unit revision");
        UnitPayload payload = new UnitPayload(pos, type, revision, base);
        payload.received = input.readRetainedSlice(input.readableBytes());
        return payload;
    }

    public long revision() {
        return revision;
    }

    public BlockPos pos() {
        return pos;
    }

    public int blockEntityType() {
        return blockEntityType;
    }

    public CompoundTag recordedTag() {
        ByteBuf body = Unpooled.buffer();
        body.writeBoolean(base == 0);
        if (received != null)
            body.writeBytes(received, received.readerIndex(), received.readableBytes());
        else snapshot.writeBody(units, body);
        CompoundTag tag = new CompoundTag();
        tag.putByteArray(SyncWire.FRAME_KEY, ByteBufUtil.getBytes(body));
        return tag;
    }

    public ThreadedPayload forBase(long requestedBase) {
        if (resolved || snapshot == null) return this;
        if (cached != null && cachedRequest == requestedBase) return cached;
        if (additional != null) {
            UnitPayload found = additional.get(requestedBase);
            if (found != null) return found;
        }
        long selectedBase = snapshot.base(requestedBase);
        if (cached != null && cachedBase == selectedBase) return cached;
        if (additional != null) {
            UnitPayload found = additional.get(selectedBase);
            if (found != null) return found;
        }

        UnitPayload payload = new UnitPayload(pos, blockEntityType, revision, selectedBase);
        payload.snapshot = snapshot;
        payload.units = snapshot.selectedUnits(selectedBase);
        payload.unitBytes = snapshot.bodySize(payload.units);
        payload.resolved = true;
        if (cached == null) {
            cachedBase = selectedBase;
            cachedRequest = requestedBase;
            cached = payload;
        } else {
            if (additional == null) additional = new Long2ObjectOpenHashMap<>();
            additional.put(selectedBase, payload);
        }
        return payload;
    }

    public boolean requiresRecipientBase() {
        return !resolved && snapshot != null;
    }

    public static void handle(UnitPayload payload, IPayloadHandlerContext context) {
        try {
            Player player = context.playerOrNull();
            if (player == null) return;
            BlockEntity be = ChunkUtil.blockEntityIfLoaded(player.level(), payload.pos);
            if (be instanceof Synced synced
                    && be instanceof SyncUnitSchema
                    && BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(be.getType())
                            == payload.blockEntityType
                    && synced.applyUnitSync(payload.revision, payload.base, payload.received)) {
                ClientSyncRecovery.received(payload.pos);
            } else ClientSyncRecovery.request(player.level(), payload.pos, payload.blockEntityType);
        } finally {
            payload.release();
        }
    }

    @Override
    protected int bodyCapacity() {
        if (!resolved) throw new IllegalStateException("Unresolved machine unit frame");
        return Long.BYTES
                + VarInt.getByteSize(blockEntityType)
                + VarLong.getByteSize(revision)
                + VarLong.getByteSize(base)
                + unitBytes;
    }

    @Override
    public void toBytes(ByteBuf output) {
        if (!resolved) throw new IllegalStateException("Unresolved machine unit frame");
        output.writeLong(pos.asLong());
        VarInt.write(output, blockEntityType);
        VarLong.write(output, revision);
        VarLong.write(output, base);
        snapshot.writeBody(units, output);
    }

    @Override
    protected void releaseBody() {
        if (received != null) received.release();
        if (cached != null) cached.release();
        if (additional != null) for (UnitPayload payload : additional.values()) payload.release();
        snapshot = null;
    }

    @Override
    public Type<UnitPayload> type() {
        return TYPE;
    }
}
