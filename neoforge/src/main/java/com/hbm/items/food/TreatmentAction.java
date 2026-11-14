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

import com.hbm.config.VersatileConfig;
import com.hbm.extprop.LivingHazards;
import com.hbm.potion.UncurableEffectInstance;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public sealed interface TreatmentAction {

    Codec<TreatmentAction> CODEC =
            Codec.STRING.partialDispatch(
                    "type",
                    step -> DataResult.success(step.type()),
                    type ->
                            switch (type) {
                                case AdjustCounter.TYPE -> DataResult.success(AdjustCounter.CODEC);
                                case GrantEffect.TYPE -> DataResult.success(GrantEffect.CODEC);
                                case ClearEffects.TYPE -> DataResult.success(ClearEffects.CODEC);
                                case Hurt.TYPE -> DataResult.success(Hurt.CODEC);
                                case PotionSickness.TYPE ->
                                        DataResult.success(PotionSickness.CODEC);
                                default ->
                                        DataResult.error(
                                                () -> "Unknown treatment action type: " + type);
                            });

    String type();

    void run(Player player, RandomSource random);

    private static <S extends TreatmentAction> MapCodec<S> strict(MapCodec<S> fields) {
        return DataCodecs.strict(fields, "type");
    }

    enum Counter implements StringRepresentable {
        RADIATION,
        DIGAMMA,
        ASBESTOS,
        BLACK_LUNG;

        static final Codec<Counter> CODEC = StringRepresentable.fromEnum(Counter::values);

        boolean lungDisease() {
            return this == ASBESTOS || this == BLACK_LUNG;
        }

        int maximum() {
            return switch (this) {
                case ASBESTOS -> LivingHazards.ASBESTOS_LIMIT;
                case BLACK_LUNG -> LivingHazards.BLACK_LUNG_LIMIT;
                case RADIATION, DIGAMMA ->
                        throw new IllegalStateException(this + " has no maximum");
            };
        }

        double read(Player player) {
            return switch (this) {
                case RADIATION -> LivingHazards.radiation(player);
                case DIGAMMA -> LivingHazards.digamma(player);
                case ASBESTOS -> LivingHazards.asbestos(player);
                case BLACK_LUNG -> LivingHazards.blackLung(player);
            };
        }

        void write(Player player, double value) {
            switch (this) {
                case RADIATION -> LivingHazards.setRadiation(player, value);
                case DIGAMMA -> LivingHazards.setDigamma(player, value);
                case ASBESTOS -> LivingHazards.setAsbestos(player, (int) value);
                case BLACK_LUNG -> LivingHazards.setBlackLung(player, (int) value);
            }
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    enum CounterOp implements StringRepresentable {
        ADD,
        SET,
        MIN;

        static final Codec<CounterOp> CODEC = StringRepresentable.fromEnum(CounterOp::values);

        double apply(double current, double operand) {
            return switch (this) {
                case ADD -> Math.max(current + operand, 0D);
                case SET -> operand;
                case MIN -> Math.min(current, operand);
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    record AdjustCounter(Counter counter, CounterOp operation, double amount, int limitDivisor)
            implements TreatmentAction {
        static final String TYPE = "counter";
        private static final double INT_LIMIT = Integer.MAX_VALUE;
        static final MapCodec<AdjustCounter> CODEC =
                strict(
                        RecordCodecBuilder.<AdjustCounter>mapCodec(
                                        i ->
                                                i.group(
                                                                Counter.CODEC
                                                                        .fieldOf("counter")
                                                                        .forGetter(
                                                                                AdjustCounter
                                                                                        ::counter),
                                                                CounterOp.CODEC
                                                                        .fieldOf("operation")
                                                                        .forGetter(
                                                                                AdjustCounter
                                                                                        ::operation),
                                                                Codec.DOUBLE
                                                                        .validate(
                                                                                value ->
                                                                                        Double
                                                                                                        .isFinite(
                                                                                                                value)
                                                                                                ? DataResult
                                                                                                        .success(
                                                                                                                value)
                                                                                                : DataResult
                                                                                                        .error(
                                                                                                                () ->
                                                                                                                        "treatment amount must be finite"))
                                                                        .optionalFieldOf(
                                                                                "amount", 0D)
                                                                        .forGetter(
                                                                                AdjustCounter
                                                                                        ::amount),
                                                                DataCodecs.intRange(
                                                                                1,
                                                                                Integer.MAX_VALUE)
                                                                        .optionalFieldOf(
                                                                                "limit_divisor", 0)
                                                                        .forGetter(
                                                                                AdjustCounter
                                                                                        ::limitDivisor))
                                                        .apply(i, AdjustCounter::new))
                                .validate(AdjustCounter::checked));

        private static DataResult<AdjustCounter> checked(AdjustCounter step) {
            if (step.limitDivisor != 0 && !step.counter.lungDisease())
                return DataResult.error(() -> "limit_divisor requires a lung-disease counter");
            if (step.limitDivisor != 0 && step.amount != 0D)
                return DataResult.error(() -> "amount and limit_divisor are exclusive");
            if (step.operation != CounterOp.ADD && step.amount < 0D)
                return DataResult.error(() -> "counter bound must be nonnegative");
            if (step.counter.lungDisease()
                    && (step.amount != Math.rint(step.amount) || Math.abs(step.amount) > INT_LIMIT))
                return DataResult.error(() -> "lung-disease amounts must be integers");
            return DataResult.success(step);
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void run(Player player, RandomSource random) {
            double operand = limitDivisor == 0 ? amount : counter.maximum() / limitDivisor;
            if (counter == Counter.RADIATION && operation == CounterOp.ADD) {
                LivingHazards.addRadiation(player, operand);
            } else {
                counter.write(player, operation.apply(counter.read(player), operand));
            }
        }
    }

    record GrantEffect(
            Holder<MobEffect> effect,
            int duration,
            int amplifier,
            boolean extend,
            boolean incurable)
            implements TreatmentAction {
        static final String TYPE = "effect";
        static final MapCodec<GrantEffect> CODEC =
                strict(
                        RecordCodecBuilder.mapCodec(
                                i ->
                                        i.group(
                                                        ((Codec<Holder<MobEffect>>)
                                                                        RegistryFixedCodec
                                                                                .<MobEffect>create(
                                                                                        (ResourceKey<
                                                                                                        ? extends
                                                                                                                Registry<
                                                                                                                        MobEffect>>)
                                                                                                Registries
                                                                                                        .MOB_EFFECT))
                                                                .fieldOf("effect")
                                                                .forGetter(GrantEffect::effect),
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .fieldOf("duration")
                                                                .forGetter(GrantEffect::duration),
                                                        DataCodecs.intRange(0, Integer.MAX_VALUE)
                                                                .optionalFieldOf("amplifier", 0)
                                                                .forGetter(GrantEffect::amplifier),
                                                        Codec.BOOL
                                                                .optionalFieldOf("extend", false)
                                                                .forGetter(GrantEffect::extend),
                                                        Codec.BOOL
                                                                .optionalFieldOf("incurable", false)
                                                                .forGetter(GrantEffect::incurable))
                                                .apply(i, GrantEffect::new)));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void run(Player player, RandomSource random) {
            int ticks = duration;
            MobEffectInstance active = extend ? player.getEffect(effect) : null;
            if (active != null) {
                ticks =
                        active.isInfiniteDuration()
                                ? MobEffectInstance.INFINITE_DURATION
                                : (int)
                                        Math.min(
                                                (long) active.getDuration() + duration,
                                                Integer.MAX_VALUE);
            }
            player.addEffect(
                    incurable
                            ? new UncurableEffectInstance(effect, ticks, amplifier)
                            : new MobEffectInstance(effect, ticks, amplifier));
        }
    }

    record ClearEffects(HolderSet<MobEffect> effects) implements TreatmentAction {
        static final String TYPE = "remove_effects";
        static final MapCodec<ClearEffects> CODEC =
                strict(
                        RegistryCodecs.<MobEffect>homogeneousList(
                                        (ResourceKey<? extends Registry<MobEffect>>)
                                                Registries.MOB_EFFECT)
                                .fieldOf("effects")
                                .xmap(ClearEffects::new, ClearEffects::effects));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void run(Player player, RandomSource random) {
            for (Holder<MobEffect> effect : effects) player.removeEffect(effect);
        }
    }

    record Hurt(HolderSet<DamageType> sources, float amount, int repeat, int oneIn)
            implements TreatmentAction {
        static final String TYPE = "damage";
        static final MapCodec<Hurt> CODEC =
                strict(
                        RecordCodecBuilder.mapCodec(
                                i ->
                                        i.group(
                                                        RegistryCodecs.<DamageType>homogeneousList(
                                                                        (ResourceKey<
                                                                                        ? extends
                                                                                                Registry<
                                                                                                        DamageType>>)
                                                                                Registries
                                                                                        .DAMAGE_TYPE)
                                                                .validate(
                                                                        set ->
                                                                                set.size() > 0
                                                                                        ? DataResult
                                                                                                .success(
                                                                                                        set)
                                                                                        : DataResult
                                                                                                .error(
                                                                                                        () ->
                                                                                                                "damage needs at least one source"))
                                                                .fieldOf("sources")
                                                                .forGetter(Hurt::sources),
                                                        Codec.floatRange(0F, Float.MAX_VALUE)
                                                                .fieldOf("amount")
                                                                .forGetter(Hurt::amount),
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .optionalFieldOf("repeat", 1)
                                                                .forGetter(Hurt::repeat),
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .optionalFieldOf("one_in", 1)
                                                                .forGetter(Hurt::oneIn))
                                                .apply(i, Hurt::new)));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void run(Player player, RandomSource random) {
            if (oneIn > 1 && random.nextInt(oneIn) != 0) return;
            ServerLevel level = (ServerLevel) player.level();
            int size = sources.size();
            for (int hit = 0; hit < repeat; hit++) {
                Holder<DamageType> source =
                        switch (size) {
                            case 1 -> sources.get(0);
                            case 2 -> sources.get(random.nextBoolean() ? 0 : 1);
                            default -> sources.get(random.nextInt(size));
                        };
                player.hurtServer(level, new DamageSource(source), amount);
            }
        }
    }

    record PotionSickness(int seconds) implements TreatmentAction {
        static final String TYPE = "potion_sickness";
        static final MapCodec<PotionSickness> CODEC =
                strict(
                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("seconds")
                                .xmap(PotionSickness::new, PotionSickness::seconds));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void run(Player player, RandomSource random) {
            VersatileConfig.applyPotionSickness(player, seconds);
        }
    }
}
