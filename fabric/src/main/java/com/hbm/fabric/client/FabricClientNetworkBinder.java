// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.platform.FabricNetworkService.DatapackSync;
import com.hbm.platform.FabricNetworkService.PreJoinSync;
import com.hbm.platform.FabricNetworkService.Registration;
import com.hbm.platform.FabricNetworkService;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class FabricClientNetworkBinder {

    private FabricClientNetworkBinder() {}

    static void bind(FabricNetworkService service) {
        for (Registration<?> r : service.clientboundRegistrations()) {
            register(r);
        }
        for (PreJoinSync<?> s : service.preJoinSyncs()) {
            registerPreJoin(s);
        }
        for (DatapackSync<?> s : service.datapackSyncs()) {
            registerDatapackSync(s);
        }
    }

    private static <T extends CustomPacketPayload> void registerDatapackSync(DatapackSync<T> s) {
        ClientPlayNetworking.registerGlobalReceiver(
                s.type(), (payload, ctx) -> s.apply().accept(payload));
    }

    private static <T extends CustomPacketPayload> void register(Registration<T> r) {
        ClientPlayNetworking.registerGlobalReceiver(
                r.type(),
                (payload, ctx) ->
                        r.handler().accept(payload, FabricNetworkService.wrap(ctx.player())));
    }

    private static <T extends CustomPacketPayload> void registerPreJoin(PreJoinSync<T> s) {
        ClientConfigurationNetworking.registerGlobalReceiver(
                s.type(), (payload, ctx) -> s.apply().accept(payload));
    }
}
