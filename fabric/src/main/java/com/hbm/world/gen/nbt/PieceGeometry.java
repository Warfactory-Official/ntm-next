// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import java.util.Map;

@FunctionalInterface
public interface PieceGeometry {

    void emit(GeometryOut out, Variant variant);

    record Variant(String suffix, Map<String, Integer> values) {
        public static final Variant SINGLE = new Variant("", Map.of());

        public int number(String key) {
            return values.getOrDefault(key, 0);
        }
    }
}
