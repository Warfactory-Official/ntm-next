// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public final class FluidIdControlPayload extends ThreadedPayload {

    public static final Type<FluidIdControlPayload> TYPE =
            new Type<>(Library.id("fluid_id_control"));
    public static final StreamCodec<ByteBuf, FluidIdControlPayload> STREAM_CODEC =
            streamCodec(FluidIdControlPayload::decode);

    private final boolean primary;
    private final String fluidId;

    public FluidIdControlPayload(boolean primary, String fluidId) {
        this.primary = primary;
        this.fluidId = fluidId;
    }

    private static FluidIdControlPayload decode(ByteBuf buf) {
        boolean primary = ByteBufCodecs.BOOL.decode(buf);
        String fluidId = ByteBufCodecs.STRING_UTF8.decode(buf);
        return new FluidIdControlPayload(primary, fluidId);
    }

    public static void handleServer(FluidIdControlPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(payload.fluidId));

        ItemStack stack = sp.getMainHandItem();
        if (stack.getItem() instanceof FluidIdentifierItem) {
            FluidIdentifierData data =
                    stack.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            stack.set(
                    ModDataComponents.FLUID_IDENTIFIER.get(),
                    payload.primary ? data.withPrimary(fluid) : data.withSecondary(fluid));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.BOOL.encode(buf, primary);
        ByteBufCodecs.STRING_UTF8.encode(buf, fluidId);
    }

    @Override
    public @NotNull Type<FluidIdControlPayload> type() {
        return TYPE;
    }
}
