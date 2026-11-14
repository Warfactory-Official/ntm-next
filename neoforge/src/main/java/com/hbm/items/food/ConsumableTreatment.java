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

package com.hbm.items.food;

import com.hbm.util.DataCodecs;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

public record ConsumableTreatment(
        HolderSet<Item> targets,
        Trigger trigger,
        boolean blockWhenPotionSick,
        Optional<Holder<Item>> remainder,
        Optional<Holder<SoundEvent>> sound,
        List<TreatmentAction> actions) {
    public static final ResourceKey<Registry<ConsumableTreatment>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("consumable_treatment"));
    public static final Codec<ConsumableTreatment> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<ConsumableTreatment>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.<Item>holderSet(
                                                                            (ResourceKey<
                                                                                            ? extends
                                                                                                    Registry<
                                                                                                            Item>>)
                                                                                    Registries.ITEM)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::targets),
                                                            Trigger.CODEC
                                                                    .fieldOf("trigger")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::trigger),
                                                            Codec.BOOL
                                                                    .optionalFieldOf(
                                                                            "block_when_potion_sick",
                                                                            false)
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::blockWhenPotionSick),
                                                            RegistryCodecs.<Item>holder(
                                                                            (ResourceKey<
                                                                                            ? extends
                                                                                                    Registry<
                                                                                                            Item>>)
                                                                                    Registries.ITEM)
                                                                    .optionalFieldOf("remainder")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::remainder),
                                                            SoundEvent.CODEC
                                                                    .optionalFieldOf("sound")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::sound),
                                                            TreatmentAction.CODEC
                                                                    .listOf()
                                                                    .fieldOf("actions")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::actions))
                                                    .apply(i, ConsumableTreatment::new)))
                    .codec()
                    .validate(
                            rule ->
                                    rule.trigger == Trigger.CONSUME && rule.remainder.isPresent()
                                            ? DataResult.error(
                                                    () ->
                                                            "consume treatments use the item's vanilla remainder component")
                                            : DataResult.success(rule));

    public enum Trigger implements StringRepresentable {
        CONSUME,
        USE;
        public static final Codec<Trigger> CODEC = StringRepresentable.fromEnum(Trigger::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
