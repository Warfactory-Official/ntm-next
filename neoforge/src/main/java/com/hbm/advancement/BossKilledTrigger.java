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

package com.hbm.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class BossKilledTrigger extends SimpleCriterionTrigger<BossKilledTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Kind kind) {
        this.trigger(player, instance -> instance.kind() == kind);
    }

    public enum Kind implements StringRepresentable {
        MASKMAN("maskman"),
        MELTDOWN("meltdown"),
        WORM("worm"),
        UFO("ufo");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Kind kind)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                LootItemCondition.CODEC
                                                        .optionalFieldOf("player")
                                                        .forGetter(TriggerInstance::player),
                                                Kind.CODEC
                                                        .fieldOf("kind")
                                                        .forGetter(TriggerInstance::kind))
                                        .apply(instance, TriggerInstance::new));
    }
}
