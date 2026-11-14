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

package com.hbm.inventory.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;

public final class FluidPropertyPatch {
    private static final Set<String> ENTRIES = Set.of("traits", "containers");
    private static final String FLAGS = "flags";

    private FluidPropertyPatch() {}

    public static JsonObject diff(JsonObject base, JsonObject edited) {
        JsonObject patch = new JsonObject();
        Set<String> keys = new TreeSet<>(base.keySet());
        keys.addAll(edited.keySet());
        for (String key : keys) {
            JsonElement before = base.get(key), after = edited.get(key);
            if (ENTRIES.contains(key)) {
                JsonObject from = before == null ? new JsonObject() : before.getAsJsonObject();
                JsonObject to = after == null ? new JsonObject() : after.getAsJsonObject();
                JsonObject entries = new JsonObject();
                Set<String> names = new TreeSet<>(from.keySet());
                names.addAll(to.keySet());
                for (String name : names) {
                    if (!to.has(name)) entries.addProperty(name, false);
                    else if (!to.get(name).equals(from.get(name))) entries.add(name, to.get(name));
                }
                if (!entries.isEmpty()) patch.add(key, entries);
            } else if (key.equals(FLAGS)) {
                Set<String> from = flags(before), to = flags(after);
                JsonObject flags = new JsonObject();
                for (String flag : new TreeSet<>(from))
                    if (!to.contains(flag)) flags.addProperty(flag, false);
                for (String flag : new TreeSet<>(to))
                    if (!from.contains(flag)) flags.addProperty(flag, true);
                if (!flags.isEmpty()) patch.add(key, flags);
            } else if (after == null) {
                patch.addProperty(key, false);
            } else if (!after.equals(before)) {
                patch.add(key, after);
            }
        }
        return patch;
    }

    public static JsonObject apply(
            Identifier fluid, JsonObject base, Map<Identifier, JsonObject> patches) {
        JsonObject result = base.deepCopy();
        Map<String, Identifier> claimed = new HashMap<>();
        for (Map.Entry<Identifier, JsonObject> patch : patches.entrySet()) {
            Identifier source = patch.getKey();
            for (Map.Entry<String, JsonElement> field : patch.getValue().entrySet()) {
                String key = field.getKey();
                JsonElement value = field.getValue();
                if (ENTRIES.contains(key)) {
                    JsonObject target =
                            result.has(key) ? result.getAsJsonObject(key) : new JsonObject();
                    for (Map.Entry<String, JsonElement> entry :
                            value.getAsJsonObject().entrySet()) {
                        claim(claimed, fluid, key + "/" + entry.getKey(), source);
                        if (removes(entry.getValue())) {
                            if (target.remove(entry.getKey()) == null)
                                throw new IllegalStateException(
                                        fluid
                                                + ": "
                                                + source
                                                + " removes absent "
                                                + key
                                                + "/"
                                                + entry.getKey());
                        } else {
                            target.add(entry.getKey(), entry.getValue());
                        }
                    }
                    result.add(key, target);
                } else if (key.equals(FLAGS)) {
                    Set<String> flags = new LinkedHashSet<>(flags(result.get(key)));
                    for (Map.Entry<String, JsonElement> flag : value.getAsJsonObject().entrySet()) {
                        claim(claimed, fluid, key + "/" + flag.getKey(), source);
                        boolean add = flag.getValue().getAsBoolean();
                        if (add ? !flags.add(flag.getKey()) : !flags.remove(flag.getKey()))
                            throw new IllegalStateException(
                                    fluid
                                            + ": "
                                            + source
                                            + (add ? " adds present" : " removes absent")
                                            + " flag "
                                            + flag.getKey());
                    }
                    JsonArray array = new JsonArray();
                    flags.forEach(flag -> array.add(new JsonPrimitive(flag)));
                    result.add(key, array);
                } else {
                    claim(claimed, fluid, key, source);
                    if (removes(value)) {
                        if (result.remove(key) == null)
                            throw new IllegalStateException(
                                    fluid + ": " + source + " removes absent " + key);
                    } else {
                        result.add(key, value);
                    }
                }
            }
        }
        return result;
    }

    private static void claim(
            Map<String, Identifier> claimed, Identifier fluid, String key, Identifier source) {
        Identifier other = claimed.putIfAbsent(key, source);
        if (other != null)
            throw new IllegalStateException(
                    fluid + ": " + key + " is patched by both " + other + " and " + source);
    }

    private static boolean removes(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) return false;
        if (value.getAsBoolean())
            throw new IllegalStateException(
                    "a fluid property patch marks removal with false, not true");
        return true;
    }

    private static Set<String> flags(JsonElement element) {
        Set<String> flags = new LinkedHashSet<>();
        if (element != null)
            element.getAsJsonArray().forEach(flag -> flags.add(flag.getAsString()));
        return flags;
    }
}
