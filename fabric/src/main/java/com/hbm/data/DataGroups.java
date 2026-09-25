// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class DataGroups {

    private DataGroups() {}

    public static Group group(List<Group> into, String legacyName) {
        Group group =
                new Group(
                        Library.id(
                                legacyName
                                        .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                                        .toLowerCase(Locale.ROOT)));
        into.add(group);
        return group;
    }

    public static void applyAll(RegistryAccess registries) {
        MachineData.applyDataPack(registries);
        ExplosionData.applyDataPack(registries);
        MobData.applyDataPack(registries);
        WorldData.applyDataPack(registries);
        ItemData.applyDataPack(registries);
        EnergyData.applyDataPack(registries);
        RadiationData.applyDataPack(registries);
    }

    public static <T extends DataValues> void apply(
            RegistryAccess registries, ResourceKey<Registry<T>> key, List<Group> groups) {
        Registry<T> registry = registries.lookupOrThrow(key);
        Set<Identifier> undeclared = new HashSet<>(registry.keySet());
        List<Map<String, Object>> resolved = new ArrayList<>(groups.size());
        for (Group group : groups) {
            undeclared.remove(group.id());
            T entry = registry.getValue(group.id());
            if (entry == null) {
                throw new IllegalStateException(
                        key.identifier() + " " + group.id() + " is missing");
            }
            resolved.add(group.resolve(entry));
        }
        if (!undeclared.isEmpty()) {
            throw new IllegalStateException(key.identifier() + " declares no group " + undeclared);
        }
        for (int i = 0; i < groups.size(); i++) groups.get(i).publish(resolved.get(i));
    }

    public static final class Group {

        private final Identifier id;
        private final List<Param<?>> params = new ArrayList<>();
        private final List<RandomRange> ranges = new ArrayList<>();

        private Group(Identifier id) {
            this.id = id;
        }

        public Identifier id() {
            return id;
        }

        public Map<String, Dynamic<?>> defaults() {
            Map<String, Dynamic<?>> values = new LinkedHashMap<>();
            for (Param<?> param : params) values.put(param.key, param.encodedDefault());
            return values;
        }

        Map<String, Object> resolve(DataValues entry) {
            Set<String> undeclared = new HashSet<>(entry.values().keySet());
            Map<String, Object> resolved = new LinkedHashMap<>();
            for (Param<?> param : params) {
                undeclared.remove(param.key);
                Dynamic<?> authored = entry.values().get(param.key);
                Object value =
                        authored == null
                                ? param.defaultValue
                                : authored.read(param.codec)
                                        .getOrThrow(
                                                error ->
                                                        new IllegalStateException(
                                                                id + " " + param.key + ": "
                                                                        + error));
                resolved.put(param.key, value);
            }
            if (!undeclared.isEmpty()) {
                throw new IllegalStateException(id + " declares no parameter " + undeclared);
            }
            for (RandomRange range : ranges) {
                long min = (Integer) resolved.get(range.minimum().key);
                long max = (Integer) resolved.get(range.maximum().key);
                if (max < min || max - min >= Integer.MAX_VALUE) {
                    throw new IllegalStateException(
                            id
                                    + " invalid inclusive random range: "
                                    + range.minimum().key
                                    + "="
                                    + min
                                    + ", "
                                    + range.maximum().key
                                    + "="
                                    + max);
                }
            }
            return resolved;
        }

        @SuppressWarnings("unchecked")
        void publish(Map<String, Object> resolved) {
            for (Param<?> param : params) ((Param<Object>) param).value = resolved.get(param.key);
        }

        public void randomRange(Param<Integer> minimum, Param<Integer> maximum) {
            ranges.add(new RandomRange(minimum, maximum));
        }

        public <T> Param<T> add(String key, T defaultValue, Codec<T> codec) {
            Param<T> param = new Param<>(key, defaultValue, codec);
            params.add(param);
            return param;
        }

        public Param<Integer> integer(String key, int defaultValue) {
            return add(key, defaultValue, DataCodecs.intRange(0, Integer.MAX_VALUE));
        }

        public Param<Integer> positive(String key, int defaultValue) {
            return add(key, defaultValue, DataCodecs.intRange(1, Integer.MAX_VALUE));
        }

        public Param<Integer> signed(String key, int defaultValue) {
            return add(key, defaultValue, DataCodecs.INT);
        }

        public Param<Long> longValue(String key, long defaultValue) {
            return add(
                    key,
                    defaultValue,
                    DataCodecs.LONG.validate(
                            value ->
                                    value >= 0
                                            ? DataResult.success(value)
                                            : DataResult.error(
                                                    () ->
                                                            "Expected nonnegative integer: "
                                                                    + value)));
        }

        public Param<Double> real(String key, double defaultValue) {
            return add(key, defaultValue, finiteRange(0D, Double.MAX_VALUE));
        }

        public Param<Integer> bounded(String key, int defaultValue, int minimum, int maximum) {
            return add(key, defaultValue, DataCodecs.intRange(minimum, maximum));
        }

        public Param<Double> bounded(
                String key, double defaultValue, double minimum, double maximum) {
            return add(key, defaultValue, finiteRange(minimum, maximum));
        }

        public Param<String> text(String key, String defaultValue, String... allowed) {
            Set<String> valid = Set.of(allowed);
            return add(
                    key,
                    defaultValue,
                    Codec.STRING.validate(
                            value ->
                                    valid.contains(value)
                                            ? DataResult.success(value)
                                            : DataResult.error(
                                                    () ->
                                                            "Expected one of "
                                                                    + valid
                                                                    + ": "
                                                                    + value)));
        }

        public Param<Double> atLeast(String key, double defaultValue, double minimum) {
            return add(key, defaultValue, finiteRange(minimum, Double.MAX_VALUE));
        }

        public Param<Double> chance(String key, double defaultValue) {
            return add(key, defaultValue, finiteRange(0D, 1D));
        }

        public Param<Boolean> bool(String key, boolean defaultValue) {
            return add(key, defaultValue, Codec.BOOL);
        }

        public Param<List<Double>> fuelGrades(String key, List<Double> defaultValue) {
            FuelGrade[] grades = FuelGrade.values();
            assert grades.length == defaultValue.size();
            Codec<List<Double>> codec =
                    Codec.unboundedMap(Codec.STRING, finiteRange(0D, Double.MAX_VALUE))
                            .comapFlatMap(
                                    values -> {
                                        Set<String> unknown = new HashSet<>(values.keySet());
                                        List<Double> result = new ArrayList<>(grades.length);
                                        for (FuelGrade grade : grades) {
                                            unknown.remove(grade.name());
                                            result.add(
                                                    values.getOrDefault(
                                                            grade.name(),
                                                            defaultValue.get(grade.ordinal())));
                                        }
                                        return unknown.isEmpty()
                                                ? DataResult.success(List.copyOf(result))
                                                : DataResult.error(
                                                        () -> "Unknown fuel grades: " + unknown);
                                    },
                                    values -> {
                                        Map<String, Double> result = new LinkedHashMap<>();
                                        for (FuelGrade grade : grades)
                                            result.put(grade.name(), values.get(grade.ordinal()));
                                        return result;
                                    });
            return add(key, defaultValue, codec);
        }
    }

    record RandomRange(Param<Integer> minimum, Param<Integer> maximum) {}

    static Codec<Double> finiteRange(double min, double max) {
        return Codec.DOUBLE.validate(
                value ->
                        Double.isFinite(value) && value >= min && value <= max
                                ? DataResult.success(value)
                                : DataResult.error(
                                        () ->
                                                "Expected finite number in ["
                                                        + min
                                                        + ", "
                                                        + max
                                                        + "]: "
                                                        + value));
    }

    public static final class Param<T> {

        private final String key;
        private final T defaultValue;
        private final Codec<T> codec;
        private volatile T value;

        private Param(String key, T defaultValue, Codec<T> codec) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.codec = codec;
            this.value = defaultValue;
        }

        private Dynamic<?> encodedDefault() {
            return new Dynamic<>(
                    JsonOps.INSTANCE,
                    codec.encodeStart(JsonOps.INSTANCE, defaultValue).getOrThrow());
        }

        public T get() {
            return value;
        }

        public String key() {
            return key;
        }

        public T defaultValue() {
            return defaultValue;
        }
    }
}
