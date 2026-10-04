// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public final class FabricHbmConfig {
    public static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve(ConfigFiles.FABRIC_FILE);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String INDENT = "  ";

    public static final File FILE = PATH.toFile();
    public static volatile Map<ConfigEntry<?>, Object> values = defaults();
    public static volatile long stampedModified;
    public static volatile long stampedLength;

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

    public static Map<ConfigEntry<?>, Object> defaults() {
        Map<ConfigEntry<?>, Object> map = new HashMap<>();
        for (ConfigEntry<?> entry : ConfigSchema.ENTRIES) {
            map.put(entry, entry.defaultValue());
        }
        return map;
    }

    public static Object read(JsonObject section, ConfigEntry<?> entry) {
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

    public static JsonObject section(JsonObject root, String name) {
        return root.get(name) instanceof JsonObject o ? o : new JsonObject();
    }

    public static <T> void set(ConfigEntry<T> entry, T value) {
        Map<ConfigEntry<?>, Object> next = new HashMap<>(values);
        next.put(entry, value);
        values = next;
        save();
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

    public static void reload() {
        load();
        NuclearTech.deriveConfigFacades();
    }

    public static ContentConfig content() {
        return CONTENT;
    }

    public static RuntimeConfig runtime() {
        return RUNTIME;
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

    public static void save() {
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

    public static void fromJson(JsonElement json) {
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
            next.put(entry, read(section(domain, entry.section()), entry));
        }
        values = next;
    }

    public static String toJsonc() {
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

    public static JsonPrimitive literal(ConfigEntry<?> entry) {
        Object value = values.get(entry);
        return switch (entry.kind()) {
            case BOOL -> new JsonPrimitive((Boolean) value);
            case INT -> new JsonPrimitive((Integer) value);
            case DOUBLE -> new JsonPrimitive((Double) value);
            case STRING -> new JsonPrimitive((String) value);
        };
    }

    public static JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Domain domain : Domain.values()) {
            JsonObject owner = new JsonObject();
            for (ConfigEntry<?> entry : ConfigSchema.of(domain)) {
                JsonObject section = owner.getAsJsonObject(entry.section());
                if (section == null) {
                    section = new JsonObject();
                    owner.add(entry.section(), section);
                }
                if (!entry.comment().isEmpty()) {
                    section.addProperty("// " + entry.key(), entry.comment());
                }
                Object value = values.get(entry);
                switch (entry.kind()) {
                    case BOOL -> section.addProperty(entry.key(), (Boolean) value);
                    case INT -> section.addProperty(entry.key(), (Integer) value);
                    case DOUBLE -> section.addProperty(entry.key(), (Double) value);
                    case STRING -> section.addProperty(entry.key(), (String) value);
                }
            }
            root.add(ConfigFiles.fabricSection(domain), owner);
        }
        return root;
    }
}
