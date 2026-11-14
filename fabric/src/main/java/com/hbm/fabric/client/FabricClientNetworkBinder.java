/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.fabric.client;

import com.hbm.platform.Network.DatapackSync;
import com.hbm.platform.Network.PreJoinSync;
import com.hbm.platform.Network.Registration;
import com.hbm.platform.Network;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class FabricClientNetworkBinder {

    private FabricClientNetworkBinder() {}

    static void bind() {
        for (Registration<?> r : Network.clientbound) {
            register(r);
        }
        for (PreJoinSync<?> s : Network.preJoin) {
            registerPreJoin(s);
        }
        for (DatapackSync<?> s : Network.datapackSync) {
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
                        r.handler().accept(payload, Network.context(payload, ctx.player())));
    }

    private static <T extends CustomPacketPayload> void registerPreJoin(PreJoinSync<T> s) {
        ClientConfigurationNetworking.registerGlobalReceiver(
                s.type(), (payload, ctx) -> s.apply().accept(payload));
    }
}
