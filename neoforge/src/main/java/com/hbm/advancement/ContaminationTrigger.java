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
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class ContaminationTrigger
        extends SimpleCriterionTrigger<ContaminationTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, double level, boolean fatal) {
        this.trigger(player, instance -> instance.matches(level, fatal));
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            MinMaxBounds.Doubles level,
            Optional<Boolean> fatal)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                EntityPredicate.ADVANCEMENT_CODEC
                                                        .optionalFieldOf("player")
                                                        .forGetter(TriggerInstance::player),
                                                MinMaxBounds.Doubles.CODEC
                                                        .optionalFieldOf(
                                                                "level", MinMaxBounds.Doubles.ANY)
                                                        .forGetter(TriggerInstance::level),
                                                Codec.BOOL
                                                        .optionalFieldOf("fatal")
                                                        .forGetter(TriggerInstance::fatal))
                                        .apply(instance, TriggerInstance::new));

        public boolean matches(double level, boolean fatal) {
            return this.level.matches(level) && (this.fatal.isEmpty() || this.fatal.get() == fatal);
        }
    }
}
