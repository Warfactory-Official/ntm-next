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

package com.hbm.handler;

import com.hbm.items.weapon.CustomMissilePartItem.PartType;
import com.hbm.items.weapon.CustomMissilePartItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record MissileStruct(
        @Nullable CustomMissilePartItem warhead,
        @Nullable CustomMissilePartItem fuselage,
        @Nullable CustomMissilePartItem fins,
        @Nullable CustomMissilePartItem thruster) {

    private static final Codec<CustomMissilePartItem> PART =
            BuiltInRegistries.ITEM
                    .byNameCodec()
                    .comapFlatMap(
                            item ->
                                    item instanceof CustomMissilePartItem part
                                            ? DataResult.success(part)
                                            : DataResult.error(() -> "not a missile part: " + item),
                            part -> part);

    public static final Codec<MissileStruct> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            PART.optionalFieldOf("warhead")
                                                    .forGetter(
                                                            s -> Optional.ofNullable(s.warhead())),
                                            PART.optionalFieldOf("fuselage")
                                                    .forGetter(
                                                            s -> Optional.ofNullable(s.fuselage())),
                                            PART.optionalFieldOf("fins")
                                                    .forGetter(s -> Optional.ofNullable(s.fins())),
                                            PART.optionalFieldOf("thruster")
                                                    .forGetter(
                                                            s -> Optional.ofNullable(s.thruster())))
                                    .apply(
                                            instance,
                                            (w, f, s, t) ->
                                                    new MissileStruct(
                                                            w.orElse(null),
                                                            f.orElse(null),
                                                            s.orElse(null),
                                                            t.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, MissileStruct> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.warhead()),
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.fuselage()),
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.fins()),
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.thruster()),
                    (w, f, s, t) -> new MissileStruct(cast(w), cast(f), cast(s), cast(t)));

    public static final MissileStruct EMPTY =
            new MissileStruct((CustomMissilePartItem) null, null, null, null);

    public MissileStruct(ItemStack w, ItemStack f, ItemStack s, ItemStack t) {
        this(
                CustomMissilePartItem.part(w),
                CustomMissilePartItem.part(f),
                CustomMissilePartItem.part(s),
                CustomMissilePartItem.part(t));
    }

    private static @Nullable CustomMissilePartItem cast(Optional<? extends Item> item) {
        return item.orElse(null) instanceof CustomMissilePartItem part ? part : null;
    }

    public MissileStruct sanitised() {
        return new MissileStruct(
                slot(this.warhead, PartType.WARHEAD),
                slot(this.fuselage, PartType.FUSELAGE),
                slot(this.fins, PartType.FINS),
                slot(this.thruster, PartType.THRUSTER));
    }

    private static @Nullable CustomMissilePartItem slot(
            @Nullable CustomMissilePartItem part, PartType expected) {
        return part != null && part.type == expected ? part : null;
    }

    public float height() {
        float height = 0F;
        if (this.warhead != null && this.warhead.type == PartType.WARHEAD)
            height += this.warhead.height;
        if (this.fuselage != null && this.fuselage.type == PartType.FUSELAGE)
            height += this.fuselage.height;
        if (this.thruster != null && this.thruster.type == PartType.THRUSTER)
            height += this.thruster.height;
        return height;
    }

    public boolean isComplete() {
        return this.warhead != null && this.fuselage != null && this.thruster != null;
    }
}
