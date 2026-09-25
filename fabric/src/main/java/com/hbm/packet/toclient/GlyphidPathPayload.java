// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.render.GlyphidPathRenderer;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;

public final class GlyphidPathPayload extends ThreadedPayload {
    public static final Type<GlyphidPathPayload> TYPE = new Type<>(Library.id("glyphid_path"));
    public static final StreamCodec<ByteBuf, GlyphidPathPayload> STREAM_CODEC =
            streamCodec(GlyphidPathPayload::decode);

    private static final int MAX_NODES = 128;

    private final int entityId;
    private final int nextIndex;
    private final int targetX;
    private final int targetY;
    private final int targetZ;
    private final int[] nodes;
    private final float[] costs;

    private GlyphidPathPayload(
            int entityId,
            int nextIndex,
            int targetX,
            int targetY,
            int targetZ,
            int[] nodes,
            float[] costs) {
        this.entityId = entityId;
        this.nextIndex = nextIndex;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.nodes = nodes;
        this.costs = costs;
    }

    public static GlyphidPathPayload of(int entityId, Path path) {
        int count = Math.min(path.getNodeCount(), MAX_NODES);
        int[] nodes = new int[count * 3];
        float[] costs = new float[count];

        for (int i = 0; i < count; i++) {
            Node node = path.getNode(i);
            nodes[i * 3] = node.x;
            nodes[i * 3 + 1] = node.y;
            nodes[i * 3 + 2] = node.z;
            costs[i] = node.f;
        }

        var target = path.getTarget();
        return new GlyphidPathPayload(
                entityId,
                path.getNextNodeIndex(),
                target.getX(),
                target.getY(),
                target.getZ(),
                nodes,
                costs);
    }

    public static GlyphidPathPayload cleared(int entityId) {
        return new GlyphidPathPayload(entityId, 0, 0, 0, 0, new int[0], new float[0]);
    }

    private static GlyphidPathPayload decode(ByteBuf buf) {
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        int nextIndex = ByteBufCodecs.VAR_INT.decode(buf);
        int targetX = buf.readInt();
        int targetY = buf.readInt();
        int targetZ = buf.readInt();
        int count = ByteBufCodecs.VAR_INT.decode(buf);

        int[] nodes = new int[count * 3];
        float[] costs = new float[count];
        for (int i = 0; i < count; i++) {
            nodes[i * 3] = buf.readInt();
            nodes[i * 3 + 1] = buf.readInt();
            nodes[i * 3 + 2] = buf.readInt();
            costs[i] = buf.readFloat();
        }
        return new GlyphidPathPayload(entityId, nextIndex, targetX, targetY, targetZ, nodes, costs);
    }

    public static void handleClient(GlyphidPathPayload payload, IPayloadHandlerContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (payload.costs.length == 0) {
            if (payload.entityId < 0) {
                GlyphidPathRenderer.clearAll();
            } else {
                GlyphidPathRenderer.clear(payload.entityId);
            }
            return;
        }
        GlyphidPathRenderer.accept(
                payload.entityId,
                payload.nodes,
                payload.costs,
                payload.nextIndex,
                payload.targetX,
                payload.targetY,
                payload.targetZ);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        ByteBufCodecs.VAR_INT.encode(buf, nextIndex);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        ByteBufCodecs.VAR_INT.encode(buf, costs.length);
        for (int i = 0; i < costs.length; i++) {
            buf.writeInt(nodes[i * 3]);
            buf.writeInt(nodes[i * 3 + 1]);
            buf.writeInt(nodes[i * 3 + 2]);
            buf.writeFloat(costs[i]);
        }
    }

    @Override
    public @NotNull Type<GlyphidPathPayload> type() {
        return TYPE;
    }
}
