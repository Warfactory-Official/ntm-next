// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.qmaw.QMAWCatalog;
import com.hbm.qmaw.QuickManualAndWiki;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public record QMAWSyncPayload(Map<Identifier, QuickManualAndWiki> pages)
        implements CustomPacketPayload {
    public static final Type<QMAWSyncPayload> TYPE = new Type<>(Library.id("qmaw_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QMAWSyncPayload> STREAM_CODEC =
            ByteBufCodecs
                    .<RegistryFriendlyByteBuf, Identifier, QuickManualAndWiki,
                            Map<Identifier, QuickManualAndWiki>>
                            map(
                                    HashMap::new,
                                    Identifier.STREAM_CODEC,
                                    ByteBufCodecs.fromCodecWithRegistries(QuickManualAndWiki.CODEC))
                    .map(QMAWSyncPayload::new, QMAWSyncPayload::pages);

    public static QMAWSyncPayload of(MinecraftServer server) {
        return new QMAWSyncPayload(QMAWCatalog.server().pages());
    }

    public static void apply(QMAWSyncPayload payload) {
        QMAWCatalog.publishRemote(new QMAWCatalog(payload.pages));
    }

    @Override
    public Type<QMAWSyncPayload> type() {
        return TYPE;
    }
}
