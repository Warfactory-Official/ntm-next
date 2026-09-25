// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.world.ore.OreGeneration;
import com.hbm.world.ore.OreGenerationProfile;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OreGenerationSyncPayload(List<OreGenerationProfile> profiles)
        implements CustomPacketPayload {
    public static final Type<OreGenerationSyncPayload> TYPE =
            new Type<>(Library.id("ore_generation"));
    public static final StreamCodec<ByteBuf, OreGenerationSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodec(OreGenerationProfile.CODEC.listOf())
                    .map(OreGenerationSyncPayload::new, OreGenerationSyncPayload::profiles);

    public OreGenerationSyncPayload {
        profiles = List.copyOf(profiles);
    }

    public static OreGenerationSyncPayload of() {
        return new OreGenerationSyncPayload(OreGeneration.serverProfiles());
    }

    public static void apply(OreGenerationSyncPayload payload) {
        OreGeneration.accept(payload.profiles);
    }

    @Override
    public Type<OreGenerationSyncPayload> type() {
        return TYPE;
    }
}
