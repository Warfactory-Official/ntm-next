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

package com.hbm.items.weapon.grenade;

import com.hbm.NuclearTech;
import com.hbm.items.weapon.grenade.GrenadeFillingItem;
import com.hbm.registration.Registrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class GrenadeFillings {

    private static final Map<Identifier, GrenadeFillingDefinition> DEFINITIONS =
            new LinkedHashMap<>();
    private static volatile List<GrenadeFillingDefinition> ordered = List.of();

    public static final Codec<GrenadeFillingDefinition> CODEC =
            Codec.STRING.comapFlatMap(GrenadeFillings::parse, GrenadeFillings::name);
    public static final StreamCodec<ByteBuf, GrenadeFillingDefinition> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(GrenadeFillings::get, GrenadeFillingDefinition::id);

    static {
        for (var filling : GrenadeFillingItem.EnumGrenadeFilling.values())
            DEFINITIONS.put(filling.id(), filling);
        ordered = List.copyOf(DEFINITIONS.values());
    }

    private GrenadeFillings() {}

    public static void register(GrenadeFillingDefinition filling) {
        Registrar.assertOpen("GrenadeFillings.register(" + filling.id() + ")");
        if (DEFINITIONS.putIfAbsent(filling.id(), filling) != null)
            throw new IllegalArgumentException("Duplicate grenade filling " + filling.id());
        ordered = List.copyOf(DEFINITIONS.values());
    }

    public static GrenadeFillingDefinition get(Identifier id) {
        var filling = DEFINITIONS.get(id);
        if (filling == null) throw new IllegalArgumentException("Unknown grenade filling " + id);
        return filling;
    }

    public static List<GrenadeFillingDefinition> values() {
        return ordered;
    }

    private static DataResult<GrenadeFillingDefinition> parse(String name) {
        Identifier id =
                name.indexOf(':') < 0
                        ? Identifier.fromNamespaceAndPath(NuclearTech.MOD_ID, name)
                        : Identifier.tryParse(name);
        var filling = id == null ? null : DEFINITIONS.get(id);
        return filling == null
                ? DataResult.error(() -> "Unknown grenade filling " + name)
                : DataResult.success(filling);
    }

    private static String name(GrenadeFillingDefinition filling) {
        Identifier id = filling.id();
        return id.getNamespace().equals(NuclearTech.MOD_ID) ? id.getPath() : id.toString();
    }
}
