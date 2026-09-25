// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class DesignatorCoordPayload extends ThreadedPayload {

    public static final Type<DesignatorCoordPayload> TYPE =
            new Type<>(Library.id("designator_coord"));
    public static final StreamCodec<ByteBuf, DesignatorCoordPayload> STREAM_CODEC =
            streamCodec(DesignatorCoordPayload::decode);

    private final InteractionHand hand;
    private final int operator;
    private final int value;
    private final int reference;

    public DesignatorCoordPayload(InteractionHand hand, int operator, int value, int reference) {
        this.hand = hand;
        this.operator = operator;
        this.value = value;
        this.reference = reference;
    }

    private static DesignatorCoordPayload decode(ByteBuf buf) {
        return new DesignatorCoordPayload(
                InteractionHand.values()[ByteBufCodecs.VAR_INT.decode(buf)],
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf));
    }

    public static void handleServer(DesignatorCoordPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        ItemStack stack = sp.getItemInHand(payload.hand);
        if (!(stack.getItem() instanceof ItemDesignatorManual)) return;
        long target = stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L);
        stack.set(
                ModDataComponents.TARGET_DESIGNATOR.get(),
                ItemDesignatorManual.apply(
                        target,
                        payload.operator,
                        payload.value,
                        payload.reference,
                        sp.getX(),
                        sp.getZ()));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, hand.ordinal());
        ByteBufCodecs.INT.encode(buf, operator);
        ByteBufCodecs.INT.encode(buf, value);
        ByteBufCodecs.INT.encode(buf, reference);
    }

    @Override
    public @NotNull Type<DesignatorCoordPayload> type() {
        return TYPE;
    }
}
