// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;

public record DataGroup(Map<String, Dynamic<?>> values) implements DataValues {

    public static final Codec<DataGroup> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
                    .xmap(DataGroup::new, DataGroup::values);

    public DataGroup {
        values = Map.copyOf(values);
    }

    public static DataGroup of(Map<String, ?> values) {
        Map<String, Dynamic<?>> encoded = new LinkedHashMap<>();
        values.forEach(
                (key, value) ->
                        encoded.put(
                                key,
                                new Dynamic<>(
                                        JsonOps.INSTANCE,
                                        JavaOps.INSTANCE.convertTo(JsonOps.INSTANCE, value))));
        return new DataGroup(encoded);
    }
}
