/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.sound.EntitySounds;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GunStateDecider {
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_DECIDER =
            (stack, ctx) -> {
                int index = ctx.configIndex();
                GunState lastState = GunBaseNTItem.getState(stack, index);
                deciderStandardFinishDraw(stack, lastState, index);
                deciderStandardClearJam(stack, lastState, index);
                deciderStandardReload(stack, ctx, lastState, 0, index);
                deciderAutoRefire(
                        stack,
                        ctx,
                        lastState,
                        0,
                        index,
                        () -> {
                            return GunBaseNTItem.getPrimary(stack, index)
                                    && GunBaseNTItem.getMode(stack, ctx.configIndex()) == 0;
                        });
            };

    public static void deciderStandardFinishDraw(ItemStack stack, GunState lastState, int index) {

        if (lastState == GunState.DRAWING) {
            GunBaseNTItem.setState(stack, index, GunState.IDLE);
            GunBaseNTItem.setTimer(stack, index, 0);
        }
    }

    public static void deciderStandardClearJam(ItemStack stack, GunState lastState, int index) {

        if (lastState == GunState.JAMMED) {
            GunBaseNTItem.setState(stack, index, GunState.IDLE);
            GunBaseNTItem.setTimer(stack, index, 0);
        }
    }

    public static void deciderStandardReload(
            ItemStack stack, LambdaContext ctx, GunState lastState, int recIndex, int gunIndex) {

        if (lastState == GunState.RELOADING) {

            LivingEntity entity = ctx.entity();
            Player player = ctx.getPlayer();
            GunConfig cfg = ctx.config();
            Receiver rec = cfg.getReceivers(stack).get(recIndex);
            Magazine mag = rec.getMagazine(stack);

            mag.reloadAction(stack, ctx.inventory());
            boolean cancel = GunBaseNTItem.getReloadCancel(stack);

            if (!cancel && mag.canReload(stack, ctx.inventory())) {
                GunBaseNTItem.setState(stack, gunIndex, GunState.RELOADING);
                GunBaseNTItem.setTimer(stack, gunIndex, rec.getReloadCycleDuration(stack));
                GunBaseNTItem.playAnimation(player, stack, GunAnimation.RELOAD_CYCLE, gunIndex);
            } else {

                if (getStandardJamChance(stack, cfg, gunIndex) > entity.getRandom().nextFloat()) {
                    GunBaseNTItem.setState(stack, gunIndex, GunState.JAMMED);
                    GunBaseNTItem.setTimer(stack, gunIndex, rec.getJamDuration(stack));
                    GunBaseNTItem.playAnimation(player, stack, GunAnimation.JAMMED, gunIndex);
                } else {
                    GunBaseNTItem.setState(stack, gunIndex, GunState.DRAWING);
                    int duration =
                            rec.getReloadEndDuration(stack)
                                    + (mag.getAmountBeforeReload(stack) <= 0
                                            ? rec.getReloadCockOnEmptyPost(stack)
                                            : 0);
                    GunBaseNTItem.setTimer(stack, gunIndex, duration);
                    GunBaseNTItem.playAnimation(player, stack, GunAnimation.RELOAD_END, gunIndex);
                }

                GunBaseNTItem.setReloadCancel(stack, false);
            }

            mag.setAmountAfterReload(stack, mag.getAmount(stack, ctx.inventory()));
        }
    }

    public static float getStandardJamChance(ItemStack stack, GunConfig config, int index) {
        float percent = GunBaseNTItem.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.66F) return 0F;
        return Math.min((percent - 0.66F) * 4F, 1F);
    }

    public static void deciderAutoRefire(
            ItemStack stack,
            LambdaContext ctx,
            GunState lastState,
            int recIndex,
            int gunIndex,
            BooleanSupplier refireCondition) {

        if (lastState == GunState.COOLDOWN) {

            LivingEntity entity = ctx.entity();
            Player player = ctx.getPlayer();
            GunConfig cfg = ctx.config();
            Receiver rec = cfg.getReceivers(stack).get(recIndex);

            if (rec.getRefireOnHold(stack) && refireCondition.getAsBoolean()) {
                if (rec.getCanFire(stack).apply(stack, ctx)) {
                    rec.getOnFire(stack).accept(stack, ctx);
                    GunBaseNTItem.setState(stack, gunIndex, GunState.COOLDOWN);
                    GunBaseNTItem.setTimer(stack, gunIndex, rec.getDelayAfterFire(stack));

                    if (rec.getFireSound(stack) != null)
                        EntitySounds.play(
                                entity,
                                entity.getX(),
                                entity.getY(),
                                entity.getZ(),
                                rec.getFireSound(stack).get(),
                                SoundSource.PLAYERS,
                                rec.getFireVolume(stack),
                                rec.getFirePitch(stack));

                    int remaining = rec.getRoundsPerCycle(stack) - 1;
                    for (int i = 0; i < remaining; i++)
                        if (rec.getCanFire(stack).apply(stack, ctx))
                            rec.getOnFire(stack).accept(stack, ctx);
                } else if (rec.getDoesDryFireAfterAuto(stack)) {
                    GunBaseNTItem.setState(
                            stack,
                            gunIndex,
                            rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                    GunBaseNTItem.setTimer(stack, gunIndex, rec.getDelayAfterDryFire(stack));
                    GunBaseNTItem.playAnimation(player, stack, GunAnimation.CYCLE_DRY, gunIndex);
                } else {
                    GunBaseNTItem.setState(stack, gunIndex, GunState.IDLE);
                    GunBaseNTItem.setTimer(stack, gunIndex, 0);
                }
            } else {
                if (rec.getReloadOnEmpty(stack)
                        && rec.getMagazine(stack).getAmount(stack, ctx.inventory()) <= 0) {
                    GunBaseNTItem.setIsAiming(stack, false);
                    Magazine mag = rec.getMagazine(stack);

                    if (mag.canReload(stack, ctx.inventory())) {
                        int loaded = mag.getAmount(stack, ctx.inventory());
                        mag.setAmountBeforeReload(stack, loaded);
                        GunBaseNTItem.setState(stack, ctx.configIndex(), GunState.RELOADING);
                        GunBaseNTItem.setTimer(
                                stack,
                                ctx.configIndex(),
                                rec.getReloadBeginDuration(stack)
                                        + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
                        GunBaseNTItem.playAnimation(
                                player, stack, GunAnimation.RELOAD, ctx.configIndex());
                    } else {
                        GunBaseNTItem.setState(stack, gunIndex, GunState.IDLE);
                        GunBaseNTItem.setTimer(stack, gunIndex, 0);
                    }

                } else {
                    GunBaseNTItem.setState(stack, gunIndex, GunState.IDLE);
                    GunBaseNTItem.setTimer(stack, gunIndex, 0);
                }
            }
        }
    }
}
