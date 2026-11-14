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

package com.hbm.util;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DataCodecs {
    public static final Codec<Integer> INT = exactInteger(BigDecimal::intValueExact, "32-bit");
    public static final Codec<Long> LONG = exactInteger(BigDecimal::longValueExact, "64-bit");

    private DataCodecs() {}

    private static <N extends Number> Codec<N> exactInteger(
            Function<BigDecimal, N> convert, String width) {
        return Codec.PASSTHROUGH.comapFlatMap(
                value ->
                        value.asNumber()
                                .flatMap(
                                        number -> {
                                            try {
                                                return DataResult.success(
                                                        convert.apply(
                                                                new BigDecimal(number.toString())));
                                            } catch (ArithmeticException
                                                    | NumberFormatException error) {
                                                return DataResult.error(
                                                        () ->
                                                                "Expected an exact "
                                                                        + width
                                                                        + " integer: "
                                                                        + number);
                                            }
                                        }),
                value -> new Dynamic<>(JavaOps.INSTANCE, value));
    }

    public static Codec<Integer> intRange(int minimum, int maximum) {
        return INT.validate(
                value ->
                        value >= minimum && value <= maximum
                                ? DataResult.success(value)
                                : DataResult.error(
                                        () ->
                                                "Expected integer in ["
                                                        + minimum
                                                        + ", "
                                                        + maximum
                                                        + "]: "
                                                        + value));
    }

    public static <A> MapCodec<A> strict(MapCodec<A> codec, String... envelopeFields) {
        Set<String> fields =
                codec.keys(JsonOps.INSTANCE)
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toCollection(HashSet::new));
        fields.addAll(List.of(envelopeFields));
        return new MapCodec<>() {
            @Override
            public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                List<String> unknown =
                        input.entries()
                                .map(pair -> ops.getStringValue(pair.getFirst()).getOrThrow())
                                .filter(name -> !fields.contains(name))
                                .sorted()
                                .toList();
                if (!unknown.isEmpty()) return DataResult.error(() -> "Unknown fields: " + unknown);
                return codec.decode(ops, input);
            }

            @Override
            public <T> RecordBuilder<T> encode(
                    A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return codec.encode(input, ops, prefix);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return codec.keys(ops);
            }
        };
    }

    public static <A> MapCodec<A> recipe(MapCodec<A> codec) {
        return strict(
                codec,
                "type",
                "fabric:load_conditions",
                "neoforge:conditions",
                "_kubejs_changed_marker");
    }

    public static <A> Codec<List<A>> listOrSingle(Codec<A> element) {
        return Codec.either(element, element.listOf())
                .xmap(
                        value -> value.map(List::of, list -> list),
                        list ->
                                list.size() == 1
                                        ? Either.left(list.getFirst())
                                        : Either.right(list));
    }
}
