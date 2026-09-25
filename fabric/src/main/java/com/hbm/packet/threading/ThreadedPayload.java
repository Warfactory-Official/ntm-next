// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.threading;

import com.hbm.packet.PacketWire;
import com.hbm.packet.compat.PayloadTypeNames;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public abstract class ThreadedPayload implements CustomPacketPayload {

    int wirePrefixLen;

    private volatile ByteBuf compiledBuffer;
    private volatile ByteBuf compressedBuffer;
    private volatile ByteBuf uncompressedBuffer;
    private volatile ByteBuf socketBuffer;
    private volatile byte[] managedBytes;
    private int managedOffset;
    private int managedLength;
    private ClientboundCustomPayloadPacket managedPacket;
    private boolean managed;
    private volatile int references = 1;
    private static final VarHandle REFERENCES;

    static {
        try {
            REFERENCES =
                    MethodHandles.lookup()
                            .findVarHandle(ThreadedPayload.class, "references", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static <T extends ThreadedPayload> StreamCodec<ByteBuf, T> streamCodec(
            StreamDecoder<ByteBuf, T> decoder) {
        return StreamCodec.of((out, payload) -> payload.writeManagedBody(out), decoder);
    }

    public final int managedLength() {
        prepareManaged();
        return managedLength;
    }

    public final void writeManagedBody(ByteBuf output) {
        prepareManaged();
        output.writeBytes(managedBytes, managedOffset, managedLength);
    }

    public final void prepareManaged() {
        if (managedBytes == null) prepareManaged0();
    }

    private synchronized void prepareManaged0() {
        if (managedBytes != null) return;
        managed = true;
        ByteBuf wire = getCompiledBuffer();
        managedLength = wire.writerIndex() - wirePrefixLen;
        byte[] bytes;
        if (wire.hasArray()) {
            bytes = wire.array();
            managedOffset = wire.arrayOffset() + wirePrefixLen;
        } else {
            bytes = new byte[managedLength];
            wire.getBytes(wirePrefixLen, bytes);
        }
        managedBytes = bytes;
    }

    public final ClientboundCustomPayloadPacket managedPacket() {
        prepareManaged();
        if (managedPacket == null) managedPacket = new ClientboundCustomPayloadPacket(this);
        return managedPacket;
    }

    public abstract void toBytes(ByteBuf buf);

    protected int bodyCapacity() {
        return 256;
    }

    public final ByteBuf getCompiledBuffer() {
        if (references == 0) throw new IllegalReferenceCountException(0);
        ByteBuf buf = compiledBuffer;
        if (buf != null) return buf;
        return compile0();
    }

    public final ByteBuf getSocketBuffer(int threshold) {
        ByteBuf wire = getCompiledBuffer();
        boolean enabled = threshold >= 0;
        boolean compress = enabled && wire.readableBytes() >= threshold;
        ByteBuf cached = enabled ? compress ? compressedBuffer : uncompressedBuffer : socketBuffer;
        if (cached != null) return cached;
        return compress0(wire, enabled, compress);
    }

    private synchronized ByteBuf compress0(ByteBuf wire, boolean enabled, boolean compress) {
        ByteBuf cached = enabled ? compress ? compressedBuffer : uncompressedBuffer : socketBuffer;
        if (cached != null) return cached;
        ByteBuf encoded = FrameCompression.encode(wire, enabled, compress);
        if (!enabled) socketBuffer = encoded;
        else if (compress) compressedBuffer = encoded;
        else uncompressedBuffer = encoded;
        return encoded;
    }

    public static void clearCompression() {
        FrameCompression.clear();
    }

    private synchronized ByteBuf compile0() {
        if (compiledBuffer != null) return compiledBuffer;
        byte[] ccpId = PacketWire.ccpPacketIdBytes();
        byte[] resourceLoc = ccpId == null ? null : resourceLocBytes();
        int prefix = ccpId == null ? 0 : ccpId.length + resourceLoc.length;
        managed |= PacketWire.managedTransport();
        ByteBuf buf =
                managed
                        ? Unpooled.buffer(prefix + bodyCapacity())
                        : PooledByteBufAllocator.DEFAULT.directBuffer(prefix + bodyCapacity());
        try {
            if (ccpId != null) {
                buf.writeBytes(ccpId);
                buf.writeBytes(resourceLoc);
                wirePrefixLen = ccpId.length + resourceLoc.length;
            }
            toBytes(buf);
        } catch (Throwable t) {
            buf.release();
            throw t;
        }
        return compiledBuffer = buf;
    }

    private byte[] resourceLocBytes() {
        return PayloadTypeNames.wire(type());
    }

    public final boolean compiledIncludesWirePrefix() {
        return wirePrefixLen > 0;
    }

    public final void retain() {
        int count;
        do {
            count = references;
            if (count == 0 || count == Integer.MAX_VALUE)
                throw new IllegalReferenceCountException(count, 1);
        } while (!REFERENCES.compareAndSet(this, count, count + 1));
    }

    public final void release() {
        int count;
        do {
            count = references;
            if (count == 0) throw new IllegalReferenceCountException(0, -1);
        } while (!REFERENCES.compareAndSet(this, count, count - 1));
        if (count != 1) return;
        if (compiledBuffer != null) compiledBuffer.release();
        if (compressedBuffer != null) compressedBuffer.release();
        if (uncompressedBuffer != null) uncompressedBuffer.release();
        if (socketBuffer != null) socketBuffer.release();
        releaseBody();
    }

    protected void releaseBody() {}
}
