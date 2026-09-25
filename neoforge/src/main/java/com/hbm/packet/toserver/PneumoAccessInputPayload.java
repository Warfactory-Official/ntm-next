// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.inventory.container.MenuPneumoStorageAccess;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class PneumoAccessInputPayload extends ThreadedPayload {

    public static final Type<PneumoAccessInputPayload> TYPE =
            new Type<>(Library.id("pneumo_access_input"));
    public static final StreamCodec<ByteBuf, PneumoAccessInputPayload> STREAM_CODEC =
            streamCodec(PneumoAccessInputPayload::decode);

    private final int containerId;
    private final MenuPneumoStorageAccess.ClickOp op;
    private final int entryId;

    public PneumoAccessInputPayload(
            int containerId, MenuPneumoStorageAccess.ClickOp op, int entryId) {
        this.containerId = containerId;
        this.op = op;
        this.entryId = entryId;
    }

    private static PneumoAccessInputPayload decode(ByteBuf buf) {
        FriendlyByteBuf b = new FriendlyByteBuf(buf);
        int containerId = b.readVarInt();
        MenuPneumoStorageAccess.ClickOp op = b.readEnum(MenuPneumoStorageAccess.ClickOp.class);
        return new PneumoAccessInputPayload(containerId, op, b.readVarInt());
    }

    public static void handleServer(PneumoAccessInputPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof MenuPneumoStorageAccess menu)) return;
        if (menu.containerId != payload.containerId || !menu.stillValid(player)) return;
        menu.handleClick(payload.op, payload.entryId);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        FriendlyByteBuf b = new FriendlyByteBuf(buf);
        b.writeVarInt(containerId);
        b.writeEnum(op);
        b.writeVarInt(entryId);
    }

    @Override
    public @NotNull Type<PneumoAccessInputPayload> type() {
        return TYPE;
    }
}
