// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.SpentCasing;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory75Bolt {

    public static BulletConfig b75;
    public static BulletConfig b75_inc;
    public static BulletConfig b75_exp;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_TINY_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 2F);
                bullet.discard();
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_INC =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    HbmLivingProps data = HbmLivingProps.getData(living);
                    if (data.phosphorus < 300) data.phosphorus = 300;
                }
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                Lego.standardExplode(bullet, mop, 5F);
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 2000, 0.05D, 1.1D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_BOLT =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_BOLTER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(1, 0, 0, 25)
                                                .addPos(0, 0, 0, 75));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new BusAnimationSequence()
                                                .addPos(1, 0, 0, 250)
                                                .addPos(1, 0, 0, 1500)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 1, 500)
                                                .addPos(1, 0, 1, 500)
                                                .addPos(0, 0, 0, 500));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(1, 0, 0, 250)
                                                .addPos(1, 0, 0, 700)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 750)
                                                .addPos(0.6, 0, 0, 250)
                                                .addPos(0, 0, 0, 250));
                }

                return null;
            };

    public static void init(IRegistrar r) {
        SpentCasing casing75 =
                new SpentCasing(CasingType.STRAIGHT)
                        .setColor(SpentCasing.COLOR_CASE_BRASS)
                        .setScale(2F, 2F, 1.5F);

        b75 =
                new BulletConfig()
                        .setItem(EnumAmmo.B75)
                        .setCasing(casing75.clone().register("b75"))
                        .setOnImpact(LAMBDA_TINY_EXPLODE);
        b75_inc =
                new BulletConfig()
                        .setItem(EnumAmmo.B75_INC)
                        .setDamage(0.8F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing75.clone().register("b75inc"))
                        .setOnImpact(LAMBDA_INC);
        b75_exp =
                new BulletConfig()
                        .setItem(EnumAmmo.B75_EXP)
                        .setDamage(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing75.clone().register("b75exp"))
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE);

        ModItems.GUN_BOLTER =
                r.registerItem(
                        "gun_bolter",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.SPECIAL,
                                                props,
                                                new GunConfig()
                                                        .dura(3_000)
                                                        .draw(20)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(15F)
                                                                        .delay(2)
                                                                        .auto(true)
                                                                        .spread(0.005F)
                                                                        .reload(40)
                                                                        .jam(55)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_POWDER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                b75,
                                                                                                b75_inc,
                                                                                                b75_exp))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_BOLT))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_BOLTER_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_BOLTER))
                                        .setDefaultAmmo(EnumAmmo.B75, 15),
                        Item.Properties::new);
    }
}
