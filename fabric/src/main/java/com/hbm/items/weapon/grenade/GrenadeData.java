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

package com.hbm.items.weapon.grenade;

import com.hbm.items.weapon.grenade.GrenadeFillingItem;
import com.hbm.items.weapon.grenade.GrenadeShellItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public record GrenadeData(
        GrenadeShellItem.EnumGrenadeShell shell,
        GrenadeFillingDefinition filling,
        GrenadeFuzeItem.EnumGrenadeFuze fuze,
        GrenadeExtraItem.@Nullable EnumGrenadeExtra extra) {

    public static final GrenadeData DEFAULT =
            new GrenadeData(
                    GrenadeShellItem.EnumGrenadeShell.FRAG,
                    GrenadeFillingItem.EnumGrenadeFilling.HE,
                    GrenadeFuzeItem.EnumGrenadeFuze.S3,
                    null);
    private static final Codec<GrenadeShellItem.EnumGrenadeShell> SHELL =
            Codec.STRING.xmap(
                    value ->
                            GrenadeShellItem.EnumGrenadeShell.valueOf(
                                    value.toUpperCase(Locale.ROOT)),
                    value -> value.name().toLowerCase(Locale.ROOT));
    private static final Codec<GrenadeFillingDefinition> FILLING = GrenadeFillings.CODEC;
    private static final Codec<GrenadeFuzeItem.EnumGrenadeFuze> FUZE =
            Codec.STRING.xmap(
                    value ->
                            GrenadeFuzeItem.EnumGrenadeFuze.valueOf(value.toUpperCase(Locale.ROOT)),
                    value -> value.name().toLowerCase(Locale.ROOT));
    private static final Codec<GrenadeExtraItem.EnumGrenadeExtra> EXTRA =
            Codec.STRING.xmap(
                    value ->
                            GrenadeExtraItem.EnumGrenadeExtra.valueOf(
                                    value.toUpperCase(Locale.ROOT)),
                    value -> value.name().toLowerCase(Locale.ROOT));

    public static final Codec<GrenadeData> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            SHELL.fieldOf("shell").forGetter(GrenadeData::shell),
                                            FILLING.fieldOf("filling")
                                                    .forGetter(GrenadeData::filling),
                                            FUZE.fieldOf("fuze").forGetter(GrenadeData::fuze),
                                            EXTRA.optionalFieldOf("extra")
                                                    .forGetter(
                                                            data ->
                                                                    Optional.ofNullable(
                                                                            data.extra())))
                                    .apply(
                                            instance,
                                            (shell, filling, fuze, extra) ->
                                                    new GrenadeData(
                                                            shell,
                                                            filling,
                                                            fuze,
                                                            extra.orElse(null))));
    public static final StreamCodec<ByteBuf, GrenadeData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    data -> data.shell().ordinal(),
                    GrenadeFillings.STREAM_CODEC,
                    GrenadeData::filling,
                    ByteBufCodecs.VAR_INT,
                    data -> data.fuze().ordinal(),
                    ByteBufCodecs.VAR_INT,
                    data -> data.extra() == null ? -1 : data.extra().ordinal(),
                    (shell, filling, fuze, extra) ->
                            new GrenadeData(
                                    GrenadeShellItem.EnumGrenadeShell.VALUES[shell],
                                    filling,
                                    GrenadeFuzeItem.EnumGrenadeFuze.VALUES[fuze],
                                    extra == -1
                                            ? null
                                            : GrenadeExtraItem.EnumGrenadeExtra.VALUES[extra]));
}
