// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.EnumCasingType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.SpentCasing;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory10ga {

    public static BulletConfig g10;
    public static BulletConfig g10_shrapnel;
    public static BulletConfig g10_du;
    public static BulletConfig g10_slug;
    public static BulletConfig g10_explosive;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_TINY_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 1.5F);
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_DOUBLE_BARREL =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DOUBLE_SECONDARY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                Player player = ctx.getPlayer();
                Receiver rec = ctx.config().getReceivers(stack)[0];
                int index = ctx.configIndex();
                GunState state = ItemGunBaseNT.getState(stack, index);

                if (state == GunState.IDLE) {

                    if (rec.getCanFire(stack).apply(stack, ctx)) {
                        rec.getOnFire(stack).accept(stack, ctx);

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

                        ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                        ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
                    } else {

                        if (rec.getDoesDryFire(stack)) {
                            ItemGunBaseNT.playAnimation(
                                    player, stack, GunAnimation.CYCLE_DRY, index);
                            ItemGunBaseNT.setState(
                                    stack,
                                    index,
                                    rec.getRefireAfterDry(stack)
                                            ? GunState.COOLDOWN
                                            : GunState.DRAWING);
                            ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
                        }
                    }
                }

                if (state == GunState.RELOADING) {
                    ItemGunBaseNT.setReloadCancel(stack, true);
                }
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DOUBLE_BARREL_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-60, 0, 0, 0)
                                                .addPos(0, 0, -3, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -1, 50)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "BUCKLE",
                                        new BusAnimationSequence()
                                                .addPos(0, -60, 0, 50)
                                                .addPos(0, 0, 0, 250));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "TURN",
                                        new BusAnimationSequence()
                                                .addPos(0, 30, 0, 350, IType.SIN_FULL)
                                                .addPos(0, 30, 0, 1150)
                                                .addPos(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -90, 100, IType.SIN_FULL)
                                                .addPos(0, 0, -90, 1300)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 300)
                                                .addPos(60, 0, 0, 150, IType.SIN_UP)
                                                .addPos(60, 0, 0, 1150)
                                                .addPos(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 350)
                                                .addPos(-5, 0, 0, 150, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 700)
                                                .addPos(-5, 0, 0, 100, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 100, IType.SIN_UP)
                                                .addPos(45, 0, 0, 150)
                                                .addPos(45, 0, 0, 150)
                                                .addPos(-5, 0, 0, 150, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "SHELLS",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 450)
                                                .addPos(0, 0, -2.5, 100)
                                                .addPos(0, -5, -5, 350, IType.SIN_DOWN)
                                                .addPos(0, -3, -2, 0)
                                                .addPos(0, 0, -2, 250)
                                                .addPos(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "SHELL_FLIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 450)
                                                .addPos(-360, 0, 0, 450)
                                                .addPos(0, 0, 0, 0));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -90, 100, IType.SIN_FULL)
                                                .addPos(0, 0, -90, 800)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 300)
                                                .addPos(60, 0, 0, 150, IType.SIN_UP)
                                                .addPos(60, 0, 0, 650)
                                                .addPos(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 350)
                                                .addPos(-5, 0, 0, 150, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 200)
                                                .addPos(-5, 0, 0, 100, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 100, IType.SIN_UP)
                                                .addPos(45, 0, 0, 150)
                                                .addPos(45, 0, 0, 150)
                                                .addPos(-5, 0, 0, 150, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                }

                return null;
            };

    public static void init(IRegistrar r) {

        float buckshotSpread = 0.035F;
        g10 =
                new BulletConfig()
                        .setItem(EnumAmmo.G10)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 4)
                        .setProjectiles(10)
                        .setDamage(1F / 10F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(15)
                        .setThresholdNegation(5F)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xB52B2B, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(1F)
                                        .register("10GA"));
        g10_shrapnel =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_SHRAPNEL)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 4)
                        .setProjectiles(10)
                        .setDamage(1F / 10F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(90)
                        .setRicochetCount(15)
                        .setThresholdNegation(5F)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xE5DD00, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(1F)
                                        .register("10GAShrapnel"));
        g10_du =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_DU)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 4)
                        .setProjectiles(10)
                        .setDamage(1F / 4F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(15)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.2F)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0x538D53, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(1F)
                                        .register("10GADU"));
        g10_slug =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_SLUG)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 4)
                        .setRicochetAngle(15)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.1F)
                        .setDoesPenetrate(true)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0x808080, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(1F)
                                        .register("10GASlug"));
        g10_explosive =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_EXPLOSIVE)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 4)
                        .setWear(3F)
                        .setProjectiles(10)
                        .setDamage(1F / 4F)
                        .setSpread(buckshotSpread)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xFAC943, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(1F)
                                        .register("10GAEXP"))
                        .setOnImpact(LAMBDA_TINY_EXPLODE);

        ModItems.GUN_DOUBLE_BARREL =
                r.registerItem(
                        "gun_double_barrel",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.SPECIAL,
                                                props,
                                                new GunConfig()
                                                        .dura(1000)
                                                        .draw(10)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(30F)
                                                                        .rounds(2)
                                                                        .delay(10)
                                                                        .reload(41)
                                                                        .reloadOnEmpty(true)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.9F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                2)
                                                                                        .addConfigs(
                                                                                                g10,
                                                                                                g10_shrapnel,
                                                                                                g10_du,
                                                                                                g10_slug,
                                                                                                g10_explosive))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_DOUBLE_BARREL))
                                                        .setupStandardConfiguration()
                                                        .ps(LAMBDA_DOUBLE_SECONDARY)
                                                        .anim(LAMBDA_DOUBLE_BARREL_ANIMS)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_DOUBLE_BARREL))
                                        .setDefaultAmmo(EnumAmmo.G10, 6),
                        Item.Properties::new);

        ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON =
                r.registerItem(
                        "gun_double_barrel_sacred_dragon",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(6000)
                                                        .draw(10)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(45F)
                                                                        .spreadAmmo(1.35F)
                                                                        .rounds(2)
                                                                        .delay(10)
                                                                        .reload(41)
                                                                        .reloadOnEmpty(true)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.9F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                2)
                                                                                        .addConfigs(
                                                                                                g10,
                                                                                                g10_shrapnel,
                                                                                                g10_du,
                                                                                                g10_slug,
                                                                                                g10_explosive))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_DOUBLE_BARREL))
                                                        .setupStandardConfiguration()
                                                        .ps(LAMBDA_DOUBLE_SECONDARY)
                                                        .anim(LAMBDA_DOUBLE_BARREL_ANIMS)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_DOUBLE_BARREL))
                                        .setDefaultAmmo(EnumAmmo.G10_DU, 6),
                        Item.Properties::new);

        ModItems.GUN_AUTOSHOTGUN_HERETIC =
                r.registerItem(
                        "gun_autoshotgun_heretic",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.DEBUG,
                                                props,
                                                new GunConfig()
                                                        .draw(20)
                                                        .inspect(65)
                                                        .reloadSequential(true)
                                                        .inspectCancel(false)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .hideCrosshair(false)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(100F)
                                                                        .delay(3)
                                                                        .auto(true)
                                                                        .dryfireAfterAuto(true)
                                                                        .reload(110)
                                                                        .jam(19)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHREDDER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                250)
                                                                                        .addConfigs(
                                                                                                g10,
                                                                                                g10_shrapnel,
                                                                                                g10_du,
                                                                                                g10_slug,
                                                                                                g10_explosive))
                                                                        .offset(0.75, -0.125, -0.25)
                                                                        .canFire(
                                                                                Lego
                                                                                        .LAMBDA_STANDARD_CAN_FIRE)
                                                                        .fire(
                                                                                Lego
                                                                                        .LAMBDA_NOWEAR_FIRE)
                                                                        .recoil(
                                                                                XFactory12ga
                                                                                        .LAMBDA_RECOIL_SEXY))
                                                        .setupStandardConfiguration()
                                                        .anim(XFactory12ga.LAMBDA_SEXY_ANIMS)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_SHREDDER_SEXY))
                                        .setDefaultAmmo(EnumAmmo.G10, 50),
                        Item.Properties::new);
    }
}
