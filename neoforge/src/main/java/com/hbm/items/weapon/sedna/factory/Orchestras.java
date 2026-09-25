// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.toclient.MuzzleFlashPayload;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.platform.Services;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Orchestras {

    public static final BiConsumer<ItemStack, LambdaContext> DEBUG_ORCHESTRA =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 3) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 10) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 34) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                    if (timer == 40) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 16) {
                        Receiver rec = ctx.config().getReceivers(stack)[0];
                        IMagazine<?> mag = rec.getMagazine(stack);
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());
                        if (casing != null)
                            for (int i = 0; i < mag.getCapacity(stack); i++) {
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.25,
                                        -0.125,
                                        -0.125,
                                        -0.05,
                                        0,
                                        0,
                                        0.01,
                                        casing.getName());
                            }
                    }
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 11) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 11) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 3) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CARBINE =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 1) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.3125,
                                    aiming ? 0 : -0.125,
                                    aiming ? 0 : -0.25D,
                                    0,
                                    0.21,
                                    -0.06,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 2.5F,
                                    2.5F + (float) entity.getRandom().nextGaussian() * 2F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    20);
                    }
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 26) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_END) {
                    if (timer == 2) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 2) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                    if (timer == 31) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 6) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_ATLAS =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                    if (timer == 44) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.9F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.9F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 24) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 12) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 34) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
            };

    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DANI =
            (stack, ctx) -> ORCHESTRA_ATLAS.accept(stack, ctx);
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_HENRY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.RELOAD) {
                    if (timer == 8) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_CYCLE) {
                    if (timer == 0) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_END) {
                    if (timer == 0) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.9F);
                    if (timer == 12
                            && ctx.config()
                                            .getReceivers(stack)[0]
                                            .getMagazine(stack)
                                            .getAmountBeforeReload(stack)
                                    <= 0) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 0) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.9F);
                    if (timer == 12) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 1F);
                    if (timer == 44) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 14) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.5,
                                    -0.125,
                                    aiming ? -0.125 : -0.375D,
                                    0,
                                    0.12,
                                    -0.12,
                                    0.01,
                                    -7.5F + (float) entity.getRandom().nextGaussian() * 5F,
                                    (float) entity.getRandom().nextGaussian() * 1.5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    20);
                    }
                    if (timer == 12) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 12) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_NOPIP =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 3) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 10) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 34) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                    if (timer == 40) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);

                    if (timer == 16) {
                        Receiver rec = ctx.config().getReceivers(stack)[0];
                        IMagazine mag = rec.getMagazine(stack);
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());
                        if (casing != null)
                            for (int i = 0; i < mag.getCapacity(stack); i++)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.25,
                                        -0.125,
                                        -0.125,
                                        -0.05,
                                        0,
                                        0,
                                        0.01,
                                        -6.5F + (float) entity.getRandom().nextGaussian() * 3F,
                                        (float) entity.getRandom().nextGaussian() * 5F,
                                        casing.getName());
                    }
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 11) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 11) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 3) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_HANGMAN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                }

                if (type == GunAnimation.RELOAD) {

                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
                    if (timer == 5) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.8F);
                    if (timer == 25) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 35) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);

                    if (timer == 10) {
                        Receiver rec = ctx.config().getReceivers(stack)[0];
                        IMagazine mag = rec.getMagazine(stack);
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());
                        if (casing != null)
                            for (int i = 0; i < mag.getCapacity(stack); i++)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.25,
                                        -0.25,
                                        -0.125,
                                        -0.05,
                                        0,
                                        0,
                                        0.01,
                                        -6.5F + (float) entity.getRandom().nextGaussian() * 3F,
                                        (float) entity.getRandom().nextGaussian() * 5F,
                                        casing.getName());
                    }
                }

                if (type == GunAnimation.INSPECT) {
                    if (timer == 16 && ctx.getPlayer() != null) {
                        HitResult mop = EntityDamageUtil.getMouseOver(ctx.getPlayer(), 3.0D);
                        if (mop != null) {
                            if (mop instanceof EntityHitResult entityHit) {
                                float damage = 10F;
                                Entity hit = entityHit.getEntity();
                                if (hit.level() instanceof ServerLevel server) {
                                    hit.hurtServer(
                                            server,
                                            ctx.getPlayer()
                                                    .damageSources()
                                                    .playerAttack(ctx.getPlayer()),
                                            damage);
                                }
                                hit.setDeltaMovement(hit.getDeltaMovement().multiply(2, 1, 2));
                                hit.level()
                                        .playSound(
                                                null,
                                                hit.getX(),
                                                hit.getY(),
                                                hit.getZ(),
                                                ModSounds.GUN_SMACK.get(),
                                                SoundSource.PLAYERS,
                                                1F,
                                                0.9F + entity.getRandom().nextFloat() * 0.2F);
                            }
                            if (mop instanceof BlockHitResult blockHit) {
                                BlockState b = entity.level().getBlockState(blockHit.getBlockPos());
                                entity.level()
                                        .playSound(
                                                null,
                                                mop.getLocation().x,
                                                mop.getLocation().y,
                                                mop.getLocation().z,
                                                b.getSoundType().getStepSound(),
                                                SoundSource.PLAYERS,
                                                2F,
                                                0.9F + entity.getRandom().nextFloat() * 0.2F);
                            }
                        }
                    }
                }

                if (type == GunAnimation.JAMMED) {
                    if (timer == 10) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
                    if (timer == 15) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.8F);
                    if (timer == 20) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 25) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.RELOAD) {
                    if (timer == 8) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
                    if (timer == 16) play(entity, ModSounds.GUN_SHOTGUN_LOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_CYCLE) {
                    if (timer == 0) play(entity, ModSounds.GUN_SHOTGUN_LOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_END) {
                    if (timer == 2) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.7F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 2) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.7F);
                    if (timer == 17) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                    if (timer == 29) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 14) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.3125,
                                    -0.125,
                                    aiming ? -0.125 : -0.375D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 5F,
                                    (float) entity.getRandom().nextGaussian() * 2.5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    20);
                    }
                    if (timer == 8) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG_SHORT =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.RELOAD) {
                    if (timer == 8) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
                    if (timer == 16) play(entity, ModSounds.GUN_SHOTGUN_LOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_CYCLE) {
                    if (timer == 0) play(entity, ModSounds.GUN_SHOTGUN_LOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_END) {
                    if (timer == 2) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.7F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 2) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.7F);
                    if (timer == 17) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                    if (timer == 29) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 14) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.3125,
                                    -0.125,
                                    aiming ? -0.125 : -0.375D,
                                    0,
                                    -0.08,
                                    0,
                                    0.01,
                                    -15F + (float) entity.getRandom().nextGaussian() * 5F,
                                    (float) entity.getRandom().nextGaussian() * 2.5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    20);
                    }
                    if (timer == 8) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG_AKIMBO =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 14) {
                        int offset = ctx.configIndex() == 0 ? -1 : 1;
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.3125,
                                    -0.125,
                                    aiming ? -0.125 * offset : -0.375D * offset,
                                    0,
                                    -0.08,
                                    0,
                                    0.01,
                                    -15F + (float) entity.getRandom().nextGaussian() * 5F,
                                    (float) entity.getRandom().nextGaussian() * 2.5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    20);
                    }
                    if (timer == 8) play(entity, ModSounds.GUN_LEVER_COCK.get(), 1F, 0.8F);
                    return;
                }

                ORCHESTRA_MARESLEG_SHORT.accept(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_GREASEGUN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.EQUIP) {
                    if (timer == 5) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 2) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.55,
                                    aiming ? 0 : -0.125,
                                    aiming ? 0 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -7.5F + (float) entity.getRandom().nextGaussian() * 5F,
                                    12F + (float) entity.getRandom().nextGaussian() * 5F,
                                    casing.getName());
                    }
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.8F);
                    if (timer == 11) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 24) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
                    if (timer == 26) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1.25F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 11) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                    if (timer == 26) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_UZI =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.EQUIP) {
                    if (timer == 8) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1.25F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 1) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    aiming ? 0 : -0.125,
                                    aiming ? 0 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -2.5F + (float) entity.getRandom().nextGaussian() * 5F,
                                    10F + entity.getRandom().nextFloat() * 15F,
                                    casing.getName());
                    }
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 4) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 26) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 17) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                    if (timer == 31) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_UZI_AKIMBO =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.EQUIP) {
                    if (timer == 8) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1.25F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 1) {
                        int mult = ctx.configIndex() == 0 ? -1 : 1;
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    -0.125,
                                    -0.375D * mult,
                                    0,
                                    0.18,
                                    -0.12 * mult,
                                    0.01,
                                    -2.5F + (float) entity.getRandom().nextGaussian() * 5F,
                                    (10F + entity.getRandom().nextFloat() * 15F) * mult,
                                    casing.getName());
                    }
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 4) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 26) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 17) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                    if (timer == 31) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_COILGUN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE && stack.getItem() == ModItems.GUN_N_I_4_N_I.get()) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_COIL_RELOAD.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DOUBLE_BARREL =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
                    if (timer == 19) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 0.9F);
                    if (timer == 29) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.8F);

                    if (timer == 12) {
                        IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                        int toEject =
                                mag.getAmountAfterReload(stack)
                                        - mag.getAmount(stack, ctx.inventory());
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());
                        if (casing != null)
                            for (int i = 0; i < toEject; i++)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0,
                                        -0.1875,
                                        -0.375D,
                                        -0.24,
                                        0.18,
                                        0,
                                        0.01,
                                        -20F + (float) entity.getRandom().nextGaussian() * 5F,
                                        (float) entity.getRandom().nextGaussian() * 2.5F,
                                        casing.getName(),
                                        true,
                                        60,
                                        0.5D,
                                        20);
                    }
                }

                if (type == GunAnimation.INSPECT) {
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
                    if (timer == 19) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.8F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_PANERSCHRECK =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 30) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MINIGUN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) {
                        if (timer == 0) muzzleFlash(ctx);
                        int rounds =
                                XWeaponModManager.hasUpgrade(
                                                stack,
                                                ctx.configIndex(),
                                                XWeaponModManager.ID_MINIGUN_SPEED)
                                        ? 3
                                        : 1;
                        for (int i = 0; i < rounds; i++) {
                            SpentCasing casing =
                                    ctx.config()
                                            .getReceivers(stack)[0]
                                            .getMagazine(stack)
                                            .getCasing(stack, ctx.inventory());
                            if (casing != null)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        aiming ? 0.125 : 0.5,
                                        aiming ? -0.125 : -0.25,
                                        aiming ? -0.25 : -0.5D,
                                        0,
                                        0.18,
                                        -0.12,
                                        0.01,
                                        (float) entity.getRandom().nextGaussian() * 15F,
                                        (float) entity.getRandom().nextGaussian() * 15F,
                                        casing.getName());
                        }
                    }

                    if (timer == (XWeaponModManager.hasUpgrade(stack, 0, 207) ? 3 : 1))
                        play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                    if (timer == 1) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MINIGUN_DUAL =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) {
                        if (timer == 0) muzzleFlash(ctx);
                        int index = ctx.configIndex() == 0 ? -1 : 1;
                        int rounds =
                                XWeaponModManager.hasUpgrade(
                                                stack,
                                                ctx.configIndex(),
                                                XWeaponModManager.ID_MINIGUN_SPEED)
                                        ? 3
                                        : 1;
                        for (int i = 0; i < rounds; i++) {
                            SpentCasing casing =
                                    ctx.config()
                                            .getReceivers(stack)[0]
                                            .getMagazine(stack)
                                            .getCasing(stack, ctx.inventory());
                            if (casing != null)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.25,
                                        -0.25,
                                        -0.5D * index,
                                        0,
                                        0.18,
                                        -0.12 * index,
                                        0.01,
                                        (float) entity.getRandom().nextGaussian() * 15F,
                                        (float) entity.getRandom().nextGaussian() * 15F,
                                        casing.getName());
                        }
                    }

                    if (timer == (XWeaponModManager.hasUpgrade(stack, 0, 207) ? 3 : 1))
                        play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                    if (timer == 1) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MAS36 =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming =
                        ItemGunBaseNT.getIsAiming(stack)
                                && !XWeaponModManager.hasUpgrade(
                                        stack, 0, XWeaponModManager.ID_SCOPE);

                if (type == GunAnimation.EQUIP) {
                    if (timer == 10) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1F);
                    if (timer == 18) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 7) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                    if (timer == 12) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    aiming ? 0 : -0.125,
                                    aiming ? 0 : -0.25D,
                                    -0.05,
                                    0.2,
                                    -0.025,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 10F,
                                    (float) entity.getRandom().nextGaussian() * 12.5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    10);
                    }
                }

                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                    if (timer == 7) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 1F, 1F);
                    if (timer == 20) play(entity, ModSounds.GUN_RIFLE_COCK.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 1F, 1F);
                }

                if (type == GunAnimation.JAMMED) {
                    if (timer == 5) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 12) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 23) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                }

                if (type == GunAnimation.INSPECT) {
                    if (timer == 0) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 17) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LIBERATOR =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
                    if (timer == 4) {
                        IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                        int toEject =
                                mag.getAmountAfterReload(stack)
                                        - mag.getAmount(stack, ctx.inventory());
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());
                        if (casing != null)
                            for (int i = 0; i < toEject; i++)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.625,
                                        -0.1875,
                                        -0.375D,
                                        -0.12,
                                        0.18,
                                        0,
                                        0.01,
                                        -15F + (float) entity.getRandom().nextGaussian() * 7.5F,
                                        (float) entity.getRandom().nextGaussian() * 5F,
                                        casing.getName(),
                                        true,
                                        60,
                                        0.5D,
                                        20);
                    }
                    if (timer == 15) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_CYCLE) {
                    if (timer == 5) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_END) {
                    if (timer == 2) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 2) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                    if (timer == 12) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
                    if (timer == 26) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
                    IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                    int toEject =
                            mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory());
                    if (timer == 4 && toEject > 0) {
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());

                        if (casing != null)
                            for (int i = 0; i < toEject; i++)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.625,
                                        -0.1875,
                                        -0.375D,
                                        -0.12,
                                        0.18,
                                        0,
                                        0.01,
                                        -15F * (float) entity.getRandom().nextGaussian() * 7.5F,
                                        (float) entity.getRandom().nextGaussian() * 5F,
                                        casing.getName(),
                                        true,
                                        60,
                                        0.5D,
                                        20);
                        mag.setAmountAfterReload(stack, 0);
                    }
                    if (timer == 20) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SPAS =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE || type == GunAnimation.ALT_CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 8) play(entity, ModSounds.GUN_SHOTGUN_COCK.get(), 1F, 1F);
                    if (timer == 10) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    aiming ? 0 : -0.125,
                                    aiming ? 0 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -3F + (float) entity.getRandom().nextGaussian() * 2.5F,
                                    -15F + entity.getRandom().nextFloat() * -5F,
                                    casing.getName());
                    }
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_SHOTGUN_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD) {
                    IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                    if (mag.getAmount(stack, ctx.inventory()) == 0) {
                        if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                        if (timer == 7) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    }
                    if (timer == 5) play(entity, ModSounds.GUN_SHOTGUN_LOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD_CYCLE) {
                    if (timer == 5) play(entity, ModSounds.GUN_SHOTGUN_LOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 5) play(entity, ModSounds.GUN_SHOTGUN_OPEN.get(), 1F, 1F);
                    if (timer == 18) play(entity, ModSounds.GUN_SHOTGUN_CLOSE.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 18) play(entity, ModSounds.GUN_WHACK.get(), 1F, 1F);
                    if (timer == 25) play(entity, ModSounds.GUN_WHACK.get(), 1F, 1F);
                    if (timer == 29) play(entity, ModSounds.GUN_SHOTGUN_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SHREDDER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 2) play(entity, ModSounds.GUN_SHREDDER_CYCLE.get(), 0.25F, 1.5F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 2) play(entity, ModSounds.GUN_SHREDDER_CYCLE.get(), 0.25F, 1.5F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 32) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 28) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SHREDDER_SEXY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) {
                        muzzleFlash(ctx);
                        if (ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getType(stack, null)
                                == XFactory12ga.g12_equestrian_bj) {
                            ItemGunBaseNT.setTimer(stack, 0, 20);
                        }
                    }

                    if (timer == 2) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    aiming ? -0.0625 : -0.125,
                                    aiming ? -0.125 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 2.5F,
                                    (float) entity.getRandom().nextGaussian() * -20F + 15F,
                                    casing.getName(),
                                    false,
                                    60,
                                    0.5D,
                                    20);
                    }
                }

                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 4) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.75F);
                    if (timer == 16) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 55) play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1F);
                    if (timer == 65) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 74) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                    if (timer == 88) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.75F);
                    if (timer == 100) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);

                    if (timer == 55)
                        ctx.config()
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .reloadAction(stack, ctx.inventory());
                }

                if (type == GunAnimation.INSPECT) {
                    if (timer == 20) play(entity, ModSounds.PLAYER_GULP.get(), 1F, 1F);
                    if (timer == 25) play(entity, ModSounds.PLAYER_GULP.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.PLAYER_GULP.get(), 1F, 1F);
                    if (timer == 35) play(entity, ModSounds.PLAYER_GULP.get(), 1F, 1F);
                    if (timer == 50) play(entity, ModSounds.PLAYER_GROAN.get(), 1F, 1F);
                    if (timer == 60) {
                        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30 * 20, 2));
                        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30 * 20, 2));
                        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 10 * 20, 0));
                    }
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STINGER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (entity.level().isClientSide()) {
                    AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);
                    if (ItemGunStinger.getLockonProgress(stack) > 0
                            && !ItemGunBaseNT.getIsLockedOn(stack)) {
                        if (runningAudio == null || !runningAudio.isPlaying()) {
                            AudioWrapper audio =
                                    AudioSystem.getLoopedSound(
                                            ModSounds.GUN_LOCKON.get(),
                                            SoundSource.PLAYERS,
                                            (float) entity.getX(),
                                            (float) entity.getY(),
                                            (float) entity.getZ(),
                                            1F,
                                            15F,
                                            1F,
                                            10);
                            if (audio != null) {
                                ItemGunBaseNT.loopedSounds.put(entity, audio);
                                audio.startSound();
                            }
                        }
                        if (runningAudio != null && runningAudio.isPlaying()) {
                            runningAudio.keepAlive();
                            runningAudio.updatePosition(
                                    (float) entity.getX(),
                                    (float) entity.getY(),
                                    (float) entity.getZ());
                        }
                    } else {
                        if (runningAudio != null && runningAudio.isPlaying())
                            runningAudio.stopSound();
                    }
                    return;
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 30) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_QUADRO =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 30) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MISSILE_LAUNCHER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1.25F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 1F, 0.9F);
                    if (timer == 30) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                    if (timer == 42) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 1F, 0.9F);
                }

                if (type == GunAnimation.JAMMED || type == GunAnimation.INSPECT) {
                    if (timer == 0) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 1F, 0.9F);
                    if (timer == 27) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 1F, 0.9F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_TAU =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.SPINUP && entity.level().isClientSide()) {
                    AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);

                    if (timer < 300) {
                        if (runningAudio == null || !runningAudio.isPlaying()) {
                            AudioWrapper audio =
                                    AudioSystem.getLoopedSound(
                                            ModSounds.GUN_TAU_LOOP.get(),
                                            SoundSource.PLAYERS,
                                            (float) entity.getX(),
                                            (float) entity.getY(),
                                            (float) entity.getZ(),
                                            1F,
                                            15F,
                                            0.75F,
                                            10);
                            if (audio != null) {
                                audio.updatePitch(0.75F);
                                ItemGunBaseNT.loopedSounds.put(entity, audio);
                                audio.startSound();
                                audio.attachTo(entity);
                            }
                        }
                        if (runningAudio != null && runningAudio.isPlaying()) {
                            runningAudio.keepAlive();
                            runningAudio.attachTo(entity);
                            runningAudio.updatePitch(0.75F + timer * 0.01F);
                        }
                    } else {
                        if (runningAudio != null && runningAudio.isPlaying())
                            runningAudio.stopSound();
                    }
                }
                if (type != GunAnimation.SPINUP && entity.level().isClientSide()) {
                    AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);
                    if (runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
                }
                if (entity.level().isClientSide()) return;

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0)
                        play(
                                entity,
                                ModSounds.GUN_TAU_FIRE.get(),
                                0.5F,
                                0.9F + entity.getRandom().nextFloat() * 0.2F);
                }

                if (type == GunAnimation.ALT_CYCLE) {
                    if (timer == 0)
                        play(
                                entity,
                                ModSounds.GUN_TAU_FIRE.get(),
                                0.5F,
                                0.7F + entity.getRandom().nextFloat() * 0.2F);
                }

                if (type == GunAnimation.SPINUP) {
                    if (timer % 10 == 0 && timer < 130) {
                        IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                        if (mag.getAmount(stack, ctx.inventory()) <= 0) {
                            ItemGunBaseNT.playAnimation(
                                    ctx.getPlayer(),
                                    stack,
                                    GunAnimation.CYCLE_DRY,
                                    ctx.configIndex());
                            return;
                        }
                        mag.useUpAmmo(stack, ctx.inventory(), 1);
                    }

                    if (timer > 200) {
                        ItemGunBaseNT.playAnimation(
                                ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex());

                        ServerLevel server = (ServerLevel) entity.level();
                        var tauBlast =
                                new DamageSource(
                                        server.registryAccess()
                                                .lookupOrThrow(Registries.DAMAGE_TYPE)
                                                .getOrThrow(ModDamageTypes.TAU_BLAST));
                        entity.hurtServer(server, tauBlast, 1_000F);

                        ItemGunBaseNT.setWear(
                                stack,
                                ctx.configIndex(),
                                Math.min(
                                        ItemGunBaseNT.getWear(stack, ctx.configIndex()) + 10_000F,
                                        ctx.config().getDurability(stack)));

                        entity.level()
                                .playSound(
                                        null,
                                        entity.getX(),
                                        entity.getY() + entity.getEyeHeight(),
                                        entity.getZ(),
                                        ModSounds.UFO_BLAST.get(),
                                        SoundSource.PLAYERS,
                                        5.0F,
                                        0.9F);
                        entity.level()
                                .playSound(
                                        null,
                                        entity.getX(),
                                        entity.getY() + entity.getEyeHeight(),
                                        entity.getZ(),
                                        SoundEvents.FIREWORK_ROCKET_BLAST,
                                        SoundSource.PLAYERS,
                                        5.0F,
                                        0.5F);

                        float yaw = entity.getRandom().nextFloat() * 180F;
                        for (int i = 0; i < 3; i++) {
                            Services.NETWORK.sendToAllAround(
                                    new PlasmaBlastPayload(
                                            entity.getX(),
                                            entity.getY() + entity.getEyeHeight(),
                                            entity.getZ(),
                                            1.0F,
                                            0.8F,
                                            0.5F,
                                            -60F + 60F * i,
                                            yaw,
                                            2F),
                                    new TargetPoint(
                                            server,
                                            entity.getX(),
                                            entity.getY() + entity.getEyeHeight(),
                                            entity.getZ(),
                                            100));
                        }
                    }
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_PEPPERBOX =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 24) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                    if (timer == 55) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 21) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.6F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.8F);
                    if (timer == 11) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 0.6F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 3) play(entity, ModSounds.GUN_REVOLVER_SPIN.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 28) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                    if (timer == 45) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.6F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_BOLTER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE && timer == 1) {
                    SpentCasing casing =
                            ctx.config()
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getCasing(stack, ctx.inventory());
                    if (casing != null)
                        CasingCreator.composeEffect(
                                entity.level(),
                                entity,
                                0.5,
                                aiming ? 0 : -0.125,
                                aiming ? -0.0625 : -0.25D,
                                0,
                                0.18,
                                -0.12,
                                0.01,
                                -10F + (float) entity.getRandom().nextGaussian() * 5F,
                                10F + entity.getRandom().nextFloat() * 10F,
                                casing.getName());
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 5) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 26) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_AM180 =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE && timer == 0) {
                    muzzleFlash(ctx);
                    SpentCasing casing =
                            ctx.config()
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getCasing(stack, ctx.inventory());
                    if (casing != null)
                        CasingCreator.composeEffect(
                                entity.level(),
                                entity,
                                0.4375,
                                aiming ? 0 : -0.125,
                                aiming ? 0 : -0.25D,
                                0,
                                -0.06,
                                0,
                                0.01,
                                (float) entity.getRandom().nextGaussian() * 10F,
                                (float) entity.getRandom().nextGaussian() * 10F,
                                casing.getName());
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 6) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.9F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F, true);
                    if (timer == 20) play(entity, ModSounds.GUN_IMPACT.get(), 0.25F, 1F, true);
                    if (timer == 32) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F, true);
                    if (timer == 40) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.9F, true);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 15) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F, true);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F, true);
                    if (timer == 35) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F, true);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 6) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F, false);
                    if (timer == 26) play(entity, ModSounds.GUN_IMPACT.get(), 0.25F, 1F, false);
                    if (timer == 48) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F, false);
                    if (timer == 54) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.9F, false);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 6) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.8F, false);
                    if (timer == 20) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1.0F, false);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 6) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F, false);
                    if (timer == 53) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F, false);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STAR_F =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE && timer == 0) {
                    SpentCasing casing =
                            ctx.config()
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getCasing(stack, ctx.inventory());
                    if (casing != null)
                        CasingCreator.composeEffect(
                                entity.level(),
                                entity,
                                0.3125,
                                aiming ? 0 : -0.125,
                                aiming ? 0 : -0.1875D,
                                0,
                                0.18,
                                -0.12,
                                0.01,
                                (float) entity.getRandom().nextGaussian() * 5F,
                                12.5F + entity.getRandom().nextFloat() * 5F,
                                casing.getName());
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.9F);
                    if (timer == 5) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1.1F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 5) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 22) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 15) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 19) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                    if (timer == 23) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 27) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 7) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STAR_F_AKIMBO =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE && timer == 0) {
                    int side = ctx.configIndex() == 0 ? -1 : 1;
                    SpentCasing casing =
                            ctx.config()
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getCasing(stack, ctx.inventory());
                    if (casing != null)
                        CasingCreator.composeEffect(
                                entity.level(),
                                entity,
                                0.3125,
                                aiming ? 0 : -0.125,
                                aiming ? 0 : -0.1875D * side,
                                0,
                                0.18,
                                -0.12 * side,
                                0.01,
                                (float) entity.getRandom().nextGaussian() * 5F,
                                12.5F + entity.getRandom().nextFloat() * 5F,
                                casing.getName());
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.9F);
                    if (timer == 5) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1.1F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 5) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 5) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 22) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 15) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 19) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                    if (timer == 23) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 27) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 7) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_AMAT =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.EQUIP) {
                    if (timer == 10) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 0.5F, 1.25F);
                    if (timer == 15) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 0.5F, 1.25F);
                }

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 7) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                    if (timer == 12) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    aiming ? 0 : -0.125,
                                    -0.25D,
                                    -0.05,
                                    0.2,
                                    -0.025,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 10F,
                                    (float) entity.getRandom().nextGaussian() * 12.5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    10);
                    }
                }

                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                    if (timer == 7) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 20) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 32) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 41) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                }

                if (type == GunAnimation.JAMMED) {
                    if (timer == 5) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 12) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                    if (timer == 16) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 0.5F, 1F);
                    if (timer == 23) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
                }

                if (type == GunAnimation.INSPECT) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 0.5F, 1F);
                    if (timer == 45) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 0.5F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_M2 =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.EQUIP) {
                    if (timer == 0) play(entity, ModSounds.TURRET_CIWS_RELOAD.get(), 1F, 1F);
                }

                if (type == GunAnimation.CYCLE && timer == 0) {
                    if (timer == 0) muzzleFlash(ctx);
                    SpentCasing casing =
                            ctx.config()
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getCasing(stack, ctx.inventory());
                    if (casing != null)
                        CasingCreator.composeEffect(
                                entity.level(),
                                entity,
                                0.375,
                                aiming ? 0 : -0.125,
                                aiming ? 0 : -0.3125D,
                                0,
                                0.06,
                                -0.18,
                                0.01,
                                (float) entity.getRandom().nextGaussian() * 20F,
                                12.5F + (float) entity.getRandom().nextGaussian() * 7.5F,
                                casing.getName());
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_G3 =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean scoped =
                        stack.getItem() == ModItems.GUN_G3_ZEBRA.get()
                                || XWeaponModManager.hasUpgrade(
                                        stack, 0, XWeaponModManager.ID_SCOPE);
                boolean aiming = ItemGunBaseNT.getIsAiming(stack) && !scoped;

                if (type == GunAnimation.CYCLE && timer == 0) {
                    SpentCasing casing =
                            ctx.config()
                                    .getReceivers(stack)[0]
                                    .getMagazine(stack)
                                    .getCasing(stack, ctx.inventory());
                    if (casing != null)
                        CasingCreator.composeEffect(
                                entity.level(),
                                entity,
                                0.5,
                                aiming ? 0 : -0.125,
                                aiming ? 0 : -0.25D,
                                0,
                                0.18,
                                -0.12,
                                0.01,
                                (float) entity.getRandom().nextGaussian() * 5F,
                                12.5F + entity.getRandom().nextFloat() * 5F,
                                casing.getName());
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.8F);
                    if (timer == 5) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.9F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 4) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                    if (timer == 32) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 36) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 28) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 16) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                    if (timer == 20) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                    if (timer == 26) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STG77 =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) {
                        muzzleFlash(ctx);
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    aiming ? 0.125 : 0.25,
                                    aiming ? -0.125 : -0.25,
                                    aiming ? -0.125 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    (float) entity.getRandom().nextGaussian() * 5F,
                                    7.5F + entity.getRandom().nextFloat() * 5F,
                                    casing.getName(),
                                    false,
                                    0,
                                    0,
                                    0,
                                    0.125);
                    }
                    if (timer == 40) play(entity, ModSounds.GUN_DRY_FIRE.get(), 0.25F, 1.25F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.8F);
                    if (timer == 5) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.9F);
                    if (timer == 40) play(entity, ModSounds.GUN_DRY_FIRE.get(), 0.25F, 1.25F);
                }
                if ((type == GunAnimation.RELOAD || type == GunAnimation.INSPECT) && timer == 0) {
                    play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 10) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F, true);
                    if (timer == 24) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F, true);
                    if (timer == 34) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F, true);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 10)
                        play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F, true);

                    if (timer == 114)
                        play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F, true);
                    if (timer == 124)
                        play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F, true);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 16) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F, false);
                    if (timer == 32) play(entity, ModSounds.GUN_IMPACT.get(), 0.25F, 1.25F, false);
                    if (timer == 38) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F, false);
                    if (timer == 43)
                        play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F, false);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 11)
                        play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F, false);

                    if (timer == 72)
                        play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F, false);
                    if (timer == 84)
                        play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F, false);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LAG =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 1) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.375,
                                    aiming ? 0 : -0.0625,
                                    aiming ? 0 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 5F,
                                    10F + entity.getRandom().nextFloat() * 10F,
                                    casing.getName());
                    }
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 8) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 8) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 26) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 40) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 8) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 1F);
                    if (timer == 20) play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1.6F);
                    if (timer == 36) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAREGUN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.8F);
                    if (timer == 4) {
                        IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                        if (mag.getAmountAfterReload(stack) > 0) {
                            SpentCasing casing =
                                    ctx.config()
                                            .getReceivers(stack)[0]
                                            .getMagazine(stack)
                                            .getCasing(stack, ctx.inventory());
                            if (casing != null)
                                CasingCreator.composeEffect(
                                        entity.level(),
                                        entity,
                                        0.625,
                                        -0.125,
                                        aiming ? -0.125 : -0.375D,
                                        -0.12,
                                        0.18,
                                        0,
                                        0.01,
                                        -15F + (float) entity.getRandom().nextGaussian() * 7.5F,
                                        (float) entity.getRandom().nextGaussian() * 5F,
                                        casing.getName(),
                                        true,
                                        60,
                                        0.5D,
                                        20);
                            mag.setAmountBeforeReload(stack, 0);
                        }
                    }
                    if (timer == 16) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                    if (timer == 24) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 10) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.8F);
                    if (timer == 29) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE) {
                    if (timer == 12) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 2) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 12) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CONGOLAKE =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 15) {
                        IMagazine mag = ctx.config().getReceivers(stack)[0].getMagazine(stack);
                        SpentCasing casing = mag.getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.625,
                                    aiming ? -0.0625 : -0.25,
                                    aiming ? 0 : -0.375D,
                                    0,
                                    0.18,
                                    0.12,
                                    0.01,
                                    -5F + (float) entity.getRandom().nextGaussian() * 3.5F,
                                    -10F + entity.getRandom().nextFloat() * 5F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    20);
                    }
                }
                if (type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE) {
                    if (timer == 0) play(entity, ModSounds.GUN_GRENADE_RELOAD.get(), 1F, 1F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 9) play(entity, ModSounds.GUN_GRENADE_OPEN.get(), 1F, 1F);
                    if (timer == 27) play(entity, ModSounds.GUN_GRENADE_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MK108 =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 2) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.5,
                                    aiming ? -0.125 : -0.3125,
                                    aiming ? -0.375 : -0.3125D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 2.5F,
                                    (float) entity.getRandom().nextGaussian() * -20F + 15F,
                                    casing.getName(),
                                    true,
                                    60,
                                    0.5D,
                                    10);
                    }
                }

                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.65F);
                    if (timer == 10) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.75F);
                    if (timer == 40) play(entity, ModSounds.GUN_MAG_REMOVE.get(), 1F, 0.75F);
                    if (timer == 60) play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1F);
                    if (timer == 90) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 0.75F);
                    if (timer == 100) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 0.75F);
                    if (timer == 125) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.65F);

                    if (timer == 60)
                        ctx.config()
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .reloadAction(stack, ctx.inventory());
                }

                if (type == GunAnimation.INSPECT) {
                    int yeetHorizontal = 750;
                    int untilImpact = yeetHorizontal * 9 / 15;
                    int delay = 250;

                    for (int i = 0; i < 3; i++) {
                        if (timer == (untilImpact + delay * i) / 50)
                            play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1.5F);
                    }
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FATMAN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_FATMAN_RELOAD.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAMER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                handleFlamerLoop(entity, type, timer);
                if (entity.level().isClientSide()) return;

                if (type == GunAnimation.RELOAD) {
                    if (timer == 15) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1F);
                    if (timer == 35) play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1F);
                    if (timer == 60) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.75F);
                    if (timer == 70) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                    if (timer == 85) play(entity, ModSounds.GUN_VALVE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAMER_DAYBREAKER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 15) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1F);
                    if (timer == 35) play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1F);
                    if (timer == 60) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.75F);
                    if (timer == 70) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                    if (timer == 85) play(entity, ModSounds.GUN_VALVE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CHEMTHROWER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                handleFlamerLoop(entity, type, timer);
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_TESLA =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 2) play(entity, ModSounds.GUN_SHREDDER_CYCLE.get(), 0.25F, 1.25F);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1F);
                    if (timer == 2) play(entity, ModSounds.GUN_SHREDDER_CYCLE.get(), 0.25F, 1.25F);
                }
                if (type == GunAnimation.INSPECT) {
                    if (timer == 12) play(entity, ModSounds.BLOCK_PLUSHY.get(), 0.25F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LASER_PISTOL =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1.5F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 10) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1.25F);
                    if (timer == 34) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 1.25F);
                    if (timer == 40) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.25F);
                }

                if (type == GunAnimation.JAMMED) {
                    if (timer == 10) play(entity, ModSounds.GUN_REVOLVER_COCK.get(), 1F, 1F);
                    if (timer == 15) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1.25F);
                    if (timer == 30) play(entity, ModSounds.GUN_IMPACT.get(), 0.25F, 1.5F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LASRIFLE =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                }
                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 0) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 1.5F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 18) play(entity, ModSounds.GUN_IMPACT.get(), 0.25F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 38) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }

                if (type == GunAnimation.INSPECT) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 12) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 20) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }

                if (type == GunAnimation.JAMMED) {
                    if (timer == 2) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
                    if (timer == 22) play(entity, ModSounds.GUN_MAG_INSERT.get(), 1F, 1F);
                    if (timer == 30) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FIREEXT =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 0) play(entity, ModSounds.GUN_VALVE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CHARGE_THROWER =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE_DRY) {
                    Entity e = entity.level().getEntity(ItemGunChargeThrower.getLastHook(stack));
                    if (timer == 0 && e == null)
                        play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                }

                if (type == GunAnimation.RELOAD) {
                    if (timer == 30) play(entity, ModSounds.GUN_ROCKET_INSERT.get(), 1F, 1F);
                    if (timer == 40) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DRILL =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (entity.level().isClientSide()) {
                    double speed = HbmAnimations.getRelevantTransformation("SPEED")[0];

                    AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);

                    if (speed > 0) {
                        if (runningAudio == null || !runningAudio.isPlaying()) {
                            boolean electric =
                                    XWeaponModManager.hasUpgrade(
                                            stack,
                                            ctx.configIndex(),
                                            XWeaponModManager.ID_ENGINE_ELECTRIC);
                            AudioWrapper audio =
                                    AudioSystem.getLoopedSound(
                                            electric
                                                    ? ModSounds.LARGE_TURBINE_LOOP.get()
                                                    : ModSounds.ENGINE_LOOP.get(),
                                            SoundSource.PLAYERS,
                                            (float) entity.getX(),
                                            (float) entity.getY(),
                                            (float) entity.getZ(),
                                            (float) speed,
                                            15F,
                                            (float) speed,
                                            25);
                            if (audio != null) {
                                ItemGunBaseNT.loopedSounds.put(entity, audio);
                                audio.startSound();
                                audio.attachTo(entity);
                            }
                        }
                        if (runningAudio != null && runningAudio.isPlaying()) {
                            runningAudio.keepAlive();
                            runningAudio.updateVolume((float) speed);
                            runningAudio.updatePitch((float) speed);
                        }
                    } else {

                    }
                }
                if (type != GunAnimation.CYCLE
                        && type != GunAnimation.CYCLE_DRY
                        && entity.level().isClientSide()) {
                    AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);
                    if (runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
                }
                if (entity.level().isClientSide()) return;

                if (type == GunAnimation.RELOAD) {
                    if (timer == 15) play(entity, ModSounds.GUN_LATCH_OPEN.get(), 1F, 1F);
                    if (timer == 35) play(entity, ModSounds.GUN_IMPACT.get(), 0.5F, 1F);
                    if (timer == 60) play(entity, ModSounds.GUN_REVOLVER_CLOSE.get(), 1F, 0.75F);
                    if (timer == 70) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                    if (timer == 85) play(entity, ModSounds.GUN_VALVE.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FOLLY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.RELOAD) {
                    if (timer == 20) play(entity, ModSounds.GUN_SCREW.get(), 1F, 1F);
                    if (timer == 80) play(entity, ModSounds.GUN_ROCKET_INSERT.get(), 1F, 1F);
                    if (timer == 120) play(entity, ModSounds.GUN_SCREW.get(), 1F, 1F);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_ABERRATOR =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);

                if (type == GunAnimation.RELOAD) {
                    if (timer == 5) play(entity, ModSounds.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.75F);
                    if (timer == 32) play(entity, ModSounds.GUN_MAG_SMALL_INSERT.get(), 1F, 0.75F);
                    if (timer == 42) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.75F);
                }

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) muzzleFlash(ctx);
                    if (timer == 1) {
                        int cba =
                                (stack.getItem() == ModItems.GUN_ABERRATOR_EOTT.get()
                                                && ctx.configIndex() == 0)
                                        ? -1
                                        : 1;
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.5,
                                    aiming ? 0 : -0.125,
                                    aiming ? -0.0625 : -0.25D * cba,
                                    -0.05,
                                    0.25,
                                    -0.05 * cba,
                                    0.01,
                                    -10F + (float) entity.getRandom().nextGaussian() * 10F,
                                    (float) entity.getRandom().nextGaussian() * 12.5F,
                                    casing.getName());
                    }
                }

                if (type == GunAnimation.CYCLE_DRY) {
                    if (timer == 1) play(entity, ModSounds.GUN_DRY_FIRE.get(), 1F, 0.75F);
                    if (timer == 9) play(entity, ModSounds.GUN_PISTOL_COCK.get(), 1F, 0.75F);
                }
            };

    private static void play(
            LivingEntity entity, SoundEvent sound, float volume, float pitch, boolean legacy) {
        Services.PLATFORM.playGunAnimationSound(entity, sound, volume, pitch, legacy);
    }

    private static void play(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        entity.level()
                .playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        volume,
                        pitch);
    }

    private static void muzzleFlash(LambdaContext ctx) {
        LivingEntity entity = ctx.entity();
        Services.NETWORK.sendToAllAround(
                new MuzzleFlashPayload(entity),
                new TargetPoint(
                        (ServerLevel) entity.level(),
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        100D));
    }

    private static void handleFlamerLoop(LivingEntity entity, GunAnimation type, int timer) {
        if (!entity.level().isClientSide()) return;

        if (type == GunAnimation.CYCLE) {
            AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);

            if (timer < 5) {
                if (runningAudio == null || !runningAudio.isPlaying()) {
                    AudioWrapper audio =
                            AudioSystem.getLoopedSound(
                                    ModSounds.GUN_FLAMER_LOOP.get(),
                                    SoundSource.PLAYERS,
                                    (float) entity.getX(),
                                    (float) entity.getY(),
                                    (float) entity.getZ(),
                                    1F,
                                    15F,
                                    1F,
                                    10);
                    if (audio != null) {
                        ItemGunBaseNT.loopedSounds.put(entity, audio);
                        audio.startSound();
                        audio.attachTo(entity);
                    }
                }
                if (runningAudio != null && runningAudio.isPlaying()) {
                    runningAudio.keepAlive();
                    runningAudio.attachTo(entity);
                }
            } else {
                if (runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
            }
        } else {
            AudioWrapper runningAudio = ItemGunBaseNT.loopedSounds.get(entity);
            if (runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
        }
    }
}
