// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.gui.ScreenPwrPrinter;
import com.hbm.items.machine.PwrPrintData;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class PwrPrintPayload extends ThreadedPayload {

    public static final Type<PwrPrintPayload> TYPE = new Type<>(Library.id("pwr_print"));
    public static final StreamCodec<ByteBuf, PwrPrintPayload> STREAM_CODEC =
            streamCodec(PwrPrintPayload::decode);
    public final PwrPrintData data;

    public PwrPrintPayload(PwrPrintData data) {
        this.data = data;
    }

    private static PwrPrintPayload decode(ByteBuf input) {
        BlockPos min = BlockPos.of(input.readLong());
        BlockPos max = BlockPos.of(input.readLong());
        Direction direction = Direction.values()[input.readUnsignedByte()];
        int count = ByteBufCodecs.VAR_INT.decode(input);
        if (count < 0 || count > input.readableBytes() / 9)
            throw new DecoderException("Invalid PWR print cell count");
        long[] positions = new long[count];
        BlockState[] states = new BlockState[count];
        for (int i = 0; i < count; i++) {
            positions[i] = input.readLong();
            int id = ByteBufCodecs.VAR_INT.decode(input);
            states[i] = Block.BLOCK_STATE_REGISTRY.byId(id);
            if (states[i] == null)
                throw new DecoderException("Unknown PWR print block state " + id);
        }
        return new PwrPrintPayload(new PwrPrintData(min, max, direction, positions, states));
    }

    public static void handle(PwrPrintPayload payload, IPayloadHandlerContext context) {
        ScreenPwrPrinter.open(payload.data);
    }

    @Override
    public void toBytes(ByteBuf output) {
        output.writeLong(data.min().asLong());
        output.writeLong(data.max().asLong());
        output.writeByte(data.direction().ordinal());
        ByteBufCodecs.VAR_INT.encode(output, data.positions().length);
        for (int i = 0; i < data.positions().length; i++) {
            output.writeLong(data.positions()[i]);
            ByteBufCodecs.VAR_INT.encode(
                    output, Block.BLOCK_STATE_REGISTRY.getId(data.states()[i]));
        }
    }

    @Override
    public @NotNull Type<PwrPrintPayload> type() {
        return TYPE;
    }
}
