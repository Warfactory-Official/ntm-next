// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerAppearancePayload extends ThreadedPayload {

    public static final Type<PlayerAppearancePayload> TYPE =
            new Type<>(Library.id("player_appearance"));
    public static final StreamCodec<ByteBuf, PlayerAppearancePayload> STREAM_CODEC =
            streamCodec(buf -> new PlayerAppearancePayload(buf.readInt(), buf.readByte()));
    private final int entityId;
    private final byte flags;

    public PlayerAppearancePayload(int entityId, byte flags) {
        this.entityId = entityId;
        this.flags = flags;
    }

    public static byte fromEffects(Player player) {
        byte flags = player.hasEffect(HbmPotion.death()) ? PlayerAppearance.MANLY : 0;
        MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
        if (invisibility != null && invisibility.getAmplifier() > 0)
            flags |= PlayerAppearance.STEALTH;
        return flags;
    }

    public static void update(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        byte flags = fromEffects(player);
        if (player.hbm$appearance() == flags) return;
        player.hbm$setAppearance(flags);
        Services.NETWORK.sendToAllTracking(
                new PlayerAppearancePayload(player.getId(), flags), player);
    }

    public static void sendInitial(Entity tracked, ServerPlayer observer) {
        if (tracked instanceof Player player) {
            byte flags = fromEffects(player);
            if (flags != 0)
                Services.NETWORK.sendTo(
                        new PlayerAppearancePayload(player.getId(), flags), observer);
        }
    }

    public static void handle(PlayerAppearancePayload payload, IPayloadHandlerContext context) {
        var viewer = context.playerOrNull();
        if (viewer == null) return;
        Entity entity = viewer.level().getEntity(payload.entityId);
        if (entity instanceof Player player) player.hbm$setAppearance(payload.flags);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(flags);
    }

    @Override
    public Type<PlayerAppearancePayload> type() {
        return TYPE;
    }
}
