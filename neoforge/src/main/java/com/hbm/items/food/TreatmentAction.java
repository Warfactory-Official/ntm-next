// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.potion.UncurableEffectInstance;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public sealed interface TreatmentAction {
    Codec<Double> FINITE =
            Codec.DOUBLE.validate(
                    value ->
                            Double.isFinite(value)
                                    ? DataResult.success(value)
                                    : DataResult.error(() -> "treatment amount must be finite"));

    Map<String, MapCodec<? extends TreatmentAction>> TYPES =
            Map.of(
                    "counter",
                    Counter.MAP_CODEC,
                    "effect",
                    Effect.MAP_CODEC,
                    "remove_effects",
                    RemoveEffects.MAP_CODEC,
                    "damage",
                    Damage.MAP_CODEC,
                    "potion_sickness",
                    Sickness.MAP_CODEC);
    Codec<TreatmentAction> CODEC =
            Codec.STRING
                    .comapFlatMap(
                            name ->
                                    TYPES.containsKey(name)
                                            ? DataResult.success(name)
                                            : DataResult.error(
                                                    () -> "unknown treatment action " + name),
                            name -> name)
                    .dispatch("type", TreatmentAction::type, TYPES::get);

    String type();

    void apply(Player player, net.minecraft.util.RandomSource random);

    record Counter(CounterKind counter, CounterOperation operation, double amount, int limitDivisor)
            implements TreatmentAction {
        static final MapCodec<Counter> MAP_CODEC =
                DataCodecs.strict(
                                RecordCodecBuilder.<Counter>mapCodec(
                                        i ->
                                                i.group(
                                                                CounterKind.CODEC
                                                                        .fieldOf("counter")
                                                                        .forGetter(
                                                                                Counter::counter),
                                                                CounterOperation.CODEC
                                                                        .fieldOf("operation")
                                                                        .forGetter(
                                                                                Counter::operation),
                                                                FINITE.optionalFieldOf("amount", 0D)
                                                                        .forGetter(Counter::amount),
                                                                DataCodecs.intRange(
                                                                                1,
                                                                                Integer.MAX_VALUE)
                                                                        .optionalFieldOf(
                                                                                "limit_divisor", 0)
                                                                        .forGetter(
                                                                                Counter
                                                                                        ::limitDivisor))
                                                        .apply(i, Counter::new)),
                                "type")
                        .validate(
                                action -> {
                                    if (action.limitDivisor != 0
                                            && (action.counter != CounterKind.ASBESTOS
                                                    && action.counter != CounterKind.BLACK_LUNG))
                                        return DataResult.error(
                                                () ->
                                                        "limit_divisor requires a lung-disease counter");
                                    if (action.limitDivisor != 0 && action.amount != 0D)
                                        return DataResult.error(
                                                () -> "amount and limit_divisor are exclusive");
                                    if (action.operation != CounterOperation.ADD
                                            && action.amount < 0D)
                                        return DataResult.error(
                                                () -> "counter bound must be nonnegative");
                                    if ((action.counter == CounterKind.ASBESTOS
                                                    || action.counter == CounterKind.BLACK_LUNG)
                                            && (action.amount != Math.rint(action.amount)
                                                    || Math.abs(action.amount) > Integer.MAX_VALUE))
                                        return DataResult.error(
                                                () -> "lung-disease amounts must be integers");
                                    return DataResult.success(action);
                                });

        @Override
        public String type() {
            return "counter";
        }

        @Override
        public void apply(Player player, net.minecraft.util.RandomSource random) {
            double operand =
                    limitDivisor == 0
                            ? amount
                            : (counter == CounterKind.ASBESTOS
                                    ? HbmLivingProps.maxAsbestos / limitDivisor
                                    : HbmLivingProps.maxBlacklung / limitDivisor);
            double current =
                    switch (counter) {
                        case RADIATION -> HbmLivingProps.getRadiation(player);
                        case DIGAMMA -> HbmLivingProps.getDigamma(player);
                        case ASBESTOS -> HbmLivingProps.getAsbestos(player);
                        case BLACK_LUNG -> HbmLivingProps.getBlackLung(player);
                    };
            double next =
                    switch (operation) {
                        case ADD -> Math.max(current + operand, 0D);
                        case SET -> operand;
                        case MIN -> Math.min(current, operand);
                    };
            switch (counter) {
                case RADIATION -> {
                    if (operation == CounterOperation.ADD)
                        HbmLivingProps.incrementRadiation(player, operand);
                    else HbmLivingProps.setRadiation(player, next);
                }
                case DIGAMMA -> HbmLivingProps.setDigamma(player, next);
                case ASBESTOS -> HbmLivingProps.setAsbestos(player, (int) next);
                case BLACK_LUNG -> HbmLivingProps.setBlackLung(player, (int) next);
            }
        }
    }

    record Effect(
            Holder<MobEffect> effect,
            int duration,
            int amplifier,
            boolean extend,
            boolean incurable)
            implements TreatmentAction {
        static final MapCodec<Effect> MAP_CODEC =
                DataCodecs.strict(
                        RecordCodecBuilder.<Effect>mapCodec(
                                i ->
                                        i.group(
                                                        MobEffect.CODEC
                                                                .fieldOf("effect")
                                                                .forGetter(Effect::effect),
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .fieldOf("duration")
                                                                .forGetter(Effect::duration),
                                                        DataCodecs.intRange(0, Integer.MAX_VALUE)
                                                                .optionalFieldOf("amplifier", 0)
                                                                .forGetter(Effect::amplifier),
                                                        Codec.BOOL
                                                                .optionalFieldOf("extend", false)
                                                                .forGetter(Effect::extend),
                                                        Codec.BOOL
                                                                .optionalFieldOf("incurable", false)
                                                                .forGetter(Effect::incurable))
                                                .apply(i, Effect::new)),
                        "type");

        @Override
        public String type() {
            return "effect";
        }

        @Override
        public void apply(Player player, net.minecraft.util.RandomSource random) {
            var existing = extend ? player.getEffect(effect) : null;
            int ticks =
                    existing == null ? duration : Math.addExact(existing.getDuration(), duration);
            player.addEffect(
                    incurable
                            ? new UncurableEffectInstance(effect, ticks, amplifier)
                            : new MobEffectInstance(effect, ticks, amplifier));
        }
    }

    record RemoveEffects(HolderSet<MobEffect> effects) implements TreatmentAction {
        static final MapCodec<RemoveEffects> MAP_CODEC =
                DataCodecs.strict(
                        RecordCodecBuilder.<RemoveEffects>mapCodec(
                                i ->
                                        i.group(
                                                        RegistryCodecs.homogeneousList(
                                                                        Registries.MOB_EFFECT)
                                                                .fieldOf("effects")
                                                                .forGetter(RemoveEffects::effects))
                                                .apply(i, RemoveEffects::new)),
                        "type");

        @Override
        public String type() {
            return "remove_effects";
        }

        @Override
        public void apply(Player player, net.minecraft.util.RandomSource random) {
            for (var effect : effects) player.removeEffect(effect);
        }
    }

    record Damage(HolderSet<DamageType> sources, float amount, int repeat, int oneIn)
            implements TreatmentAction {
        static final MapCodec<Damage> MAP_CODEC =
                DataCodecs.strict(
                        RecordCodecBuilder.<Damage>mapCodec(
                                i ->
                                        i.group(
                                                        RegistryCodecs.homogeneousList(
                                                                        Registries.DAMAGE_TYPE)
                                                                .fieldOf("sources")
                                                                .forGetter(Damage::sources),
                                                        Codec.floatRange(0F, Float.MAX_VALUE)
                                                                .fieldOf("amount")
                                                                .forGetter(Damage::amount),
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .optionalFieldOf("repeat", 1)
                                                                .forGetter(Damage::repeat),
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .optionalFieldOf("one_in", 1)
                                                                .forGetter(Damage::oneIn))
                                                .apply(i, Damage::new)),
                        "type");

        @Override
        public String type() {
            return "damage";
        }

        @Override
        public void apply(Player player, net.minecraft.util.RandomSource random) {
            assert sources.size() > 0;
            if (oneIn > 1 && random.nextInt(oneIn) != 0) return;
            var level = (net.minecraft.server.level.ServerLevel) player.level();
            for (int i = 0; i < repeat; i++) {
                int choice = sources.size() == 1 ? 0 : random.nextInt(sources.size());
                player.hurtServer(
                        level,
                        level.damageSources().source(sources.get(choice).unwrapKey().orElseThrow()),
                        amount);
            }
        }
    }

    record Sickness(int seconds) implements TreatmentAction {
        static final MapCodec<Sickness> MAP_CODEC =
                DataCodecs.strict(
                        RecordCodecBuilder.<Sickness>mapCodec(
                                i ->
                                        i.group(
                                                        DataCodecs.intRange(1, Integer.MAX_VALUE)
                                                                .fieldOf("seconds")
                                                                .forGetter(Sickness::seconds))
                                                .apply(i, Sickness::new)),
                        "type");

        @Override
        public String type() {
            return "potion_sickness";
        }

        @Override
        public void apply(Player player, net.minecraft.util.RandomSource random) {
            VersatileConfig.applyPotionSickness(player, seconds);
        }
    }

    enum CounterKind implements net.minecraft.util.StringRepresentable {
        RADIATION,
        DIGAMMA,
        ASBESTOS,
        BLACK_LUNG;
        static final Codec<CounterKind> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(CounterKind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    enum CounterOperation implements net.minecraft.util.StringRepresentable {
        ADD,
        SET,
        MIN;
        static final Codec<CounterOperation> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(CounterOperation::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
