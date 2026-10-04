// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class ServoEffects {
    private static final int DURATION = 60;

    private ServoEffects() {}

    public static int source(ArmorMods mods, EquipmentSlot slot) {
        if (!(mods.item(ArmorModHandler.SERVOS) instanceof ItemModServos servos)) return 0;
        return switch (slot) {
            case CHEST -> servos.effectAmplifier(slot) + 1;
            case LEGS -> (servos.effectAmplifier(slot) + 1) << 4;
            default -> 0;
        };
    }

    public static void serverTick(LivingEntity wearer, int sources) {
        if ((sources & 15) != 0) sustain(wearer, MobEffects.HASTE, (sources & 15) - 1);
        if ((sources >>> 4) != 0) sustain(wearer, MobEffects.JUMP_BOOST, (sources >>> 4) - 1);
    }

    private static boolean flagsDiffer(MobEffectInstance effect) {
        return effect.isAmbient() || !effect.isVisible() || !effect.showIcon();
    }

    private static void sustain(LivingEntity wearer, Holder<MobEffect> type, int amplifier) {
        MobEffectInstance current = wearer.getEffect(type);
        if (current == null || current.getAmplifier() < amplifier || flagsDiffer(current)) {
            wearer.addEffect(new MobEffectInstance(type, DURATION, amplifier));
            return;
        }
        while (true) {
            if (current.getAmplifier() < amplifier || flagsDiffer(current)) {
                current.update(new MobEffectInstance(type, DURATION, amplifier));
                return;
            }
            if (current.isInfiniteDuration() || current.getDuration() >= DURATION) return;
            if (current.getAmplifier() == amplifier) {
                current.duration = DURATION;
                return;
            }
            if (current.hiddenEffect == null) {
                current.hiddenEffect = new MobEffectInstance(type, DURATION, amplifier);
                return;
            }
            current = current.hiddenEffect;
        }
    }

    public static void clientTick(LivingEntity wearer, int sources) {
        if ((sources & 15) != 0)
            holdClientDuration(wearer.getEffect(MobEffects.HASTE), (sources & 15) - 1);
        if ((sources >>> 4) != 0)
            holdClientDuration(wearer.getEffect(MobEffects.JUMP_BOOST), (sources >>> 4) - 1);
    }

    private static void holdClientDuration(MobEffectInstance current, int amplifier) {
        if (current != null
                && !current.isInfiniteDuration()
                && current.getAmplifier() == amplifier
                && current.getDuration() < DURATION) current.duration = DURATION;
    }

    public static void sourcesChanged(LivingEntity wearer, int previous, int next) {
        if ((next & 15) < (previous & 15)) syncRemainingDuration(wearer, MobEffects.HASTE);
        if ((next >>> 4) < (previous >>> 4)) syncRemainingDuration(wearer, MobEffects.JUMP_BOOST);
    }

    private static void syncRemainingDuration(LivingEntity wearer, Holder<MobEffect> type) {
        MobEffectInstance current = wearer.getEffect(type);
        if (current == null) return;
        wearer.sendEffectToPassengers(current);
        if (wearer instanceof ServerPlayer player)
            player.connection.send(
                    new ClientboundUpdateMobEffectPacket(wearer.getId(), current, false));
    }
}
