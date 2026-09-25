// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.handler.radiation.RadVisOverlay;
import com.hbm.handler.radiation.RadVisSnapshot;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class RadVisSnapshotPayload extends ThreadedPayload {

    public static final Type<RadVisSnapshotPayload> TYPE =
            new Type<>(Library.id("radvis_snapshot"));
    public static final StreamCodec<ByteBuf, RadVisSnapshotPayload> STREAM_CODEC =
            streamCodec(buf -> new RadVisSnapshotPayload(RadVisSnapshot.read(buf), new BitSet()));

    private final RadVisSnapshot snapshot;
    private final BitSet layout;

    public RadVisSnapshotPayload(RadVisSnapshot snapshot, BitSet layout) {
        this.snapshot = snapshot;
        this.layout = layout;
    }

    public static void handle(RadVisSnapshotPayload payload, IPayloadHandlerContext context) {
        RadVisOverlay.accept(payload.snapshot);
    }

    @Override
    protected int bodyCapacity() {
        return 64 + 8 * snapshot.size() + 1024 * layout.cardinality();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        snapshot.write(buf, layout);
    }

    @Override
    public @NotNull Type<RadVisSnapshotPayload> type() {
        return TYPE;
    }
}
