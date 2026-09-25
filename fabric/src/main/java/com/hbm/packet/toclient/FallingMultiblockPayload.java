// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.entity.item.EntityFallingMultiblock;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public final class FallingMultiblockPayload extends ThreadedPayload {

    public static final Type<FallingMultiblockPayload> TYPE =
            new Type<>(Library.id("falling_multiblock"));
    public static final StreamCodec<ByteBuf, FallingMultiblockPayload> STREAM_CODEC =
            streamCodec(FallingMultiblockPayload::decode);
    private static final StreamCodec<ByteBuf, Optional<CompoundTag>> CARRIED =
            ByteBufCodecs.optional(ByteBufCodecs.TRUSTED_COMPOUND_TAG);

    private final int entityId;
    private final long corePos;
    private final long[] members;
    private final @Nullable CompoundTag carried;

    public FallingMultiblockPayload(
            int entityId, long corePos, long[] members, @Nullable CompoundTag carried) {
        this.entityId = entityId;
        this.corePos = corePos;
        this.members = members;
        this.carried = carried;
    }

    private static FallingMultiblockPayload decode(ByteBuf buf) {
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        long corePos = buf.readLong();
        long[] members = new long[ByteBufCodecs.VAR_INT.decode(buf)];
        for (int i = 0; i < members.length; i++) members[i] = buf.readLong();
        return new FallingMultiblockPayload(
                entityId, corePos, members, CARRIED.decode(buf).orElse(null));
    }

    public static void handleClient(FallingMultiblockPayload payload, IPayloadHandlerContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(payload.entityId);
        if (entity instanceof EntityFallingMultiblock falling) {
            falling.setMembersFromNetwork(payload.members, payload.corePos, payload.carried);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        buf.writeLong(corePos);
        ByteBufCodecs.VAR_INT.encode(buf, members.length);
        for (long member : members) buf.writeLong(member);
        CARRIED.encode(buf, Optional.ofNullable(carried));
    }

    @Override
    public @NotNull Type<FallingMultiblockPayload> type() {
        return TYPE;
    }
}
