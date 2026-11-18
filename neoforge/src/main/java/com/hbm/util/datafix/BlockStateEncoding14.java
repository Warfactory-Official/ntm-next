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

package com.hbm.util.datafix;

import com.mojang.serialization.Dynamic;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

public final class BlockStateEncoding14 {
    private static final Set<String> FIELDS = Set.of("Name", "Properties", "id", "properties");

    private BlockStateEncoding14() {}

    public static String name(Tag state) {
        if (state instanceof StringTag text) return text.value();
        var compound = (CompoundTag) state;
        return compound.getString("Name").orElseGet(() -> compound.getString("id").orElseThrow());
    }

    public static CompoundTag properties(CompoundTag state) {
        return state.getCompound("Properties")
                .orElseGet(() -> state.getCompoundOrEmpty("properties"));
    }

    public static CompoundTag canonical(Tag state) {
        var result = new CompoundTag();
        result.putString("Name", name(state));
        if (state instanceof CompoundTag compound) {
            var properties = properties(compound);
            if (!properties.isEmpty()) result.put("Properties", properties.copy());
        }
        return result;
    }

    public static ListTag canonicalList(ListTag states) {
        var result = new ListTag();
        for (var state : states) result.add(canonical(state));
        return result;
    }

    public static CompoundTag current(CompoundTag state) {
        var result = canonical(state);
        return result;
    }

    public static boolean stateMap(CompoundTag value) {
        String id = value.getString("Name").orElseGet(() -> value.getStringOr("id", ""));
        return value.keySet().stream().allMatch(FIELDS::contains)
                && (value.contains("Name") || value.contains("properties"))
                && id.indexOf(':') > 0
                && Identifier.tryParse(id) != null;
    }

    public static boolean stateMap(Dynamic<?> value) {
        var entries = value.getMapValues().result();
        String id = name(value);
        return entries.isPresent()
                && entries.get().keySet().stream()
                        .allMatch(key -> FIELDS.contains(key.asString().getOrThrow()))
                && (value.get("Name").result().isPresent()
                        || value.get("properties").result().isPresent())
                && id.indexOf(':') > 0
                && Identifier.tryParse(id) != null;
    }

    public static String name(Dynamic<?> state) {
        return state.get("Name").asString().result().orElseGet(() -> state.get("id").asString(""));
    }

    public static Dynamic<?> properties(Dynamic<?> state) {
        var legacy = state.get("Properties").result();
        return legacy.isPresent() ? legacy.get() : state.get("properties").orElseEmptyMap();
    }

    public static Dynamic<?> current(Dynamic<?> state, Dynamic<?> properties) {
        var result = state.remove("Name").remove("Properties").remove("id").remove("properties");
        boolean empty = properties.getMapValues().result().orElseThrow().isEmpty();
        result = result.set("Name", result.createString(name(state)));
        return empty ? result : result.set("Properties", properties);
    }
}
