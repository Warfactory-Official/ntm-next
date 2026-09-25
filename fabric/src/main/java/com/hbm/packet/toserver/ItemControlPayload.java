// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.items.IItemControlReceiver;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemControlPayload extends ThreadedPayload {

    public static final Type<ItemControlPayload> TYPE = new Type<>(Library.id("item_control"));
    public static final StreamCodec<ByteBuf, ItemControlPayload> STREAM_CODEC =
            streamCodec(buf -> new ItemControlPayload(ByteBufCodecs.COMPOUND_TAG.decode(buf)));

    private final CompoundTag data;

    public ItemControlPayload(CompoundTag data) {
        this.data = data;
    }

    public static void handleServer(ItemControlPayload payload, IPayloadHandlerContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof IItemControlReceiver receiver) {
            receiver.receiveControl(player, stack, payload.data);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.COMPOUND_TAG.encode(buf, data);
    }

    @Override
    public @NotNull Type<ItemControlPayload> type() {
        return TYPE;
    }
}
