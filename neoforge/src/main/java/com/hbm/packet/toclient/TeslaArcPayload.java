// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class TeslaArcPayload extends ThreadedPayload {
    public static final Type<TeslaArcPayload> TYPE = new Type<>(Library.id("tesla_arc"));
    public static final StreamCodec<ByteBuf, TeslaArcPayload> STREAM_CODEC =
            streamCodec(TeslaArcPayload::decode);

    private final int entityId;
    private final List<Vec3> targets;

    public TeslaArcPayload(Entity entity, List<Vec3> targets) {
        this(entity.getId(), targets);
    }

    private TeslaArcPayload(int entityId, List<Vec3> targets) {
        this.entityId = entityId;
        this.targets = targets;
    }

    private static TeslaArcPayload decode(ByteBuf buf) {
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        int count = buf.readShort();
        List<Vec3> targets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            targets.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new TeslaArcPayload(entityId, targets);
    }

    public static void handleClient(TeslaArcPayload payload, IPayloadHandlerContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(payload.entityId);

        if (entity instanceof EntityTeslaCrab tesla) {
            tesla.targets = payload.targets;
        } else if (entity instanceof EntityTaintCrab taint) {
            taint.targets = payload.targets;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        buf.writeShort((short) targets.size());
        for (Vec3 t : targets) {
            buf.writeDouble(t.x);
            buf.writeDouble(t.y);
            buf.writeDouble(t.z);
        }
    }

    @Override
    public @NotNull Type<TeslaArcPayload> type() {
        return TYPE;
    }
}
