// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.logic.EntityC130.C130PayloadType;
import com.hbm.entity.logic.EntityC130;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.EnumCasingType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
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
import com.hbm.util.DamageClass;
import com.hbm.util.EntityDamageUtil;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactory40mm {

    public static BulletConfig g26_flare;
    public static BulletConfig g26_flare_supply;
    public static BulletConfig g26_flare_weapon;

    public static BulletConfig g40_he;
    public static BulletConfig g40_heat;
    public static BulletConfig g40_demo;
    public static BulletConfig g40_inc;
    public static BulletConfig g40_phosphorus;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_IGNITE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    HbmLivingProps props = HbmLivingProps.getData(living);
                    props.fire += 200;
                }
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                Lego.standardExplode(bullet, mop, 5F);
                bullet.discard();
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_HEAT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.standardExplode(bullet, mop, 3.5F);
                bullet.discard();
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    EntityDamageUtil.attackEntityFromNT(
                            living,
                            BulletConfig.getDamage(
                                    bullet, bullet.getThrower(), DamageClass.EXPLOSIVE),
                            bullet.damage * 3F,
                            true,
                            true,
                            0.5F,
                            3F,
                            0.15F);
                } else if (mop instanceof EntityHitResult entityHit) {
                    entityHit
                            .getEntity()
                            .hurtServer(
                                    (ServerLevel) bullet.level(),
                                    BulletConfig.getDamage(
                                            bullet, bullet.getThrower(), DamageClass.EXPLOSIVE),
                                    bullet.damage * 3F);
                }
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_DEMO =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Vec3 hit = mop.getLocation();
                ExplosionVNT vnt =
                        new ExplosionVNT(
                                bullet.level(), hit.x, hit.y, hit.z, 5F, bullet.getThrower());
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                bullet.discard();
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_INC =
            (bullet, mop) -> {
                spawnFire(bullet, mop, false, 200);
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_PHOSPHORUS =
            (bullet, mop) -> {
                spawnFire(bullet, mop, true, 400);
            };
    public static Consumer<Entity> LAMBDA_SPAWN_C130_SUPPLIESS =
            (entity) -> {
                spawnPlane(entity, C130PayloadType.SUPPLIES);
            };
    public static Consumer<Entity> LAMBDA_SPAWN_C130_WEAPONS =
            (entity) -> {
                spawnPlane(entity, C130PayloadType.WEAPONS);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 1500, 0.025D, 1.05D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_GL =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MK108 =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian()) + 1F,
                        (float) (ctx.getPlayer().getRandom().nextGaussian()));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAREGUN_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-90, 0, 0, 0)
                                                .addPos(0, 0, 0, 350, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 50)
                                                .addPos(0, 0, -3, 50)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 50)
                                                .addPos(15, 0, 0, 550)
                                                .addPos(0, 0, 0, 100));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 50)
                                                .addPos(15, 0, 0, 550)
                                                .addPos(0, 0, 0, 100));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "OPEN",
                                        new BusAnimationSequence()
                                                .addPos(45, 0, 0, 200, IType.SIN_FULL)
                                                .addPos(45, 0, 0, 750)
                                                .addPos(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "SHELL",
                                        new BusAnimationSequence()
                                                .addPos(4, -8, -4, 0)
                                                .addPos(4, -8, -4, 200)
                                                .addPos(0, 0, -5, 500, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "FLIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 200)
                                                .addPos(25, 0, 0, 200, IType.SIN_DOWN)
                                                .addPos(25, 0, 0, 800)
                                                .addPos(0, 0, 0, 200, IType.SIN_DOWN));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "OPEN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(45, 0, 0, 200, IType.SIN_FULL)
                                                .addPos(45, 0, 0, 500)
                                                .addPos(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "FLIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(0, 0, 0, 200)
                                                .addPos(25, 0, 0, 200, IType.SIN_DOWN)
                                                .addPos(25, 0, 0, 550)
                                                .addPos(0, 0, 0, 200, IType.SIN_DOWN));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "FLIP",
                                        new BusAnimationSequence()
                                                .addPos(-360 * 3, 0, 0, 1500, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CONGOLAKE_ANIMS =
            (stack, type) -> {
                int ammo =
                        ((ItemGunBaseNT) stack.getItem())
                                .getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, ClientPlayerAccess.player().getInventory());
                switch (type) {
                    case EQUIP:
                        return ResourceManager.congolake_anim.get("Equip");
                    case CYCLE:
                        return ResourceManager.congolake_anim.get(ammo <= 1 ? "FireEmpty" : "Fire");
                    case RELOAD:
                        return ResourceManager.congolake_anim.get(
                                ammo == 0 ? "ReloadEmpty" : "ReloadStart");
                    case RELOAD_CYCLE:
                        return ResourceManager.congolake_anim.get("Reload");
                    case RELOAD_END:
                        return ResourceManager.congolake_anim.get("ReloadEnd");
                    case JAMMED:
                        return ResourceManager.congolake_anim.get("Jammed");
                    case INSPECT:
                        return ResourceManager.congolake_anim.get("Inspect");
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MK108_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .setPos(45, 0, 0)
                                                .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
                    case CYCLE:
                        int amount =
                                ((ItemGunBaseNT) stack.getItem())
                                        .getConfig(stack, 0)
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getAmount(stack, null);
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .hold(50)
                                                .addPos(0, 0, -0.25, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -1, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "CYCLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 100)
                                                .addPos(1, 0, 0, 150))
                                .addBus(
                                        "SHELLS",
                                        new BusAnimationSequence().setPos(amount - 1, 0, 0));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 50)
                                                .addPos(15, 0, 0, 550)
                                                .addPos(0, 0, 0, 100));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(10, 0, 0, 500, IType.SIN_FULL)
                                                .holdUntil(1250)
                                                .addPos(-50, 0, 0, 750, IType.SIN_FULL)
                                                .holdUntil(5500)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL)
                                                .hold(500)
                                                .addPos(1, 0, 0, 100, IType.SIN_UP)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "LID",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 500, IType.SIN_FULL)
                                                .holdUntil(6000)
                                                .addPos(0, 0, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "BELT",
                                        new BusAnimationSequence()
                                                .setPos(1, 0, 0)
                                                .hold(500)
                                                .addPos(0, 0, 0, 750, IType.SIN_UP)
                                                .holdUntil(4500)
                                                .addPos(1, 0, 0, 750, IType.SIN_UP))
                                .addBus(
                                        "DRUM",
                                        new BusAnimationSequence()
                                                .hold(2000)
                                                .addPos(2.5, 0, 0, 500, IType.SIN_DOWN)
                                                .addPos(2.5, -2, -8, 500, IType.SIN_UP)
                                                .setPos(4, -3, -8)
                                                .addPos(2.5, 0, 0, 1000, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 500, IType.SIN_UP));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LID",
                                        new BusAnimationSequence()
                                                .hold(250)
                                                .addPos(45, 0, 0, 500, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .hold(1000)
                                                .addPos(1, 0, 0, 100, IType.SIN_UP)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL));
                    case INSPECT:
                        int yeetHorizontal = 750;
                        int untilImpact = yeetHorizontal * 9 / 15;
                        int delay = 250;
                        int height = 6;
                        int arcUp = untilImpact * 5 / 8;
                        int arcDown = untilImpact * 3 / 8;
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .hold(untilImpact)
                                                .addPos(1, 0, 0, 50, IType.SIN_UP)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL)
                                                .hold(delay - 150)
                                                .addPos(1, 0, 0, 50, IType.SIN_UP)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL)
                                                .hold(delay - 150)
                                                .addPos(1, 0, 0, 50, IType.SIN_UP)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "GRENH1",
                                        new BusAnimationSequence()
                                                .setPos(9, 0, 0)
                                                .addPos(-6, 0, 0, yeetHorizontal))
                                .addBus(
                                        "GRENV1",
                                        new BusAnimationSequence()
                                                .setPos(0, -2, 0)
                                                .addPos(0, height, 0, arcUp, IType.SIN_DOWN)
                                                .addPos(0, 2, 0, arcDown, IType.SIN_UP)
                                                .addPos(
                                                        0,
                                                        3,
                                                        0,
                                                        yeetHorizontal - untilImpact,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "GRENS1",
                                        new BusAnimationSequence()
                                                .addPos(360 * 2, 0, 0, untilImpact)
                                                .setPos(0, 0, 0)
                                                .addPos(360, 0, 0, yeetHorizontal - untilImpact))
                                .addBus(
                                        "GRENH2",
                                        new BusAnimationSequence()
                                                .setPos(9, 0, 0)
                                                .hold(delay)
                                                .addPos(-6, 0, 0, yeetHorizontal))
                                .addBus(
                                        "GRENV2",
                                        new BusAnimationSequence()
                                                .setPos(0, -2, 0)
                                                .hold(delay)
                                                .addPos(0, height, 0, arcUp, IType.SIN_DOWN)
                                                .addPos(0, 2, 0, arcDown, IType.SIN_UP)
                                                .addPos(
                                                        0,
                                                        3,
                                                        0,
                                                        yeetHorizontal - untilImpact,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "GRENS2",
                                        new BusAnimationSequence()
                                                .hold(delay)
                                                .addPos(360 * 2, 0, 0, untilImpact)
                                                .setPos(0, 0, 0)
                                                .addPos(360, 0, 0, yeetHorizontal - untilImpact))
                                .addBus(
                                        "GRENH3",
                                        new BusAnimationSequence()
                                                .setPos(9, 0, 0)
                                                .hold(delay * 2)
                                                .addPos(-6, 0, 0, yeetHorizontal))
                                .addBus(
                                        "GRENV3",
                                        new BusAnimationSequence()
                                                .setPos(0, -2, 0)
                                                .hold(delay * 2)
                                                .addPos(0, height, 0, arcUp, IType.SIN_DOWN)
                                                .addPos(0, 2, 0, arcDown, IType.SIN_UP)
                                                .addPos(
                                                        0,
                                                        3,
                                                        0,
                                                        yeetHorizontal - untilImpact,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "GRENS3",
                                        new BusAnimationSequence()
                                                .hold(delay * 2)
                                                .addPos(360 * 2, 0, 0, untilImpact)
                                                .setPos(0, 0, 0)
                                                .addPos(360, 0, 0, yeetHorizontal - untilImpact));
                }
                return null;
            };

    public static void spawnFire(
            EntityBulletBaseMK4 bullet, HitResult mop, boolean phosphorus, int duration) {
        if (mop instanceof EntityHitResult entityHit
                && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) return;
        Level world = bullet.level();
        Lego.standardExplode(bullet, mop, 3F);
        EntityFireLingering fire =
                new EntityFireLingering(world)
                        .setArea(5, 2)
                        .setDuration(duration)
                        .setType(
                                phosphorus
                                        ? EntityFireLingering.TYPE_PHOSPHORUS
                                        : EntityFireLingering.TYPE_DIESEL);
        fire.setPos(mop.getLocation());
        world.addFreshEntity(fire);
        bullet.discard();
        Vec3 hit = mop.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = BlockPos.containing(hit.x, hit.y, hit.z).offset(dx, dy, dz);
                    if (world.getBlockState(pos).isAir())
                        for (Direction dir : Direction.VALUES) {
                            BlockPos neighbor = pos.relative(dir);
                            BlockState state = world.getBlockState(neighbor);
                            if (Services.PLATFORM.isFlammable(
                                    world, neighbor, state, dir.getOpposite())) {
                                world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                                break;
                            }
                        }
                }
            }
        }
    }

    public static void spawnPlane(Entity entity, C130PayloadType payload) {
        if (!entity.level().isClientSide() && entity.tickCount == 40) {
            EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) entity;
            if (bullet.getThrower() != null)
                entity.level()
                        .playSound(
                                null,
                                bullet.getThrower(),
                                ModSounds.TECH_BLEEP.get(),
                                SoundSource.PLAYERS,
                                1.0F,
                                1.0F);
            EntityC130 c130 = new EntityC130(ModEntities.C130.get(), bullet.level());
            int x = (int) Math.floor(bullet.getX());
            int z = (int) Math.floor(bullet.getZ());

            int y = bullet.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            c130.fac(bullet.level(), x, y, z, payload);
            bullet.level().addFreshEntity(c130);
        }
    }

    public static void init(IRegistrar r) {

        g26_flare =
                new BulletConfig()
                        .setItem(EnumAmmo.G26_FLARE)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setOnImpact(LAMBDA_STANDARD_IGNITE)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0x9E1616)
                                        .setScale(2F)
                                        .register("g26Flare"));
        g26_flare_supply =
                new BulletConfig()
                        .setItem(EnumAmmo.G26_FLARE_SUPPLY)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setOnImpact(LAMBDA_STANDARD_IGNITE)
                        .setOnUpdate(LAMBDA_SPAWN_C130_SUPPLIESS)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0x3C80F0)
                                        .setScale(2F)
                                        .register("g26FlareSupply"));
        g26_flare_weapon =
                new BulletConfig()
                        .setItem(EnumAmmo.G26_FLARE_WEAPON)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setOnImpact(LAMBDA_STANDARD_IGNITE)
                        .setOnUpdate(LAMBDA_SPAWN_C130_WEAPONS)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0x278400)
                                        .setScale(2F)
                                        .register("g26FlareWeapon"));

        BulletConfig g40_base = new BulletConfig().setLife(200).setVel(2F).setGrav(0.035D);
        g40_he =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_HE)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0x777777)
                                        .setScale(2, 2F, 1.5F)
                                        .register("g40"));
        g40_heat =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_HEAT)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT)
                        .setDamage(0.5F)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0x5E6854)
                                        .setScale(2, 2F, 1.5F)
                                        .register("g40heat"));
        g40_demo =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_DEMO)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO)
                        .setDamage(0.75F)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0xE30000)
                                        .setScale(2, 2F, 1.5F)
                                        .register("g40demo"));
        g40_inc =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_INC)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_INC)
                        .setDamage(0.75F)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0xE86F20)
                                        .setScale(2, 2F, 1.5F)
                                        .register("g40inc"));
        g40_phosphorus =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_PHOSPHORUS)
                        .setCasing(EnumCasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS)
                        .setDamage(0.75F)
                        .setCasing(
                                new SpentCasing(CasingType.STRAIGHT)
                                        .setColor(0xC8C8C8)
                                        .setScale(2, 2F, 1.5F)
                                        .register("g40phos"));

        ModItems.GUN_FLAREGUN =
                r.registerItem(
                        "gun_flaregun",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(100)
                                                        .draw(7)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(15F)
                                                                        .delay(20)
                                                                        .reload(28)
                                                                        .jam(33)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_UNDERBARREL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                g26_flare,
                                                                                                g26_flare_supply,
                                                                                                g26_flare_weapon))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_GL))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAREGUN_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_FLAREGUN))
                                        .setDefaultAmmo(EnumAmmo.G26_FLARE, 3),
                        Item.Properties::new);

        ModItems.GUN_CONGOLAKE =
                r.registerItem(
                        "gun_congolake",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(400)
                                                        .draw(7)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .reloadChangeType(true)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(20F)
                                                                        .delay(24)
                                                                        .reload(16, 16, 16, 0)
                                                                        .jam(0)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_CONGO_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                4)
                                                                                        .addConfigs(
                                                                                                g40_he,
                                                                                                g40_heat,
                                                                                                g40_demo,
                                                                                                g40_inc,
                                                                                                g40_phosphorus))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_GL))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_CONGOLAKE_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_CONGOLAKE))
                                        .setDefaultAmmo(EnumAmmo.G40_HE, 8),
                        Item.Properties::new);

        ModItems.GUN_MK108 =
                r.registerItem(
                        "gun_mk108",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(5_000)
                                                        .draw(20)
                                                        .inspect(65)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .hideCrosshair(false)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(25F)
                                                                        .delay(10)
                                                                        .auto(true)
                                                                        .dryfireAfterAuto(true)
                                                                        .reload(135)
                                                                        .jam(25)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_MK108_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                g40_he,
                                                                                                g40_heat,
                                                                                                g40_demo,
                                                                                                g40_inc,
                                                                                                g40_phosphorus))
                                                                        .offset(
                                                                                0.75, -0.125,
                                                                                -0.125)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MK108))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MK108_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_MK108))
                                        .setDefaultAmmo(EnumAmmo.G40_HE, 50),
                        Item.Properties::new);
    }
}
