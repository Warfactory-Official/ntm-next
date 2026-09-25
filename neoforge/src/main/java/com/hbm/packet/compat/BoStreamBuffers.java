// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import java.nio.ByteBuffer;

public final class BoStreamBuffers {
    private static final int INPUT_CAPACITY = 65_536;
    private final Object bridge, compressor, decoder, termination;
    private final ZstdAccess zstd;
    private final int limit;
    private ByteBuffer encodeInput, encodeOutput, decodeInput, decodeOutput;

    public BoStreamBuffers(Object layer) {
        bridge = __asm__(Object) {
            aload layer;
            checkcast "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/KineticStreamingLayer";
            getfield "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/KineticStreamingLayer" "zstdContext"
            "Lcom/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context;";
        };
        Object nativeBridge = bridge;
        compressor = __asm__(Object) {
            aload nativeBridge;
            checkcast "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context";
            getfield "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context"
            "compressContext" "Ljava/lang/Object;";
        };
        decoder = __asm__(Object) {
            aload nativeBridge;
            checkcast "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context";
            getfield "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context"
            "decompressContext" "Ljava/lang/Object;";
        };
        zstd = ZstdAccess.of(compressor);
        Enum<?> ending = __asm__(Enum < ? >) {
            aload layer;
            checkcast "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/KineticStreamingLayer";
            getfield "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/KineticStreamingLayer" "frameTermination"
            "Lcom/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/KineticStreamingLayer$FrameTermination;";
            checkcast "java/lang/Enum";
        };
        termination = ending.name().equals("FLUSH") ? zstd.flushDirective : zstd.endDirective;
        limit = __asm__( int){
            getstatic "com/PinkCats/bandwidthoptimizer/channel/algorithm/ChannelTransportPayloadLimits"
            "MAX_STREAMING_FRAME_PAYLOAD_BYTES" "I";
        };
    }

    private void open() {
        Object nativeBridge = bridge;
        boolean closed = __asm__( boolean){
            aload nativeBridge;
            checkcast "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context";
            getfield "com/PinkCats/bandwidthoptimizer/channel/algorithm/zstd/ZstdRuntimeBridge$Context" "closed" "Z";
        };
        if (closed) throw new IllegalStateException("zstd runtime context is closed");
    }

    public byte[] encode(byte[] bytes) {
        open();
        int size = bytes == null ? 0 : bytes.length;
        if (size > limit)
            throw new IllegalStateException("Channel streaming packet bytes out of range");
        if (encodeInput == null) encodeInput = ByteBuffer.allocateDirect(INPUT_CAPACITY);
        int batchSize = 1 + varSize(size) + size;
        int framedSize = varSize(batchSize) + batchSize;
        if (framedSize > limit)
            throw new IllegalStateException("Channel streaming packet bytes out of range");
        int capacity = zstd.bound(framedSize) + 64;
        if (encodeOutput == null || encodeOutput.capacity() < capacity)
            encodeOutput = ByteBuffer.allocateDirect(capacity);
        ByteBuffer input = encodeInput.clear(), output = encodeOutput.clear();
        varInt(input, batchSize);
        varInt(input, 1);
        varInt(input, size);
        int cursor = 0;
        do {
            int count = Math.min(input.remaining(), size - cursor);
            if (count > 0) input.put(bytes, cursor, count);
            cursor += count;
            input.flip();
            while (input.hasRemaining()) {
                int before = input.position(), written = output.position();
                zstd.compress(compressor, output, input, zstd.continueDirective);
                if (!output.hasRemaining()
                        || before == input.position() && written == output.position())
                    throw new IllegalStateException("zstd compression made no progress");
            }
            input.clear();
        } while (cursor < size);
        input.limit(0);
        while (!zstd.compress(compressor, output, input, termination)) {
            if (!output.hasRemaining())
                throw new IllegalStateException("zstd compressed output exceeded its bound");
        }
        byte[] result = new byte[output.position()];
        output.flip().get(result);
        return result;
    }

    public byte[] decode(byte[] bytes) {
        open();
        if (decodeInput == null) decodeInput = ByteBuffer.allocateDirect(INPUT_CAPACITY);
        if (decodeOutput == null) decodeOutput = ByteBuffer.allocateDirect(INPUT_CAPACITY);
        ByteBuffer input = decodeInput.clear();
        ByteBuffer output = decodeOutput.clear();
        int size = bytes == null ? 0 : bytes.length, cursor = 0;
        boolean full;
        do {
            int count = Math.min(input.remaining(), size - cursor);
            if (count > 0) input.put(bytes, cursor, count);
            cursor += count;
            input.flip();
            do {
                if (!output.hasRemaining()) {
                    if (output.capacity() > limit)
                        throw new IllegalStateException(
                                "Channel streaming decoded bytes out of range");
                    int expected = frameSize(output);
                    int capacity = Math.min(limit + 1, Math.max(output.capacity() * 2, expected));
                    ByteBuffer larger = ByteBuffer.allocateDirect(capacity);
                    output.flip();
                    larger.put(output);
                    output = decodeOutput = larger;
                }
                int before = input.position(), written = output.position();
                zstd.decompress(decoder, output, input);
                full = !output.hasRemaining();
                if (input.hasRemaining()
                        && before == input.position()
                        && written == output.position())
                    throw new IllegalStateException("zstd decompression made no progress");
            } while (input.hasRemaining() || full);
            input.clear();
        } while (cursor < size);
        output.flip();
        if (output.remaining() > limit)
            throw new IllegalStateException("Channel streaming decoded bytes out of range");
        int batchSize = readVarInt(output);
        if (batchSize < 0 || batchSize != output.remaining())
            throw new IllegalStateException("Channel streaming packet batch length out of range");
        if (readVarInt(output) != 1)
            throw new IllegalStateException(
                    "Expected exactly one packet in channel transport batch");
        int length = readVarInt(output);
        if (length < 0 || length != output.remaining())
            throw new IllegalStateException("Channel streaming packet length out of range");
        byte[] result = new byte[length];
        output.get(result);
        return result;
    }

    private static int frameSize(ByteBuffer buffer) {
        int result = 0;
        for (int i = 0; i < 5 && i < buffer.position(); i++) {
            int value = buffer.get(i) & 255;
            result |= (value & 127) << (7 * i);
            if (value < 128) return result < 0 ? 0 : result + i + 1;
        }
        return 0;
    }

    private static int varSize(int value) {
        return Math.max(1, (32 - Integer.numberOfLeadingZeros(value) + 6) / 7);
    }

    private static void varInt(ByteBuffer output, int value) {
        while ((value & ~127) != 0) {
            output.put((byte) (value | 128));
            value >>>= 7;
        }
        output.put((byte) value);
    }

    private static int readVarInt(ByteBuffer input) {
        int result = 0;
        for (int i = 0; i < 5; i++) {
            int value = input.get() & 255;
            result |= (value & 127) << (7 * i);
            if (value < 128) return result;
        }
        throw new IllegalStateException("VarInt too large in channel streaming packet frame");
    }
}
