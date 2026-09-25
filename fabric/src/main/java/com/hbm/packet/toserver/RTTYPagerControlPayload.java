// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.items.tool.ItemRTTYPager;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class RTTYPagerControlPayload extends ThreadedPayload {

    public static final Type<RTTYPagerControlPayload> TYPE =
            new Type<>(Library.id("rtty_pager_control"));
    public static final StreamCodec<ByteBuf, RTTYPagerControlPayload> STREAM_CODEC =
            streamCodec(RTTYPagerControlPayload::decode);

    private final String channel;

    public RTTYPagerControlPayload(String channel) {
        this.channel = channel;
    }

    private static RTTYPagerControlPayload decode(ByteBuf buf) {
        return new RTTYPagerControlPayload(
                new FriendlyByteBuf(buf).readUtf(ItemRTTYPager.MAX_CHANNEL_LENGTH));
    }

    public static void handleServer(
            RTTYPagerControlPayload payload, IPayloadHandlerContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ItemRTTYPager)
            ItemRTTYPager.setChannel(stack, payload.channel);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        new FriendlyByteBuf(buf).writeUtf(channel, ItemRTTYPager.MAX_CHANNEL_LENGTH);
    }

    @Override
    public @NotNull Type<RTTYPagerControlPayload> type() {
        return TYPE;
    }
}
