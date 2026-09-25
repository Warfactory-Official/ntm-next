// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.config.GunVisualConfig;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.EnumCasingType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.SpentCasing;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory556mm {

    public static final Identifier scope = Library.id("textures/misc/scope_bolt.png");

    public static BulletConfig r556_sp;
    public static BulletConfig r556_fmj;
    public static BulletConfig r556_jhp;
    public static BulletConfig r556_ap;

    public static BulletConfig r556_inc_sp;
    public static BulletConfig r556_inc_fmj;
    public static BulletConfig r556_inc_jhp;
    public static BulletConfig r556_inc_ap;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> INCENDIARY =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    HbmLivingProps data = HbmLivingProps.getData(living);
                    if (data.phosphorus < 300) data.phosphorus = 300;
                }
            };
    public static Function<ItemStack, String> LAMBDA_NAME_G3 =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER)
                        && XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK)
                        && XWeaponModManager.hasUpgrade(
                                stack, 0, XWeaponModManager.ID_FURNITURE_BLACK)
                        && XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE))
                    return stack.getItem().getDescriptionId() + "_infiltrator";
                if (!XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK)
                        && XWeaponModManager.hasUpgrade(
                                stack, 0, XWeaponModManager.ID_FURNITURE_GREEN))
                    return stack.getItem().getDescriptionId() + "_a3";
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 1500, 0.075D, 1.1D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STG77_DECIDER =
            (stack, ctx) -> {
                int index = ctx.configIndex();
                GunState lastState = ItemGunBaseNT.getState(stack, index);
                GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
                GunStateDecider.deciderStandardClearJam(stack, lastState, index);
                GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
                GunStateDecider.deciderAutoRefire(
                        stack,
                        ctx,
                        lastState,
                        0,
                        index,
                        () -> {
                            return ItemGunBaseNT.getSecondary(stack, index);
                        });
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ZEBRA =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_STG = (stack, ctx) -> {};

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_G3_ANIMS =
            (stack, type) -> {
                boolean empty =
                        ((ItemGunBaseNT) stack.getItem())
                                        .getConfig(stack, 0)
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getAmount(
                                                stack, ClientPlayerAccess.player().getInventory())
                                <= 0;
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(45, 0, 0, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "BOLT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 20)
                                                .addPos(0, 0, -4.5, 40)
                                                .addPos(0, 0, 0, 40))
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(
                                                        0,
                                                        0,
                                                        (ItemGunBaseNT.getIsAiming(stack)
                                                                        || !XWeaponModManager
                                                                                .hasUpgrade(
                                                                                        stack,
                                                                                        0,
                                                                                        XWeaponModManager
                                                                                                .ID_NO_STOCK))
                                                                ? -0.25
                                                                : -0.75,
                                                        25,
                                                        IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 75, IType.SIN_FULL));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "BOLT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -0.3125, 100)
                                                .hold(25)
                                                .addPos(0, 0, -2.75, 130)
                                                .hold(50)
                                                .addPos(0, 0, -2.4375, 50)
                                                .addPos(0, 0, 0, 85))
                                .addBus(
                                        "PLUG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .hold(125)
                                                .addPos(0, 0, -2.4375, 130)
                                                .hold(100)
                                                .addPos(0, 0, 0, 85))
                                .addBus(
                                        "HANDLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 90, 0, 100)
                                                .hold(25)
                                                .hold(180)
                                                .addPos(0, 0, 0, 50))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 400)
                                                .addPos(-1, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, -8, 0, 250, IType.SIN_UP)
                                                .addPos(0, -8, 0, 1050)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "BOLT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 200)
                                                .addPos(0, 0, -0.3125, 100)
                                                .hold(10)
                                                .addPos(0, 0, -3.25, 200)
                                                .holdUntil(1875)
                                                .addPos(0, 0, -2.9375, 50)
                                                .addPos(0, 0, 0, 100))
                                .addBus(
                                        "PLUG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 310)
                                                .addPos(0, 0, -2.9375, 200)
                                                .holdUntil(1925)
                                                .addPos(0, 0, 0, 100))
                                .addBus(
                                        "HANDLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 200)
                                                .addPos(0, 90, 0, 100)
                                                .hold(210)
                                                .addPos(0, 90, 45, 75)
                                                .holdUntil(1775)
                                                .addPos(0, 90, 0, 100)
                                                .addPos(0, 0, 0, 50))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 750)
                                                .addPos(-25, 0, 0, 500, IType.SIN_FULL)
                                                .holdUntil(1550)
                                                .addPos(-26, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(-25, 0, 0, 100, IType.SIN_FULL)
                                                .holdUntil(2000)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new BusAnimationSequence()
                                                .addPos(empty ? 1 : 0, 0, 0, 0)
                                                .addPos(0, 0, 0, 1000));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, -1, 0, 150)
                                                .addPos(2, -1, 0, 150)
                                                .addPos(2, 8, 0, 350, IType.SIN_DOWN)
                                                .addPos(2, -2, 0, 350, IType.SIN_UP)
                                                .addPos(2, -1, 0, 50)
                                                .addPos(2, -1, 0, 100)
                                                .addPos(0, -1, 0, 150, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "SPEEN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 300)
                                                .addPos(0, 360, 360, 700))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 1450)
                                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new BusAnimationSequence().addPos(empty ? 1 : 0, 0, 0, 0));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(-25, 0, 0, 250, IType.SIN_FULL)
                                                .addPos(-25, 0, 0, 1250)
                                                .addPos(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "BOLT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 1000)
                                                .addPos(0, 0, -3.25, 150)
                                                .addPos(0, 0, 0, 100)
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -3.25, 150)
                                                .addPos(0, 0, 0, 100))
                                .addBus(
                                        "PLUG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 1000)
                                                .addPos(0, 0, -3.25, 150)
                                                .addPos(0, 0, 0, 100)
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -3.25, 150)
                                                .addPos(0, 0, 0, 100));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_STG77_ANIMS =
            (stack, type) -> {
                if (GunVisualConfig.legacyAnimations) {
                    switch (type) {
                        case EQUIP:
                            return new BusAnimation()
                                    .addBus(
                                            "EQUIP",
                                            new BusAnimationSequence()
                                                    .addPos(45, 0, 0, 0)
                                                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
                        case CYCLE:
                            return new BusAnimation()
                                    .addBus(
                                            "RECOIL",
                                            new BusAnimationSequence()
                                                    .addPos(
                                                            0,
                                                            0,
                                                            ItemGunBaseNT.getIsAiming(stack)
                                                                    ? -0.125
                                                                    : -0.375,
                                                            25,
                                                            IType.SIN_DOWN)
                                                    .addPos(0, 0, 0, 75, IType.SIN_FULL))
                                    .addBus(
                                            "SAFETY",
                                            new BusAnimationSequence()
                                                    .addPos(0.25, 0, 0, 0)
                                                    .addPos(0.25, 0, 0, 2000)
                                                    .addPos(0, 0, 0, 50));
                        case CYCLE_DRY:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 250)
                                                    .addPos(0, 0, -2, 150)
                                                    .addPos(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "SAFETY",
                                            new BusAnimationSequence()
                                                    .addPos(0.25, 0, 0, 0)
                                                    .addPos(0.25, 0, 0, 2000)
                                                    .addPos(0, 0, 0, 50));
                        case RELOAD:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, -2, 150)
                                                    .addPos(0, 0, -2, 1600)
                                                    .addPos(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "HANDLE",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 150)
                                                    .addPos(0, 0, 20, 50)
                                                    .addPos(0, 0, 20, 1500)
                                                    .addPos(0, 0, 0, 50))
                                    .addBus(
                                            "LIFT",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 200)
                                                    .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                    .addPos(0, 0, 0, 100, IType.SIN_FULL));
                        case INSPECT:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, -2, 150)
                                                    .addPos(0, 0, -2, 6100)
                                                    .addPos(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "HANDLE",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 150)
                                                    .addPos(0, 0, 20, 50)
                                                    .addPos(0, 0, 20, 6000)
                                                    .addPos(0, 0, 0, 50))
                                    .addBus(
                                            "INSPECT_LEVER",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 500)
                                                    .addPos(0, 0, -10, 100)
                                                    .addPos(0, 0, -10, 100)
                                                    .addPos(0, 0, 0, 100))
                                    .addBus(
                                            "INSPECT_BARREL",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 600)
                                                    .addPos(0, 0, 20, 150)
                                                    .addPos(0, 0, 0, 400)
                                                    .addPos(0, 0, 0, 500)
                                                    .addPos(15, 0, 0, 500)
                                                    .addPos(15, 0, 0, 2000)
                                                    .addPos(0, 0, 0, 500)
                                                    .addPos(0, 0, 0, 500)
                                                    .addPos(0, 0, 20, 200)
                                                    .addPos(0, 0, 20, 400)
                                                    .addPos(0, 0, 0, 150))
                                    .addBus(
                                            "INSPECT_MOVE",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 750)
                                                    .addPos(0, 0, 6, 1000)
                                                    .addPos(2, 0, 3, 500, IType.SIN_FULL)
                                                    .addPos(2, 0.75, 0, 500, IType.SIN_FULL)
                                                    .addPos(2, 0.75, 0, 1000)
                                                    .addPos(2, 0, 3, 500, IType.SIN_FULL)
                                                    .addPos(0, 0, 6, 500)
                                                    .addPos(0, 0, 0, 1000))
                                    .addBus(
                                            "INSPECT_GUN",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 1750)
                                                    .addPos(15, 0, -70, 500, IType.SIN_FULL)
                                                    .addPos(15, 0, -70, 1500)
                                                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
                    }
                } else {
                    switch (type) {
                        case EQUIP:
                            return new BusAnimation()
                                    .addBus(
                                            "EQUIP",
                                            new BusAnimationSequence()
                                                    .addPos(45, 0, 0, 0)
                                                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
                        case CYCLE:
                            return new BusAnimation()
                                    .addBus(
                                            "RECOIL",
                                            new BusAnimationSequence()
                                                    .addPos(
                                                            0,
                                                            0,
                                                            ItemGunBaseNT.getIsAiming(stack)
                                                                    ? -0.125
                                                                    : -0.375,
                                                            25,
                                                            IType.SIN_DOWN)
                                                    .addPos(0, 0, 0, 75, IType.SIN_FULL))
                                    .addBus(
                                            "SAFETY",
                                            new BusAnimationSequence()
                                                    .addPos(0.25, 0, 0, 0)
                                                    .addPos(0.25, 0, 0, 2000)
                                                    .addPos(0, 0, 0, 50));
                        case CYCLE_DRY:
                            return ResourceManager.stg77_anim.get("FireDry");
                        case RELOAD:
                            return ResourceManager.stg77_anim.get("Reload");
                        case INSPECT:
                            return ResourceManager.stg77_anim.get("Inspect");
                    }
                }

                return null;
            };

    public static void init(IRegistrar r) {
        SpentCasing casing556 =
                new SpentCasing(CasingType.BOTTLENECK)
                        .setColor(SpentCasing.COLOR_CASE_BRASS)
                        .setScale(0.8F);
        r556_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_SP)
                        .setCasing(EnumCasingType.SMALL, 8)
                        .setCasing(casing556.clone().register("r556"));
        r556_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_FMJ)
                        .setCasing(EnumCasingType.SMALL, 8)
                        .setDamage(0.8F)
                        .setThresholdNegation(4F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing556.clone().register("r556fmj"));
        r556_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_JHP)
                        .setCasing(EnumCasingType.SMALL, 8)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing556.clone().register("r556jhp"));
        r556_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_AP)
                        .setCasing(EnumCasingType.SMALL_STEEL, 8)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.15F)
                        .setCasing(
                                casing556
                                        .clone()
                                        .setColor(SpentCasing.COLOR_CASE_44)
                                        .register("r556ap"));

        r556_inc_sp = r556_sp.clone().setOnImpact(INCENDIARY);
        r556_inc_fmj = r556_fmj.clone().setOnImpact(INCENDIARY);
        r556_inc_jhp = r556_jhp.clone().setOnImpact(INCENDIARY);
        r556_inc_ap = r556_ap.clone().setOnImpact(INCENDIARY);

        ModItems.GUN_G3 =
                r.registerItem(
                        "gun_g3",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(3_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(5F)
                                                                        .delay(2)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .reload(50)
                                                                        .jam(47)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ASSAULT_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                r556_sp,
                                                                                                r556_fmj,
                                                                                                r556_jhp,
                                                                                                r556_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_G3))
                                                        .setupStandardConfiguration()
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY)
                                                        .anim(LAMBDA_G3_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_G3))
                                        .setDefaultAmmo(EnumAmmo.R556_SP, 30)
                                        .setNameMutator(LAMBDA_NAME_G3),
                        Item.Properties::new);
        ModItems.GUN_G3_ZEBRA =
                r.registerItem(
                        "gun_g3_zebra",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(6_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .scopeTexture(scope)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(7.5F)
                                                                        .delay(2)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .spreadHipfire(0.01F)
                                                                        .reload(50)
                                                                        .jam(47)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_RIFLE_SILENCER
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                r556_inc_sp,
                                                                                                r556_inc_fmj,
                                                                                                r556_inc_jhp,
                                                                                                r556_inc_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ZEBRA))
                                                        .setupStandardConfiguration()
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY)
                                                        .anim(LAMBDA_G3_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_G3))
                                        .setDefaultAmmo(EnumAmmo.R556_JHP, 30)
                                        .setNameMutator(LAMBDA_NAME_G3),
                        Item.Properties::new);

        ModItems.GUN_STG77 =
                r.registerItem(
                        "gun_stg77",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(3_000)
                                                        .draw(10)
                                                        .inspect(125)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .scopeTexture(scope)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(10F)
                                                                        .delay(2)
                                                                        .dry(15)
                                                                        .auto(true)
                                                                        .reload(46)
                                                                        .jam(0)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ASSAULT_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                r556_sp,
                                                                                                r556_fmj,
                                                                                                r556_jhp,
                                                                                                r556_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_STG))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .pt(Lego.LAMBDA_TOGGLE_AIM)
                                                        .decider(LAMBDA_STG77_DECIDER)
                                                        .anim(LAMBDA_STG77_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_STG77))
                                        .setDefaultAmmo(EnumAmmo.R556_FMJ, 30),
                        Item.Properties::new);
    }
}
