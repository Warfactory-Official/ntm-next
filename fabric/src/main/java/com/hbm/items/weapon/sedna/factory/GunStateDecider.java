// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
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
                GunState lastState = ItemGunBaseNT.getState(stack, index);
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
                            return ItemGunBaseNT.getPrimary(stack, index)
                                    && ItemGunBaseNT.getMode(stack, ctx.configIndex()) == 0;
                        });
            };

    public static void deciderStandardFinishDraw(ItemStack stack, GunState lastState, int index) {

        if (lastState == GunState.DRAWING) {
            ItemGunBaseNT.setState(stack, index, GunState.IDLE);
            ItemGunBaseNT.setTimer(stack, index, 0);
        }
    }

    public static void deciderStandardClearJam(ItemStack stack, GunState lastState, int index) {

        if (lastState == GunState.JAMMED) {
            ItemGunBaseNT.setState(stack, index, GunState.IDLE);
            ItemGunBaseNT.setTimer(stack, index, 0);
        }
    }

    public static void deciderStandardReload(
            ItemStack stack, LambdaContext ctx, GunState lastState, int recIndex, int gunIndex) {

        if (lastState == GunState.RELOADING) {

            LivingEntity entity = ctx.entity();
            Player player = ctx.getPlayer();
            GunConfig cfg = ctx.config();
            Receiver rec = cfg.getReceivers(stack)[recIndex];
            IMagazine mag = rec.getMagazine(stack);

            mag.reloadAction(stack, ctx.inventory());
            boolean cancel = ItemGunBaseNT.getReloadCancel(stack);

            if (!cancel && mag.canReload(stack, ctx.inventory())) {
                ItemGunBaseNT.setState(stack, gunIndex, GunState.RELOADING);
                ItemGunBaseNT.setTimer(stack, gunIndex, rec.getReloadCycleDuration(stack));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD_CYCLE, gunIndex);
            } else {

                if (getStandardJamChance(stack, cfg, gunIndex) > entity.getRandom().nextFloat()) {
                    ItemGunBaseNT.setState(stack, gunIndex, GunState.JAMMED);
                    ItemGunBaseNT.setTimer(stack, gunIndex, rec.getJamDuration(stack));
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimation.JAMMED, gunIndex);
                } else {
                    ItemGunBaseNT.setState(stack, gunIndex, GunState.DRAWING);
                    int duration =
                            rec.getReloadEndDuration(stack)
                                    + (mag.getAmountBeforeReload(stack) <= 0
                                            ? rec.getReloadCockOnEmptyPost(stack)
                                            : 0);
                    ItemGunBaseNT.setTimer(stack, gunIndex, duration);
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD_END, gunIndex);
                }

                ItemGunBaseNT.setReloadCancel(stack, false);
            }

            mag.setAmountAfterReload(stack, mag.getAmount(stack, ctx.inventory()));
        }
    }

    public static float getStandardJamChance(ItemStack stack, GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
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
            Receiver rec = cfg.getReceivers(stack)[recIndex];

            if (rec.getRefireOnHold(stack) && refireCondition.getAsBoolean()) {
                if (rec.getCanFire(stack).apply(stack, ctx)) {
                    rec.getOnFire(stack).accept(stack, ctx);
                    ItemGunBaseNT.setState(stack, gunIndex, GunState.COOLDOWN);
                    ItemGunBaseNT.setTimer(stack, gunIndex, rec.getDelayAfterFire(stack));

                    if (rec.getFireSound(stack) != null)
                        entity.level()
                                .playSound(
                                        null,
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

                    ItemGunBaseNT.setState(
                            stack,
                            gunIndex,
                            rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                    ItemGunBaseNT.setTimer(stack, gunIndex, rec.getDelayAfterDryFire(stack));
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, gunIndex);
                } else {
                    ItemGunBaseNT.setState(stack, gunIndex, GunState.IDLE);
                    ItemGunBaseNT.setTimer(stack, gunIndex, 0);
                }
            } else {

                if (rec.getReloadOnEmpty(stack)
                        && rec.getMagazine(stack).getAmount(stack, ctx.inventory()) <= 0) {
                    ItemGunBaseNT.setIsAiming(stack, false);
                    IMagazine mag = rec.getMagazine(stack);

                    if (mag.canReload(stack, ctx.inventory())) {
                        int loaded = mag.getAmount(stack, ctx.inventory());
                        mag.setAmountBeforeReload(stack, loaded);
                        ItemGunBaseNT.setState(stack, ctx.configIndex(), GunState.RELOADING);
                        ItemGunBaseNT.setTimer(
                                stack,
                                ctx.configIndex(),
                                rec.getReloadBeginDuration(stack)
                                        + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
                        ItemGunBaseNT.playAnimation(
                                player, stack, GunAnimation.RELOAD, ctx.configIndex());
                    } else {
                        ItemGunBaseNT.setState(stack, gunIndex, GunState.IDLE);
                        ItemGunBaseNT.setTimer(stack, gunIndex, 0);
                    }

                } else {
                    ItemGunBaseNT.setState(stack, gunIndex, GunState.IDLE);
                    ItemGunBaseNT.setTimer(stack, gunIndex, 0);
                }
            }
        }
    }
}
