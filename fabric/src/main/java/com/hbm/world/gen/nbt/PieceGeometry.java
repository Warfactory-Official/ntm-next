// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface PieceGeometry {

    void emit(GeometryOut out, Variant variant);

    record Variant(String suffix, Map<String, Integer> values) {

        public static final Variant SINGLE = new Variant("", Map.of());

        public boolean flag(String key) {
            return values.getOrDefault(key, 0) != 0;
        }

        public int number(String key) {
            return values.getOrDefault(key, 0);
        }

        public static List<Variant> flags(String... keys) {
            List<Variant> out = new ArrayList<>(1 << keys.length);
            for (int mask = 0; mask < (1 << keys.length); mask++) {
                Map<String, Integer> values = new LinkedHashMap<>();
                StringBuilder suffix = new StringBuilder();
                for (int i = 0; i < keys.length; i++) {
                    int bit = (mask >> i) & 1;
                    values.put(keys[i], bit);
                    suffix.append('_').append(bit == 1 ? keys[i] : "no_" + keys[i]);
                }
                out.add(new Variant(suffix.toString(), Map.copyOf(values)));
            }
            return List.copyOf(out);
        }
    }
}
