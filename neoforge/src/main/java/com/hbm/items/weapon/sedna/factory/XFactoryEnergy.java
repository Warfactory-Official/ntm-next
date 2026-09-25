// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import com.hbm.util.DamageClass;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public class XFactoryEnergy {

    public static final Identifier scope_luna = Library.id("textures/misc/scope_amat.png");

    public static BulletConfig energy_tesla;
    public static BulletConfig energy_tesla_overcharge;
    public static BulletConfig energy_tesla_ir;
    public static BulletConfig energy_tesla_ir_sub;

    public static BulletConfig energy_las;
    public static BulletConfig energy_las_overcharge;
    public static BulletConfig energy_las_ir;
    public static BulletConfig energy_emerald;
    public static BulletConfig energy_emerald_overcharge;
    public static BulletConfig energy_emerald_ir;

    public static BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_LIGHTNING_HIT =
            (beam, mop) -> {
                Vec3 hit = mop.getLocation();
                if (mop instanceof BlockHitResult blockHit) {
                    Direction dir = blockHit.getDirection();
                    hit = hit.add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
                }

                ExplosionVNT vnt =
                        new ExplosionVNT(beam.level(), hit.x, hit.y, hit.z, 2F, beam.getThrower());
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(1, beam.damage)
                                .setDamageClass(beam.config.dmgClass));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();
                beam.level()
                        .playSound(
                                null,
                                hit.x,
                                hit.y,
                                hit.z,
                                ModSounds.UFO_BLAST.get(),
                                SoundSource.PLAYERS,
                                5.0F,
                                0.9F + beam.level().getRandom().nextFloat() * 0.2F);
                beam.level()
                        .playSound(
                                null,
                                hit.x,
                                hit.y,
                                hit.z,
                                SoundEvents.FIREWORK_ROCKET_BLAST,
                                SoundSource.PLAYERS,
                                5.0F,
                                0.5F);

                if (beam.level() instanceof ServerLevel server) {
                    float yaw = server.getRandom().nextFloat() * 180F;
                    for (int i = 0; i < 3; i++) {
                        Services.NETWORK.sendToAllAround(
                                new PlasmaBlastPayload(
                                        hit.x,
                                        hit.y,
                                        hit.z,
                                        0.5F,
                                        0.5F,
                                        1.0F,
                                        -60F + 60F * i,
                                        yaw,
                                        2F),
                                new TargetPoint(server, hit.x, hit.y, hit.z, 100));
                    }
                }

                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 9));
                    living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 9));
                }
            };

    public static BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_LIGHTNING_SPLIT =
            (beam, mop) -> {
                LAMBDA_LIGHTNING_HIT.accept(beam, mop);
                if (!(mop instanceof EntityHitResult primaryHit)) return;

                Vec3 hit = mop.getLocation();
                double range = 20;
                List<LivingEntity> potentialTargets =
                        beam.level()
                                .getEntitiesOfClass(
                                        LivingEntity.class,
                                        new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                                                .inflate(range, range, range));
                Collections.shuffle(potentialTargets);

                for (LivingEntity target : potentialTargets) {
                    if (target == beam.thrower) continue;
                    if (target == primaryHit.getEntity()) continue;

                    Vec3 delta =
                            new Vec3(
                                    target.getX() - hit.x,
                                    target.getY() + target.getBbHeight() / 2 - hit.y,
                                    target.getZ() - hit.z);
                    if (delta.length() > 20) continue;
                    EntityBulletBeamBase sub =
                            new EntityBulletBeamBase(
                                    beam.thrower, energy_tesla_ir_sub, beam.damage);
                    sub.setPos(hit.x, hit.y, hit.z);
                    sub.setRotationsFromVector(delta);
                    sub.performHitscanExternal(delta.length());
                    beam.level().addFreshEntity(sub);
                }
            };

    public static BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_IR_HIT =
            (beam, mop) -> {
                BulletConfig.LAMBDA_STANDARD_BEAM_HIT.accept(beam, mop);

                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    HbmLivingProps props = HbmLivingProps.getData(living);
                    if (props.fire < 100) props.fire = 100;
                }

                if (mop instanceof BlockHitResult blockHit) {
                    Level world = beam.level();
                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = world.getBlockState(pos);
                    Direction dir = blockHit.getDirection();
                    if (Services.PLATFORM.isFlammable(world, pos, state, dir.getOpposite())) {
                        BlockPos firePos = pos.relative(dir);
                        if (world.getBlockState(firePos).isAir()) {
                            world.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                            return;
                        }
                    }

                    EntityFireLingering fire =
                            new EntityFireLingering(world)
                                    .setArea(2, 1)
                                    .setDuration(100)
                                    .setType(EntityFireLingering.TYPE_DIESEL);
                    fire.setPos(mop.getLocation());
                    world.addFreshEntity(fire);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ENERGY = (stack, ctx) -> {};

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TESLA_ANIMS =
            (stack, type) -> {
                int amount =
                        ((ItemGunBaseNT) stack.getItem())
                                .getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, ClientPlayerAccess.player().getInventory());
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 0)
                                                .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(
                                                        0,
                                                        0,
                                                        ItemGunBaseNT.getIsAiming(stack)
                                                                ? -0.5
                                                                : -1,
                                                        100,
                                                        IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "CYCLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 150)
                                                .addPos(0, 0, 22.5, 350))
                                .addBus(
                                        "COUNT",
                                        new BusAnimationSequence().addPos(amount, 0, 0, 0));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "CYCLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 150)
                                                .addPos(0, 0, 22.5, 350));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "YOMI",
                                        new BusAnimationSequence()
                                                .addPos(8, -4, 0, 0)
                                                .addPos(4, -1, 0, 500, IType.SIN_DOWN)
                                                .addPos(4, -1, 0, 1000)
                                                .addPos(6, -6, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "SQUEEZE",
                                        new BusAnimationSequence()
                                                .addPos(1, 1, 1, 0)
                                                .addPos(1, 1, 1, 750)
                                                .addPos(1, 1, 0.5, 125)
                                                .addPos(1, 1, 1, 125));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASER_PISTOL =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -0.5, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new BusAnimationSequence()
                                                .addPos(0, -20, 0, 100)
                                                .hold(1900)
                                                .addPos(0, 0, 0, 100))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .hold(100)
                                                .addPos(-45, 0, 0, 250, IType.SIN_FULL)
                                                .hold(500)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "JOLT",
                                        new BusAnimationSequence()
                                                .hold(350)
                                                .addPos(0, 0, 0.5, 100, IType.SIN_FULL)
                                                .addPos(0, 0, -1.5, 100, IType.SIN_UP)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL)
                                                .holdUntil(2100)
                                                .addPos(-0.0625, 0, 0, 50, IType.SIN_UP)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BATTERY",
                                        new BusAnimationSequence()
                                                .hold(550)
                                                .addPos(0, 0, 5, 250)
                                                .hold(550)
                                                .setPos(0, -2, -2)
                                                .addPos(0, 0, -2, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_UP));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new BusAnimationSequence()
                                                .hold(500)
                                                .addPos(0, -20, 0, 100)
                                                .hold(250)
                                                .addPos(0, 0, 0, 100))
                                .addBus(
                                        "JOLT",
                                        new BusAnimationSequence()
                                                .hold(950)
                                                .addPos(-0.0625, 0, 0, 50, IType.SIN_UP)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .hold(1500)
                                                .addPos(7.5, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "SWIRL",
                                        new BusAnimationSequence()
                                                .addPos(-720, 0, 0, 750, IType.SIN_FULL)
                                                .hold(500)
                                                .addPos(0, 0, 0, 750, IType.SIN_FULL));
                }
                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASRIFLE =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -0.5, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(-90, 0, 0, 350, IType.SIN_UP)
                                                .addPos(-90, 0, 0, 1500)
                                                .addPos(0, 0, 0, 350, IType.SIN_UP))
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 350)
                                                .addPos(0, -5, 0, 350, IType.SIN_UP)
                                                .addPos(0, -5, 0, 500)
                                                .addPos(0, -0.25, 0, 500, IType.SIN_FULL)
                                                .addPos(0, -0.25, 0, 150)
                                                .addPos(0, 0, 0, 350))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 1700)
                                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(-90, 0, 0, 350, IType.SIN_UP)
                                                .addPos(-90, 0, 0, 600)
                                                .addPos(0, 0, 0, 350, IType.SIN_UP))
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(0, 0, 0, 350)
                                                .addPos(0, -2, 0, 200, IType.SIN_UP)
                                                .addPos(0, -0.25, 0, 250, IType.SIN_FULL)
                                                .addPos(0, -0.25, 0, 150)
                                                .addPos(0, 0, 0, 350))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(0, 0, 0, 800)
                                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(-90, 0, 0, 350, IType.SIN_UP)
                                                .addPos(-90, 0, 0, 600)
                                                .addPos(0, 0, 0, 350, IType.SIN_UP))
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 350)
                                                .addPos(0, -2, 0, 200, IType.SIN_UP)
                                                .addPos(0, -0.25, 0, 250, IType.SIN_FULL)
                                                .addPos(0, -0.25, 0, 150)
                                                .addPos(0, 0, 0, 350))
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 800)
                                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                }

                return null;
            };

    private static ItemStack ingotPolymer(int count) {
        return new ItemStack(ModItems.ingot(Mats.MAT_POLYMER), count);
    }

    public static void init(IRegistrar r) {

        energy_tesla =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR)
                        .setCasing(() -> ingotPolymer(2), 4)
                        .setupDamageClass(DamageClass.ELECTRIC)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setOnBeamImpact(LAMBDA_LIGHTNING_HIT);
        energy_tesla_overcharge =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_OVERCHARGE)
                        .setCasing(() -> ingotPolymer(2), 4)
                        .setupDamageClass(DamageClass.ELECTRIC)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setDamage(1.5F)
                        .setOnBeamImpact(LAMBDA_LIGHTNING_HIT);
        energy_tesla_ir =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_IR)
                        .setCasing(() -> ingotPolymer(2), 4)
                        .setupDamageClass(DamageClass.ELECTRIC)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDamage(0.8F)
                        .setOnBeamImpact(LAMBDA_LIGHTNING_SPLIT);
        energy_tesla_ir_sub =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_IR)
                        .setupDamageClass(DamageClass.ELECTRIC)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(3)
                        .setWear(3F)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setDamage(0.5F)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);

        energy_las =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR)
                        .setCasing(() -> ingotPolymer(2), 4)
                        .setupDamageClass(DamageClass.LASER)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        energy_las_overcharge =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_OVERCHARGE)
                        .setCasing(() -> ingotPolymer(2), 4)
                        .setupDamageClass(DamageClass.LASER)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        energy_las_ir =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_IR)
                        .setCasing(() -> ingotPolymer(2), 4)
                        .setupDamageClass(DamageClass.FIRE)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setOnBeamImpact(LAMBDA_IR_HIT);

        energy_emerald = energy_las.clone().setArmorPiercing(0.5F).setThresholdNegation(10F);
        energy_emerald_overcharge =
                energy_las_overcharge.clone().setArmorPiercing(0.5F).setThresholdNegation(15F);
        energy_emerald_ir = energy_las_ir.clone().setArmorPiercing(0.5F).setThresholdNegation(10F);

        ModItems.GUN_TESLA_CANNON =
                r.registerItem(
                        "gun_tesla_cannon",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(1_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(35F)
                                                                        .delay(20)
                                                                        .spreadHipfire(1.5F)
                                                                        .reload(44)
                                                                        .jam(19)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_TESLA_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                energy_tesla,
                                                                                                energy_tesla_overcharge,
                                                                                                energy_tesla_ir))
                                                                        .offset(0.75, 0, -0.375)
                                                                        .offsetScoped(
                                                                                0.75, 0, -0.25)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ENERGY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_TESLA_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_TESLA))
                                        .setDefaultAmmo(EnumAmmo.CAPACITOR, 15),
                        Item.Properties::new);

        ModItems.GUN_LASER_PISTOL =
                r.registerItem(
                        "gun_laser_pistol",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(500)
                                                        .draw(10)
                                                        .inspect(26)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(25F)
                                                                        .delay(5)
                                                                        .spread(1F)
                                                                        .spreadHipfire(1F)
                                                                        .reload(45)
                                                                        .jam(37)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_LASER_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                energy_las,
                                                                                                energy_las_overcharge,
                                                                                                energy_las_ir))
                                                                        .offset(
                                                                                0.75,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ENERGY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LASER_PISTOL)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_LASER_PISTOL))
                                        .setDefaultAmmo(EnumAmmo.CAPACITOR, 15),
                        Item.Properties::new);
        ModItems.GUN_LASER_PISTOL_PEW_PEW =
                r.registerItem(
                        "gun_laser_pistol_pew_pew",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(500)
                                                        .draw(10)
                                                        .inspect(26)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(30F)
                                                                        .rounds(5)
                                                                        .delay(10)
                                                                        .spread(0.25F)
                                                                        .spreadHipfire(1F)
                                                                        .reload(45)
                                                                        .jam(37)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_LASER_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.8F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                10)
                                                                                        .addConfigs(
                                                                                                energy_las,
                                                                                                energy_las_overcharge,
                                                                                                energy_las_ir))
                                                                        .offset(
                                                                                0.75,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ENERGY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LASER_PISTOL)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_LASER_PISTOL))
                                        .setDefaultAmmo(EnumAmmo.CAPACITOR_OVERCHARGE, 10),
                        Item.Properties::new);
        ModItems.GUN_LASER_PISTOL_MORNING_GLORY =
                r.registerItem(
                        "gun_laser_pistol_morning_glory",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                new GunConfig()
                                                        .dura(1_500)
                                                        .draw(10)
                                                        .inspect(26)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(20F)
                                                                        .delay(7)
                                                                        .spread(0F)
                                                                        .spreadHipfire(0.5F)
                                                                        .reload(45)
                                                                        .jam(37)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_LASER_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.1F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                20)
                                                                                        .addConfigs(
                                                                                                energy_emerald,
                                                                                                energy_emerald_overcharge,
                                                                                                energy_emerald_ir))
                                                                        .offset(
                                                                                0.75,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ENERGY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LASER_PISTOL)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_LASER_PISTOL))
                                        .setDefaultAmmo(EnumAmmo.CAPACITOR_OVERCHARGE, 20),
                        Item.Properties::new);

        ModItems.GUN_LASRIFLE =
                r.registerItem(
                        "gun_lasrifle",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(2_000)
                                                        .draw(10)
                                                        .inspect(26)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .scopeTexture(scope_luna)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(50F)
                                                                        .delay(8)
                                                                        .spreadHipfire(1F)
                                                                        .reload(44)
                                                                        .jam(36)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_LASER_RIFLE_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                24)
                                                                                        .addConfigs(
                                                                                                energy_las,
                                                                                                energy_las_overcharge,
                                                                                                energy_las_ir))
                                                                        .offset(
                                                                                0.75,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ENERGY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LASRIFLE)
                                                        .orchestra(Orchestras.ORCHESTRA_LASRIFLE))
                                        .setDefaultAmmo(EnumAmmo.CAPACITOR, 24),
                        Item.Properties::new);
    }
}
