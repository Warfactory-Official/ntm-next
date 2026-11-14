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

package com.hbm.handler.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

final class AbilityCodecs {

    private AbilityCodecs() {}

    static <A extends BaseAbility> Codec<A> byId(A[] members, Function<A, String> id) {
        var named = new HashMap<String, A>(members.length * 2);
        for (var member : members) named.put(id.apply(member), member);
        var lookup = Map.copyOf(named);
        return Codec.STRING.comapFlatMap(
                name -> {
                    A member = lookup.get(name);
                    return member != null
                            ? DataResult.success(member)
                            : DataResult.error(() -> "Unknown ability id " + name);
                },
                id);
    }

    static <A extends BaseAbility> StreamCodec<ByteBuf, A> byIndex(A[] members) {
        return ByteBufCodecs.idMapper(
                index -> {
                    if (index < 0 || index >= members.length)
                        throw new DecoderException("Unknown ability index " + index);
                    return members[index];
                },
                BaseAbility::index);
    }
}
