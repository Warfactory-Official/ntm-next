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

package com.hbm.items.weapon.sedna.cue;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MuzzleFlashLayout;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.GunSoundEvents;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface GunAnimationAction {

    void play(ItemStack stack, LambdaContext ctx);

    GunAnimationAction MUZZLE_FLASH =
            (_, ctx) -> {
                var shooter = ctx.entity();
                PacketWire.sendToAllAround(
                        MuzzleFlashLayout.encode(shooter.getId()),
                        new TargetPoint(
                                (ServerLevel) shooter.level(),
                                shooter.getX(),
                                shooter.getY(),
                                shooter.getZ(),
                                100D));
            };

    static Magazine<?> magazine(ItemStack stack, LambdaContext ctx) {
        return ctx.config().getReceivers(stack).get(0).getMagazine(stack);
    }

    static GunAnimationAction sound(Supplier<SoundEvent> sound) {
        return sound(sound, 1F, 1F);
    }

    static GunAnimationAction sound(Supplier<SoundEvent> sound, float volume, float pitch) {
        var event = sound.get();
        return (_, ctx) -> {
            var shooter = ctx.entity();
            EntitySounds.play(
                    shooter,
                    shooter.getX(),
                    shooter.getY(),
                    shooter.getZ(),
                    event,
                    SoundSource.PLAYERS,
                    volume,
                    pitch);
        };
    }

    static GunAnimationAction sound(
            Supplier<SoundEvent> sound, float volume, RandomFloatProvider pitch) {
        var event = sound.get();
        return (_, ctx) -> {
            var shooter = ctx.entity();
            float rolled = pitch.sample(shooter.getRandom());
            EntitySounds.play(
                    shooter,
                    shooter.getX(),
                    shooter.getY(),
                    shooter.getZ(),
                    event,
                    SoundSource.PLAYERS,
                    volume,
                    rolled);
        };
    }

    static GunAnimationAction legacySound(Supplier<SoundEvent> sound, float volume, float pitch) {
        var event = sound.get();
        return (_, ctx) ->
                GunSoundEvents.play(ctx.entity(), event, volume, pitch, true);
    }

    static GunAnimationAction modernSound(Supplier<SoundEvent> sound, float volume, float pitch) {
        var event = sound.get();
        return (_, ctx) ->
                GunSoundEvents.play(ctx.entity(), event, volume, pitch, false);
    }

    static GunAnimationAction eject(CasingEjection casing) {
        return (stack, ctx) -> {
            var spent = magazine(stack, ctx).getCasing(stack, ctx.inventory());
            if (spent != null) casing.toss(ctx.entity(), stack, spent.name());
        };
    }

    static GunAnimationAction eject(
            CasingEjection casing, ToIntBiFunction<ItemStack, LambdaContext> count) {
        return (stack, ctx) -> {
            var spent = magazine(stack, ctx).getCasing(stack, ctx.inventory());
            if (spent == null) return;
            int casings = count.applyAsInt(stack, ctx);
            for (int i = 0; i < casings; i++) casing.toss(ctx.entity(), stack, spent.name());
        };
    }

    static GunAnimationAction byHand(GunAnimationAction left, GunAnimationAction right) {
        return (stack, ctx) -> (ctx.configIndex() == 0 ? left : right).play(stack, ctx);
    }

    static GunAnimationAction when(
            BiPredicate<ItemStack, LambdaContext> test, GunAnimationAction... cues) {
        return (stack, ctx) -> {
            if (!test.test(stack, ctx)) return;
            for (var cue : cues) cue.play(stack, ctx);
        };
    }
}
