// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import com.hbm.util.DamageClass;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public class XFactoryFlamer {

    public static BulletConfig flame_nograv;
    public static BulletConfig flame_nograv_bf;

    public static BulletConfig flame_diesel;
    public static BulletConfig flame_gas;
    public static BulletConfig flame_napalm;
    public static BulletConfig flame_balefire;

    public static BulletConfig flame_topaz_diesel;
    public static BulletConfig flame_topaz_gas;
    public static BulletConfig flame_topaz_napalm;
    public static BulletConfig flame_topaz_balefire;

    public static BulletConfig flame_daybreaker_diesel;
    public static BulletConfig flame_daybreaker_gas;
    public static BulletConfig flame_daybreaker_napalm;
    public static BulletConfig flame_daybreaker_balefire;

    public static Consumer<Entity> LAMBDA_FIRE =
            (bullet) -> {
                if (bullet.level().isClientSide() && nearClientPlayer(bullet))
                    FlameCreator.composeEffectClient(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY() - 0.125,
                            bullet.getZ(),
                            FlameCreator.META_FIRE);
            };
    public static Consumer<Entity> LAMBDA_BALEFIRE =
            (bullet) -> {
                if (bullet.level().isClientSide() && nearClientPlayer(bullet))
                    FlameCreator.composeEffectClient(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY() - 0.125,
                            bullet.getZ(),
                            FlameCreator.META_BALEFIRE);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_IGNITE_FIRE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    HbmLivingProps props = HbmLivingProps.getData(living);
                    if (props.fire < 100) props.fire = 100;
                }
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_IGNITE_BALEFIRE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    HbmLivingProps props = HbmLivingProps.getData(living);
                    if (props.balefire < 200) props.balefire = 200;
                }
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_DIESEL =
            (bullet, mop) -> {
                if (!igniteIfPossible(bullet, mop))
                    spawnFire(bullet, mop, 2F, 1F, 100, EntityFireLingering.TYPE_DIESEL);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_GAS =
            (bullet, mop) -> {
                igniteIfPossible(bullet, mop);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_NAPALM =
            (bullet, mop) -> {
                if (!igniteIfPossible(bullet, mop))
                    spawnFire(bullet, mop, 2.5F, 1F, 200, EntityFireLingering.TYPE_DIESEL);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_BALEFIRE =
            (bullet, mop) -> {
                spawnFire(bullet, mop, 3F, 1F, 300, EntityFireLingering.TYPE_BALEFIRE);
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAMER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-45, 0, 0, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                    case RELOAD:
                        return ResourceManager.flamethrower_anim.get("Reload");
                    case INSPECT:
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 45, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 45, 350)
                                                .addPos(0, 0, -15, 150, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CHEMTHROWER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-45, 0, 0, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                }

                return null;
            };

    private static boolean nearClientPlayer(Entity bullet) {
        Player player = ClientPlayerAccess.player();
        return player != null && player.getEyePosition().distanceTo(bullet.position()) < 100;
    }

    public static boolean igniteIfPossible(EntityBulletBaseMK4 bullet, HitResult mop) {
        if (mop instanceof BlockHitResult blockHit) {
            Level world = bullet.level();
            BlockPos pos = blockHit.getBlockPos();
            Direction dir = blockHit.getDirection();
            BlockState state = world.getBlockState(pos);
            if (Services.PLATFORM.isFlammable(world, pos, state, dir)) {
                BlockPos target = pos.relative(dir);
                if (world.getBlockState(target).isAir()) {
                    world.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                    return true;
                }
            }
            bullet.discard();
        }
        return false;
    }

    public static void spawnFire(
            EntityBulletBaseMK4 bullet,
            HitResult mop,
            float width,
            float height,
            int duration,
            int type) {
        if (mop instanceof BlockHitResult) {
            Vec3 hit = mop.getLocation();
            List<EntityFireLingering> fires =
                    bullet.level()
                            .getEntitiesOfClass(
                                    EntityFireLingering.class,
                                    new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                                            .inflate(
                                                    width / 2 + 0.5,
                                                    height / 2 + 0.5,
                                                    width / 2 + 0.5));
            if (fires.isEmpty()) {
                EntityFireLingering fire =
                        new EntityFireLingering(bullet.level())
                                .setArea(width, height)
                                .setDuration(duration)
                                .setType(type);
                fire.setPos(hit);
                bullet.level().addFreshEntity(fire);
            }
            bullet.discard();
        }
    }

    private static ItemStack plateSteel(int count) {
        return new ItemStack(
                BuiltInRegistries.ITEM.getOptional(Library.id("plate_steel")).orElseThrow(), count);
    }

    public static void init(IRegistrar r) {
        flame_diesel =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_DIESEL)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(DamageClass.FIRE)
                        .setLife(100)
                        .setVel(1F)
                        .setGrav(0.02D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_FIRE)
                        .setOnUpdate(LAMBDA_FIRE)
                        .setOnRicochet(LAMBDA_LINGER_DIESEL);
        flame_gas =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_GAS)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(DamageClass.FIRE)
                        .setLife(10)
                        .setSpread(0.05F)
                        .setVel(1F)
                        .setGrav(0.0D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_FIRE)
                        .setOnUpdate(LAMBDA_FIRE)
                        .setOnRicochet(LAMBDA_LINGER_GAS);
        flame_napalm =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_NAPALM)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(DamageClass.FIRE)
                        .setLife(200)
                        .setVel(1F)
                        .setGrav(0.02D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_FIRE)
                        .setOnUpdate(LAMBDA_FIRE)
                        .setOnRicochet(LAMBDA_LINGER_NAPALM);
        flame_balefire =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_BALEFIRE)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(DamageClass.FIRE)
                        .setLife(200)
                        .setVel(1F)
                        .setGrav(0.02D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_BALEFIRE)
                        .setOnUpdate(LAMBDA_BALEFIRE)
                        .setOnRicochet(LAMBDA_LINGER_BALEFIRE);

        flame_nograv = flame_diesel.clone().setGrav(0);
        flame_nograv_bf = flame_balefire.clone().setGrav(0).setLife(100);

        flame_topaz_diesel =
                flame_diesel.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);
        flame_topaz_gas = flame_gas.clone().setProjectiles(2).setSpread(0.05F);
        flame_topaz_napalm =
                flame_napalm.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);
        flame_topaz_balefire =
                flame_balefire.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);

        flame_daybreaker_diesel =
                flame_diesel
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    spawnFire(
                                            bullet,
                                            mop,
                                            6F,
                                            2F,
                                            200,
                                            EntityFireLingering.TYPE_DIESEL);
                                    bullet.discard();
                                });
        flame_daybreaker_gas =
                flame_gas
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    bullet.discard();
                                });
        flame_daybreaker_napalm =
                flame_napalm
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 7.5F);
                                    spawnFire(
                                            bullet,
                                            mop,
                                            6F,
                                            2F,
                                            300,
                                            EntityFireLingering.TYPE_DIESEL);
                                    bullet.discard();
                                });
        flame_daybreaker_balefire =
                flame_balefire
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    spawnFire(
                                            bullet,
                                            mop,
                                            7.5F,
                                            2.5F,
                                            400,
                                            EntityFireLingering.TYPE_BALEFIRE);
                                    bullet.discard();
                                });

        ModItems.GUN_FLAMER =
                r.registerItem(
                        "gun_flamer",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(20_000)
                                                        .draw(10)
                                                        .inspect(17)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(1F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .reload(90)
                                                                        .jam(17)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                300)
                                                                                        .addConfigs(
                                                                                                flame_diesel,
                                                                                                flame_gas,
                                                                                                flame_napalm,
                                                                                                flame_balefire))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.25D)
                                                                        .setupStandardFire())
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAMER_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_FLAMER))
                                        .setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1),
                        Item.Properties::new);
        ModItems.GUN_FLAMER_TOPAZ =
                r.registerItem(
                        "gun_flamer_topaz",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(20_000)
                                                        .draw(10)
                                                        .inspect(17)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(1.5F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .reload(90)
                                                                        .jam(17)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                500)
                                                                                        .addConfigs(
                                                                                                flame_topaz_diesel,
                                                                                                flame_topaz_gas,
                                                                                                flame_topaz_napalm,
                                                                                                flame_topaz_balefire))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.25D)
                                                                        .setupStandardFire())
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAMER_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_FLAMER))
                                        .setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1),
                        Item.Properties::new);
        ModItems.GUN_FLAMER_DAYBREAKER =
                r.registerItem(
                        "gun_flamer_daybreaker",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                new GunConfig()
                                                        .dura(20_000)
                                                        .draw(10)
                                                        .inspect(17)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(25F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(10)
                                                                        .auto(true)
                                                                        .reload(90)
                                                                        .jam(17)
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
                                                                                                50)
                                                                                        .addConfigs(
                                                                                                flame_daybreaker_diesel,
                                                                                                flame_daybreaker_gas,
                                                                                                flame_daybreaker_napalm,
                                                                                                flame_daybreaker_balefire))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.25D)
                                                                        .setupStandardFire())
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAMER_ANIMS)
                                                        .orchestra(
                                                                Orchestras
                                                                        .ORCHESTRA_FLAMER_DAYBREAKER))
                                        .setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1),
                        Item.Properties::new);

        ModItems.GUN_CHEMTHROWER =
                r.registerItem(
                        "gun_chemthrower",
                        props ->
                                new ItemGunChemthrower(
                                        WeaponQuality.A_SIDE,
                                        props,
                                        new GunConfig()
                                                .dura(90_000)
                                                .draw(10)
                                                .inspect(17)
                                                .crosshair(Crosshair.L_CIRCLE)
                                                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                .rec(
                                                        new Receiver(0)
                                                                .delay(1)
                                                                .spreadHipfire(0F)
                                                                .auto(true)
                                                                .mag(new MagazineFluid(0, 3_000))
                                                                .offset(0.75, -0.0625, -0.25D)
                                                                .canFire(
                                                                        ItemGunChemthrower
                                                                                .LAMBDA_CAN_FIRE)
                                                                .fire(
                                                                        ItemGunChemthrower
                                                                                .LAMBDA_FIRE))
                                                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_CHEMTHROWER_ANIMS)
                                                .orchestra(Orchestras.ORCHESTRA_CHEMTHROWER)),
                        Item.Properties::new);
    }
}
