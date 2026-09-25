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

public final class FabricHbmConfig implements HbmConfig {

    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve(ConfigFiles.FABRIC_FILE);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String INDENT = "  ";

    private final File file = PATH.toFile();

    private volatile Map<ConfigEntry<?>, Object> values = defaults();
    private volatile long stampedModified;
    private volatile long stampedLength;

    private final ConfigStore store =
            new ConfigStore() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(ConfigEntry<T> entry) {
                    return (T) values.getOrDefault(entry, entry.defaultValue());
                }
            };

    private final ConfigStore.Content content = new ConfigStore.Content(store);
    private final ConfigStore.Runtime runtime = new ConfigStore.Runtime(store);

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

    private static String sectionName(Domain domain) {
        return ConfigFiles.fabricSection(domain);
    }

    private static JsonObject section(JsonObject root, String name) {
        return root.get(name) instanceof JsonObject o ? o : new JsonObject();
    }

    @Override
    public <T> void set(ConfigEntry<T> entry, T value) {
        Map<ConfigEntry<?>, Object> next = new HashMap<>(values);
        next.put(entry, value);
        values = next;
        save();
        NuclearTech.deriveConfigFacades();
    }

    @Override
    public boolean pollForExternalEdit() {

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

    @Override
    public void reload() {
        load();
        NuclearTech.deriveConfigFacades();
    }

    @Override
    public ContentConfig content() {
        return content;
    }

    @Override
    public RuntimeConfig runtime() {
        return runtime;
    }

    @Override
    public ConfigStore clientStore() {
        return store;
    }

    public void load() {
        if (file.exists()) {
            try (JsonReader reader = new JsonReader(new FileReader(file))) {

                reader.setStrictness(Strictness.LENIENT);
                fromJson(JsonParser.parseReader(reader));
            } catch (Exception e) {
                NuclearTech.LOGGER.warn(
                        "Could not load config from '{}'", file.getAbsolutePath(), e);
            }
        }
        save();
    }

    private void save() {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(toJsonc());
        } catch (Exception e) {
            NuclearTech.LOGGER.warn("Could not save config to '{}'", file.getAbsolutePath(), e);
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

    private void fromJson(JsonElement json) {
        if (!(json instanceof JsonObject root)) {
            NuclearTech.LOGGER.warn("Config JSON must be an object");
            values = defaults();
            return;
        }
        Map<ConfigEntry<?>, Object> next = new HashMap<>();
        for (ConfigEntry<?> entry : ConfigSchema.ENTRIES) {
            JsonObject domain = section(root, sectionName(entry.domain()));
            next.put(entry, read(section(domain, entry.section()), entry));
        }
        values = next;
    }

    private String toJsonc() {
        StringBuilder out = new StringBuilder("{\n");
        Domain[] domains = Domain.values();
        for (int d = 0; d < domains.length; d++) {
            Domain domain = domains[d];
            out.append(INDENT).append('"').append(sectionName(domain)).append("\": {\n");
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

    private JsonPrimitive literal(ConfigEntry<?> entry) {
        Object value = values.get(entry);
        return switch (entry.kind()) {
            case BOOL -> new JsonPrimitive((Boolean) value);
            case INT -> new JsonPrimitive((Integer) value);
            case DOUBLE -> new JsonPrimitive((Double) value);
            case STRING -> new JsonPrimitive((String) value);
        };
    }

    private JsonObject toJson() {
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
            root.add(sectionName(domain), owner);
        }
        return root;
    }
}
