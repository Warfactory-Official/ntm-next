// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public final class PlayerPropsPayload extends ThreadedPayload {

    public static final Type<PlayerPropsPayload> TYPE = new Type<>(Library.id("player_props"));
    public static final StreamCodec<ByteBuf, PlayerPropsPayload> STREAM_CODEC =
            streamCodec(
                    buf ->
                            new PlayerPropsPayload(
                                    buf.readFloat(),
                                    buf.readFloat(),
                                    buf.readInt(),
                                    buf.readBoolean(),
                                    buf.readBoolean(),
                                    buf.readBoolean()));
    private final float shield;
    private final float maxShield;
    private final int reputation;
    private final boolean enableBackpack;
    private final boolean enableHUD;
    private final boolean enableMagnet;

    public PlayerPropsPayload(HbmPlayerProps props) {
        this(
                props.shield,
                props.maxShield,
                props.reputation,
                props.enableBackpack,
                props.enableHUD,
                props.enableMagnet);
    }

    private PlayerPropsPayload(
            float shield,
            float maxShield,
            int reputation,
            boolean enableBackpack,
            boolean enableHUD,
            boolean enableMagnet) {
        this.shield = shield;
        this.maxShield = maxShield;
        this.reputation = reputation;
        this.enableBackpack = enableBackpack;
        this.enableHUD = enableHUD;
        this.enableMagnet = enableMagnet;
    }

    public static void handle(PlayerPropsPayload payload, IPayloadHandlerContext context) {
        Player player = context.playerOrNull();
        if (player == null) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        props.shield = payload.shield;
        props.maxShield = payload.maxShield;
        props.reputation = payload.reputation;
        props.enableBackpack = payload.enableBackpack;
        props.enableHUD = payload.enableHUD;
        props.enableMagnet = payload.enableMagnet;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeFloat(shield);
        buffer.writeFloat(maxShield);
        buffer.writeInt(reputation);
        buffer.writeBoolean(enableBackpack);
        buffer.writeBoolean(enableHUD);
        buffer.writeBoolean(enableMagnet);
    }

    @Override
    public Type<PlayerPropsPayload> type() {
        return TYPE;
    }
}
