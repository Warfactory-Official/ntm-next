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

package com.hbm.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.hbm.NuclearTech;
import com.hbm.config.ConfigEntry.Domain;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

public class HbmConfig {
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve(ConfigFiles.FABRIC_FILE);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String INDENT = "  ";

    private static final File FILE = PATH.toFile();
    public static volatile Map<ConfigEntry<?>, Object> values = defaults();
    private static volatile long stampedModified;
    private static volatile long stampedLength;

    public static final ConfigStore STORE =
            new ConfigStore() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(ConfigEntry<T> entry) {
                    return (T) values.getOrDefault(entry, entry.defaultValue());
                }
            };

    public static final ConfigStore.Content CONTENT = new ConfigStore.Content(STORE);
    public static final ConfigStore.Runtime RUNTIME = new ConfigStore.Runtime(STORE);

    public static <T> void set(ConfigEntry<T> entry, T value) {
        Map<ConfigEntry<?>, Object> next = new HashMap<>(values);
        next.put(entry, value);
        values = next;
        save();
        NuclearTech.deriveConfigFacades();
    }

    public static void reload() {
        load();
        NuclearTech.deriveConfigFacades();
    }

    public static boolean pollForExternalEdit() {
        long modified;
        long length;
        try {
            BasicFileAttributes attributes = Files.readAttributes(PATH, BasicFileAttributes.class);
            modified = attributes.lastModifiedTime().toMillis();
            length = attributes.size();
        } catch (IOException absent) {
            return false;
        }
        if (modified == stampedModified && length == stampedLength) return false;
        load();
        return true;
    }

    private static Map<ConfigEntry<?>, Object> defaults() {
        Map<ConfigEntry<?>, Object> map = new HashMap<>();
        for (ConfigEntry<?> entry : ConfigSchema.ENTRIES) {
            map.put(entry, entry.defaultValue());
        }
        return map;
    }

    private static Object read(JsonObject section, ConfigEntry<?> entry) {
        JsonElement e = section.get(entry.key());
        switch (entry.kind()) {
            case BOOL -> {
                if (e instanceof JsonPrimitive p && p.isBoolean()) return p.getAsBoolean();
            }
            case INT -> {
                if (e instanceof JsonPrimitive p && p.isNumber()) {
                    return Mth.clamp(p.getAsInt(), entry.intMin(), entry.intMax());
                }
            }
            case DOUBLE -> {
                if (e instanceof JsonPrimitive p && p.isNumber()) {
                    return Mth.clamp(p.getAsDouble(), entry.min(), entry.max());
                }
            }
            case STRING -> {
                if (e instanceof JsonPrimitive p && p.isString()) return p.getAsString();
            }
        }
        if (e != null) {
            NuclearTech.LOGGER.warn(
                    "'{}' value must be {}",
                    entry.key(),
                    entry.kind().name().toLowerCase(Locale.ROOT));
        }
        return entry.defaultValue();
    }

    public static void load() {
        if (FILE.exists()) {
            try (JsonReader reader = new JsonReader(new FileReader(FILE))) {
                reader.setStrictness(Strictness.LENIENT);
                fromJson(JsonParser.parseReader(reader));
            } catch (Exception e) {
                NuclearTech.LOGGER.warn(
                        "Could not load config from '{}'", FILE.getAbsolutePath(), e);
            }
        }
        save();
    }

    private static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            writer.write(toJsonc());
        } catch (Exception e) {
            NuclearTech.LOGGER.warn("Could not save config to '{}'", FILE.getAbsolutePath(), e);
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(PATH, BasicFileAttributes.class);
            stampedModified = attributes.lastModifiedTime().toMillis();
            stampedLength = attributes.size();
        } catch (IOException e) {
            stampedModified = 0L;
            stampedLength = -1L;
        }
    }

    private static void fromJson(JsonElement json) {
        if (!(json instanceof JsonObject root)) {
            NuclearTech.LOGGER.warn("Config JSON must be an object");
            values = defaults();
            return;
        }
        Map<ConfigEntry<?>, Object> next = new HashMap<>();
        for (ConfigEntry<?> entry : ConfigSchema.ENTRIES) {
            JsonObject domain =
                    root.get(ConfigFiles.fabricSection(entry.domain())) instanceof JsonObject o
                            ? o
                            : new JsonObject();
            next.put(
                    entry,
                    read(
                            domain.get(entry.section()) instanceof JsonObject o2
                                    ? o2
                                    : new JsonObject(),
                            entry));
        }
        values = next;
    }

    private static String toJsonc() {
        StringBuilder out = new StringBuilder("{\n");
        Domain[] domains = Domain.values();
        for (int d = 0; d < domains.length; d++) {
            Domain domain = domains[d];
            out.append(INDENT)
                    .append('"')
                    .append(ConfigFiles.fabricSection(domain))
                    .append("\": {\n");
            List<String> sections = new ArrayList<>();
            Map<String, List<ConfigEntry<?>>> bySection = new LinkedHashMap<>();
            for (ConfigEntry<?> entry : ConfigSchema.of(domain)) {
                bySection
                        .computeIfAbsent(
                                entry.section(),
                                key -> {
                                    sections.add(key);
                                    return new ArrayList<>();
                                })
                        .add(entry);
            }
            for (int s = 0; s < sections.size(); s++) {
                String section = sections.get(s);
                out.append(INDENT.repeat(2)).append('"').append(section).append("\": {\n");
                List<ConfigEntry<?>> entries = bySection.get(section);
                for (int e = 0; e < entries.size(); e++) {
                    ConfigEntry<?> entry = entries.get(e);
                    for (String line : entry.comment().split("\n")) {
                        if (!line.isBlank()) {
                            out.append(INDENT.repeat(3)).append("// ").append(line).append('\n');
                        }
                    }
                    out.append(INDENT.repeat(3))
                            .append('"')
                            .append(entry.key())
                            .append("\": ")
                            .append(GSON.toJson(literal(entry)))
                            .append(e + 1 < entries.size() ? ",\n" : "\n");
                }
                out.append(INDENT.repeat(2)).append(s + 1 < sections.size() ? "},\n" : "}\n");
            }
            out.append(INDENT).append(d + 1 < domains.length ? "},\n" : "}\n");
        }
        return out.append("}\n").toString();
    }

    private static JsonPrimitive literal(ConfigEntry<?> entry) {
        Object value = values.get(entry);
        return switch (entry.kind()) {
            case BOOL -> new JsonPrimitive((Boolean) value);
            case INT -> new JsonPrimitive((Integer) value);
            case DOUBLE -> new JsonPrimitive((Double) value);
            case STRING -> new JsonPrimitive((String) value);
        };
    }
}
