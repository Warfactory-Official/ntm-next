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

import com.hbm.client.weapon.GunClientState.SmokeNode;
import com.hbm.client.weapon.GunClientState;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.items.weapon.sedna.BulletConfig.ProjectileType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.stats.ModStats;
import com.hbm.util.GameTime;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Lego {

    public static final Random ANIM_RAND = new Random();

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_RELOAD =
            (stack, ctx) -> {
                Player player = ctx.getPlayer();
                Receiver rec = ctx.config().getReceivers(stack).get(0);
                GunState state = GunBaseNTItem.getState(stack, ctx.configIndex());

                if (state == GunState.IDLE) {

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
                        if (ctx.config().getReloadChangesType(stack))
                            mag.initNewType(stack, ctx.inventory());
                    } else {
                        GunBaseNTItem.playAnimation(
                                player, stack, GunAnimation.INSPECT, ctx.configIndex());
                        if (!ctx.config().getInspectCancel(stack)) {
                            GunBaseNTItem.setState(stack, ctx.configIndex(), GunState.DRAWING);
                            GunBaseNTItem.setTimer(
                                    stack,
                                    ctx.configIndex(),
                                    ctx.config().getInspectDuration(stack));
                        }
                    }
                }
            };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_PRIMARY =
            (stack, ctx) -> {
                clickReceiver(stack, ctx, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_SECONDARY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                int index = ctx.configIndex();
                GunState state = GunBaseNTItem.getState(stack, index);

                if (state == GunState.IDLE) {
                    int mode = GunBaseNTItem.getMode(stack, 0);
                    GunBaseNTItem.setMode(stack, index, 1 - mode);
                    if (mode == 0)
                        EntitySounds.play(
                                entity,
                                entity.getX(),
                                entity.getY(),
                                entity.getZ(),
                                ModSounds.WEAPON_SWITCH_MODE_1.get(),
                                SoundSource.PLAYERS,
                                1F,
                                1F);
                    else
                        EntitySounds.play(
                                entity,
                                entity.getX(),
                                entity.getY(),
                                entity.getZ(),
                                ModSounds.WEAPON_SWITCH_MODE_2.get(),
                                SoundSource.PLAYERS,
                                1F,
                                1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_SMOKE =
            (stack, ctx) -> {
                handleStandardSmoke(ctx.entity(), stack, 2000, 0.025D, 1.15D, ctx.configIndex());
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM =
            (stack, ctx) -> {
                GunBaseNTItem.setIsAiming(stack, !GunBaseNTItem.getIsAiming(stack));
            };
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_STANDARD_CAN_FIRE =
            (stack, ctx) -> {
                return ctx.config()
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, ctx.inventory())
                        > 0;
            };
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_SECOND_CAN_FIRE =
            (stack, ctx) -> {
                return ctx.config()
                                .getReceivers(stack)
                                .get(1)
                                .getMagazine(stack)
                                .getAmount(stack, ctx.inventory())
                        > 0;
            };
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_LOCKON_CAN_FIRE =
            (stack, ctx) -> {
                return ctx.config()
                                        .getReceivers(stack)
                                        .get(0)
                                        .getMagazine(stack)
                                        .getAmount(stack, ctx.inventory())
                                > 0
                        && GunBaseNTItem.getIsLockedOn(stack);
            };
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_DEBUG_CAN_FIRE =
            (stack, ctx) -> {
                return true;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_FIRE =
            (stack, ctx) -> {
                doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_FIRE =
            (stack, ctx) -> {
                doStandardFire(stack, ctx, GunAnimation.CYCLE, 1, true);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE =
            (stack, ctx) -> {
                doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_LOCKON_FIRE =
            (stack, ctx) -> {
                doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
                GunBaseNTItem.setIsLockedOn(stack, false);
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DEBUG_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(0, 0, -3, 50)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(0, 0, 1, 50)
                                                .to(0, 0, 1, 400)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence().to(0, 0, 0, 450).to(0, 0, 1, 200));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(0, 0, 1, 50)
                                                .to(0, 0, 1, 300 + 100)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence().to(0, 0, 0, 450).to(0, 0, 1, 200));
                    case EQUIP:
                        return new BusAnimation()
                                .addBus("ROTATE", new AnimationSequence().to(-360, 0, 0, 350));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "RELAOD_TILT",
                                        new AnimationSequence()
                                                .to(-15, 0, 0, 100)
                                                .to(65, 0, 0, 100)
                                                .to(45, 0, 0, 50)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 0, 1450)
                                                .to(-80, 0, 0, 100)
                                                .to(-80, 0, 0, 100)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "RELOAD_CYLINDER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(90, 0, 0, 100)
                                                .to(90, 0, 0, 1700)
                                                .to(0, 0, 0, 70))
                                .addBus(
                                        "RELOAD_LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(-45, 0, 0, 250)
                                                .to(-45, 0, 0, 350)
                                                .to(-15, 0, 0, 200)
                                                .to(-15, 0, 0, 1050)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "RELOAD_JOLT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(2, 0, 0, 50)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "RELOAD_BULLETS",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 650)
                                                .to(10, 0, 0, 300)
                                                .to(10, 0, 0, 200)
                                                .to(0, 0, 0, 700))
                                .addBus(
                                        "RELOAD_BULLETS_CON",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 0)
                                                .to(1, 0, 0, 950)
                                                .to(0, 0, 0, 1));
                    case INSPECT:
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "RELAOD_TILT",
                                        new AnimationSequence()
                                                .to(-15, 0, 0, 100)
                                                .to(65, 0, 0, 100)
                                                .to(45, 0, 0, 50)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 0, 200)
                                                .to(-80, 0, 0, 100)
                                                .to(-80, 0, 0, 100)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "RELOAD_CYLINDER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(90, 0, 0, 100)
                                                .to(90, 0, 0, 450)
                                                .to(0, 0, 0, 70));
                }

                return null;
            };

    public static void clickReceiver(ItemStack stack, LambdaContext ctx, int receiver) {

        LivingEntity entity = ctx.entity();
        Player player = ctx.getPlayer();
        Receiver rec = ctx.config().getReceivers(stack).get(receiver);
        int index = ctx.configIndex();
        GunState state = GunBaseNTItem.getState(stack, index);

        if (state == GunState.IDLE) {

            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);

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

                GunBaseNTItem.setState(stack, index, GunState.COOLDOWN);
                GunBaseNTItem.setTimer(stack, index, rec.getDelayAfterFire(stack));
            } else {

                if (rec.getDoesDryFire(stack)) {
                    GunBaseNTItem.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
                    GunBaseNTItem.setState(
                            stack,
                            index,
                            rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                    GunBaseNTItem.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
                }
            }
        }

        if (state == GunState.RELOADING) {
            GunBaseNTItem.setReloadCancel(stack, true);
        }
    }

    public static void handleStandardSmoke(
            LivingEntity entity,
            ItemStack stack,
            int smokeDuration,
            double alphaDecay,
            double widthGrowth,
            int index) {
        GunBaseNTItem gun = (GunBaseNTItem) stack.getItem();
        long lastShot = GunClientState.getLastShotTime(gun, index);
        List<SmokeNode> smokeNodes = GunClientState.getSmokeParticles(gun, index);

        boolean smoking = lastShot + smokeDuration > GameTime.millis(entity.level());
        if (!smoking && !smokeNodes.isEmpty()) smokeNodes.clear();

        if (smoking) {
            Vec3 prev =
                    new Vec3(
                                    -entity.getDeltaMovement().x,
                                    -entity.getDeltaMovement().y,
                                    -entity.getDeltaMovement().z)
                            .yRot((float) (entity.getYRot() * Math.PI / 180D));
            double accel = 15D;
            double side = (entity.getYRot() - entity.yHeadRotO) * 0.1D;
            double waggle = 0.025D;

            for (SmokeNode node : smokeNodes) {
                node.forward += -prev.z * accel + entity.getRandom().nextGaussian() * waggle;
                node.lift += prev.y + 1.5D;
                node.side += prev.x * accel + entity.getRandom().nextGaussian() * waggle + side;
                if (node.alpha > 0) node.alpha -= alphaDecay;
                node.width *= widthGrowth;
            }

            double alpha = (GameTime.millis(entity.level()) - lastShot) / (double) smokeDuration;
            alpha = (1 - alpha) * 0.5D;

            if (GunBaseNTItem.getState(stack, index) == GunState.RELOADING
                    || smokeNodes.size() == 0) alpha = 0;
            smokeNodes.add(new SmokeNode(alpha));
        }
    }

    public static void doStandardFire(
            ItemStack stack, LambdaContext ctx, GunAnimation anim, int receiver, boolean calcWear) {
        LivingEntity entity = ctx.entity();
        Player player = ctx.getPlayer();
        int index = ctx.configIndex();
        if (anim != null) GunBaseNTItem.playAnimation(player, stack, anim, ctx.configIndex());

        boolean aim = GunBaseNTItem.getIsAiming(stack);
        Receiver primary = ctx.config().getReceivers(stack).get(receiver);
        Magazine mag = primary.getMagazine(stack);
        BulletConfig config = (BulletConfig) mag.getType(stack, ctx.inventory());

        Vec3 offset =
                GunBaseNTItem.getIsAiming(stack)
                        ? primary.getProjectileOffsetScoped(stack)
                        : primary.getProjectileOffset(stack);
        double forwardOffset = offset.x;
        double heightOffset = offset.y;
        double sideOffset = offset.z;

        int projectiles = config.projectilesMin;
        if (config.projectilesMax > config.projectilesMin)
            projectiles +=
                    entity.getRandom().nextInt(config.projectilesMax - config.projectilesMin + 1);
        projectiles = (int) (projectiles * primary.getSplitProjectiles(stack));

        for (int i = 0; i < projectiles; i++) {
            float damage = calcDamage(ctx, stack, primary, calcWear, index);
            float spread = calcSpread(ctx, stack, primary, config, calcWear, index, aim);

            if (config.pType == ProjectileType.BULLET
                    || config.pType == ProjectileType.BULLET_CHUNKLOADING) {
                BulletBaseMK4Entity mk4 =
                        new BulletBaseMK4Entity(
                                entity,
                                config,
                                damage,
                                spread,
                                sideOffset,
                                heightOffset,
                                forwardOffset);
                if (GunBaseNTItem.getIsLockedOn(stack)
                        && entity.level() instanceof ServerLevel server)
                    mk4.lockonTarget = server.getEntity(GunBaseNTItem.getLockonTarget(stack));
                if (i == 0 && config.blackPowder) {
                    Vec3 motion = mk4.getFlightDirection();
                    ParticleCreators.blackPowder(
                            entity.level(),
                            mk4.getX(),
                            mk4.getY(),
                            mk4.getZ(),
                            motion.x,
                            motion.y,
                            motion.z,
                            10,
                            0.25F,
                            0.5F,
                            10,
                            0.25F);
                }
                entity.level().addFreshEntity(mk4);
            } else if (config.pType == ProjectileType.BEAM) {
                BulletBeamBaseEntity beam =
                        new BulletBeamBaseEntity(
                                entity,
                                config,
                                damage,
                                spread,
                                sideOffset,
                                heightOffset,
                                forwardOffset);
                entity.level().addFreshEntity(beam);
            }
        }

        if (player != null) player.awardStat(ModStats.BULLETS.get());
        mag.consumeAmmo(stack, ctx.inventory(), 1);
        if (calcWear)
            GunBaseNTItem.setWear(
                    stack,
                    index,
                    Math.min(
                            GunBaseNTItem.getWear(stack, index) + config.wear,
                            ctx.config().getDurability(stack)));
    }

    public static float getStandardWearSpread(ItemStack stack, GunConfig config, int index) {
        float percent = GunBaseNTItem.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.5F) return 0F;
        return (percent - 0.5F) * 2F;
    }

    public static float getStandardWearDamage(ItemStack stack, GunConfig config, int index) {
        float percent = GunBaseNTItem.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.75F) return 1F;
        return 1F - (percent - 0.75F) * 2F;
    }

    public static float calcDamage(
            LambdaContext ctx, ItemStack stack, Receiver primary, boolean calcWear, int index) {
        return primary.getBaseDamage(stack)
                * (calcWear ? getStandardWearDamage(stack, ctx.config(), index) : 1);
    }

    public static float calcSpread(
            LambdaContext ctx,
            ItemStack stack,
            Receiver primary,
            BulletConfig config,
            boolean calcWear,
            int index,
            boolean aim) {
        float spreadInnate = primary.getInnateSpread(stack);
        float spreadAmmo = config.spread * primary.getAmmoSpread(stack);
        float spreadHipfire = aim ? 0F : primary.getHipfireSpread(stack);
        float spreadWear =
                !calcWear
                        ? 0F
                        : (getStandardWearSpread(stack, ctx.config(), index)
                                * primary.getDurabilitySpread(stack));

        return spreadInnate + spreadAmmo + spreadHipfire + spreadWear;
    }

    public static void standardExplode(BulletBaseMK4Entity bullet, HitResult mop, float range) {
        standardExplode(bullet, mop, range, 1F);
    }

    public static void standardExplode(
            BulletBaseMK4Entity bullet, HitResult mop, float range, float damageMod) {
        Vec3 hit = mop.getLocation();
        ComposableExplosion vnt =
                new ComposableExplosion(
                        bullet.level(), hit.x, hit.y, hit.z, range, bullet.getThrower());
        vnt.entityProcessor(
                new WeaponExplosionEntityProcessor(1, bullet.damage * damageMod)
                        .piercing(
                                bullet.config.armorThresholdNegation,
                                bullet.config.armorPiercingPercent));
        vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
        vnt.explode();
    }

    public static void tinyExplode(BulletBaseMK4Entity bullet, HitResult mop, float range) {
        tinyExplode(bullet, mop, range, 1F);
    }

    public static void tinyExplode(
            BulletBaseMK4Entity bullet, HitResult mop, float range, float damageMod) {
        Direction dir = mop instanceof BlockHitResult block ? block.getDirection() : Direction.UP;
        Vec3 hit = mop.getLocation();
        double x = hit.x + dir.getStepX() * 0.25D;
        double y = hit.y + dir.getStepY() * 0.25D;
        double z = hit.z + dir.getStepZ() * 0.25D;
        ComposableExplosion vnt =
                new ComposableExplosion(bullet.level(), x, y, z, range, bullet.getThrower());
        vnt.entityProcessor(
                new WeaponExplosionEntityProcessor(0.5, bullet.damage * damageMod)
                        .piercing(
                                bullet.config.armorThresholdNegation,
                                bullet.config.armorPiercingPercent)
                        .knockback(0.25D));
        vnt.effects(new ExplosionEffectTiny());
        vnt.explode();
    }
}
