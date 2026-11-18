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

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public sealed interface BlockSelector {

    boolean matches(Holder.Reference<Block> block);

    static boolean valid(String text) {
        return tryParse(text) != null;
    }

    static BlockSelector parse(String text) {
        var selector = tryParse(text);
        if (selector == null)
            throw new IllegalArgumentException("Invalid block selector '" + text + "'");
        return selector;
    }

    private static @Nullable BlockSelector tryParse(String text) {
        boolean tag = text.startsWith("#");
        var body = tag ? text.substring(1) : text;
        int colon = body.indexOf(':');
        if (colon <= 0 || colon == body.length() - 1) return null;
        var namespace = body.substring(0, colon);
        var path = body.substring(colon + 1);
        if (!Identifier.isValidNamespace(namespace)) return null;
        if (!tag && path.equals("*")) return new Namespace(namespace);
        if (!Identifier.isValidPath(path)) return null;
        var id = Identifier.fromNamespaceAndPath(namespace, path);
        return tag ? new Tag(TagKey.create(Registries.BLOCK, id)) : new Exact(id);
    }

    record Exact(Identifier id) implements BlockSelector {
        @Override
        public boolean matches(Holder.Reference<Block> block) {
            return block.is(id);
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }

    record Namespace(String namespace) implements BlockSelector {
        @Override
        public boolean matches(Holder.Reference<Block> block) {
            return block.key().identifier().getNamespace().equals(namespace);
        }

        @Override
        public String toString() {
            return namespace + ":*";
        }
    }

    record Tag(TagKey<Block> tag) implements BlockSelector {
        @Override
        public boolean matches(Holder.Reference<Block> block) {
            return block.is(tag);
        }

        @Override
        public String toString() {
            return "#" + tag.location();
        }
    }
}
