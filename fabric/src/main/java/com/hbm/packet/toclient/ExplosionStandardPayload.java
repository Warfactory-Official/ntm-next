// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class ExplosionStandardPayload extends ThreadedPayload {

    public static final Type<ExplosionStandardPayload> TYPE =
            new Type<>(Library.id("explosion_standard"));
    public static final StreamCodec<ByteBuf, ExplosionStandardPayload> STREAM_CODEC =
            streamCodec(ExplosionStandardPayload::decode);

    final double x, y, z;
    final float size;
    final List<BlockPos> blocks;

    public ExplosionStandardPayload(
            double x, double y, double z, float size, List<BlockPos> blocks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.blocks = blocks;
    }

    private static ExplosionStandardPayload decode(ByteBuf buf) {
        double x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat();
        float size = buf.readFloat();
        int count = buf.readInt();
        int bx = (int) x, by = (int) y, bz = (int) z;
        List<BlockPos> blocks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            blocks.add(new BlockPos(buf.readByte() + bx, buf.readByte() + by, buf.readByte() + bz));
        }
        return new ExplosionStandardPayload(x, y, z, size, blocks);
    }

    public static void handleClient(ExplosionStandardPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat((float) x);
        buf.writeFloat((float) y);
        buf.writeFloat((float) z);
        buf.writeFloat(size);
        buf.writeInt(blocks.size());
        int bx = (int) (float) x, by = (int) (float) y, bz = (int) (float) z;
        for (BlockPos pos : blocks) {
            buf.writeByte(pos.getX() - bx);
            buf.writeByte(pos.getY() - by);
            buf.writeByte(pos.getZ() - bz);
        }
    }

    @Override
    public @NotNull Type<ExplosionStandardPayload> type() {
        return TYPE;
    }
}
