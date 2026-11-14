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

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.Exposure;
import com.hbm.util.ArmorRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public record ToxinTrait(List<Dose> doses) implements FluidTrait {
    public sealed interface Dose permits Harm, Afflict {

        Exposure mask();

        boolean fullBody();
    }

    public record Harm(
            ResourceKey<DamageType> damage,
            float amount,
            int delay,
            Exposure mask,
            boolean fullBody)
            implements Dose {

        public Harm {
            Objects.requireNonNull(mask, "a toxin dose needs a hazard class");
        }
    }

    public record Afflict(Exposure mask, boolean fullBody, List<MobEffectInstance> effects)
            implements Dose {

        public Afflict {
            Objects.requireNonNull(mask, "a toxin dose needs a hazard class");
            effects = List.copyOf(effects);
        }

        public Afflict(Exposure mask, boolean fullBody, MobEffectInstance... effects) {
            this(mask, fullBody, List.of(effects));
        }
    }

    public ToxinTrait {
        doses = List.copyOf(doses);
    }

    public static ToxinTrait of(Dose... doses) {
        return new ToxinTrait(List.of(doses));
    }

    public void expose(LivingEntity living, double intensity) {
        for (var dose : doses) {
            if (stopped(living, dose)) continue;
            switch (dose) {
                case Harm harm -> {
                    if (living.level() instanceof ServerLevel level
                            && (harm.delay == 0 || level.getGameTime() % harm.delay == 0)) {
                        living.hurtServer(
                                level,
                                level.damageSources().source(harm.damage),
                                (float) (harm.amount * intensity));
                    }
                }
                case Afflict afflict -> {
                    for (var effect : afflict.effects) {
                        living.addEffect(
                                new MobEffectInstance(
                                        effect.getEffect(),
                                        (int) (effect.getDuration() * intensity),
                                        effect.getAmplifier()));
                    }
                }
            }
        }
    }

    private static boolean stopped(LivingEntity living, Dose dose) {
        if (!ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, dose.mask())) return false;
        ArmorUtil.damageGasMaskFilter(living, 1);
        return !dose.fullBody() || ArmorUtil.checkForHazmat(living);
    }

    @Override
    public void describeDetails(Consumer<Component> lines) {
        lines.accept(FluidTraitText.label("hbmfluid.trait.toxin", ChatFormatting.LIGHT_PURPLE));
        for (var dose : doses) {
            switch (dose) {
                case Harm harm -> {
                    var perSecond = (double) (harm.amount * 20F / harm.delay);
                    var rate = String.format(Locale.ROOT, "%,.1f", perSecond) + " ";
                    lines.accept(
                            header(harm)
                                    .append(
                                            Component.literal(": ")
                                                    .withStyle(
                                                            harm.fullBody
                                                                    ? ChatFormatting.RED
                                                                    : ChatFormatting.YELLOW))
                                    .append(
                                            Component.literal(rate)
                                                    .withStyle(ChatFormatting.YELLOW))
                                    .append(
                                            Component.translatable("hbmfluid.trait.perDamage")
                                                    .withStyle(ChatFormatting.YELLOW)));
                }
                case Afflict afflict -> {
                    lines.accept(
                            header(afflict)
                                    .append(
                                            Component.literal(":")
                                                    .withStyle(ChatFormatting.YELLOW)));
                    for (var effect : afflict.effects) lines.accept(effectLine(effect));
                }
            }
        }
    }

    private static MutableComponent header(Dose dose) {
        var line =
                Component.empty()
                        .append(Component.literal("- ").withStyle(ChatFormatting.YELLOW))
                        .append(
                                Component.translatable(dose.mask().translationKey)
                                        .withStyle(ChatFormatting.YELLOW));
        if (!dose.fullBody()) return line;
        return line.append(Component.literal(" (").withStyle(ChatFormatting.RED))
                .append(
                        Component.translatable("hbmfluid.trait.hazmat")
                                .withStyle(ChatFormatting.RED))
                .append(Component.literal(")").withStyle(ChatFormatting.RED));
    }

    private static Component effectLine(MobEffectInstance effect) {
        var line = Component.literal("   - ").append(effect.getEffect().value().getDisplayName());
        if (effect.getAmplifier() > 0) {
            line.append(" ")
                    .append(Component.translatable("potion.potency." + effect.getAmplifier()));
        }
        return line.append(" ")
                .append(MobEffectUtil.formatDuration(effect, 1F, 20F))
                .withStyle(ChatFormatting.YELLOW);
    }
}
