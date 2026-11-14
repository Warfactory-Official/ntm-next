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

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.HeldItemAnimations;
import com.hbm.client.weapon.GunClientState;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.cue.CasingEjection;
import com.hbm.items.weapon.sedna.cue.GunAnimationAction;
import com.hbm.items.weapon.sedna.cue.GunAnimationTimeline.Score;
import com.hbm.items.weapon.sedna.cue.GunAnimationTimeline;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.impl.GunChargeThrowerItem;
import com.hbm.items.weapon.sedna.impl.GunStingerItem;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlasmaBlastLayout;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import com.hbm.util.EntityTargeting;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import static com.hbm.items.weapon.sedna.cue.GunAnimationAction.*;
import static com.hbm.items.weapon.sedna.cue.RandomFloatProvider.gaussian;
import static com.hbm.items.weapon.sedna.cue.RandomFloatProvider.uniform;
import static com.hbm.render.anim.AnimationEnums.GunAnimation.*;
import static com.hbm.sound.ModSounds.*;

public final class GunAnimationActions {

    private GunAnimationActions() {}

    private static final ToIntBiFunction<ItemStack, LambdaContext> CAPACITY =
            (stack, ctx) -> magazine(stack, ctx).getCapacity(stack);
    private static final ToIntBiFunction<ItemStack, LambdaContext> FIRED =
            (stack, ctx) -> {
                var mag = magazine(stack, ctx);
                return mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory());
            };
    private static final ToIntBiFunction<ItemStack, LambdaContext> MINIGUN_ROUNDS =
            (stack, ctx) ->
                    XWeaponModManager.hasUpgrade(
                                    stack, ctx.configIndex(), XWeaponModManager.ID_MINIGUN_SPEED)
                            ? 3
                            : 1;

    private static final BiPredicate<ItemStack, LambdaContext> EMPTY =
            (stack, ctx) -> magazine(stack, ctx).getAmount(stack, ctx.inventory()) == 0;
    private static final BiPredicate<ItemStack, LambdaContext> RELOADED_FROM_EMPTY =
            (stack, ctx) -> magazine(stack, ctx).getAmountBeforeReload(stack) <= 0;
    private static final BiPredicate<ItemStack, LambdaContext> REFILLED =
            (stack, ctx) -> magazine(stack, ctx).getAmountAfterReload(stack) > 0;
    private static final BiPredicate<ItemStack, LambdaContext> HAS_FIRED =
            (stack, ctx) -> FIRED.applyAsInt(stack, ctx) > 0;
    private static final BiPredicate<ItemStack, LambdaContext> MOD_207 =
            (stack, _) ->
                    XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SLOWDOWN);
    private static final BiPredicate<ItemStack, LambdaContext> NO_HOOK =
            (stack, ctx) ->
                    ctx.entity().level().getEntity(GunChargeThrowerItem.getLastHook(stack)) == null;

    private static final GunAnimationAction FORGET_PRE_RELOAD =
            (stack, ctx) -> magazine(stack, ctx).setAmountBeforeReload(stack, 0);
    private static final GunAnimationAction FORGET_REFILL =
            (stack, ctx) -> magazine(stack, ctx).setAmountAfterReload(stack, 0);
    private static final GunAnimationAction LOAD =
            (stack, ctx) -> magazine(stack, ctx).reloadAction(stack, ctx.inventory());
    private static final GunAnimationAction NOISE =
            (_, ctx) -> {
                var shooter = ctx.entity();
                WorldSounds.entityNoise(
                        shooter, shooter.getX(), shooter.getY(), shooter.getZ(), 1F);
            };
    private static final GunAnimationAction RUNNING_NOISE =
            (_, ctx) -> WorldSounds.entityLoop(ctx.entity(), false);
    private static final GunAnimationAction SLOW_EQUESTRIAN =
            (stack, ctx) -> {
                if (magazine(stack, ctx).getType(stack, null) == XFactory12ga.g12_equestrian_bj) {
                    GunBaseNTItem.setTimer(stack, 0, 20);
                }
            };
    private static final GunAnimationAction BINGE =
            (_, ctx) -> {
                var drinker = ctx.entity();
                drinker.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30 * 20, 2));
                drinker.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30 * 20, 2));
                drinker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 10 * 20, 0));
            };

    private static final CasingEjection REVOLVER_CASING =
            CasingEjection.from(0.25, -0.125, -0.125).kick(-0.05, 0, 0);
    private static final CasingEjection CARBINE_CASING =
            CasingEjection.from(0.3125, -0.125, -0.25)
                    .aimed(0.3125, 0, 0)
                    .kick(0, 0.21, -0.06)
                    .spin(gaussian(-10F, 2.5F), gaussian(2.5F, 2F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection HENRY_CASING =
            CasingEjection.from(0.5, -0.125, -0.375)
                    .aimed(0.5, -0.125, -0.125)
                    .kick(0, 0.12, -0.12)
                    .spin(gaussian(-7.5F, 5F), gaussian(1.5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection HANGMAN_CASING =
            CasingEjection.from(0.25, -0.25, -0.125)
                    .kick(-0.05, 0, 0)
                    .spin(gaussian(-6.5F, 3F), gaussian(5F));
    private static final CasingEjection MARESLEG_CASING =
            CasingEjection.from(0.3125, -0.125, -0.375)
                    .aimed(0.3125, -0.125, -0.125)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-10F, 5F), gaussian(2.5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection SHORT_CASING =
            MARESLEG_CASING.kick(0, -0.08, 0).spin(gaussian(-15F, 5F), gaussian(2.5F));
    private static final CasingEjection SHORT_LEFT_CASING =
            CasingEjection.from(0.3125, -0.125, 0.375)
                    .aimed(0.3125, -0.125, 0.125)
                    .kick(0, -0.08, 0)
                    .spin(gaussian(-15F, 5F), gaussian(2.5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection GREASEGUN_CASING =
            CasingEjection.from(0.55, -0.125, -0.25)
                    .aimed(0.55, 0, 0)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-7.5F, 5F), gaussian(12F, 5F));
    private static final CasingEjection UZI_CASING =
            CasingEjection.from(0.375, -0.125, -0.25)
                    .aimed(0.375, 0, 0)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-2.5F, 5F), uniform(10F, 15F));
    private static final CasingEjection UZI_RIGHT_CASING =
            CasingEjection.from(0.375, -0.125, -0.375)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-2.5F, 5F), uniform(10F, 15F));
    private static final CasingEjection UZI_LEFT_CASING =
            CasingEjection.from(0.375, -0.125, 0.375)
                    .kick(0, 0.18, 0.12)
                    .spin(gaussian(-2.5F, 5F), uniform(-10F, -15F));
    private static final CasingEjection DOUBLE_BARREL_CASING =
            CasingEjection.from(0, -0.1875, -0.375)
                    .kick(-0.24, 0.18, 0)
                    .spin(gaussian(-20F, 5F), gaussian(2.5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection MINIGUN_CASING =
            CasingEjection.from(0.5, -0.25, -0.5)
                    .aimed(0.125, -0.125, -0.25)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(15F), gaussian(15F));
    private static final CasingEjection MINIGUN_RIGHT_CASING =
            CasingEjection.from(0.25, -0.25, -0.5)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(15F), gaussian(15F));
    private static final CasingEjection MINIGUN_LEFT_CASING =
            CasingEjection.from(0.25, -0.25, 0.5)
                    .kick(0, 0.18, 0.12)
                    .spin(gaussian(15F), gaussian(15F));
    private static final CasingEjection MAS36_CASING =
            CasingEjection.from(0.375, -0.125, -0.25)
                    .aimed(0.375, 0, 0)
                    .aimedWhile(
                            stack ->
                                    GunBaseNTItem.getIsAiming(stack)
                                            && !XWeaponModManager.hasUpgrade(
                                                    stack, 0, XWeaponModManager.ID_SCOPE))
                    .kick(-0.05, 0.2, -0.025)
                    .spin(gaussian(-10F, 10F), gaussian(12.5F))
                    .smoke(60, 0.5, 10);
    private static final CasingEjection LIBERATOR_CASING =
            CasingEjection.from(0.625, -0.1875, -0.375)
                    .kick(-0.12, 0.18, 0)
                    .spin(gaussian(-15F, 7.5F), gaussian(5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection LIBERATOR_INSPECT_CASING =
            LIBERATOR_CASING.spin(
                    random -> -15F * (float) random.nextGaussian() * 7.5F, gaussian(5F));
    private static final CasingEjection SPAS_CASING =
            CasingEjection.from(0.375, -0.125, -0.25)
                    .aimed(0.375, 0, 0)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-3F, 2.5F), uniform(-15F, -5F));
    private static final CasingEjection SEXY_CASING =
            CasingEjection.from(0.375, -0.125, -0.25)
                    .aimed(0.375, -0.0625, -0.125)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-10F, 2.5F), gaussian(15F, -20F))
                    .smoke(new CasingEjection.Smoke(false, 60, 0.5, 20));
    private static final CasingEjection BOLTER_CASING =
            CasingEjection.from(0.5, -0.125, -0.25)
                    .aimed(0.5, 0, -0.0625)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-10F, 5F), uniform(10F, 10F));
    private static final CasingEjection AM180_CASING =
            CasingEjection.from(0.4375, -0.125, -0.25)
                    .aimed(0.4375, 0, 0)
                    .kick(0, -0.06, 0)
                    .spin(gaussian(10F), gaussian(10F));
    private static final CasingEjection STAR_F_CASING =
            CasingEjection.from(0.3125, -0.125, -0.1875)
                    .aimed(0.3125, 0, 0)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(5F), uniform(12.5F, 5F));
    private static final CasingEjection STAR_F_LEFT_CASING =
            CasingEjection.from(0.3125, -0.125, 0.1875)
                    .aimed(0.3125, 0, 0)
                    .kick(0, 0.18, 0.12)
                    .spin(gaussian(5F), uniform(12.5F, 5F));
    private static final CasingEjection AMAT_CASING =
            CasingEjection.from(0.375, -0.125, -0.25)
                    .aimed(0.375, 0, -0.25)
                    .kick(-0.05, 0.2, -0.025)
                    .spin(gaussian(-10F, 10F), gaussian(12.5F))
                    .smoke(60, 0.5, 10);
    private static final CasingEjection M2_CASING =
            CasingEjection.from(0.375, -0.125, -0.3125)
                    .aimed(0.375, 0, 0)
                    .kick(0, 0.06, -0.18)
                    .spin(gaussian(20F), gaussian(12.5F, 7.5F));
    private static final CasingEjection G3_CASING =
            CasingEjection.from(0.5, -0.125, -0.25)
                    .aimed(0.5, 0, 0)
                    .aimedWhile(
                            stack ->
                                    GunBaseNTItem.getIsAiming(stack)
                                            && stack.getItem() != ModItems.GUN_G3_ZEBRA.get()
                                            && !XWeaponModManager.hasUpgrade(
                                                    stack, 0, XWeaponModManager.ID_SCOPE))
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(5F), uniform(12.5F, 5F));
    private static final CasingEjection STG77_CASING =
            CasingEjection.from(0.25, -0.25, -0.25)
                    .aimed(0.125, -0.125, -0.125)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(5F), uniform(7.5F, 5F))
                    .legacyFront(0.125);
    private static final CasingEjection LAG_CASING =
            CasingEjection.from(0.375, -0.0625, -0.25)
                    .aimed(0.375, 0, 0)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-10F, 5F), uniform(10F, 10F));
    private static final CasingEjection FLARE_CASING =
            CasingEjection.from(0.625, -0.125, -0.375)
                    .aimed(0.625, -0.125, -0.125)
                    .kick(-0.12, 0.18, 0)
                    .spin(gaussian(-15F, 7.5F), gaussian(5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection CONGOLAKE_CASING =
            CasingEjection.from(0.625, -0.25, -0.375)
                    .aimed(0.625, -0.0625, 0)
                    .kick(0, 0.18, 0.12)
                    .spin(gaussian(-5F, 3.5F), uniform(-10F, 5F))
                    .smoke(60, 0.5, 20);
    private static final CasingEjection MK108_CASING =
            CasingEjection.from(0.5, -0.3125, -0.3125)
                    .aimed(0.5, -0.125, -0.375)
                    .kick(0, 0.18, -0.12)
                    .spin(gaussian(-10F, 2.5F), gaussian(15F, -20F))
                    .smoke(60, 0.5, 10);
    private static final CasingEjection ABERRATOR_CASING =
            CasingEjection.from(0.5, -0.125, -0.25)
                    .aimed(0.5, 0, -0.0625)
                    .kick(-0.05, 0.25, -0.05)
                    .spin(gaussian(-10F, 10F), gaussian(12.5F));
    private static final CasingEjection ABERRATOR_LEFT_CASING =
            CasingEjection.from(0.5, -0.125, 0.25)
                    .aimed(0.5, 0, -0.0625)
                    .kick(-0.05, 0.25, 0.05)
                    .spin(gaussian(-10F, 10F), gaussian(12.5F));

    public static final GunAnimationTimeline DEBUG =
            GunAnimationTimeline.of(revolver(REVOLVER_CASING));

    public static final GunAnimationTimeline NOPIP =
            GunAnimationTimeline.of(
                    revolver(REVOLVER_CASING.spin(gaussian(-6.5F, 3F), gaussian(5F))));

    public static final GunAnimationTimeline CARBINE =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_PISTOL_COCK, 1F, 0.8F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(1, eject(CARBINE_CASING))
                                .on(CYCLE_DRY)
                                .at(2, sound(GUN_DRY_FIRE))
                                .at(8, cock)
                                .on(RELOAD)
                                .at(2, sound(GUN_MAG_REMOVE))
                                .at(26, sound(GUN_MAG_INSERT))
                                .on(RELOAD_END)
                                .at(2, cock)
                                .on(JAMMED)
                                .at(cock, 2, 31)
                                .on(INSPECT)
                                .at(6, sound(GUN_REVOLVER_CLOSE))
                                .at(30, sound(GUN_REVOLVER_CLOSE, 1F, 0.9F));
                    });

    public static final GunAnimationTimeline ATLAS =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_REVOLVER_COCK, 1F, 0.9F);
                        var remove = sound(GUN_MAG_SMALL_REMOVE);
                        var close = sound(GUN_REVOLVER_CLOSE);
                        t.on(RELOAD)
                                .at(2, remove)
                                .at(36, sound(GUN_MAG_SMALL_INSERT))
                                .at(44, close)
                                .on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(5, cock)
                                .on(CYCLE_DRY)
                                .at(2, sound(GUN_DRY_FIRE))
                                .at(5, cock)
                                .on(INSPECT)
                                .at(2, remove)
                                .at(24, close)
                                .on(JAMMED)
                                .at(12, remove)
                                .at(34, close);
                    });

    public static final GunAnimationTimeline HENRY =
            GunAnimationTimeline.of(
                    t -> {
                        var lever = sound(GUN_LEVER_COCK);
                        var lift = sound(GUN_MAG_SMALL_REMOVE, 1F, 0.9F);
                        var load = sound(GUN_MAG_SMALL_INSERT);
                        t.on(RELOAD)
                                .at(8, sound(GUN_MAG_SMALL_REMOVE))
                                .at(16, load)
                                .on(RELOAD_CYCLE)
                                .at(0, load)
                                .on(RELOAD_END)
                                .at(0, lift)
                                .at(12, when(RELOADED_FROM_EMPTY, lever))
                                .on(JAMMED)
                                .at(0, lift)
                                .at(lever, 12, 36, 44)
                                .on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(12, lever)
                                .at(14, eject(HENRY_CASING))
                                .on(CYCLE_DRY)
                                .at(2, sound(GUN_DRY_FIRE))
                                .at(12, lever);
                    });

    public static final GunAnimationTimeline HANGMAN =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_REVOLVER_COCK, 1F, 0.8F);
                        var remove = sound(GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
                        var close = sound(GUN_REVOLVER_CLOSE);
                        var recock = sound(GUN_REVOLVER_COCK, 1F, 0.75F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .on(RELOAD)
                                .at(0, cock)
                                .at(5, remove)
                                .at(10, eject(HANGMAN_CASING, CAPACITY))
                                .at(25, close)
                                .at(35, recock)
                                .on(INSPECT)
                                .at(16, GunAnimationActions::pistolWhip)
                                .on(JAMMED)
                                .at(10, cock)
                                .at(15, remove)
                                .at(20, close)
                                .at(25, recock);
                    });

    public static final GunAnimationTimeline MARESLEG =
            GunAnimationTimeline.of(maresleg(eject(MARESLEG_CASING)));

    public static final GunAnimationTimeline MARESLEG_SHORT =
            GunAnimationTimeline.of(maresleg(eject(SHORT_CASING)));

    public static final GunAnimationTimeline MARESLEG_AKIMBO =
            GunAnimationTimeline.of(
                    maresleg(byHand(eject(SHORT_LEFT_CASING), eject(SHORT_CASING))));

    public static final GunAnimationTimeline GREASEGUN =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_PISTOL_COCK, 1F, 0.8F);
                        t.on(EQUIP)
                                .at(5, sound(GUN_LATCH_OPEN))
                                .on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(2, eject(GREASEGUN_CASING))
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 0.8F))
                                .at(11, cock)
                                .on(RELOAD)
                                .at(2, sound(GUN_MAG_REMOVE))
                                .at(24, sound(GUN_MAG_INSERT))
                                .at(36, cock)
                                .on(INSPECT)
                                .at(5, sound(GUN_REVOLVER_COCK, 1F, 0.8F))
                                .at(26, sound(GUN_MAG_SMALL_INSERT, 1F, 1.25F))
                                .on(JAMMED)
                                .at(cock, 11, 26);
                    });

    public static final GunAnimationTimeline GREASEGUN_CLEAN =
            GREASEGUN.with(t -> t.replace(CYCLE).at(1, eject(GREASEGUN_CASING)));

    public static final GunAnimationTimeline UZI = GunAnimationTimeline.of(uzi(eject(UZI_CASING)));

    public static final GunAnimationTimeline UZI_AKIMBO =
            GunAnimationTimeline.of(uzi(byHand(eject(UZI_LEFT_CASING), eject(UZI_RIGHT_CASING))));

    public static final GunAnimationTimeline COILGUN =
            GunAnimationTimeline.of(t -> t.on(RELOAD).at(0, sound(GUN_COIL_RELOAD)));

    public static final GunAnimationTimeline NI4NI =
            COILGUN.with(t -> t.on(CYCLE).at(0, MUZZLE_FLASH));

    public static final GunAnimationTimeline DOUBLE_BARREL =
            GunAnimationTimeline.of(
                    t -> {
                        var open = sound(GUN_REVOLVER_COCK, 1F, 0.75F);
                        var close = sound(GUN_REVOLVER_CLOSE, 1F, 0.8F);
                        t.on(RELOAD)
                                .at(5, open)
                                .at(12, eject(DOUBLE_BARREL_CASING, FIRED))
                                .at(19, sound(GUN_MAG_SMALL_INSERT, 1F, 0.9F))
                                .at(29, close)
                                .on(INSPECT)
                                .at(5, open)
                                .at(19, close)
                                .on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .on(CYCLE_DRY)
                                .at(2, sound(GUN_DRY_FIRE));
                    });

    public static final GunAnimationTimeline LAUNCHER =
            GunAnimationTimeline.of(
                    t ->
                            t.on(CYCLE)
                                    .at(0, MUZZLE_FLASH)
                                    .on(RELOAD)
                                    .at(30, sound(GUN_CANISTER_INSERT)));

    public static final GunAnimationTimeline STINGER =
            LAUNCHER.with(
                    t ->
                            t.client(GunAnimationActions::lockOnTone)
                                    .always(GunAnimationActions::lockOnNoise));

    public static final GunAnimationTimeline MINIGUN =
            GunAnimationTimeline.of(minigun(eject(MINIGUN_CASING, MINIGUN_ROUNDS)));

    public static final GunAnimationTimeline MINIGUN_DUAL =
            GunAnimationTimeline.of(
                    minigun(
                            byHand(
                                    eject(MINIGUN_LEFT_CASING, MINIGUN_ROUNDS),
                                    eject(MINIGUN_RIGHT_CASING, MINIGUN_ROUNDS))));

    public static final GunAnimationTimeline MAS36 =
            GunAnimationTimeline.of(
                    t ->
                            bolt(t, MAS36_CASING)
                                    .on(EQUIP)
                                    .at(10, sound(GUN_LATCH_OPEN))
                                    .at(18, sound(GUN_REVOLVER_CLOSE))
                                    .on(RELOAD)
                                    .at(0, sound(GUN_BOLT_OPEN))
                                    .at(20, sound(GUN_RIFLE_COCK))
                                    .at(36, sound(GUN_BOLT_CLOSE))
                                    .on(INSPECT)
                                    .at(0, sound(GUN_BOLT_OPEN, 0.5F, 1F))
                                    .at(17, sound(GUN_BOLT_CLOSE, 0.5F, 1F)));

    public static final GunAnimationTimeline AMAT =
            GunAnimationTimeline.of(
                    t ->
                            bolt(t, AMAT_CASING)
                                    .on(EQUIP)
                                    .at(10, sound(GUN_REVOLVER_COCK, 0.5F, 1.25F))
                                    .at(15, sound(GUN_REVOLVER_CLOSE, 0.5F, 1.25F))
                                    .on(RELOAD)
                                    .at(2, sound(GUN_MAG_REMOVE))
                                    .at(20, sound(GUN_MAG_INSERT))
                                    .at(32, sound(GUN_BOLT_OPEN, 0.5F, 1F))
                                    .at(41, sound(GUN_BOLT_CLOSE, 0.5F, 1F))
                                    .on(INSPECT)
                                    .at(0, sound(GUN_REVOLVER_COCK, 0.5F, 1F))
                                    .at(45, sound(GUN_REVOLVER_CLOSE, 0.5F, 1F)));

    public static final GunAnimationTimeline LIBERATOR =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_REVOLVER_COCK, 1F, 0.75F);
                        var close = sound(GUN_REVOLVER_CLOSE, 1F, 0.9F);
                        var load = sound(GUN_MAG_SMALL_INSERT);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .on(RELOAD)
                                .at(0, cock)
                                .at(4, eject(LIBERATOR_CASING, FIRED))
                                .at(15, load)
                                .on(RELOAD_CYCLE)
                                .at(5, load)
                                .on(RELOAD_END)
                                .at(2, close)
                                .on(JAMMED)
                                .at(2, close)
                                .at(12, cock)
                                .at(26, close)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .on(INSPECT)
                                .at(0, cock)
                                .at(
                                        4,
                                        when(
                                                HAS_FIRED,
                                                eject(LIBERATOR_INSPECT_CASING, FIRED),
                                                FORGET_REFILL))
                                .at(20, close);
                    });

    public static final GunAnimationTimeline SPAS =
            GunAnimationTimeline.of(
                    t -> {
                        var pump = sound(GUN_SHOTGUN_COCK);
                        var load = sound(GUN_SHOTGUN_LOAD);
                        t.on(CYCLE, ALT_CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(8, pump)
                                .at(10, eject(SPAS_CASING))
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .at(8, pump)
                                .on(RELOAD)
                                .at(0, when(EMPTY, sound(GUN_REVOLVER_COCK)))
                                .at(5, load)
                                .at(7, when(EMPTY, sound(GUN_REVOLVER_CLOSE)))
                                .on(RELOAD_CYCLE)
                                .at(5, load)
                                .on(INSPECT)
                                .at(5, sound(GUN_SHOTGUN_OPEN))
                                .at(18, sound(GUN_SHOTGUN_CLOSE))
                                .on(JAMMED)
                                .at(sound(GUN_WHACK), 18, 25)
                                .at(29, sound(GUN_SHOTGUN_CLOSE));
                    });

    public static final GunAnimationTimeline SHREDDER =
            GunAnimationTimeline.of(
                    t -> {
                        var cycle = sound(GUN_SHREDDER_CYCLE, 0.25F, 1.5F);
                        var remove = sound(GUN_MAG_REMOVE);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(2, cycle)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .at(2, cycle)
                                .on(RELOAD)
                                .at(2, remove)
                                .at(32, sound(GUN_MAG_INSERT))
                                .on(INSPECT)
                                .at(2, remove)
                                .at(28, sound(GUN_MAG_INSERT));
                    });

    public static final GunAnimationTimeline SHREDDER_SEXY =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_REVOLVER_COCK);
                        var close = sound(GUN_REVOLVER_CLOSE, 1F, 0.75F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH, SLOW_EQUESTRIAN)
                                .at(2, eject(SEXY_CASING))
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .on(RELOAD)
                                .at(0, cock)
                                .at(4, close)
                                .at(16, sound(GUN_MAG_SMALL_REMOVE))
                                .at(30, sound(GUN_MAG_REMOVE))
                                .at(55, sound(GUN_IMPACT, 0.5F, 1F), LOAD)
                                .at(65, sound(GUN_MAG_INSERT))
                                .at(74, sound(GUN_MAG_SMALL_INSERT))
                                .at(88, close)
                                .at(100, cock)
                                .on(INSPECT)
                                .at(sound(PLAYER_GULP), 20, 25, 30, 35)
                                .at(50, sound(PLAYER_GROAN))
                                .at(60, BINGE);
                    });

    public static final GunAnimationTimeline TAU =
            GunAnimationTimeline.of(
                    t ->
                            t.client(GunAnimationActions::tauSpinTone)
                                    .on(CYCLE)
                                    .at(0, sound(GUN_TAU_FIRE, 0.5F, uniform(0.9F, 0.2F)))
                                    .on(ALT_CYCLE)
                                    .at(0, sound(GUN_TAU_FIRE, 0.5F, uniform(0.7F, 0.2F)))
                                    .on(SPINUP)
                                    .at(0, NOISE)
                                    .during(1, 300, RUNNING_NOISE)
                                    .every(10, 130, GunAnimationActions::drawCharge)
                                    .from(201, GunAnimationActions::overcharge));

    public static final GunAnimationTimeline PEPPERBOX =
            GunAnimationTimeline.of(
                    t -> {
                        var spin = sound(GUN_REVOLVER_SPIN);
                        var cock = sound(GUN_REVOLVER_COCK, 1F, 0.6F);
                        t.on(RELOAD)
                                .at(24, sound(GUN_MAG_SMALL_INSERT))
                                .at(55, spin)
                                .on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(21, cock)
                                .on(CYCLE_DRY)
                                .at(2, sound(GUN_DRY_FIRE, 1F, 0.8F))
                                .at(11, cock)
                                .on(INSPECT)
                                .at(3, spin)
                                .on(JAMMED)
                                .at(28, sound(GUN_DRY_FIRE, 1F, 0.75F))
                                .at(45, sound(GUN_DRY_FIRE, 1F, 0.6F));
                    });

    public static final GunAnimationTimeline BOLTER =
            GunAnimationTimeline.of(
                    t ->
                            t.on(CYCLE)
                                    .at(1, eject(BOLTER_CASING))
                                    .on(RELOAD)
                                    .at(5, sound(GUN_MAG_REMOVE))
                                    .at(26, sound(GUN_MAG_INSERT)));

    public static final GunAnimationTimeline AM180 =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_PISTOL_COCK, 1F, 0.9F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH, eject(AM180_CASING))
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .at(6, cock)
                                .on(RELOAD)
                                .at(2, legacySound(GUN_MAG_REMOVE, 1F, 1F))
                                .at(20, legacySound(GUN_IMPACT, 0.25F, 1F))
                                .at(32, legacySound(GUN_MAG_INSERT, 1F, 1F))
                                .at(40, legacySound(GUN_PISTOL_COCK, 1F, 0.9F))
                                .at(6, modernSound(GUN_MAG_REMOVE, 1F, 1F))
                                .at(26, modernSound(GUN_IMPACT, 0.25F, 1F))
                                .at(48, modernSound(GUN_MAG_INSERT, 1F, 1F))
                                .at(54, modernSound(GUN_PISTOL_COCK, 1F, 0.9F))
                                .on(JAMMED)
                                .at(15, legacySound(GUN_PISTOL_COCK, 1F, 0.8F))
                                .at(6, modernSound(GUN_PISTOL_COCK, 1F, 0.8F))
                                .at(20, modernSound(GUN_PISTOL_COCK, 1F, 1F))
                                .on(INSPECT)
                                .at(2, legacySound(GUN_MAG_REMOVE, 1F, 1F))
                                .at(35, legacySound(GUN_MAG_INSERT, 1F, 1F))
                                .at(6, modernSound(GUN_MAG_REMOVE, 1F, 1F))
                                .at(53, modernSound(GUN_MAG_INSERT, 1F, 1F));
                    });

    public static final GunAnimationTimeline STAR_F =
            GunAnimationTimeline.of(starF(eject(STAR_F_CASING)));

    public static final GunAnimationTimeline STAR_F_AKIMBO =
            GunAnimationTimeline.of(starF(byHand(eject(STAR_F_LEFT_CASING), eject(STAR_F_CASING))));

    public static final GunAnimationTimeline M2 =
            GunAnimationTimeline.of(
                    t ->
                            t.on(EQUIP)
                                    .at(0, sound(TURRET_CIWS_RELOAD))
                                    .on(CYCLE)
                                    .at(0, MUZZLE_FLASH, eject(M2_CASING)));

    public static final GunAnimationTimeline G3 =
            GunAnimationTimeline.of(
                    t -> {
                        var close = sound(GUN_REVOLVER_CLOSE);
                        var softClose = sound(GUN_REVOLVER_CLOSE, 1F, 0.9F);
                        t.on(CYCLE)
                                .at(0, eject(G3_CASING), MUZZLE_FLASH)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 0.8F))
                                .at(5, sound(GUN_PISTOL_COCK, 1F, 0.9F))
                                .on(RELOAD)
                                .at(2, sound(GUN_MAG_REMOVE))
                                .at(4, softClose)
                                .at(32, sound(GUN_MAG_INSERT))
                                .at(36, close)
                                .on(INSPECT)
                                .at(2, sound(GUN_MAG_REMOVE))
                                .at(28, sound(GUN_MAG_INSERT))
                                .on(JAMMED)
                                .at(softClose, 16, 26)
                                .at(close, 20, 30);
                    });

    public static final GunAnimationTimeline STG77 =
            GunAnimationTimeline.of(
                    t -> {
                        var tap = sound(GUN_DRY_FIRE, 0.25F, 1.25F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH, eject(STG77_CASING))
                                .at(40, tap)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 0.8F))
                                .at(5, sound(GUN_PISTOL_COCK, 1F, 0.9F))
                                .at(40, tap)
                                .on(RELOAD, INSPECT)
                                .at(0, sound(GUN_REVOLVER_CLOSE, 1F, 0.9F))
                                .on(RELOAD)
                                .at(10, legacySound(GUN_MAG_REMOVE, 1F, 1F))
                                .at(24, legacySound(GUN_MAG_INSERT, 1F, 1F))
                                .at(34, legacySound(GUN_REVOLVER_CLOSE, 1F, 1F))
                                .at(16, modernSound(GUN_MAG_REMOVE, 1F, 1F))
                                .at(32, modernSound(GUN_IMPACT, 0.25F, 1.25F))
                                .at(38, modernSound(GUN_MAG_INSERT, 1F, 1F))
                                .at(43, modernSound(GUN_REVOLVER_CLOSE, 1F, 1F))
                                .on(INSPECT)
                                .at(10, legacySound(GUN_MAG_SMALL_REMOVE, 1F, 1F))
                                .at(114, legacySound(GUN_MAG_SMALL_INSERT, 1F, 1F))
                                .at(124, legacySound(GUN_REVOLVER_CLOSE, 1F, 1F))
                                .at(11, modernSound(GUN_MAG_SMALL_REMOVE, 1F, 1F))
                                .at(72, modernSound(GUN_MAG_SMALL_INSERT, 1F, 1F))
                                .at(84, modernSound(GUN_REVOLVER_CLOSE, 1F, 1F));
                    });

    public static final GunAnimationTimeline LAG =
            GunAnimationTimeline.of(
                    t ->
                            t.on(CYCLE)
                                    .at(0, MUZZLE_FLASH)
                                    .at(1, eject(LAG_CASING))
                                    .on(CYCLE_DRY)
                                    .at(0, sound(GUN_DRY_FIRE))
                                    .at(8, sound(GUN_REVOLVER_COCK))
                                    .on(RELOAD)
                                    .at(8, sound(GUN_MAG_REMOVE))
                                    .at(26, sound(GUN_MAG_INSERT))
                                    .at(40, sound(GUN_PISTOL_COCK))
                                    .on(JAMMED)
                                    .at(8, sound(GUN_MAG_REMOVE))
                                    .at(20, sound(GUN_IMPACT, 0.5F, 1.6F))
                                    .at(36, sound(GUN_MAG_INSERT)));

    public static final GunAnimationTimeline FLAREGUN =
            GunAnimationTimeline.of(
                    t -> {
                        var open = sound(GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
                        var cock = sound(GUN_REVOLVER_COCK);
                        t.on(RELOAD)
                                .at(0, open)
                                .at(4, when(REFILLED, eject(FLARE_CASING), FORGET_PRE_RELOAD))
                                .at(16, sound(GUN_CANISTER_INSERT))
                                .at(24, sound(GUN_MAG_SMALL_INSERT))
                                .on(JAMMED)
                                .at(10, open)
                                .at(29, sound(GUN_MAG_SMALL_INSERT))
                                .on(CYCLE)
                                .at(12, cock)
                                .on(CYCLE_DRY)
                                .at(2, sound(GUN_DRY_FIRE))
                                .at(12, cock);
                    });

    public static final GunAnimationTimeline CONGOLAKE =
            GunAnimationTimeline.of(
                    t ->
                            t.on(CYCLE)
                                    .at(0, MUZZLE_FLASH)
                                    .at(15, eject(CONGOLAKE_CASING))
                                    .on(RELOAD, RELOAD_CYCLE)
                                    .at(0, sound(GUN_GRENADE_RELOAD))
                                    .on(INSPECT)
                                    .at(9, sound(GUN_GRENADE_OPEN))
                                    .at(27, sound(GUN_GRENADE_CLOSE)));

    public static final GunAnimationTimeline MK108 =
            GunAnimationTimeline.of(
                    t -> {
                        var close = sound(GUN_REVOLVER_CLOSE, 1F, 0.65F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .at(2, eject(MK108_CASING))
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 0.75F))
                                .on(RELOAD)
                                .at(0, close)
                                .at(10, sound(GUN_MAG_SMALL_REMOVE, 1F, 0.75F))
                                .at(40, sound(GUN_MAG_REMOVE, 1F, 0.75F))
                                .at(60, sound(GUN_IMPACT, 0.5F, 1F), LOAD)
                                .at(90, sound(GUN_MAG_INSERT, 1F, 0.75F))
                                .at(100, sound(GUN_MAG_SMALL_INSERT, 1F, 0.75F))
                                .at(125, close)
                                .on(INSPECT)
                                .at(sound(GUN_IMPACT, 0.5F, 1.5F), 9, 14, 19);
                    });

    public static final GunAnimationTimeline FATMAN =
            GunAnimationTimeline.of(t -> t.on(RELOAD).at(0, sound(GUN_FATMAN_RELOAD)));

    public static final GunAnimationTimeline FLAMER_DAYBREAKER =
            GunAnimationTimeline.of(GunAnimationActions::refuel);

    public static final GunAnimationTimeline CHEMTHROWER =
            GunAnimationTimeline.of(
                    t ->
                            t.client(GunAnimationActions::flameTone)
                                    .on(CYCLE)
                                    .at(0, NOISE)
                                    .during(1, 5, RUNNING_NOISE));

    public static final GunAnimationTimeline FLAMER = CHEMTHROWER.with(GunAnimationActions::refuel);

    public static final GunAnimationTimeline DRILL =
            GunAnimationTimeline.of(
                    t ->
                            refuel(t)
                                    .client(GunAnimationActions::engineTone)
                                    .on(CYCLE, CYCLE_DRY)
                                    .at(0, NOISE));

    public static final GunAnimationTimeline TESLA =
            GunAnimationTimeline.of(
                    t -> {
                        var cycle = sound(GUN_SHREDDER_CYCLE, 0.25F, 1.25F);
                        t.on(CYCLE)
                                .at(2, cycle)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE))
                                .at(2, cycle)
                                .on(INSPECT)
                                .at(12, sound(BLOCK_PLUSHY, 0.25F, 1F));
                    });

    public static final GunAnimationTimeline LASER_PISTOL =
            GunAnimationTimeline.of(
                    t -> {
                        var cock = sound(GUN_REVOLVER_COCK);
                        var close = sound(GUN_REVOLVER_CLOSE, 1F, 1.25F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 1.5F))
                                .on(RELOAD)
                                .at(0, cock)
                                .at(10, sound(GUN_MAG_SMALL_REMOVE, 1F, 1.25F))
                                .at(34, sound(GUN_MAG_SMALL_INSERT, 1F, 1.25F))
                                .at(40, close)
                                .on(JAMMED)
                                .at(10, cock)
                                .at(15, close)
                                .at(30, sound(GUN_IMPACT, 0.25F, 1.5F));
                    });

    public static final GunAnimationTimeline LASRIFLE =
            GunAnimationTimeline.of(
                    t -> {
                        var remove = sound(GUN_MAG_SMALL_REMOVE);
                        var insert = sound(GUN_MAG_INSERT);
                        var close = sound(GUN_REVOLVER_CLOSE);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 1.5F))
                                .on(RELOAD)
                                .at(2, remove)
                                .at(18, sound(GUN_IMPACT, 0.25F, 1F))
                                .at(30, insert)
                                .at(38, close)
                                .on(INSPECT)
                                .at(2, remove)
                                .at(12, insert)
                                .at(20, close)
                                .on(JAMMED)
                                .at(2, remove)
                                .at(22, insert)
                                .at(30, close);
                    });

    public static final GunAnimationTimeline FIREEXT =
            GunAnimationTimeline.of(t -> t.on(RELOAD).at(0, sound(GUN_VALVE)));

    public static final GunAnimationTimeline CHARGE_THROWER =
            GunAnimationTimeline.of(
                    t ->
                            t.on(CYCLE_DRY)
                                    .at(0, when(NO_HOOK, sound(GUN_DRY_FIRE, 1F, 0.75F)))
                                    .on(RELOAD)
                                    .at(30, sound(GUN_ROCKET_INSERT))
                                    .at(40, sound(GUN_BOLT_CLOSE)));

    public static final GunAnimationTimeline FOLLY =
            GunAnimationTimeline.of(
                    t -> {
                        var screw = sound(GUN_SCREW);
                        t.on(RELOAD).at(20, screw).at(80, sound(GUN_ROCKET_INSERT)).at(120, screw);
                    });

    public static final GunAnimationTimeline ABERRATOR =
            GunAnimationTimeline.of(aberrator(eject(ABERRATOR_CASING)));

    public static final GunAnimationTimeline ABERRATOR_EOTT =
            GunAnimationTimeline.of(
                    aberrator(byHand(eject(ABERRATOR_LEFT_CASING), eject(ABERRATOR_CASING))));

    public static final GunAnimationTimeline MISSILE_LAUNCHER =
            GunAnimationTimeline.of(
                    t -> {
                        var open = sound(GUN_BOLT_OPEN, 1F, 0.9F);
                        var close = sound(GUN_BOLT_CLOSE, 1F, 0.9F);
                        t.on(CYCLE)
                                .at(0, MUZZLE_FLASH)
                                .on(CYCLE_DRY)
                                .at(0, sound(GUN_DRY_FIRE, 1F, 1.25F))
                                .on(RELOAD)
                                .at(0, open)
                                .at(30, sound(GUN_CANISTER_INSERT))
                                .at(42, close)
                                .on(JAMMED, INSPECT)
                                .at(0, open)
                                .at(27, close);
                    });

    private static Consumer<Score> revolver(CasingEjection casing) {
        return t -> {
            var cock = sound(GUN_REVOLVER_COCK);
            var close = sound(GUN_REVOLVER_CLOSE);
            t.on(RELOAD)
                    .at(3, cock)
                    .at(10, sound(GUN_MAG_SMALL_REMOVE))
                    .at(16, eject(casing, CAPACITY))
                    .at(34, sound(GUN_MAG_SMALL_INSERT))
                    .at(40, close)
                    .on(CYCLE)
                    .at(0, MUZZLE_FLASH)
                    .at(11, cock)
                    .on(CYCLE_DRY)
                    .at(2, sound(GUN_DRY_FIRE))
                    .at(11, cock)
                    .on(INSPECT)
                    .at(3, cock)
                    .at(16, close);
        };
    }

    private static Consumer<Score> maresleg(GunAnimationAction casing) {
        return t -> {
            var cock = sound(GUN_REVOLVER_COCK, 1F, 0.7F);
            var lever = sound(GUN_LEVER_COCK, 1F, 0.8F);
            var load = sound(GUN_SHOTGUN_LOAD);
            t.on(RELOAD)
                    .at(8, sound(GUN_REVOLVER_COCK, 1F, 0.8F))
                    .at(16, load)
                    .on(RELOAD_CYCLE)
                    .at(0, load)
                    .on(RELOAD_END)
                    .at(2, cock)
                    .on(JAMMED)
                    .at(2, cock)
                    .at(lever, 17, 29)
                    .on(CYCLE)
                    .at(0, MUZZLE_FLASH)
                    .at(8, lever)
                    .at(14, casing)
                    .on(CYCLE_DRY)
                    .at(2, sound(GUN_DRY_FIRE))
                    .at(8, lever);
        };
    }

    private static Consumer<Score> uzi(GunAnimationAction casing) {
        return t -> {
            var cock = sound(GUN_PISTOL_COCK);
            t.on(EQUIP)
                    .at(8, sound(GUN_LATCH_OPEN, 1F, 1.25F))
                    .on(CYCLE)
                    .at(0, MUZZLE_FLASH)
                    .at(1, casing)
                    .on(CYCLE_DRY)
                    .at(0, sound(GUN_DRY_FIRE))
                    .at(8, cock)
                    .on(RELOAD)
                    .at(4, sound(GUN_MAG_REMOVE))
                    .at(26, sound(GUN_MAG_INSERT))
                    .at(36, cock)
                    .on(JAMMED)
                    .at(cock, 17, 31);
        };
    }

    private static Consumer<Score> minigun(GunAnimationAction casings) {
        return t -> {
            var spin = sound(GUN_REVOLVER_SPIN, 1F, 0.75F);
            t.on(CYCLE)
                    .at(0, MUZZLE_FLASH, casings)
                    .at(1, when(MOD_207.negate(), spin))
                    .at(3, when(MOD_207, spin))
                    .on(CYCLE_DRY)
                    .at(0, sound(GUN_DRY_FIRE, 1F, 0.75F))
                    .at(1, spin)
                    .on(RELOAD, INSPECT)
                    .at(0, spin);
        };
    }

    private static Score bolt(Score t, CasingEjection casing) {
        var open = sound(GUN_BOLT_OPEN, 0.5F, 1F);
        var close = sound(GUN_BOLT_CLOSE, 0.5F, 1F);
        return t.on(CYCLE)
                .at(0, MUZZLE_FLASH)
                .at(7, open)
                .at(12, eject(casing))
                .at(16, close)
                .on(CYCLE_DRY)
                .at(0, sound(GUN_DRY_FIRE, 1F, 0.75F))
                .at(7, open)
                .at(16, close)
                .on(JAMMED)
                .at(open, 5, 16)
                .at(close, 12, 23);
    }

    private static Consumer<Score> starF(GunAnimationAction casing) {
        return t -> {
            var close = sound(GUN_REVOLVER_CLOSE);
            var bright = sound(GUN_REVOLVER_CLOSE, 1F, 1.1F);
            t.on(CYCLE)
                    .at(0, casing, MUZZLE_FLASH)
                    .on(CYCLE_DRY)
                    .at(0, sound(GUN_DRY_FIRE, 1F, 0.9F))
                    .at(5, sound(GUN_PISTOL_COCK, 1F, 1.1F))
                    .on(RELOAD)
                    .at(5, close, sound(GUN_MAG_REMOVE))
                    .at(22, sound(GUN_MAG_INSERT))
                    .at(30, bright)
                    .on(JAMMED)
                    .at(close, 15, 23)
                    .at(bright, 19, 27)
                    .on(INSPECT)
                    .at(7, close)
                    .at(30, bright);
        };
    }

    private static Consumer<Score> aberrator(GunAnimationAction casing) {
        return t -> {
            var cock = sound(GUN_PISTOL_COCK, 1F, 0.75F);
            t.on(RELOAD)
                    .at(5, sound(GUN_MAG_SMALL_REMOVE, 1F, 0.75F))
                    .at(32, sound(GUN_MAG_SMALL_INSERT, 1F, 0.75F))
                    .at(42, cock)
                    .on(CYCLE)
                    .at(0, MUZZLE_FLASH)
                    .at(1, casing)
                    .on(CYCLE_DRY)
                    .at(1, sound(GUN_DRY_FIRE, 1F, 0.75F))
                    .at(9, cock);
        };
    }

    private static Score refuel(Score t) {
        return t.on(RELOAD)
                .at(15, sound(GUN_LATCH_OPEN))
                .at(35, sound(GUN_IMPACT, 0.5F, 1F))
                .at(60, sound(GUN_REVOLVER_CLOSE, 1F, 0.75F))
                .at(70, sound(GUN_CANISTER_INSERT))
                .at(85, sound(GUN_VALVE));
    }

    private static void pistolWhip(ItemStack stack, LambdaContext ctx) {
        var player = ctx.getPlayer();
        if (player == null) return;
        var shooter = ctx.entity();
        var target = EntityTargeting.getMouseOver(player, 3.0D);
        if (target instanceof EntityHitResult hit) {
            var victim = hit.getEntity();
            if (victim.level() instanceof ServerLevel level) {
                victim.hurtServer(level, player.damageSources().playerAttack(player), 10F);
            }
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(2, 1, 2));
            EntitySounds.play(
                    victim,
                    victim.getX(),
                    victim.getY(),
                    victim.getZ(),
                    GUN_SMACK.get(),
                    SoundSource.PLAYERS,
                    1F,
                    0.9F + shooter.getRandom().nextFloat() * 0.2F);
        } else if (target instanceof BlockHitResult hit) {
            var at = hit.getLocation();
            var step =
                    shooter.level().getBlockState(hit.getBlockPos()).getSoundType().getStepSound();
            WorldSounds.play(
                    shooter.level(),
                    null,
                    at.x,
                    at.y,
                    at.z,
                    step,
                    SoundSource.PLAYERS,
                    2F,
                    0.9F + shooter.getRandom().nextFloat() * 0.2F);
        }
    }

    private static void drawCharge(ItemStack stack, LambdaContext ctx) {
        var mag = magazine(stack, ctx);
        if (mag.getAmount(stack, ctx.inventory()) <= 0) {
            GunBaseNTItem.playAnimation(ctx.getPlayer(), stack, CYCLE_DRY, ctx.configIndex());
        } else {
            mag.consumeAmmo(stack, ctx.inventory(), 1);
        }
    }

    private static void overcharge(ItemStack stack, LambdaContext ctx) {
        var shooter = ctx.entity();
        int config = ctx.configIndex();
        GunBaseNTItem.playAnimation(ctx.getPlayer(), stack, CYCLE_DRY, config);
        var level = (ServerLevel) shooter.level();
        var blast =
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.TAU_BLAST);
        shooter.hurtServer(level, new DamageSource(blast), 1_000F);
        GunBaseNTItem.setWear(
                stack,
                config,
                Math.min(
                        GunBaseNTItem.getWear(stack, config) + 10_000F,
                        ctx.config().getDurability(stack)));
        double eyes = shooter.getY() + shooter.getEyeHeight();
        EntitySounds.play(
                shooter,
                shooter.getX(),
                eyes,
                shooter.getZ(),
                UFO_BLAST.get(),
                SoundSource.PLAYERS,
                5.0F,
                0.9F);
        EntitySounds.play(
                shooter,
                shooter.getX(),
                eyes,
                shooter.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS,
                5.0F,
                0.5F);
        float yaw = shooter.getRandom().nextFloat() * 180F;
        var around = new TargetPoint(level, shooter.getX(), eyes, shooter.getZ(), 100);
        for (int ring = -1; ring <= 1; ring++) {
            PacketWire.sendToAllAround(
                    PlasmaBlastLayout.encode(
                            shooter.getX(),
                            eyes,
                            shooter.getZ(),
                            1.0F,
                            0.8F,
                            0.5F,
                            60F * ring,
                            yaw,
                            2F),
                    around);
        }
    }

    private static void lockOnNoise(ItemStack stack, LambdaContext ctx) {
        int progress = GunStingerItem.getLockonProgress(stack);
        if (progress > 0 && !GunBaseNTItem.getIsLockedOn(stack))
            WorldSounds.entityLoop(ctx.entity(), progress == 1);
    }

    private static void lockOnTone(ItemStack stack, LambdaContext ctx) {
        var shooter = ctx.entity();
        if (GunStingerItem.getLockonProgress(stack) <= 0 || GunBaseNTItem.getIsLockedOn(stack)) {
            stopLoop(shooter);
            return;
        }
        Supplier<SoundHandle> start =
                () ->
                        AudioSystem.getLoopedSound(
                                GUN_LOCKON.get(),
                                SoundSource.PLAYERS,
                                (float) shooter.getX(),
                                (float) shooter.getY(),
                                (float) shooter.getZ(),
                                1F,
                                15F,
                                1F,
                                10);
        sustainLoop(
                shooter,
                start,
                false,
                loop -> {
                    loop.refresh();
                    loop.moveTo(
                            (float) shooter.getX(), (float) shooter.getY(), (float) shooter.getZ());
                });
    }

    private static void tauSpinTone(ItemStack stack, LambdaContext ctx) {
        var shooter = ctx.entity();
        int timer = GunClientState.getAnimationAge(ctx.configIndex());
        if (GunClientState.getLastAnimation(ctx.configIndex()) != SPINUP || timer >= 300) {
            stopLoop(shooter);
            return;
        }
        Supplier<SoundHandle> start =
                () -> {
                    var loop =
                            AudioSystem.getLoopedSound(
                                    GUN_TAU_LOOP.get(),
                                    SoundSource.PLAYERS,
                                    (float) shooter.getX(),
                                    (float) shooter.getY(),
                                    (float) shooter.getZ(),
                                    1F,
                                    15F,
                                    0.75F,
                                    10);
                    if (loop != null) loop.setPitch(0.75F);
                    return loop;
                };
        sustainLoop(
                shooter,
                start,
                true,
                loop -> {
                    loop.refresh();
                    loop.follow(shooter);
                    loop.setPitch(0.75F + timer * 0.01F);
                });
    }

    private static void flameTone(ItemStack stack, LambdaContext ctx) {
        var shooter = ctx.entity();
        int config = ctx.configIndex();
        if (GunClientState.getLastAnimation(config) != CYCLE
                || GunClientState.getAnimationAge(config) >= 5) {
            stopLoop(shooter);
            return;
        }
        Supplier<SoundHandle> start =
                () ->
                        AudioSystem.getLoopedSound(
                                GUN_FLAMER_LOOP.get(),
                                SoundSource.PLAYERS,
                                (float) shooter.getX(),
                                (float) shooter.getY(),
                                (float) shooter.getZ(),
                                1F,
                                15F,
                                1F,
                                10);
        sustainLoop(
                shooter,
                start,
                true,
                loop -> {
                    loop.refresh();
                    loop.follow(shooter);
                });
    }

    private static void engineTone(ItemStack stack, LambdaContext ctx) {
        var shooter = ctx.entity();
        double speed = HeldItemAnimations.getChannelValue("SPEED", 0);
        if (speed > 0) {
            Supplier<SoundHandle> start =
                    () -> {
                        var sound =
                                XWeaponModManager.hasUpgrade(
                                                stack,
                                                ctx.configIndex(),
                                                XWeaponModManager.ID_ENGINE_ELECTRIC)
                                        ? LARGE_TURBINE_LOOP
                                        : ENGINE_LOOP;
                        return AudioSystem.getLoopedSound(
                                sound.get(),
                                SoundSource.PLAYERS,
                                (float) shooter.getX(),
                                (float) shooter.getY(),
                                (float) shooter.getZ(),
                                (float) speed,
                                15F,
                                (float) speed,
                                25);
                    };
            sustainLoop(
                    shooter,
                    start,
                    true,
                    loop -> {
                        loop.refresh();
                        loop.setVolume((float) speed);
                        loop.setPitch((float) speed);
                    });
        }
        var animation = GunBaseNTItem.getLastAnim(stack, ctx.configIndex());
        if (animation != CYCLE && animation != CYCLE_DRY) stopLoop(shooter);
    }

    private static void sustainLoop(
            LivingEntity shooter,
            Supplier<SoundHandle> start,
            boolean follow,
            Consumer<SoundHandle> refresh) {
        var playing = GunBaseNTItem.loopedSounds.get(shooter);
        if (playing != null && playing.isPlaying()) {
            refresh.accept(playing);
            return;
        }
        var loop = start.get();
        if (loop == null) return;
        GunBaseNTItem.loopedSounds.put(shooter, loop);
        loop.start();
        if (follow) loop.follow(shooter);
    }

    private static void stopLoop(LivingEntity shooter) {
        var playing = GunBaseNTItem.loopedSounds.get(shooter);
        if (playing != null && playing.isPlaying()) playing.stop();
    }
}
