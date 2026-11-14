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

package com.hbm.qmaw;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class QMAWSyncCodecs {
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, QuickManualAndWiki>>
            PAGES =
                    ByteBufCodecs
                            .<RegistryFriendlyByteBuf, Identifier, QuickManualAndWiki,
                                    Map<Identifier, QuickManualAndWiki>>
                                    map(
                                            HashMap::new,
                                            Identifier.STREAM_CODEC,
                                            ByteBufCodecs.fromCodecWithRegistries(
                                                    QuickManualAndWiki.CODEC));

    private QMAWSyncCodecs() {}

    public static Map<Identifier, QuickManualAndWiki> snapshot(MinecraftServer server) {
        return QMAWCatalog.server().pages();
    }
}
