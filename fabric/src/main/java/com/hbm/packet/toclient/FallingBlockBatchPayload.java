// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.entity.item.EntityFallingBlockBatch;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class FallingBlockBatchPayload extends ThreadedPayload {
    public static final Type<FallingBlockBatchPayload> TYPE =
            new Type<>(Library.id("falling_block_batch"));
    public static final StreamCodec<ByteBuf, FallingBlockBatchPayload> STREAM_CODEC =
            streamCodec(FallingBlockBatchPayload::decode);

    private final int entityId;
    private final int revision;
    private final boolean running;
    private final int[] ids;
    private final long[] sources;
    private final BlockState[] states;
    private final double[] ys;
    private final double[] velocities;
    private final int[] ages;

    public FallingBlockBatchPayload(
            int entityId,
            int revision,
            boolean running,
            int[] ids,
            long[] sources,
            BlockState[] states,
            double[] ys,
            double[] velocities,
            int[] ages) {
        this.entityId = entityId;
        this.revision = revision;
        this.running = running;
        this.ids = ids;
        this.sources = sources;
        this.states = states;
        this.ys = ys;
        this.velocities = velocities;
        this.ages = ages;
    }

    private static FallingBlockBatchPayload decode(ByteBuf buf) {
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        int revision = ByteBufCodecs.VAR_INT.decode(buf);
        boolean running = buf.readBoolean();
        int size = ByteBufCodecs.VAR_INT.decode(buf);
        if (size < 0 || size > EntityFallingBlockBatch.MAX_CHILDREN) {
            throw new IllegalArgumentException("Invalid falling block batch size " + size);
        }
        int[] ids = new int[size];
        long[] sources = new long[size];
        BlockState[] states = new BlockState[size];
        double[] ys = new double[size];
        double[] velocities = new double[size];
        int[] ages = new int[size];
        for (int i = 0; i < size; i++) {
            ids[i] = ByteBufCodecs.VAR_INT.decode(buf);
            sources[i] = buf.readLong();
            states[i] = Block.BLOCK_STATE_REGISTRY.byId(ByteBufCodecs.VAR_INT.decode(buf));
            ys[i] = buf.readDouble();
            velocities[i] = buf.readDouble();
            ages[i] = ByteBufCodecs.VAR_INT.decode(buf);
        }
        return new FallingBlockBatchPayload(
                entityId, revision, running, ids, sources, states, ys, velocities, ages);
    }

    public static void handleClient(
            FallingBlockBatchPayload payload, IPayloadHandlerContext context) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(payload.entityId);
        if (entity instanceof EntityFallingBlockBatch batch) batch.applySnapshot(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        ByteBufCodecs.VAR_INT.encode(buf, revision);
        buf.writeBoolean(running);
        ByteBufCodecs.VAR_INT.encode(buf, ids.length);
        for (int i = 0; i < ids.length; i++) {
            ByteBufCodecs.VAR_INT.encode(buf, ids[i]);
            buf.writeLong(sources[i]);
            ByteBufCodecs.VAR_INT.encode(buf, Block.BLOCK_STATE_REGISTRY.getId(states[i]));
            buf.writeDouble(ys[i]);
            buf.writeDouble(velocities[i]);
            ByteBufCodecs.VAR_INT.encode(buf, ages[i]);
        }
    }

    public int revision() {
        return revision;
    }

    public boolean running() {
        return running;
    }

    public int[] ids() {
        return ids;
    }

    public long[] sources() {
        return sources;
    }

    public BlockState[] states() {
        return states;
    }

    public double[] ys() {
        return ys;
    }

    public double[] velocities() {
        return velocities;
    }

    public int[] ages() {
        return ages;
    }

    @Override
    public @NotNull Type<FallingBlockBatchPayload> type() {
        return TYPE;
    }
}
