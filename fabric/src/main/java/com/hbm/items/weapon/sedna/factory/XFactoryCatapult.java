// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.main.Polaroid;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactoryCatapult {

    public static BulletConfig nuke_standard;
    public static BulletConfig nuke_demo;
    public static BulletConfig nuke_high;
    public static BulletConfig nuke_tots;
    public static BulletConfig nuke_hive;
    public static BulletConfig nuke_balefire;

    public static BulletConfig cluster_submunition;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_STANDARD =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();

                ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 10);
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                incrementRad(bullet.level(), hit.x, hit.y, hit.z, 1F);
                spawnMush(bullet, mop);
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_DEMO =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();

                ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 15);
                vnt.setBlockAllocator(new BlockAllocatorStandard(64));
                vnt.setBlockProcessor(
                        new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                incrementRad(bullet.level(), hit.x, hit.y, hit.z, 1.5F);
                spawnMush(bullet, mop);
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_HIGH =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();
                bullet.level()
                        .addFreshEntity(
                                EntityNukeExplosionMK5.statFac(
                                        bullet.level(), 35, hit.x, hit.y, hit.z));
                spawnMush(bullet, mop);
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_BALEFIRE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();

                ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 10);
                vnt.setBlockAllocator(new BlockAllocatorStandard(64));
                vnt.setBlockProcessor(
                        new BlockProcessorStandard().withBlockEffect(new BlockMutatorBalefire()));
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                incrementRad(bullet.level(), hit.x, hit.y, hit.z, 1.5F);

                if (bullet.level() instanceof ServerLevel server) {
                    SatelliteDetector.reportEvent(
                            server,
                            SatelliteDetector.DURATION_LOW,
                            SatelliteDetector.BurstIntensity.LOW,
                            bullet.getX(),
                            bullet.getZ());
                }

                bullet.level()
                        .playSound(
                                null,
                                hit.x,
                                hit.y + 0.5,
                                hit.z,
                                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                                SoundSource.BLOCKS,
                                15.0F,
                                1.0F);
                sendMuke(bullet.level(), hit, false, true);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_TINYTOT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();

                ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 5);
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                incrementRad(bullet.level(), hit.x, hit.y, hit.z, 0.25F);
                if (bullet.level() instanceof ServerLevel server) {
                    SatelliteDetector.reportEvent(
                            server,
                            SatelliteDetector.DURATION_LOW,
                            SatelliteDetector.BurstIntensity.LOW,
                            bullet.getX(),
                            bullet.getZ());
                }
                bullet.level()
                        .playSound(
                                null,
                                hit.x,
                                hit.y + 0.5,
                                hit.z,
                                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                                SoundSource.BLOCKS,
                                15.0F,
                                1.0F);

                sendMuke(
                        bullet.level(),
                        hit,
                        true,
                        Polaroid.isBalefireDay() || bullet.level().getRandom().nextInt(100) == 0);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_HIVE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();
                ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 5);
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(1, bullet.damage).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_SUBMUNITION =
            (bullet, mop) -> {
                Vec3 hit = mop.getLocation();
                ExplosionVNT vnt =
                        new ExplosionVNT(
                                bullet.level(), hit.x, hit.y, hit.z, 7.5F, bullet.getThrower());
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_FATMAN = (stack, ctx) -> {};

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FATMAN_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 0)
                                                .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
                    case CYCLE:
                        RandomSource rand = ClientPlayerAccess.player().getRandom();
                        return new BusAnimation()
                                .addBus(
                                        "GAUGE",
                                        new BusAnimationSequence()
                                                .addPos(
                                                        0,
                                                        0,
                                                        135 + rand.nextInt(136),
                                                        100,
                                                        IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN))
                                .addBus(
                                        "PISTON",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 3, 100, IType.SIN_UP))
                                .addBus(
                                        "NUKE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 3, 100, IType.SIN_UP)
                                                .addPos(0, 0, 0, 0));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LID",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -45, 250, IType.SIN_UP)
                                                .addPos(0, 0, -45, 1200)
                                                .addPos(0, 0, 0, 250, IType.SIN_UP))
                                .addBus(
                                        "HANDLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -2, 500, IType.SIN_FULL)
                                                .addPos(0, 0, -2, 1700)
                                                .addPos(0, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "NUKE",
                                        new BusAnimationSequence()
                                                .addPos(5, -4, 3, 0)
                                                .addPos(5, -4, 3, 750)
                                                .addPos(2, 0.5, 3, 500, IType.SIN_UP)
                                                .addPos(1, 0.5, 3, 100)
                                                .addPos(0, 0, 3, 100)
                                                .addPos(0, 0, 3, 750)
                                                .addPos(0, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "PISTON",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 3, 0)
                                                .addPos(0, 0, 3, 2200)
                                                .addPos(0, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(5, 0, 0, 500, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 450)
                                                .addPos(3, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 500)
                                                .addPos(-10, 0, 0, 375, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 375, IType.SIN_UP));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "HANDLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 750)
                                                .addPos(0, 0, -2, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL)
                                                .addPos(0, 0, -2, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(-15, 0, 0, 250, IType.SIN_FULL)
                                                .addPos(-15, 0, 0, 1000)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "HANDLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 250)
                                                .addPos(0, 0, -2, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL)
                                                .addPos(0, 0, -2, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-15, 0, 0, 250, IType.SIN_FULL)
                                                .addPos(-15, 0, 0, 1000)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL));
                }
                return null;
            };

    public static void incrementRad(
            Level world, double posX, double posY, double posZ, float mult) {
        if (!(world instanceof ServerLevel server)) return;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) < 4) {
                    double amount = 50F / (Math.abs(i) + Math.abs(j) + 1) * mult;
                    BlockPos pos = BlockPos.containing(posX + i * 16, posY, posZ + j * 16);
                    RadiationSystemNT.incrementRad(server, pos, amount, amount * 1024D + 1D);
                }
            }
        }
    }

    public static void spawnMush(EntityBulletBaseMK4 bullet, HitResult mop) {
        Vec3 hit = mop.getLocation();
        if (bullet.level() instanceof ServerLevel server) {
            SatelliteDetector.reportEvent(
                    server,
                    SatelliteDetector.DURATION_LOW,
                    SatelliteDetector.BurstIntensity.LOW,
                    bullet.getX(),
                    bullet.getZ());
        }
        bullet.level()
                .playSound(
                        null,
                        hit.x,
                        hit.y + 0.5,
                        hit.z,
                        ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                        SoundSource.BLOCKS,
                        15.0F,
                        1.0F);
        sendMuke(
                bullet.level(),
                hit,
                false,
                Polaroid.isBalefireDay() || bullet.level().getRandom().nextInt(100) == 0);
    }

    private static void sendMuke(Level level, Vec3 hit, boolean tinytot, boolean balefire) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new MukePayload(hit.x, hit.y + 0.5, hit.z, tinytot, balefire),
                new TargetPoint(server, hit.x, hit.y, hit.z, 250));
    }

    public static void init(IRegistrar r) {

        nuke_standard =
                new BulletConfig()
                        .setItem(EnumAmmo.NUKE_STANDARD)
                        .setLife(300)
                        .setVel(3F)
                        .setGrav(0.025F)
                        .setOnImpact(LAMBDA_NUKE_STANDARD);
        nuke_demo =
                new BulletConfig()
                        .setItem(EnumAmmo.NUKE_DEMO)
                        .setLife(300)
                        .setVel(3F)
                        .setGrav(0.025F)
                        .setOnImpact(LAMBDA_NUKE_DEMO);
        nuke_high =
                new BulletConfig()
                        .setItem(EnumAmmo.NUKE_HIGH)
                        .setLife(300)
                        .setVel(3F)
                        .setGrav(0.025F)
                        .setOnImpact(LAMBDA_NUKE_HIGH);
        nuke_tots =
                new BulletConfig()
                        .setItem(EnumAmmo.NUKE_TOTS)
                        .setProjectiles(8)
                        .setLife(300)
                        .setVel(3F)
                        .setGrav(0.025F)
                        .setSpread(0.1F)
                        .setDamage(0.35F)
                        .setOnImpact(LAMBDA_NUKE_TINYTOT);
        nuke_hive =
                new BulletConfig()
                        .setItem(EnumAmmo.NUKE_HIVE)
                        .setProjectiles(12)
                        .setLife(300)
                        .setVel(1F)
                        .setGrav(0.025F)
                        .setSpread(0.15F)
                        .setDamage(0.25F)
                        .setOnImpact(LAMBDA_NUKE_HIVE);
        nuke_balefire =
                new BulletConfig()
                        .setItem(EnumAmmo.NUKE_BALEFIRE)
                        .setDamage(2.5F)
                        .setLife(300)
                        .setVel(3F)
                        .setGrav(0.025F)
                        .setOnImpact(LAMBDA_NUKE_BALEFIRE);

        cluster_submunition =
                new BulletConfig().setLife(1_200).setGrav(0.025F).setOnImpact(LAMBDA_SUBMUNITION);

        ModItems.GUN_FATMAN =
                r.registerItem(
                        "gun_fatman",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(300)
                                                        .draw(20)
                                                        .inspect(30)
                                                        .reloadChangeType(true)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .hideCrosshair(false)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(100F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(10)
                                                                        .reload(57)
                                                                        .jam(40)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_FATMAN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                nuke_standard,
                                                                                                nuke_demo,
                                                                                                nuke_high,
                                                                                                nuke_tots,
                                                                                                nuke_hive,
                                                                                                nuke_balefire))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .offsetScoped(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.125D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_FATMAN))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FATMAN_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_FATMAN))
                                        .setDefaultAmmoExpensive(EnumAmmo.NUKE_STANDARD, 1),
                        Item.Properties::new);
    }
}
