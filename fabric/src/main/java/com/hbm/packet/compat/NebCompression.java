// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public final class NebCompression {
    private final Object compressor, decoder;
    private final boolean streaming;
    private final ZstdAccess zstd;
    private ByteBuffer encodeInput, encodeOutput, decodeInput, decodeTail;

    public NebCompression(Object compressor, Object decoder, boolean streaming) {
        this.compressor = compressor;
        this.decoder = decoder;
        this.streaming = streaming;
        zstd = ZstdAccess.of(compressor);
    }

    public void compress(ByteBuf input, ByteBuf output) {
        int length = input.readableBytes(), maximum = zstd.bound(length) + 64;
        ByteBuffer source;
        if (input.isDirect() && input.nioBufferCount() == 1)
            source = input.internalNioBuffer(input.readerIndex(), length);
        else {
            if (encodeInput == null || encodeInput.capacity() < length)
                encodeInput = ByteBuffer.allocateDirect(length);
            source = encodeInput.clear().limit(length);
            input.getBytes(input.readerIndex(), source);
            source.flip();
        }
        output.ensureWritable(maximum);
        ByteBuffer target;
        int start = output.writerIndex();
        boolean direct = output.isDirect() && output.nioBufferCount() == 1;
        if (direct) target = output.internalNioBuffer(start, maximum);
        else {
            if (encodeOutput == null || encodeOutput.capacity() < maximum)
                encodeOutput = ByteBuffer.allocateDirect(maximum);
            target = encodeOutput.clear();
        }
        int offset = target.position();
        if (streaming) {
            while (!zstd.compress(compressor, target, source, zstd.flushDirective)) {
                if (!target.hasRemaining())
                    throw new IllegalStateException("NEB compressed output exceeded its bound");
            }
        } else zstd.oneShot(compressor, target, source);
        if (source.hasRemaining())
            throw new IllegalStateException("NEB compression left unread input");
        int size = target.position() - offset;
        if (direct) output.writerIndex(start + size);
        else {
            target.flip();
            output.writeBytes(target);
        }
    }

    public ByteBuf decompress(ByteBuf input, int size) {
        ByteBuffer source;
        int length = input.readableBytes();
        if (input.isDirect() && input.nioBufferCount() == 1)
            source = input.internalNioBuffer(input.readerIndex(), length);
        else {
            if (decodeInput == null || decodeInput.capacity() < length)
                decodeInput = ByteBuffer.allocateDirect(length);
            source = decodeInput.clear().limit(length);
            input.getBytes(input.readerIndex(), source);
            source.flip();
        }
        ByteBuf output = DecodeBuffers.acquire(size);
        try {
            ByteBuffer target = output.internalNioBuffer(0, size);
            int offset = target.position();
            while (source.hasRemaining() && target.hasRemaining()) {
                int before = source.position(), written = target.position();
                zstd.decompress(decoder, target, source);
                if (before == source.position() && written == target.position())
                    throw new IllegalStateException("NEB decompression made no progress");
            }
            if (target.position() - offset != size)
                throw new IllegalStateException("NEB decoded length differs from its header");
            if (decodeTail == null) decodeTail = ByteBuffer.allocateDirect(1);
            ByteBuffer tail = decodeTail.clear();
            do {
                int before = source.position();
                zstd.decompress(decoder, tail, source);
                if (tail.position() != 0)
                    throw new IllegalStateException("NEB decoded length exceeds its header");
                if (source.hasRemaining() && before == source.position())
                    throw new IllegalStateException("NEB compressed input has an incomplete tail");
            } while (source.hasRemaining());
            output.writerIndex(size);
            return output;
        } catch (Throwable failure) {
            output.release();
            throw failure;
        }
    }
}
