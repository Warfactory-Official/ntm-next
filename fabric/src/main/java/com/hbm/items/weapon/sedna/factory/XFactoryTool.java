// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.ClientEffects;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.interfaces.IRepairable.EnumExtinguishType;
import com.hbm.interfaces.IRepairable;
import com.hbm.items.ItemAmmoEnums.AmmoFireExt;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.Library;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactoryTool {

    public static final Identifier scope = Library.id("textures/misc/scope_tool.png");

    public static BulletConfig fext_water;
    public static BulletConfig fext_foam;
    public static BulletConfig fext_sand;

    public static BulletConfig ct_hook;
    public static BulletConfig ct_mortar;
    public static BulletConfig ct_mortar_charge;

    private static boolean extinguishCore(Level world, BlockPos pos, EnumExtinguishType type) {
        if (!(world instanceof ServerLevel server)) return false;

        BlockState state = server.getBlockState(pos);
        if (!(MultiblockSurface.resolveOwner(server, pos, state, null)
                instanceof IRepairable repairable)) {
            return false;
        }
        repairable.tryExtinguish(server, pos, type);
        return true;
    }

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_WATER_HIT =
            (bullet, mop) -> {
                if (!bullet.level().isClientSide() && mop instanceof BlockHitResult blockHit) {
                    BlockPos hitPos = blockHit.getBlockPos();
                    boolean fizz = false;
                    for (int i = -1; i <= 1; i++)
                        for (int j = -1; j <= 1; j++)
                            for (int k = -1; k <= 1; k++) {
                                BlockPos pos = hitPos.offset(i, j, k);
                                BlockState state = bullet.level().getBlockState(pos);
                                if (state.is(Blocks.FIRE)
                                        || state.is(ModBlocks.FOAM_LAYER.get())
                                        || state.is(ModBlocks.BLOCK_FOAM.get())) {
                                    bullet.level()
                                            .setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                                    fizz = true;
                                }
                            }
                    extinguishCore(bullet.level(), hitPos, EnumExtinguishType.WATER);
                    if (fizz)
                        bullet.level()
                                .playSound(
                                        null,
                                        bullet.getX(),
                                        bullet.getY(),
                                        bullet.getZ(),
                                        SoundEvents.FIRE_EXTINGUISH,
                                        SoundSource.BLOCKS,
                                        1.0F,
                                        1.5F + bullet.level().getRandom().nextFloat() * 0.5F);
                    bullet.discard();
                }
            };

    public static Consumer<Entity> LAMBDA_WATER_UPDATE =
            (bullet) -> {
                if (bullet.level().isClientSide()) {
                    Vec3 motion = bullet.getDeltaMovement();
                    ClientEffects.spawnExtBlockDust(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY(),
                            bullet.getZ(),
                            motion.x + bullet.level().getRandom().nextGaussian() * 0.05,
                            motion.y - 0.2 + bullet.level().getRandom().nextGaussian() * 0.05,
                            motion.z + bullet.level().getRandom().nextGaussian() * 0.05,
                            Blocks.WATER.defaultBlockState());
                } else {
                    BlockPos pos = bullet.blockPosition();
                    BlockState state = bullet.level().getBlockState(pos);
                    if (state.is(ModBlocks.VOLCANIC_LAVA_BLOCK.get())
                            && state.getFluidState().isSource()) {
                        bullet.level().setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                        bullet.discard();
                    }
                }
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_FOAM_HIT =
            (bullet, mop) -> {
                if (!bullet.level().isClientSide() && mop instanceof BlockHitResult blockHit) {
                    Level world = bullet.level();
                    BlockPos hitPos = blockHit.getBlockPos();
                    boolean fizz = false;
                    for (int i = -1; i <= 1; i++)
                        for (int j = -1; j <= 1; j++)
                            for (int k = -1; k <= 1; k++) {
                                BlockPos pos = hitPos.offset(i, j, k);
                                if (world.getBlockState(pos).is(BlockTags.FIRE)) {
                                    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                                    fizz = true;
                                }
                            }

                    if (extinguishCore(world, hitPos, EnumExtinguishType.FOAM)) return;
                    BlockPos target = hitPos;
                    if (world.getRandom().nextBoolean()) {
                        target = target.relative(blockHit.getDirection());
                    }
                    BlockState state = world.getBlockState(target);
                    BlockState layer = ModBlocks.FOAM_LAYER.get().defaultBlockState();
                    if (state.canBeReplaced() && layer.canSurvive(world, target)) {
                        if (!state.is(ModBlocks.FOAM_LAYER.get())) {
                            world.setBlockAndUpdate(target, layer);
                        } else {
                            int layers = state.getValue(SnowLayerBlock.LAYERS);

                            if (layers < 7)
                                world.setBlockAndUpdate(
                                        target, state.setValue(SnowLayerBlock.LAYERS, layers + 1));
                            else
                                world.setBlockAndUpdate(
                                        target, ModBlocks.BLOCK_FOAM.get().defaultBlockState());
                        }
                    }
                    if (fizz)
                        world.playSound(
                                null,
                                bullet.getX(),
                                bullet.getY(),
                                bullet.getZ(),
                                SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.BLOCKS,
                                1.0F,
                                1.5F + world.getRandom().nextFloat() * 0.5F);
                }
            };

    public static Consumer<Entity> LAMBDA_FOAM_UPDATE =
            (bullet) -> {
                if (bullet.level().isClientSide()) {
                    Vec3 motion = bullet.getDeltaMovement();
                    ClientEffects.spawnExtBlockDust(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY(),
                            bullet.getZ(),
                            motion.x + bullet.level().getRandom().nextGaussian() * 0.1,
                            motion.y - 0.2 + bullet.level().getRandom().nextGaussian() * 0.1,
                            motion.z + bullet.level().getRandom().nextGaussian() * 0.1,
                            ModBlocks.BLOCK_FOAM.get().defaultBlockState());
                }
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_SAND_HIT =
            (bullet, mop) -> {
                if (!bullet.level().isClientSide() && mop instanceof BlockHitResult blockHit) {
                    Level world = bullet.level();
                    BlockPos target = blockHit.getBlockPos();

                    if (extinguishCore(world, target, EnumExtinguishType.SAND)) return;
                    if (world.getRandom().nextBoolean()) {
                        target = target.relative(blockHit.getDirection());
                    }
                    BlockState state = world.getBlockState(target);
                    BlockState layer = ModBlocks.SAND_BORON_LAYER.get().defaultBlockState();
                    if ((state.canBeReplaced() || state.is(ModBlocks.SAND_BORON_LAYER.get()))
                            && layer.canSurvive(world, target)) {
                        boolean wasFire = state.is(BlockTags.FIRE);
                        if (!state.is(ModBlocks.SAND_BORON_LAYER.get())) {
                            world.setBlockAndUpdate(target, layer);
                        } else {
                            int layers = state.getValue(SnowLayerBlock.LAYERS);
                            if (layers < 7)
                                world.setBlockAndUpdate(
                                        target, state.setValue(SnowLayerBlock.LAYERS, layers + 1));
                            else
                                world.setBlockAndUpdate(
                                        target, ModBlocks.SAND_BORON.get().defaultBlockState());
                        }
                        if (wasFire)
                            world.playSound(
                                    null,
                                    bullet.getX(),
                                    bullet.getY(),
                                    bullet.getZ(),
                                    SoundEvents.FIRE_EXTINGUISH,
                                    SoundSource.BLOCKS,
                                    1.0F,
                                    1.5F + world.getRandom().nextFloat() * 0.5F);
                    }
                }
            };

    public static Consumer<Entity> LAMBDA_SAND_UPDATE =
            (bullet) -> {
                if (bullet.level().isClientSide()) {
                    Vec3 motion = bullet.getDeltaMovement();
                    ClientEffects.spawnExtBlockDust(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY(),
                            bullet.getZ(),
                            motion.x + bullet.level().getRandom().nextGaussian() * 0.1,
                            motion.y - 0.2 + bullet.level().getRandom().nextGaussian() * 0.1,
                            motion.z + bullet.level().getRandom().nextGaussian() * 0.1,
                            ModBlocks.SAND_BORON.get().defaultBlockState());
                }
            };

    public static Consumer<Entity> LAMBDA_SET_HOOK =
            (entity) -> {
                EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) entity;
                if (!bullet.level().isClientSide()
                        && bullet.tickCount < 2
                        && bullet.getThrower() instanceof Player player) {
                    ItemStack held = player.getMainHandItem();
                    if (!held.isEmpty() && held.getItem() == ModItems.GUN_CHARGE_THROWER.get()) {
                        ItemGunChargeThrower.setLastHook(held, bullet.getId());
                    }
                }
                bullet.ignoreFrustum = true;
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_HOOK =
            (bullet, mop) -> {
                if (mop instanceof BlockHitResult blockHit) {
                    Vec3 hit = mop.getLocation();
                    Vec3 vec = bullet.getDeltaMovement().scale(-1).normalize().scale(0.05);
                    bullet.setPos(hit.x + vec.x, hit.y + vec.y, hit.z + vec.z);
                    bullet.getStuck(
                            blockHit.getBlockPos(), blockHit.getDirection().get3DDataValue());
                }
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_MORTAR =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Vec3 hit = mop.getLocation();
                ExplosionVNT vnt =
                        new ExplosionVNT(
                                bullet.level(), hit.x, hit.y, hit.z, 5, bullet.getThrower());
                vnt.setBlockAllocator(new BlockAllocatorBulkie(60, 8));
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(1, bullet.damage)
                                .setupPiercing(
                                        bullet.config.armorThresholdNegation,
                                        bullet.config.armorPiercingPercent));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                bullet.discard();
            };

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_MORTAR_CHARGE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Vec3 hit = mop.getLocation();
                ExplosionVNT vnt =
                        new ExplosionVNT(
                                bullet.level(), hit.x, hit.y, hit.z, 15, bullet.getThrower());
                vnt.setBlockAllocator(new BlockAllocatorStandard());

                vnt.setBlockProcessor(
                        new BlockProcessorStandard()
                                .setNoDrop()
                                .withBlockEffect(
                                        new BlockMutatorDebris(
                                                () ->
                                                        ModBlocks.BLOCK_SLAG
                                                                .get()
                                                                .defaultBlockState())));
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(1, bullet.damage)
                                .setupPiercing(
                                        bullet.config.armorThresholdNegation,
                                        bullet.config.armorPiercingPercent));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                ExplosionCreator.composeEffectSmall(bullet.level(), hit.x, hit.y + 0.5, hit.z);
                vnt.explode();
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_CT =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CT_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-45, 0, 0, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -1, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "RAISE",
                                        new BusAnimationSequence()
                                                .addPos(-45, 0, 0, 500, IType.SIN_FULL)
                                                .hold(2000)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "AMMO",
                                        new BusAnimationSequence()
                                                .setPos(0, -10, -5)
                                                .hold(500)
                                                .addPos(0, 0, 5, 750, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 500, IType.SIN_UP)
                                                .hold(4000))
                                .addBus(
                                        "TWIST",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 25)
                                                .hold(2000)
                                                .addPos(0, 0, 0, 150));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "TURN",
                                        new BusAnimationSequence()
                                                .addPos(0, 60, 0, 500, IType.SIN_FULL)
                                                .hold(1750)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "ROLL",
                                        new BusAnimationSequence()
                                                .hold(750)
                                                .addPos(0, 0, -90, 500, IType.SIN_FULL)
                                                .hold(1000)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL));
                }

                return null;
            };

    public static void init(IRegistrar r) {

        ModItems.AMMO_FIREEXT =
                Reg.family(
                        AmmoFireExt.class,
                        type ->
                                type == AmmoFireExt.WATER
                                        ? "ammo_fireext"
                                        : "ammo_fireext_" + type.name().toLowerCase(Locale.ROOT),
                        (props, type) -> new Item(props),
                        Item.Properties::new);

        fext_water =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_FIREEXT.get(AmmoFireExt.WATER))
                        .setReloadCount(300)
                        .setLife(100)
                        .setVel(0.75F)
                        .setGrav(0.04D)
                        .setSpread(0.025F)
                        .setOnUpdate(LAMBDA_WATER_UPDATE)
                        .setOnEntityHit(
                                (bulletEntity, target) -> {
                                    if (target instanceof EntityHitResult entityHit)
                                        entityHit.getEntity().clearFire();
                                })
                        .setOnRicochet(LAMBDA_WATER_HIT);
        fext_foam =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_FIREEXT.get(AmmoFireExt.FOAM))
                        .setReloadCount(300)
                        .setLife(100)
                        .setVel(0.75F)
                        .setGrav(0.04D)
                        .setSpread(0.05F)
                        .setOnUpdate(LAMBDA_FOAM_UPDATE)
                        .setOnEntityHit(
                                (bulletEntity, target) -> {
                                    if (target instanceof EntityHitResult entityHit)
                                        entityHit.getEntity().clearFire();
                                })
                        .setOnRicochet(LAMBDA_FOAM_HIT);
        fext_sand =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_FIREEXT.get(AmmoFireExt.SAND))
                        .setReloadCount(300)
                        .setLife(100)
                        .setVel(0.75F)
                        .setGrav(0.04D)
                        .setSpread(0.05F)
                        .setOnUpdate(LAMBDA_SAND_UPDATE)
                        .setOnEntityHit(
                                (bulletEntity, target) -> {
                                    if (target instanceof EntityHitResult entityHit)
                                        entityHit.getEntity().clearFire();
                                })
                        .setOnRicochet(LAMBDA_SAND_HIT);

        ct_hook =
                new BulletConfig()
                        .setItem(EnumAmmo.CT_HOOK)
                        .setRenderRotations(false)
                        .setLife(6_000)
                        .setVel(3F)
                        .setGrav(0.035D)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setOnUpdate(LAMBDA_SET_HOOK)
                        .setOnImpact(LAMBDA_HOOK);
        ct_mortar =
                new BulletConfig()
                        .setItem(EnumAmmo.CT_MORTAR)
                        .setDamage(2.5F)
                        .setLife(200)
                        .setVel(3F)
                        .setGrav(0.035D)
                        .setOnImpact(LAMBDA_MORTAR);
        ct_mortar_charge =
                new BulletConfig()
                        .setItem(EnumAmmo.CT_MORTAR_CHARGE)
                        .setDamage(5F)
                        .setLife(200)
                        .setVel(3F)
                        .setGrav(0.035D)
                        .setOnImpact(LAMBDA_MORTAR_CHARGE);

        ModItems.GUN_FIREEXT =
                Reg.item(
                        "gun_fireext",
                        props ->
                                new ItemGunBaseNT(
                                        WeaponQuality.UTILITY,
                                        props,
                                        new GunConfig()
                                                .dura(5_000)
                                                .draw(10)
                                                .inspect(55)
                                                .reloadChangeType(true)
                                                .hideCrosshair(false)
                                                .crosshair(Crosshair.L_CIRCLE)
                                                .rec(
                                                        new Receiver(0)
                                                                .dmg(0F)
                                                                .delay(1)
                                                                .dry(0)
                                                                .auto(true)
                                                                .spread(0F)
                                                                .spreadHipfire(0F)
                                                                .reload(20)
                                                                .jam(0)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_EXTINGUISHER_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineFullReload(
                                                                                        0, 300)
                                                                                .addConfigs(
                                                                                        fext_water,
                                                                                        fext_foam,
                                                                                        fext_sand))
                                                                .offset(1, -0.0625 * 2.5, -0.25D)
                                                                .setupStandardFire())
                                                .setupStandardConfiguration()
                                                .orchestra(Orchestras.ORCHESTRA_FIREEXT)),
                        Item.Properties::new);

        ModItems.GUN_CHARGE_THROWER =
                r.registerItem(
                        "gun_charge_thrower",
                        props ->
                                (ItemGunChargeThrower)
                                        new ItemGunChargeThrower(
                                                        WeaponQuality.UTILITY,
                                                        props,
                                                        new GunConfig()
                                                                .dura(3_000)
                                                                .draw(10)
                                                                .inspect(55)
                                                                .reloadChangeType(true)
                                                                .hideCrosshair(false)
                                                                .crosshair(Crosshair.L_CIRCUMFLEX)
                                                                .rec(
                                                                        new Receiver(0)
                                                                                .dmg(10F)
                                                                                .delay(4)
                                                                                .dry(10)
                                                                                .auto(true)
                                                                                .spread(0F)
                                                                                .spreadHipfire(0F)
                                                                                .reload(60)
                                                                                .jam(0)
                                                                                .sound(
                                                                                        () ->
                                                                                                ModSounds
                                                                                                        .GUN_CHARGE_FIRE
                                                                                                        .get(),
                                                                                        1.0F,
                                                                                        1.0F)
                                                                                .mag(
                                                                                        new MagazineFullReload(
                                                                                                        0,
                                                                                                        1)
                                                                                                .addConfigs(
                                                                                                        ct_hook,
                                                                                                        ct_mortar,
                                                                                                        ct_mortar_charge))
                                                                                .offset(
                                                                                        1,
                                                                                        -0.0625
                                                                                                * 2.5,
                                                                                        -0.25D)
                                                                                .setupStandardFire()
                                                                                .recoil(
                                                                                        LAMBDA_RECOIL_CT))
                                                                .setupStandardConfiguration()
                                                                .anim(LAMBDA_CT_ANIMS)
                                                                .orchestra(
                                                                        Orchestras
                                                                                .ORCHESTRA_CHARGE_THROWER))
                                                .setDefaultAmmo(EnumAmmo.CT_MORTAR, 3),
                        Item.Properties::new);
    }
}
