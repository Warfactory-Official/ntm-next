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

package com.hbm.registration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import org.jspecify.annotations.Nullable;

public final class MapColors {
    public static final int FORMAT = 1;

    private final String namespace;
    private final boolean generating = DatagenModLoader.isRunningDataGen();
    private final Map<String, Profile> profiles;
    private final Map<String, BlockBehaviour.Properties> declared = new LinkedHashMap<>();
    private final Set<String> consumed = new HashSet<>();

    public MapColors(Class<?> anchor, String namespace) {
        this.namespace = namespace;
        profiles = generating ? new HashMap<>() : load(anchor, namespace);
    }

    public static String resource(String namespace) {
        return "/" + namespace + "/map_colors.nbt";
    }

    public BlockBehaviour.Properties apply(String name, BlockBehaviour.Properties properties) {
        if (generating) {
            if (declared.putIfAbsent(name, properties) != null)
                throw new IllegalStateException(
                        "duplicate map-color properties: " + namespace + ":" + name);
            return properties;
        }
        Profile profile = profiles.get(name);
        if (profile == null)
            throw new IllegalStateException("missing baked map color: " + namespace + ":" + name);
        consumed.add(name);
        return profile.apply(properties);
    }

    public MapColor constant(String name, MapColor sourceMaterial) {
        Profile profile = profiles.get(name);
        if (profile == null && generating) return sourceMaterial;
        if (profile == null || profile.constant() == null)
            throw new IllegalStateException(
                    "map-color owner is not constant: " + namespace + ":" + name);
        return profile.constant();
    }

    public Set<String> declaredNames() {
        return Set.copyOf(declared.keySet());
    }

    public void install(String name, Profile profile) {
        if (!generating)
            throw new IllegalStateException("cannot install sampled colors outside datagen");
        profile.apply(declared.get(name));
        profiles.put(name, profile);
    }

    public void finishRegistration() {
        if (generating) return;
        Set<String> unused = new HashSet<>(profiles.keySet());
        unused.removeAll(consumed);
        unused.removeIf(
                name ->
                        Shared.isOtherCopy(
                                Registries.BLOCK,
                                Identifier.fromNamespaceAndPath(namespace, name)));
        if (!unused.isEmpty())
            throw new IllegalStateException(
                    "unused baked map colors in " + namespace + ": " + unused);
        profiles.clear();
        consumed.clear();
    }

    private static Map<String, Profile> load(Class<?> anchor, String namespace) {
        String resource = resource(namespace);
        try (InputStream input = anchor.getResourceAsStream(resource)) {
            if (input == null)
                throw new IllegalStateException(
                        "missing baked map colors " + resource + "; run datagen");
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            if (root.getInt("format").orElseThrow() != FORMAT)
                throw new IllegalStateException("unsupported map-color data format in " + resource);
            CompoundTag entries = root.getCompound("profiles").orElseThrow();
            Map<String, Profile> profiles = new HashMap<>();
            for (String name : entries.keySet())
                profiles.put(name, Profile.read(entries.getCompound(name).orElseThrow()));
            return profiles;
        } catch (IOException error) {
            throw new UncheckedIOException("reading baked map colors " + resource, error);
        }
    }

    public record Profile(
            @Nullable MapColor constant,
            List<String> properties,
            Map<String, Integer> states,
            boolean passthrough) {
        public static Profile preserve() {
            return new Profile(null, List.of(), Map.of(), true);
        }

        public static Profile constant(MapColor color) {
            return new Profile(color, List.of(), Map.of(), false);
        }

        public static Profile states(List<String> properties, Map<String, Integer> states) {
            return new Profile(null, List.copyOf(properties), Map.copyOf(states), false);
        }

        public BlockBehaviour.Properties apply(BlockBehaviour.Properties target) {
            if (passthrough) return target;
            return constant != null ? target.mapColor(constant) : target.mapColor(this::color);
        }

        public MapColor color(BlockState state) {
            if (constant != null) return constant;
            String key = key(state, properties);
            Integer color = states.get(key);
            if (color == null)
                throw new IllegalStateException(
                        "missing baked map color for " + state + " [" + key + "]");
            return MapColor.byId(color);
        }

        public static String key(BlockState state, List<String> properties) {
            StringBuilder key = new StringBuilder();
            for (String name : properties) {
                if (!key.isEmpty()) key.append(',');
                boolean found = false;
                for (Property<?> property : state.getProperties()) {
                    if (!property.getName().equals(name)) continue;
                    key.append(valueName(state, property));
                    found = true;
                    break;
                }
                if (!found)
                    throw new IllegalStateException(
                            "map-color property " + name + " is absent from " + state);
            }
            return key.toString();
        }

        private static <T extends Comparable<T>> String valueName(
                BlockState state, Property<T> property) {
            return property.getName(state.getValue(property));
        }

        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            if (passthrough) {
                tag.putBoolean("preserve", true);
            } else if (constant != null) {
                tag.putByte("constant", (byte) constant.id);
            } else {
                ListTag names = new ListTag();
                properties.forEach(name -> names.add(StringTag.valueOf(name)));
                tag.put("properties", names);
                CompoundTag values = new CompoundTag();
                states.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(
                                entry ->
                                        values.putByte(
                                                entry.getKey(), entry.getValue().byteValue()));
                tag.put("states", values);
            }
            return tag;
        }

        public static Profile read(CompoundTag tag) {
            if (tag.getBoolean("preserve").orElse(false)) return preserve();
            if (tag.contains("constant"))
                return constant(palette(tag.getByte("constant").orElseThrow()));
            List<String> properties = new ArrayList<>();
            for (var name : tag.getList("properties").orElseThrow())
                properties.add(name.asString().orElseThrow());
            Map<String, Integer> states = new HashMap<>();
            CompoundTag values = tag.getCompound("states").orElseThrow();
            for (String key : values.keySet())
                states.put(key, palette(values.getByte(key).orElseThrow()).id);
            return states(properties, states);
        }

        private static MapColor palette(int id) {
            if (id < 0 || id > MapColor.GLOW_LICHEN.id)
                throw new IllegalStateException("invalid baked map-color id " + id);
            return MapColor.byId(id);
        }
    }
}
