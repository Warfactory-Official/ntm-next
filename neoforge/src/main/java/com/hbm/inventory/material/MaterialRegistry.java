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

package com.hbm.inventory.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class MaterialRegistry {

    private static Material[] byId = new Material[0];
    private static final Map<String, Material> BY_NAME = new HashMap<>();
    private static final List<Material> ORDER = new ArrayList<>();
    private static final List<Material> ORDERED = Collections.unmodifiableList(ORDER);
    private static final Map<TagKey<Item>, ShapeBinding> BINDINGS = new HashMap<>();

    public static final Codec<Material> CODEC =
            Codec.STRING.comapFlatMap(
                    name -> {
                        Material material = BY_NAME.get(name);
                        return material != null
                                ? DataResult.success(material)
                                : DataResult.error(() -> "Unknown NTM material: " + name);
                    },
                    Material::name);

    public static final StreamCodec<ByteBuf, Material> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    name -> {
                        Material material = BY_NAME.get(name);
                        if (material == null)
                            throw new IllegalArgumentException("Unknown NTM material: " + name);
                        return material;
                    },
                    Material::name);

    static {
        Materials.load();
    }

    private MaterialRegistry() {}

    public static @Nullable Material byId(int id) {
        Material[] table = byId;
        return id >= 0 && id < table.length ? table[id] : null;
    }

    public static @Nullable Material byName(String name) {
        return BY_NAME.get(name);
    }

    public static List<Material> ordered() {
        return ORDERED;
    }

    public static @Nullable ShapeBinding binding(TagKey<Item> tag) {
        return BINDINGS.get(tag);
    }

    public static List<ShapeBinding> bindings() {
        List<ShapeBinding> all = new ArrayList<>(ORDER.size() * MaterialShape.ALL.size());
        for (Material material : ORDER) {
            for (MaterialShape shape : MaterialShape.ALL) {
                if (shape.hasTags() && shape.quanta() > 0)
                    all.add(BINDINGS.get(material.tag(shape)));
            }
        }
        return all;
    }

    static void add(Material material, @Nullable Material anchor) {
        int id = material.id();
        int at = ORDER.size();
        if (anchor != null) {
            at = ORDER.indexOf(anchor) + 1;
        }
        if (id >= byId.length) byId = Arrays.copyOf(byId, Math.max(id + 1, byId.length * 2));
        byId[id] = material;
        BY_NAME.put(material.name(), material);
        ORDER.add(at, material);
        for (MaterialShape shape : MaterialShape.ALL) {
            if (shape.hasTags() && shape.quanta() > 0) {
                BINDINGS.put(material.tag(shape), new ShapeBinding(material, shape));
            }
        }
    }

    public record ShapeBinding(Material material, MaterialShape shape) {
        public int quanta() {
            return shape.quanta();
        }
    }
}
