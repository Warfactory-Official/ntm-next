// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.threading;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.EncoderException;
import io.netty.util.concurrent.FastThreadLocal;
import java.util.zip.Deflater;
import net.minecraft.network.VarInt;

final class FrameCompression {
    private static final FastThreadLocal<Deflater> DEFLATER =
            new FastThreadLocal<>() {
                @Override
                protected Deflater initialValue() {
                    return new Deflater();
                }

                @Override
                protected void onRemoval(Deflater deflater) {
                    deflater.end();
                }
            };

    private FrameCompression() {}

    static ByteBuf encode(ByteBuf wire, boolean enabled, boolean compress) {
        int length = wire.readableBytes();
        int framedLength = length + (enabled ? 1 : 0);
        if (VarInt.getByteSize(framedLength) > 3)
            throw new EncoderException("Packet exceeds the frame limit");
        ByteBuf output =
                PooledByteBufAllocator.DEFAULT.directBuffer(
                        compress
                                ? 3 + VarInt.getByteSize(length) + Math.min(length + 13, 512)
                                : VarInt.getByteSize(framedLength) + framedLength);
        try {
            if (!compress) {
                VarInt.write(output, framedLength);
                if (enabled) VarInt.write(output, 0);
                output.writeBytes(wire, wire.readerIndex(), length);
                return output;
            }

            output.writeZero(3);
            VarInt.write(output, length);
            Deflater deflater = DEFLATER.get();
            try {

                deflater.setInput(wire.internalNioBuffer(wire.readerIndex(), length));
                deflater.finish();
                while (!deflater.finished()) {
                    if (!output.isWritable()) output.ensureWritable(1);
                    int offset = output.writerIndex();
                    int written =
                            deflater.deflate(
                                    output.internalNioBuffer(offset, output.writableBytes()));
                    output.writerIndex(offset + written);
                }
            } finally {
                deflater.reset();
            }
            int end = output.writerIndex();
            int packetLength = end - 3;
            int prefix = VarInt.getByteSize(packetLength);
            if (prefix > 3) throw new EncoderException("Compressed packet exceeds the frame limit");
            output.readerIndex(3 - prefix);
            output.writerIndex(3 - prefix);
            VarInt.write(output, packetLength);
            output.writerIndex(end);
            return output;
        } catch (Throwable failure) {
            output.release();
            throw failure;
        }
    }

    static void clear() {
        DEFLATER.remove();
    }
}
