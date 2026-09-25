// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.inventory.container.MenuPneumoStorageAccess;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public final class PneumoAccessSyncPayload extends ThreadedPayload {

    public static final Type<PneumoAccessSyncPayload> TYPE =
            new Type<>(Library.id("pneumo_access_sync"));
    public static final StreamCodec<ByteBuf, PneumoAccessSyncPayload> STREAM_CODEC =
            streamCodec(PneumoAccessSyncPayload::decode);

    private final int containerId;
    private final List<Delta> deltas;

    public PneumoAccessSyncPayload(int containerId, List<Delta> deltas) {
        this.containerId = containerId;
        this.deltas = deltas;
    }

    private static PneumoAccessSyncPayload decode(ByteBuf buf) {

        RegistryFriendlyByteBuf reg = (RegistryFriendlyByteBuf) buf;
        int containerId = reg.readVarInt();
        int size = reg.readVarInt();
        List<Delta> deltas = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int id = reg.readVarInt();
            ItemStack display = reg.readBoolean() ? ItemStack.STREAM_CODEC.decode(reg) : null;
            deltas.add(new Delta(id, display, reg.readVarLong()));
        }
        return new PneumoAccessSyncPayload(containerId, deltas);
    }

    public static void handleClient(PneumoAccessSyncPayload payload, IPayloadHandlerContext ctx) {
        Player player = ctx.playerOrNull();
        if (player == null) return;
        if (!(player.containerMenu instanceof MenuPneumoStorageAccess menu)) return;
        if (menu.containerId != payload.containerId) return;
        menu.applyDeltas(payload.deltas);
    }

    @Override
    public void toBytes(ByteBuf buf) {

        RegistryFriendlyByteBuf reg =
                new RegistryFriendlyByteBuf(
                        buf, Services.SERVER.getCurrentServer().registryAccess());
        reg.writeVarInt(containerId);
        reg.writeVarInt(deltas.size());
        for (Delta delta : deltas) {
            reg.writeVarInt(delta.id());
            reg.writeBoolean(delta.display() != null);
            if (delta.display() != null) ItemStack.STREAM_CODEC.encode(reg, delta.display());
            reg.writeVarLong(delta.amount());
        }
    }

    @Override
    public @NotNull Type<PneumoAccessSyncPayload> type() {
        return TYPE;
    }

    public record Delta(int id, @Nullable ItemStack display, long amount) {}
}
