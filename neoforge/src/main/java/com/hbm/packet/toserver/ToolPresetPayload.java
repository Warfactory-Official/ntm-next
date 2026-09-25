// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ToolPresetPayload extends ThreadedPayload {

    public static final Type<ToolPresetPayload> TYPE = new Type<>(Library.id("tool_preset"));
    public static final StreamCodec<ByteBuf, ToolPresetPayload> STREAM_CODEC =
            streamCodec(ToolPresetPayload::decode);

    private static final int MAX_PRESETS = 99;

    private final List<ToolPreset> presets;
    private final int current;

    public ToolPresetPayload(List<ToolPreset> presets, int current) {
        this.presets = List.copyOf(presets);
        this.current = current;
    }

    private static ToolPresetPayload decode(ByteBuf buf) {
        List<ToolPreset> presets =
                ToolPreset.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PRESETS)).decode(buf);
        return new ToolPresetPayload(presets, ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleServer(ToolPresetPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || payload.presets.isEmpty()) return;

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemToolAbility tool) {
            tool.setPresets(held, payload.presets, payload.current);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ToolPreset.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PRESETS)).encode(buf, presets);
        ByteBufCodecs.VAR_INT.encode(buf, current);
    }

    @Override
    public @NotNull Type<ToolPresetPayload> type() {
        return TYPE;
    }
}
