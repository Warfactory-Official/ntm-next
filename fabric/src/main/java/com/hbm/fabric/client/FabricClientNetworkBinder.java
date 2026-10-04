// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.platform.FabricNetwork.DatapackSync;
import com.hbm.platform.FabricNetwork.PreJoinSync;
import com.hbm.platform.FabricNetwork.Registration;
import com.hbm.platform.FabricNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class FabricClientNetworkBinder {

    private FabricClientNetworkBinder() {}

    static void bind() {
        for (Registration<?> r : FabricNetwork.clientbound) {
            register(r);
        }
        for (PreJoinSync<?> s : FabricNetwork.preJoin) {
            registerPreJoin(s);
        }
        for (DatapackSync<?> s : FabricNetwork.datapackSync) {
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
                (payload, ctx) -> r.handler().accept(payload, FabricNetwork.wrap(ctx.player())));
    }

    private static <T extends CustomPacketPayload> void registerPreJoin(PreJoinSync<T> s) {
        ClientConfigurationNetworking.registerGlobalReceiver(
                s.type(), (payload, ctx) -> s.apply().accept(payload));
    }
}
