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

package com.hbm.entity;

import com.hbm.entity.projectile.ChemicalEntity;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.trait.PoisonTrait;
import com.hbm.inventory.fluid.trait.ToxinTrait;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidContact<S extends Entity> {

    @FunctionalInterface
    public interface Step<S extends Entity> {
        boolean touch(
                S source,
                ServerLevel level,
                Entity target,
                @Nullable LivingEntity living,
                double intensity);
    }

    @FunctionalInterface
    public interface Effect<S extends Entity> {
        void touch(S source, ServerLevel level, Entity target, double intensity);
    }

    @FunctionalInterface
    public interface LivingEffect<S extends Entity> {
        void touch(S source, ServerLevel level, LivingEntity living, double intensity);
    }

    @FunctionalInterface
    public interface Damage<S extends Entity> {
        DamageSource of(S source);
    }

    private final Step<S>[] steps;

    private FluidContact(Step<S>[] steps) {
        this.steps = steps;
    }

    public static <S extends Entity> Builder<S> builder() {
        return new Builder<>();
    }

    public void touch(S source, ServerLevel level, Entity target, double intensity) {
        var living = target instanceof LivingEntity l ? l : null;
        for (var step : steps) {
            if (!step.touch(source, level, target, living, intensity)) return;
        }
    }

    public static final class Builder<S extends Entity> {
        private final List<Step<S>> steps = new ArrayList<>();

        private Builder() {}

        public Builder<S> step(Step<S> step) {
            steps.add(step);
            return this;
        }

        public Builder<S> any(Effect<S> effect) {
            return step(
                    (source, level, target, _, intensity) -> {
                        effect.touch(source, level, target, intensity);
                        return true;
                    });
        }

        public Builder<S> living(LivingEffect<S> effect) {
            return step(
                    (source, level, _, living, intensity) -> {
                        if (living != null) effect.touch(source, level, living, intensity);
                        return true;
                    });
        }

        public Builder<S> exclusive(Effect<S> any, LivingEffect<S> living) {
            return step(
                    (source, level, target, alive, intensity) -> {
                        any.touch(source, level, target, intensity);
                        if (alive == null) return true;
                        living.touch(source, level, alive, intensity);
                        return false;
                    });
        }

        @SuppressWarnings("unchecked")
        public FluidContact<S> build() {
            return new FluidContact<>(steps.toArray(Step[]::new));
        }
    }

    public static <S extends Entity> Effect<S> hurt(Damage<S> damage, float amount) {
        return (source, level, target, _) ->
                target.hurtServer(level, damage.of(source), amount);
    }

    public static <S extends Entity> LivingEffect<S> hurtLiving(Damage<S> damage, float amount) {
        return (source, level, living, _) ->
                living.hurtServer(level, damage.of(source), amount);
    }

    public static <S extends Entity> Effect<S> ignite(float seconds) {
        return (_,
                _,
                target,
                _) -> target.igniteForSeconds(seconds);
    }

    public static <S extends Entity> Effect<S> extinguish() {
        return (_,
                _,
                target,
                _) -> target.clearFire();
    }

    public static <S extends Entity> LivingEffect<S> heal() {
        return (_, _, living, intensity) -> {
            if (living.isAlive()) living.heal(2F * (float) intensity);
        };
    }

    public static <S extends Entity> LivingEffect<S> oil(int ticks) {
        return (_,
                _,
                living,
                _) -> LivingHazards.getOrCreate(living).oil = ticks;
    }

    public static <S extends Entity> LivingEffect<S> irradiate(float amount) {
        return (_,
                _,
                living,
                _) ->
                ContaminationUtil.contaminate(
                        living, HazardType.RADIATION, ContaminationType.CREATIVE, amount);
    }

    public static <S extends Entity> LivingEffect<S> corrode(
            Damage<S> damage, float amount, int wear) {
        return (source, level, living, _) -> {
            living.hurtServer(level, damage.of(source), amount);
            ArmorUtil.damageWholeSuit(living, wear);
        };
    }

    public static <S extends Entity> LivingEffect<S> poison(PoisonTrait trait) {
        return (_, _, living, intensity) ->
                trait.expose(living, intensity);
    }

    public static <S extends Entity> LivingEffect<S> toxin(ToxinTrait trait) {
        return (_, _, living, intensity) ->
                trait.expose(living, intensity);
    }

    public static <S extends Entity> LivingEffect<S> effects(
            Class<? extends LivingEntity> kind, MobEffectInstance... templates) {
        return (_,
                _,
                living,
                _) -> {
            if (!kind.isInstance(living)) return;
            for (var template : templates) living.addEffect(new MobEffectInstance(template));
        };
    }

    public static boolean scatter(ServerLevel level, Entity source, Entity target) {
        var random = source.getRandom();
        var x = source.getX() + (random.nextDouble() - 0.5D) * 64.0D;
        var y = source.getY() + (random.nextInt(64) - 32);
        var z = source.getZ() + (random.nextDouble() - 0.5D) * 64.0D;
        var from = target.position();
        var moved =
                target instanceof LivingEntity living
                        ? living.randomTeleport(x, y, z, false)
                        : ChemicalEntity.walkTo(level, target, x, y, z);
        if (!moved) return false;
        WorldSounds.play(
                level,
                null,
                from.x,
                from.y,
                from.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1F,
                1F);
        EntitySounds.play(
                target,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1F,
                1F);
        return true;
    }

    public static final class Memo<T> {
        private final BiFunction<Fluid, @Nullable NTMFluidProperty, T> derive;
        private @Nullable Derived<T> last;

        public Memo(BiFunction<Fluid, @Nullable NTMFluidProperty, T> derive) {
            this.derive = derive;
        }

        public T get(Fluid fluid) {
            var property = NTMFluidProperties.get(fluid);
            var known = last;
            if (known == null || known.fluid() != fluid || known.property() != property) {
                known = new Derived<>(fluid, property, derive.apply(fluid, property));
                last = known;
            }
            return known.value();
        }

        private record Derived<T>(Fluid fluid, @Nullable NTMFluidProperty property, T value) {}
    }
}
