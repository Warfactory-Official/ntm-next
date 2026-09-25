// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

public class DetonationTrigger extends SimpleCriterionTrigger<DetonationTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Kind kind) {
        this.trigger(player, instance -> instance.kinds().contains(kind));
    }

    public enum Kind implements StringRepresentable {
        NUKE("nuke"),

        RBMK("rbmk"),

        ZIRNOX("zirnox"),

        WATZ("watz"),

        REFINERY("refinery"),
        FLUID_TANK("fluid_tank"),

        DIGAMMA_SPEAR("digamma_spear"),

        NUCLEAR_CREEPER("nuclear_creeper"),

        SOYUZ_EXHAUST("soyuz_exhaust");

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

    public record TriggerInstance(Optional<ContextAwarePredicate> player, List<Kind> kinds)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                EntityPredicate.ADVANCEMENT_CODEC
                                                        .optionalFieldOf("player")
                                                        .forGetter(TriggerInstance::player),
                                                Kind.CODEC
                                                        .listOf()
                                                        .fieldOf("kinds")
                                                        .forGetter(TriggerInstance::kinds))
                                        .apply(instance, TriggerInstance::new));
    }
}
