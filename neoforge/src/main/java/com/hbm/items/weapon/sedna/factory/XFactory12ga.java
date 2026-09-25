// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockDetonatable;
import com.hbm.blocks.generic.BlockDecoCRT;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityDuchessGambit;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.EnumCasingType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.SpentCasing;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import com.hbm.util.DamageClass;
import com.hbm.util.EntityDamageUtil;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.*;

public class XFactory12ga {

    public static BulletConfig g12_bp;
    public static BulletConfig g12_bp_magnum;
    public static BulletConfig g12_bp_slug;
    public static BulletConfig g12;
    public static BulletConfig g12_slug;
    public static BulletConfig g12_flechette;
    public static BulletConfig g12_magnum;
    public static BulletConfig g12_explosive;
    public static BulletConfig g12_phosphorus;
    public static BulletConfig g12_equestrian_bj;
    public static BulletConfig g12_equestrian_tkr;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_BOAT =
            (bullet, mop) -> {
                EntityDuchessGambit pippo =
                        new EntityDuchessGambit(ModEntities.DUCHESS_GAMBIT.get(), bullet.level());
                Vec3 hit = mop.getLocation();
                pippo.setPos(hit.x, hit.y + 50, hit.z);
                bullet.level().addFreshEntity(pippo);
                bullet.level()
                        .playSound(
                                null,
                                pippo.getX(),
                                pippo.getY() + 50,
                                pippo.getZ(),
                                ModSounds.GUN_SOLDIER_TF2_BOAT_EXE_WAV_MP3.get(),
                                SoundSource.PLAYERS,
                                100F,
                                1F);
                bullet.discard();
            };
    public static BulletConfig g12_sub;
    public static BulletConfig g12_sub_slug;
    public static BulletConfig g12_sub_flechette;
    public static BulletConfig g12_sub_magnum;
    public static BulletConfig g12_sub_explosive;
    public static BulletConfig g12_sub_phosphorus;
    public static BulletConfig g12_shredder;
    public static BulletConfig g12_shredder_slug;
    public static BulletConfig g12_shredder_flechette;
    public static BulletConfig g12_shredder_magnum;
    public static BulletConfig g12_shredder_explosive;
    public static BulletConfig g12_shredder_phosphorus;

    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                Lego.standardExplode(bullet, mop, 2F);
                bullet.discard();
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_SHREDDER_RICOCHET =
            (bullet, mop) -> {
                if (mop instanceof BlockHitResult blockHit) {

                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = bullet.level().getBlockState(pos);

                    if (state.instrument() == NoteBlockInstrument.HAT
                            || state.is(Blocks.GLOWSTONE)) {
                        bullet.level().destroyBlock(pos, false);
                        bullet.setPos(mop.getLocation());
                        return;
                    }
                    if (state.getBlock() instanceof BlockDetonatable det) {
                        det.onShot(bullet.level(), pos);
                    }
                    if (state.getBlock() instanceof BlockDecoCRT
                            && !state.is(ModBlocks.DECO_CRT_BROKEN.get())) {

                        bullet.level()
                                .setBlock(
                                        pos,
                                        ModBlocks.DECO_CRT_BROKEN
                                                .get()
                                                .defaultBlockState()
                                                .setValue(
                                                        BlockDecoCRT.ROTATION,
                                                        state.getValue(BlockDecoCRT.ROTATION)),
                                        3);
                    }

                    Direction dir = blockHit.getDirection();
                    Vec3 face = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                    Vec3 vel = bullet.getDeltaMovement().normalize();

                    double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

                    if (angle <= bullet.config.ricochetAngle) {

                        spawnPulse(bullet.level(), mop, bullet.getYRot(), bullet.getXRot());

                        List<Entity> blast =
                                bullet.level()
                                        .getEntities(
                                                bullet,
                                                new AABB(
                                                                bullet.getX(),
                                                                bullet.getY(),
                                                                bullet.getZ(),
                                                                bullet.getX(),
                                                                bullet.getY(),
                                                                bullet.getZ())
                                                        .inflate(0.5, 0.5, 0.5));
                        DamageSource source =
                                BulletConfig.getDamage(
                                        bullet, bullet.getThrower(), DamageClass.PLASMA);

                        for (Entity e : blast) {
                            if (!e.isAlive()) continue;
                            if (e instanceof LivingEntity living) {
                                EntityDamageUtil.attackEntityFromNT(
                                        living, source, bullet.damage, true, false, 0D, 0F, 0F);
                                if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
                            } else {
                                e.hurtServer((ServerLevel) bullet.level(), source, bullet.damage);
                            }
                        }

                        bullet.ricochets++;
                        if (bullet.ricochets > bullet.config.maxRicochetCount) {
                            bullet.setPos(mop.getLocation());
                            bullet.discard();
                        }

                        Vec3 motion = bullet.getDeltaMovement();
                        switch (dir.getAxis()) {
                            case Y:
                                bullet.setDeltaMovement(motion.x, -motion.y, motion.z);
                                break;
                            case Z:
                                bullet.setDeltaMovement(motion.x, motion.y, -motion.z);
                                break;
                            case X:
                                bullet.setDeltaMovement(-motion.x, motion.y, motion.z);
                                break;
                        }
                        bullet.setPos(mop.getLocation());
                        bullet.sendTeleport();
                    } else {
                        bullet.setPos(mop.getLocation());
                        bullet.discard();
                    }
                }
            };
    public static Function<ItemStack, String> LAMBDA_NAME_MARESLEG =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF))
                    return stack.getItem().getDescriptionId() + "_short";
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MARESLEG =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LIBERATOR =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AUTOSHOTGUN =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5) + 1.5F,
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_SEXY =
            (stack, ctx) -> {
                ItemGunBaseNT.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SPAS_SECONDARY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                Player player = ctx.getPlayer();
                Receiver rec = ctx.config().getReceivers(stack)[0];
                int index = ctx.configIndex();
                GunState state = ItemGunBaseNT.getState(stack, index);
                if (state == GunState.IDLE) {
                    if (rec.getCanFire(stack).apply(stack, ctx)) {
                        rec.getOnFire(stack).accept(stack, ctx);
                        int remaining = rec.getRoundsPerCycle(stack);
                        int timeFired = 1;
                        for (int i = 0; i < remaining; i++) {
                            if (rec.getCanFire(stack).apply(stack, ctx)) {
                                rec.getOnFire(stack).accept(stack, ctx);
                                timeFired++;
                            }
                        }
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
                                            rec.getFirePitch(stack) * (timeFired > 1 ? 0.9F : 1F));
                        ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                        ItemGunBaseNT.setTimer(stack, index, 20);
                    } else {
                        if (rec.getDoesDryFire(stack)) {
                            ItemGunBaseNT.playAnimation(
                                    player, stack, GunAnimation.CYCLE_DRY, index);
                            ItemGunBaseNT.setState(stack, index, GunState.DRAWING);
                            ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
                        }
                    }
                }
                if (state == GunState.RELOADING) {
                    ItemGunBaseNT.setReloadCancel(stack, true);
                }
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LIBERATOR_ANIMS =
            (stack, type) -> {
                int ammo =
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
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -2.5, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 350, IType.SIN_FULL));
                    case CYCLE_DRY:
                        return new BusAnimation();
                    case RELOAD:
                        if (ammo == 0)
                            return new BusAnimation()
                                    .addBus(
                                            "LATCH",
                                            new BusAnimationSequence().addPos(15, 0, 0, 100))
                                    .addBus(
                                            "BREAK",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 100)
                                                    .addPos(60, 0, 0, 350, IType.SIN_DOWN))
                                    .addBus(
                                            "SHELL1",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(2, -4, -2, 400)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP))
                                    .addBus(
                                            "SHELL2",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0))
                                    .addBus(
                                            "SHELL3",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0));
                        if (ammo == 1)
                            return new BusAnimation()
                                    .addBus(
                                            "LATCH",
                                            new BusAnimationSequence().addPos(15, 0, 0, 100))
                                    .addBus(
                                            "BREAK",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 100)
                                                    .addPos(60, 0, 0, 350, IType.SIN_DOWN))
                                    .addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus(
                                            "SHELL2",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(2, -4, -2, 400)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP))
                                    .addBus(
                                            "SHELL3",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0));
                        if (ammo == 2)
                            return new BusAnimation()
                                    .addBus(
                                            "LATCH",
                                            new BusAnimationSequence().addPos(15, 0, 0, 100))
                                    .addBus(
                                            "BREAK",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 100)
                                                    .addPos(60, 0, 0, 350, IType.SIN_DOWN))
                                    .addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus(
                                            "SHELL3",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(2, -4, -2, 400)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0));
                        if (ammo == 3)
                            return new BusAnimation()
                                    .addBus(
                                            "LATCH",
                                            new BusAnimationSequence().addPos(15, 0, 0, 100))
                                    .addBus(
                                            "BREAK",
                                            new BusAnimationSequence()
                                                    .addPos(0, 0, 0, 100)
                                                    .addPos(60, 0, 0, 350, IType.SIN_DOWN))
                                    .addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus("SHELL3", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(2, -4, -2, 400)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP));
                    case RELOAD_CYCLE:
                        if (ammo == 0)
                            return new BusAnimation()
                                    .addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0))
                                    .addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0))
                                    .addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus(
                                            "SHELL2",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP))
                                    .addBus(
                                            "SHELL3",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0));
                        if (ammo == 1)
                            return new BusAnimation()
                                    .addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0))
                                    .addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0))
                                    .addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus(
                                            "SHELL3",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence().addPos(2, -4, -2, 0));
                        if (ammo == 2)
                            return new BusAnimation()
                                    .addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0))
                                    .addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0))
                                    .addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus("SHELL3", new BusAnimationSequence().addPos(0, 0, 0, 0))
                                    .addBus(
                                            "SHELL4",
                                            new BusAnimationSequence()
                                                    .addPos(2, -4, -2, 0)
                                                    .addPos(0, 0, -2, 450, IType.SIN_FULL)
                                                    .addPos(0, 0, 0, 50, IType.SIN_UP));
                        return null;
                    case RELOAD_END:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 0)
                                                .addPos(15, 0, 0, 250)
                                                .addPos(0, 0, 0, 50))
                                .addBus(
                                        "BREAK",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 0)
                                                .addPos(0, 0, 0, 250, IType.SIN_UP))
                                .addBus(
                                        ammo >= 0 ? "SHELL1" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo >= 1 ? "SHELL2" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo >= 2 ? "SHELL3" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo >= 3 ? "SHELL4" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo < 0 ? "SHELL1" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 1 ? "SHELL2" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 2 ? "SHELL3" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 3 ? "SHELL4" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 0)
                                                .addPos(15, 0, 0, 250)
                                                .addPos(0, 0, 0, 50)
                                                .addPos(0, 0, 0, 550)
                                                .addPos(15, 0, 0, 100)
                                                .addPos(15, 0, 0, 600)
                                                .addPos(0, 0, 0, 50))
                                .addBus(
                                        "BREAK",
                                        new BusAnimationSequence()
                                                .addPos(60, 0, 0, 0)
                                                .addPos(0, 0, 0, 250, IType.SIN_UP)
                                                .addPos(0, 0, 0, 600)
                                                .addPos(45, 0, 0, 250, IType.SIN_DOWN)
                                                .addPos(45, 0, 0, 300)
                                                .addPos(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        ammo >= 0 ? "SHELL1" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo >= 1 ? "SHELL2" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo >= 2 ? "SHELL3" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo >= 3 ? "SHELL4" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo < 0 ? "SHELL1" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 1 ? "SHELL2" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 2 ? "SHELL3" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 3 ? "SHELL4" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 100)
                                                .addPos(15, 0, 0, 1100)
                                                .addPos(0, 0, 0, 50))
                                .addBus(
                                        "BREAK",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 100)
                                                .addPos(60, 0, 0, 350, IType.SIN_DOWN)
                                                .addPos(60, 0, 0, 500)
                                                .addPos(0, 0, 0, 250, IType.SIN_UP))
                                .addBus(
                                        ammo > 0 ? "SHELL1" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo > 1 ? "SHELL2" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo > 2 ? "SHELL3" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo > 3 ? "SHELL4" : "NULL",
                                        new BusAnimationSequence().addPos(0, 0, 0, 0))
                                .addBus(
                                        ammo < 1 ? "SHELL1" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 2 ? "SHELL2" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 3 ? "SHELL3" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0))
                                .addBus(
                                        ammo < 4 ? "SHELL4" : "NULL",
                                        new BusAnimationSequence().addPos(2, -8, -2, 0));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_SPAS_ANIMS =
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
                        return ResourceManager.spas_12_anim.get("Fire");
                    case CYCLE_DRY:
                        return ResourceManager.spas_12_anim.get("FireDry");
                    case ALT_CYCLE:
                        return ResourceManager.spas_12_anim.get("FireAlt");
                    case RELOAD:
                        boolean empty =
                                ((ItemGunBaseNT) stack.getItem())
                                                .getConfig(stack, 0)
                                                .getReceivers(stack)[0]
                                                .getMagazine(stack)
                                                .getAmount(
                                                        stack,
                                                        ClientPlayerAccess.player().getInventory())
                                        <= 0;
                        return ResourceManager.spas_12_anim.get(
                                empty ? "ReloadEmptyStart" : "ReloadStart");
                    case RELOAD_CYCLE:
                        return ResourceManager.spas_12_anim.get("Reload");
                    case RELOAD_END:
                        return ResourceManager.spas_12_anim.get("ReloadEnd");
                    case JAMMED:
                        return ResourceManager.spas_12_anim.get("Jammed");
                    case INSPECT:
                        return ResourceManager.spas_12_anim.get("Inspect");
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_SHREDDER_ANIMS =
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
                                                .addPos(0, 0, -1, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "CYCLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 150)
                                                .addPos(0, 0, 18, 100));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "CYCLE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 150)
                                                .addPos(0, 0, 18, 100));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, -8, 0, 250, IType.SIN_UP)
                                                .addPos(0, -8, 0, 1000)
                                                .addPos(0, 0, 0, 300))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 750)
                                                .addPos(-25, 0, 0, 300, IType.SIN_FULL)
                                                .addPos(-25, 0, 0, 500)
                                                .addPos(-27, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(-25, 0, 0, 100, IType.SIN_FULL)
                                                .addPos(-25, 0, 0, 150)
                                                .addPos(0, 0, 0, 300, IType.SIN_FULL));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 500)
                                                .addPos(0, -2, 0, 150, IType.SIN_UP)
                                                .addPos(0, 0, 0, 100))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 750)
                                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .addPos(0, -1, 0, 150)
                                                .addPos(6, -1, 0, 150)
                                                .addPos(6, 12, 0, 350, IType.SIN_DOWN)
                                                .addPos(6, -2, 0, 350, IType.SIN_UP)
                                                .addPos(6, -1, 0, 50)
                                                .addPos(6, -1, 0, 100)
                                                .addPos(0, -1, 0, 150, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "SPEEN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 300)
                                                .addPos(360, 0, 0, 700))
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 1450)
                                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_SEXY_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(45, 0, 0, 0)
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
                                                .addPos(0, 0, -0.25, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, -1, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 150))
                                .addBus("CYCLE", new BusAnimationSequence().addPos(1, 0, 0, 150))
                                .addBus(
                                        "HOOD",
                                        new BusAnimationSequence()
                                                .hold(50)
                                                .addPos(3, 0, 0, 50, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 50, IType.SIN_UP))
                                .addBus(
                                        "SHELLS",
                                        new BusAnimationSequence().setPos(amount - 1, 0, 0));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 18, 50));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LOWER",
                                        new BusAnimationSequence()
                                                .addPos(15, 0, 0, 500, IType.SIN_FULL)
                                                .hold(2750)
                                                .addPos(12, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(15, 0, 0, 100, IType.SIN_FULL)
                                                .hold(1050)
                                                .addPos(18, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(15, 0, 0, 100, IType.SIN_FULL)
                                                .hold(300)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 1, 150)
                                                .hold(4700)
                                                .addPos(0, 0, 0, 150))
                                .addBus(
                                        "HOOD",
                                        new BusAnimationSequence()
                                                .hold(250)
                                                .addPos(60, 0, 0, 500, IType.SIN_FULL)
                                                .hold(3250)
                                                .addPos(0, 0, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "BELT",
                                        new BusAnimationSequence()
                                                .setPos(1, 0, 0)
                                                .hold(750)
                                                .addPos(0, 0, 0, 500, IType.SIN_UP)
                                                .hold(2000)
                                                .addPos(1, 0, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "MAG",
                                        new BusAnimationSequence()
                                                .hold(1500)
                                                .addPos(0, -1, 0, 250, IType.SIN_UP)
                                                .addPos(2, -1, 0, 500, IType.SIN_UP)
                                                .addPos(7, 1, 0, 250, IType.SIN_UP)
                                                .addPos(15, 2, 0, 250)
                                                .setPos(0, -2, 0)
                                                .addPos(0, 0, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "MAGROT",
                                        new BusAnimationSequence()
                                                .hold(2250)
                                                .addPos(0, 0, -180, 500, IType.SIN_FULL)
                                                .setPos(0, 0, 0));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "BOTTLE",
                                        new BusAnimationSequence()
                                                .setPos(8, -8, -2)
                                                .addPos(6, -4, -2, 500, IType.SIN_DOWN)
                                                .addPos(3, -3, -5, 500, IType.SIN_FULL)
                                                .addPos(3, -2, -5, 1000)
                                                .addPos(4, -6, -2, 750, IType.SIN_FULL)
                                                .addPos(6, -8, -2, 500, IType.SIN_UP))
                                .addBus(
                                        "SIP",
                                        new BusAnimationSequence()
                                                .setPos(25, 0, 0)
                                                .hold(500)
                                                .addPos(-90, 0, 0, 500, IType.SIN_FULL)
                                                .addPos(-110, 0, 0, 1000)
                                                .addPos(25, 0, 0, 750, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MARESLEG_ANIMS =
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
                                                .addPos(0, 0, 0, 50)
                                                .addPos(0, 0, -1, 50)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "SIGHT",
                                        new BusAnimationSequence()
                                                .addPos(35, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(-85, 0, 0, 200)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(0, 0, 45, 200, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 50)
                                                .addPos(30, 0, 0, 550)
                                                .addPos(0, 0, 0, 200));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(-90, 0, 0, 200)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(0, 0, 45, 200, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 50)
                                                .addPos(30, 0, 0, 550)
                                                .addPos(0, 0, 0, 200));
                    case RELOAD:
                        boolean empty =
                                ((ItemGunBaseNT) stack.getItem())
                                                .getConfig(stack, 0)
                                                .getReceivers(stack)[0]
                                                .getMagazine(stack)
                                                .getAmount(
                                                        stack,
                                                        ClientPlayerAccess.player().getInventory())
                                        <= 0;
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 400)
                                                .addPos(-85, 0, 0, 200))
                                .addBus(
                                        "SHELL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(0, 0.25, -3, 0)
                                                .addPos(
                                                        0,
                                                        empty ? 0.25 : 0.125,
                                                        -1.5,
                                                        150,
                                                        IType.SIN_UP)
                                                .addPos(
                                                        0,
                                                        empty ? 0.25 : -0.25,
                                                        0,
                                                        150,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "FLAG",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, empty ? 900 : 0)
                                                .addPos(1, 1, 1, 0));
                    case RELOAD_CYCLE:
                        return new BusAnimation()
                                .addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 0))
                                .addBus("LEVER", new BusAnimationSequence().addPos(-85, 0, 0, 0))
                                .addBus(
                                        "SHELL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0.25, -3, 0)
                                                .addPos(0, 0.125, -1.5, 150, IType.SIN_UP)
                                                .addPos(0, -0.125, 0, 150, IType.SIN_DOWN))
                                .addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
                    case RELOAD_END:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 0)
                                                .addPos(30, 0, 0, 250)
                                                .addPos(0, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(-85, 0, 0, 0)
                                                .addPos(0, 0, 0, 200))
                                .addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 0)
                                                .addPos(30, 0, 0, 250)
                                                .addPos(0, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(-85, 0, 0, 0)
                                                .addPos(-15, 0, 0, 200)
                                                .addPos(-15, 0, 0, 650)
                                                .addPos(-85, 0, 0, 200)
                                                .addPos(-15, 0, 0, 200)
                                                .addPos(-15, 0, 0, 200)
                                                .addPos(-85, 0, 0, 200)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 850)
                                                .addPos(0, 0, 45, 200, IType.SIN_DOWN)
                                                .addPos(0, 0, 45, 800)
                                                .addPos(0, 0, 0, 200, IType.SIN_UP))
                                .addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(-35, 0, 0, 300, IType.SIN_FULL)
                                                .addPos(-35, 0, 0, 1150)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 450)
                                                .addPos(0, 0, -90, 500, IType.SIN_FULL)
                                                .addPos(0, 0, -90, 500)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MARESLEG_SHORT_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new BusAnimationSequence()
                                                .addPos(-60, 0, 0, 0)
                                                .addPos(0, 0, -3, 250, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 50)
                                                .addPos(0, 0, -1, 50)
                                                .addPos(0, 0, 0, 250))
                                .addBus(
                                        "SIGHT",
                                        new BusAnimationSequence()
                                                .addPos(35, 0, 0, 100, IType.SIN_DOWN)
                                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(-85, 0, 0, 200)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 50)
                                                .addPos(30, 0, 0, 550)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "FLIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(360, 0, 0, 400))
                                .addBus("SHELL", new BusAnimationSequence().addPos(-20, 0, 0, 0));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(-90, 0, 0, 200)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "HAMMER",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 50)
                                                .addPos(30, 0, 0, 550)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "FLIP",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 600)
                                                .addPos(360, 0, 0, 400))
                                .addBus("SHELL", new BusAnimationSequence().addPos(-20, 0, 0, 0));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(30, 0, 0, 0)
                                                .addPos(30, 0, 0, 250)
                                                .addPos(0, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new BusAnimationSequence()
                                                .addPos(-85, 0, 0, 0)
                                                .addPos(-15, 0, 0, 200)
                                                .addPos(-15, 0, 0, 650)
                                                .addPos(-85, 0, 0, 200)
                                                .addPos(-15, 0, 0, 200)
                                                .addPos(-15, 0, 0, 200)
                                                .addPos(-85, 0, 0, 200)
                                                .addPos(0, 0, 0, 200))
                                .addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
                }

                return LAMBDA_MARESLEG_ANIMS.apply(stack, type);
            };

    public static BulletConfig makeShredderConfig(BulletConfig original, BulletConfig submunition) {
        BulletConfig cfg =
                new BulletConfig()
                        .setBeam()
                        .setRenderRotations(false)
                        .setLife(5)
                        .setDamage(original.damageMult * original.projectilesMax)
                        .setupDamageClass(DamageClass.LASER);
        cfg.ammoItem = original.ammoItem;
        cfg.setCasing(original.casing);
        cfg.setOnBeamImpact(
                (beam, mop) -> {
                    int projectiles = submunition.projectilesMin;
                    if (submunition.projectilesMax > submunition.projectilesMin)
                        projectiles +=
                                beam.level()
                                        .getRandom()
                                        .nextInt(
                                                submunition.projectilesMax
                                                        - submunition.projectilesMin
                                                        + 1);

                    if (mop instanceof BlockHitResult blockHit) {

                        Direction dir = blockHit.getDirection();
                        Vec3 hit =
                                mop.getLocation()
                                        .add(
                                                dir.getStepX() * 0.1,
                                                dir.getStepY() * 0.1,
                                                dir.getStepZ() * 0.1);

                        spawnPulse(beam.level(), mop, beam.getYRot(), beam.getXRot());

                        List<Entity> blast =
                                beam.level()
                                        .getEntities(
                                                beam,
                                                new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                                                        .inflate(0.75, 0.75, 0.75));
                        DamageSource source =
                                BulletConfig.getDamage(beam, beam.getThrower(), DamageClass.LASER);

                        for (Entity e : blast) {
                            if (!e.isAlive()) continue;
                            if (e instanceof LivingEntity living) {
                                EntityDamageUtil.attackEntityFromNT(
                                        living, source, beam.damage, true, false, 0D, 0F, 0F);
                                if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
                            } else {
                                e.hurtServer((ServerLevel) beam.level(), source, beam.damage);
                            }
                        }

                        for (int i = 0; i < projectiles; i++) {
                            EntityBulletBaseMK4 bullet =
                                    new EntityBulletBaseMK4(
                                            beam.level(),
                                            beam.getThrower(),
                                            submunition,
                                            beam.damage * submunition.damageMult,
                                            0.2F,
                                            hit.x,
                                            hit.y,
                                            hit.z,
                                            dir.getStepX(),
                                            dir.getStepY(),
                                            dir.getStepZ());
                            beam.level().addFreshEntity(bullet);
                        }
                    }

                    if (mop instanceof EntityHitResult) {

                        spawnPulse(beam.level(), mop, beam.getYRot(), beam.getXRot());
                        Vec3 hit = mop.getLocation();

                        for (int i = 0; i < projectiles; i++) {
                            Vec3 vec =
                                    new Vec3(
                                                    beam.level().getRandom().nextGaussian(),
                                                    beam.level().getRandom().nextGaussian(),
                                                    beam.level().getRandom().nextGaussian())
                                            .normalize();
                            EntityBulletBaseMK4 bullet =
                                    new EntityBulletBaseMK4(
                                            beam.level(),
                                            beam.getThrower(),
                                            submunition,
                                            beam.damage * submunition.damageMult,
                                            0.2F,
                                            hit.x,
                                            hit.y,
                                            hit.z,
                                            vec.x,
                                            vec.y,
                                            vec.z);
                            beam.level().addFreshEntity(bullet);
                        }
                    }
                });
        return cfg;
    }

    public static BulletConfig makeShredderSubmunition(BulletConfig original) {
        BulletConfig cfg = original.clone();
        cfg.setRicochetAngle(90)
                .setRicochetCount(3)
                .setVel(0.5F)
                .setLife(50)
                .setupDamageClass(DamageClass.PLASMA)
                .setOnRicochet(LAMBDA_SHREDDER_RICOCHET);
        return cfg;
    }

    public static void spawnPulse(Level world, HitResult mop, float yaw, float pitch) {

        double x = mop.getLocation().x;
        double y = mop.getLocation().y;
        double z = mop.getLocation().z;

        if (mop instanceof BlockHitResult blockHit) {
            Direction side = blockHit.getDirection();
            if (side == Direction.UP) {
                yaw = 0F;
                pitch = 0F;
            }
            if (side == Direction.DOWN) {
                yaw = 0F;
                pitch = 0F;
            }
            if (side == Direction.NORTH) {
                yaw = 0F;
                pitch = 90F;
            }
            if (side == Direction.SOUTH) {
                yaw = 180F;
                pitch = 90F;
            }
            if (side == Direction.EAST) {
                yaw = 90F;
                pitch = 90F;
            }
            if (side == Direction.WEST) {
                yaw = 270F;
                pitch = 90F;
            }

            x += side.getStepX() * 0.05;
            y += side.getStepY() * 0.05;
            z += side.getStepZ() * 0.05;
        }

        if (world instanceof ServerLevel server) {
            Services.NETWORK.sendToAllAround(
                    new PlasmaBlastPayload(x, y, z, 0.5F, 0.5F, 1.0F, pitch, yaw, 0.75F),
                    new TargetPoint(server, x, y, z, 100));
        }
    }

    public static void init(IRegistrar r) {

        float buckshotSpread = 0.035F;
        float magnumSpread = 0.015F;
        g12_bp =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_BP)
                        .setCasing(EnumCasingType.SHOTSHELL, 12)
                        .setBlackPowder(true)
                        .setProjectiles(8)
                        .setDamage(0.75F / 8F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(15)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(
                                                SpentCasing.COLOR_CASE_BRASS,
                                                SpentCasing.COLOR_CASE_BRASS)
                                        .setScale(0.75F)
                                        .register("12GA_BP"));
        g12_bp_magnum =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_BP_MAGNUM)
                        .setCasing(EnumCasingType.SHOTSHELL, 12)
                        .setBlackPowder(true)
                        .setProjectiles(4)
                        .setDamage(0.75F / 4F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(25)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(
                                                SpentCasing.COLOR_CASE_BRASS,
                                                SpentCasing.COLOR_CASE_BRASS)
                                        .setScale(0.75F)
                                        .register("12GA_BP_MAGNUM"));
        g12_bp_slug =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_BP_SLUG)
                        .setCasing(EnumCasingType.SHOTSHELL, 12)
                        .setBlackPowder(true)
                        .setDamage(0.75F)
                        .setSpread(0.01F)
                        .setRicochetAngle(5)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(
                                                SpentCasing.COLOR_CASE_BRASS,
                                                SpentCasing.COLOR_CASE_BRASS)
                                        .setScale(0.75F)
                                        .register("12GA_BP_SLUG"));
        g12 =
                new BulletConfig()
                        .setItem(EnumAmmo.G12)
                        .setCasing(EnumCasingType.BUCKSHOT, 6)
                        .setProjectiles(8)
                        .setDamage(1F / 8F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(15)
                        .setThresholdNegation(2F)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xB52B2B, SpentCasing.COLOR_CASE_BRASS)
                                        .setScale(0.75F)
                                        .register("12GA"));
        g12_slug =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_SLUG)
                        .setCasing(EnumCasingType.BUCKSHOT, 6)
                        .setHeadshot(1.5F)
                        .setSpread(0.0F)
                        .setRicochetAngle(25)
                        .setThresholdNegation(4F)
                        .setArmorPiercing(0.15F)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0x393939, SpentCasing.COLOR_CASE_BRASS)
                                        .setScale(0.75F)
                                        .register("12GA_SLUG"));
        g12_flechette =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_FLECHETTE)
                        .setCasing(EnumCasingType.BUCKSHOT, 6)
                        .setProjectiles(8)
                        .setDamage(1F / 8F)
                        .setThresholdNegation(5F)
                        .setArmorPiercing(0.2F)
                        .setSpread(0.025F)
                        .setRicochetAngle(5)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0x3C80F0, SpentCasing.COLOR_CASE_BRASS)
                                        .setScale(0.75F)
                                        .register("12GA_FLECHETTE"));
        g12_magnum =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_MAGNUM)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6)
                        .setProjectiles(4)
                        .setDamage(2F / 4F)
                        .setSpread(magnumSpread)
                        .setRicochetAngle(15)
                        .setThresholdNegation(4F)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0x278400, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(0.75F)
                                        .register("12GA_MAGNUM"));
        g12_explosive =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_EXPLOSIVE)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6)
                        .setDamage(2.5F)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE)
                        .setSpread(0F)
                        .setRicochetAngle(15)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xDA4127, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(0.75F)
                                        .register("12GA_EXPLOSIVE"));
        g12_phosphorus =
                new BulletConfig()
                        .setItem(EnumAmmo.G12_PHOSPHORUS)
                        .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6)
                        .setProjectiles(8)
                        .setDamage(1F / 8F)
                        .setSpread(magnumSpread)
                        .setRicochetAngle(15)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0x910001, SpentCasing.COLOR_CASE_12GA)
                                        .setScale(0.75F)
                                        .register("12GA_PHOSPHORUS"))
                        .setOnImpact(
                                (bullet, mop) -> {
                                    if (mop instanceof EntityHitResult entityHit
                                            && entityHit.getEntity()
                                                    instanceof LivingEntity living) {
                                        HbmLivingProps data = HbmLivingProps.getData(living);
                                        if (data.phosphorus < 300) data.phosphorus = 300;
                                    }
                                });
        g12_equestrian_bj =
                new BulletConfig()
                        .setItem(GunFactory.EnumAmmoSecret.G12_EQUESTRIAN)
                        .setDamage(0F)
                        .setOnImpact(LAMBDA_BOAT)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xB52B2B, SpentCasing.COLOR_CASE_EQUESTRIAN)
                                        .setScale(0.75F)
                                        .register("12gaEquestrianBJ"));
        g12_equestrian_tkr =
                new BulletConfig()
                        .setItem(GunFactory.EnumAmmoSecret.G12_EQUESTRIAN)
                        .setDamage(0F)
                        .setCasing(
                                new SpentCasing(CasingType.SHOTGUN)
                                        .setColor(0xB52B2B, SpentCasing.COLOR_CASE_EQUESTRIAN)
                                        .setScale(0.75F)
                                        .register("12gaEquestrianTKR"));

        BulletConfig[] all =
                new BulletConfig[] {
                    g12_bp,
                    g12_bp_magnum,
                    g12_bp_slug,
                    g12,
                    g12_slug,
                    g12_flechette,
                    g12_magnum,
                    g12_explosive,
                    g12_phosphorus
                };

        g12_sub = makeShredderSubmunition(g12);
        g12_sub_slug = makeShredderSubmunition(g12_slug);
        g12_sub_flechette = makeShredderSubmunition(g12_flechette);
        g12_sub_magnum = makeShredderSubmunition(g12_magnum);
        g12_sub_explosive = makeShredderSubmunition(g12_explosive);
        g12_sub_phosphorus = makeShredderSubmunition(g12_phosphorus);
        g12_shredder = makeShredderConfig(g12, g12_sub);
        g12_shredder_slug = makeShredderConfig(g12_slug, g12_sub_slug);
        g12_shredder_flechette = makeShredderConfig(g12_flechette, g12_sub_flechette);
        g12_shredder_magnum = makeShredderConfig(g12_magnum, g12_sub_magnum);
        g12_shredder_explosive = makeShredderConfig(g12_explosive, g12_sub_explosive);
        g12_shredder_phosphorus = makeShredderConfig(g12_phosphorus, g12_sub_phosphorus);

        ModItems.GUN_MARESLEG =
                r.registerItem(
                        "gun_maresleg",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(600)
                                                        .draw(10)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(16F)
                                                                        .delay(20)
                                                                        .reload(22, 10, 13, 0)
                                                                        .jam(24)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                all))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MARESLEG))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MARESLEG_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_MARESLEG))
                                        .setDefaultAmmo(EnumAmmo.G12, 12)
                                        .setNameMutator(LAMBDA_NAME_MARESLEG),
                        Item.Properties::new);

        ModItems.GUN_MARESLEG_AKIMBO =
                r.registerItem(
                        "gun_maresleg_akimbo",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(600)
                                                        .draw(5)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(16F)
                                                                        .spreadHipfire(0F)
                                                                        .spreadAmmo(1.35F)
                                                                        .delay(20)
                                                                        .reload(22, 10, 13, 0)
                                                                        .jam(24)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                all))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MARESLEG))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_MARESLEG_SHORT_ANIMS)
                                                        .orchestra(
                                                                Orchestras
                                                                        .ORCHESTRA_MARESLEG_AKIMBO),
                                                new GunConfig()
                                                        .dura(600)
                                                        .draw(5)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(16F)
                                                                        .spreadHipfire(0F)
                                                                        .spreadAmmo(1.35F)
                                                                        .delay(20)
                                                                        .reload(22, 10, 13, 0)
                                                                        .jam(24)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                1,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                all))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MARESLEG))
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_MARESLEG_SHORT_ANIMS)
                                                        .orchestra(
                                                                Orchestras
                                                                        .ORCHESTRA_MARESLEG_AKIMBO))
                                        .setDefaultAmmo(EnumAmmo.G12, 24),
                        Item.Properties::new);

        ModItems.GUN_MARESLEG_BROKEN =
                r.registerItem(
                        "gun_maresleg_broken",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                new GunConfig()
                                                        .dura(0)
                                                        .draw(5)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(48F)
                                                                        .spreadAmmo(1.15F)
                                                                        .delay(20)
                                                                        .reload(22, 10, 13, 0)
                                                                        .jam(24)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                g12_equestrian_tkr,
                                                                                                g12_bp,
                                                                                                g12_bp_magnum,
                                                                                                g12_bp_slug,
                                                                                                g12,
                                                                                                g12_slug,
                                                                                                g12_flechette,
                                                                                                g12_magnum,
                                                                                                g12_explosive,
                                                                                                g12_phosphorus))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .canFire(
                                                                                Lego
                                                                                        .LAMBDA_STANDARD_CAN_FIRE)
                                                                        .fire(
                                                                                Lego
                                                                                        .LAMBDA_NOWEAR_FIRE)
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MARESLEG))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MARESLEG_SHORT_ANIMS)
                                                        .orchestra(
                                                                Orchestras
                                                                        .ORCHESTRA_MARESLEG_SHORT))
                                        .setDefaultAmmo(EnumAmmo.G12_MAGNUM, 24),
                        Item.Properties::new);

        ModItems.GUN_LIBERATOR =
                r.registerItem(
                        "gun_liberator",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(200)
                                                        .draw(20)
                                                        .inspect(21)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(16F)
                                                                        .delay(20)
                                                                        .rounds(4)
                                                                        .reload(25, 15, 7, 0)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_LIBERATOR_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                4)
                                                                                        .addConfigs(
                                                                                                all))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_LIBERATOR))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LIBERATOR_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_LIBERATOR))
                                        .setDefaultAmmo(EnumAmmo.G12, 12),
                        Item.Properties::new);

        ModItems.GUN_SPAS12 =
                r.registerItem(
                        "gun_spas12",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(600)
                                                        .draw(20)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .reloadChangeType(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(32F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(20)
                                                                        .reload(5, 10, 10, 10, 0)
                                                                        .jam(36)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SPAS_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                8)
                                                                                        .addConfigs(
                                                                                                all))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MARESLEG))
                                                        .setupStandardConfiguration()
                                                        .ps(LAMBDA_SPAS_SECONDARY)
                                                        .pt(null)
                                                        .anim(LAMBDA_SPAS_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_SPAS))
                                        .setDefaultAmmo(EnumAmmo.G12, 16),
                        Item.Properties::new);

        ModItems.GUN_AUTOSHOTGUN =
                r.registerItem(
                        "gun_autoshotgun",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(2_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(48F)
                                                                        .delay(10)
                                                                        .auto(true)
                                                                        .autoAfterDry(true)
                                                                        .dryfireAfterAuto(true)
                                                                        .reload(44)
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
                                                                                                20)
                                                                                        .addConfigs(
                                                                                                all))
                                                                        .offset(0.75, -0.125, -0.25)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_AUTOSHOTGUN))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_SHREDDER_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_SHREDDER))
                                        .setDefaultAmmo(EnumAmmo.G12, 20),
                        Item.Properties::new);

        ModItems.GUN_AUTOSHOTGUN_SHREDDER =
                r.registerItem(
                        "gun_autoshotgun_shredder",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                new GunConfig()
                                                        .dura(2_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(50F)
                                                                        .delay(10)
                                                                        .auto(true)
                                                                        .autoAfterDry(true)
                                                                        .dryfireAfterAuto(true)
                                                                        .reload(44)
                                                                        .jam(19)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHREDDER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                g12_shredder,
                                                                                                g12_shredder_slug,
                                                                                                g12_shredder_flechette,
                                                                                                g12_shredder_magnum,
                                                                                                g12_shredder_explosive,
                                                                                                g12_shredder_phosphorus))
                                                                        .offset(0.75, -0.125, -0.25)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_AUTOSHOTGUN))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_SHREDDER_ANIMS)
                                                        .orchestra(Orchestras.ORCHESTRA_SHREDDER))
                                        .setDefaultAmmo(EnumAmmo.G12, 20),
                        Item.Properties::new);

        ModItems.GUN_AUTOSHOTGUN_SEXY =
                r.registerItem(
                        "gun_autoshotgun_sexy",
                        props ->
                                new ItemGunBaseNT(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                new GunConfig()
                                                        .dura(5_000)
                                                        .draw(20)
                                                        .inspect(65)
                                                        .reloadSequential(true)
                                                        .inspectCancel(false)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .hideCrosshair(false)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                new Receiver(0)
                                                                        .dmg(64F)
                                                                        .delay(4)
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
                                                                                                100)
                                                                                        .addConfigs(
                                                                                                g12_equestrian_bj,
                                                                                                g12_bp,
                                                                                                g12_bp_magnum,
                                                                                                g12_bp_slug,
                                                                                                g12,
                                                                                                g12_slug,
                                                                                                g12_flechette,
                                                                                                g12_magnum,
                                                                                                g12_explosive,
                                                                                                g12_phosphorus))
                                                                        .offset(0.75, -0.125, -0.25)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_SEXY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_SEXY_ANIMS)
                                                        .orchestra(
                                                                Orchestras.ORCHESTRA_SHREDDER_SEXY))
                                        .setDefaultAmmo(EnumAmmo.G12_MAGNUM, 50),
                        Item.Properties::new);
    }
}
