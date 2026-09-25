// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.extprop.ContaminationEffect;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public final class HbmPlayerSyncPayload extends ThreadedPayload {

    public static final Type<HbmPlayerSyncPayload> TYPE = new Type<>(Library.id("player_sync"));
    public static final StreamCodec<ByteBuf, HbmPlayerSyncPayload> STREAM_CODEC =
            streamCodec(HbmPlayerSyncPayload::decode);

    private final double radiation;
    private final double digamma;
    private final double radBuf;
    private final int asbestos;
    private final int bombTimer;
    private final int contagion;
    private final int blacklung;
    private final int oil;
    private final ContaminationEffect[] contamination;

    public HbmPlayerSyncPayload(HbmLivingProps p) {
        this.radiation = p.radiation;
        this.digamma = p.digamma;
        this.radBuf = p.radBuf;
        this.asbestos = p.asbestos;
        this.bombTimer = p.bombTimer;
        this.contagion = p.contagion;
        this.blacklung = p.blacklung;
        this.oil = p.oil;

        this.contamination = p.getCont().toArray(new ContaminationEffect[0]);
    }

    private HbmPlayerSyncPayload(
            double radiation,
            double digamma,
            double radBuf,
            int asbestos,
            int bombTimer,
            int contagion,
            int blacklung,
            int oil,
            ContaminationEffect[] contamination) {
        this.radiation = radiation;
        this.digamma = digamma;
        this.radBuf = radBuf;
        this.asbestos = asbestos;
        this.bombTimer = bombTimer;
        this.contagion = contagion;
        this.blacklung = blacklung;
        this.oil = oil;
        this.contamination = contamination;
    }

    private static HbmPlayerSyncPayload decode(ByteBuf buf) {
        double radiation = buf.readDouble();
        double digamma = buf.readDouble();
        double radBuf = buf.readDouble();
        int asbestos = buf.readInt();
        int bombTimer = buf.readInt();
        int contagion = buf.readInt();
        int blacklung = buf.readInt();
        int oil = buf.readInt();
        int count = buf.readInt();
        ContaminationEffect[] cont = new ContaminationEffect[count];
        for (int i = 0; i < count; i++) {
            double maxRad = buf.readDouble();
            int maxTime = buf.readInt();
            int time = buf.readInt();
            boolean ignoreArmor = buf.readBoolean();
            ContaminationEffect e = new ContaminationEffect(maxRad, maxTime, ignoreArmor);
            e.time = time;
            cont[i] = e;
        }
        return new HbmPlayerSyncPayload(
                radiation, digamma, radBuf, asbestos, bombTimer, contagion, blacklung, oil, cont);
    }

    public static void handle(HbmPlayerSyncPayload payload, IPayloadHandlerContext ctx) {
        Player player = ctx.playerOrNull();
        if (player == null) return;

        HbmLivingProps p = HbmLivingProps.getData(player);
        p.radiation = payload.radiation;
        p.digamma = payload.digamma;
        p.radBuf = payload.radBuf;
        p.asbestos = payload.asbestos;
        p.bombTimer = payload.bombTimer;
        p.contagion = payload.contagion;
        p.blacklung = payload.blacklung;
        p.oil = payload.oil;
        p.setCont(payload.contamination);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(radiation);
        buf.writeDouble(digamma);
        buf.writeDouble(radBuf);
        buf.writeInt(asbestos);
        buf.writeInt(bombTimer);
        buf.writeInt(contagion);
        buf.writeInt(blacklung);
        buf.writeInt(oil);
        buf.writeInt(contamination.length);
        for (ContaminationEffect e : contamination) {
            buf.writeDouble(e.maxRad);
            buf.writeInt(e.maxTime);
            buf.writeInt(e.time);
            buf.writeBoolean(e.ignoreArmor);
        }
    }

    @Override
    public @NotNull Type<HbmPlayerSyncPayload> type() {
        return TYPE;
    }
}
