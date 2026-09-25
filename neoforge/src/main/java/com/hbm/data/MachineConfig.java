// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;

public record MachineConfig(Map<String, Dynamic<?>> values) implements DataValues {

    public static final Codec<MachineConfig> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
                    .xmap(MachineConfig::new, MachineConfig::values);

    public MachineConfig {
        values = Map.copyOf(values);
    }

    public static MachineConfig of(Map<String, ?> values) {
        Map<String, Dynamic<?>> encoded = new LinkedHashMap<>();
        values.forEach(
                (key, value) ->
                        encoded.put(
                                key,
                                new Dynamic<>(
                                        JsonOps.INSTANCE,
                                        JavaOps.INSTANCE.convertTo(JsonOps.INSTANCE, value))));
        return new MachineConfig(encoded);
    }
}
