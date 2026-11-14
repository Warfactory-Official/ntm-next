/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.weapon.sedna.mods;

import com.hbm.extprop.LivingHazards;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.cue.GunAnimationTimeline;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.EntityTargeting;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

final class DerivedMagazines {
    private DerivedMagazines() {}

    static MagazineSingleReload single(int index, int capacity, List<BulletConfig> bullets) {
        var mag = new MagazineSingleReload(index, capacity);
        mag.acceptedBullets = bullets;
        return mag;
    }

    static MagazineFullReload full(int index, int capacity, List<BulletConfig> bullets) {
        var mag = new MagazineFullReload(index, capacity);
        mag.acceptedBullets = bullets;
        return mag;
    }

    static MagazineBelt belt(List<BulletConfig> bullets) {
        var mag = new MagazineBelt();
        mag.acceptedBullets = bullets;
        return mag;
    }
}

final class WeaponModChoke extends WeaponModBase {
    WeaponModChoke() {
        super(0, Slot.BARREL);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.SPREAD_AMMO, 0.5F);
    }
}

final class WeaponModGenericDamage extends WeaponModBase {
    WeaponModGenericDamage() {
        super(PRIORITY_MULTIPLICATIVE, Slot.GENERIC_DAMAGE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.BASE_DAMAGE, 1.15F);
    }
}

final class WeaponModGenericDurability extends WeaponModBase {
    WeaponModGenericDurability() {
        super(PRIORITY_MULTIPLICATIVE, Slot.GENERIC_DURABILITY);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(GunConfig.DURABILITY, 2F);
    }
}

final class WeaponModLasAuto extends WeaponModBase {
    WeaponModLasAuto() {
        super(PRIORITY_SET, Slot.RECEIVER);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.BASE_DAMAGE, 0.66F)
                .set(Receiver.REFIRE_ON_HOLD, true)
                .set(Receiver.DELAY_AFTER_FIRE, 5)
                .set(GunConfig.SCOPE_TEXTURE, null);
    }
}

final class WeaponModLasCapacitor extends WeaponModBase {
    WeaponModLasCapacitor() {
        super(PRIORITY_MULTIPLICATIVE, Slot.UNDERBARREL);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.BASE_DAMAGE, 1.05F)
                .map(
                        Receiver.MAGAZINE,
                        mag ->
                                mag instanceof MagazineFullReload full
                                        ? DerivedMagazines.full(
                                                full.index,
                                                full.capacity * 3 / 2,
                                                full.acceptedBullets)
                                        : mag);
    }
}

final class WeaponModLasShotgun extends WeaponModBase {
    WeaponModLasShotgun() {
        super(PRIORITY_MULTIPLICATIVE, Slot.BARREL);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.BASE_DAMAGE, 0.35F)
                .scale(Receiver.SPLIT_PROJECTILES, 3F)
                .add(Receiver.SPREAD_INNATE, 3F)
                .set(Receiver.SPREAD_HIPFIRE, 0F)
                .set(GunConfig.CROSSHAIR, Crosshair.L_CIRCLE);
    }
}

final class WeaponModMinigunSpeedup extends WeaponModBase {
    WeaponModMinigunSpeedup() {
        super(0, Slot.SPEED);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(Receiver.ROUNDS_PER_CYCLE, v -> v * 3).scale(Receiver.SPREAD_INNATE, 1.5F);
    }
}

final class WeaponModOverride extends WeaponModBase {
    private final float baseDamage;

    WeaponModOverride(float baseDamage) {
        super(PRIORITY_SET, Slot.OVERRIDE);
        this.baseDamage = baseDamage;
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(Receiver.BASE_DAMAGE, baseDamage);
    }
}

final class WeaponModPolymerFurniture extends WeaponModBase {
    static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 =
            (_, ctx) -> {
                Player player = ctx.getPlayer();
                if (player != null)
                    GunBaseNTItem.setupRecoil(
                            (float) (player.getRandom().nextGaussian() * 0.125),
                            (float) (player.getRandom().nextGaussian() * 0.125));
            };

    WeaponModPolymerFurniture() {
        super(0, Slot.FURNITURE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(Receiver.ON_RECOIL, LAMBDA_RECOIL_G3);
    }
}

final class WeaponModSawedOff extends WeaponModBase {
    WeaponModSawedOff() {
        super(0, Slot.BARREL);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(Receiver.SPREAD_INNATE, v -> Math.max(0.025F, v))
                .scale(Receiver.SPREAD_AMMO, 1.5F)
                .scale(Receiver.BASE_DAMAGE, 1.35F);
    }
}

final class WeaponModScope extends WeaponModBase {
    WeaponModScope() {
        super(0, Slot.SCOPE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(GunConfig.SCOPE_TEXTURE, XFactory556mm.scope).set(GunConfig.HIDE_CROSSHAIR, true);
    }
}

final class WeaponModShredderSpeedup extends WeaponModBase {
    WeaponModShredderSpeedup() {
        super(0, Slot.SPEED);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(Receiver.DELAY_AFTER_FIRE, v -> v / 2)
                .map(Receiver.DELAY_AFTER_DRY_FIRE, v -> v / 2);
    }
}

final class WeaponModSlowdown extends WeaponModBase {
    WeaponModSlowdown() {
        super(0, Slot.SPEED);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(Receiver.DELAY_AFTER_FIRE, v -> v * 2).set(Receiver.SPREAD_INNATE, 0F);
    }
}

final class WeaponModTestDamage extends WeaponModBase {
    WeaponModTestDamage() {
        super(PRIORITY_MULT_FINAL, Slot.DAMAGE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.BASE_DAMAGE, 1.5F);
    }
}

final class WeaponModTestFirerate extends WeaponModBase {
    WeaponModTestFirerate() {
        super(PRIORITY_MULT_FINAL, Slot.FIRERATE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(Receiver.DELAY_AFTER_FIRE, v -> Math.max(v / 2, 1));
    }
}

final class WeaponModTestMulti extends WeaponModBase {
    WeaponModTestMulti() {
        super(PRIORITY_MULT_FINAL, Slot.MULTI);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(Receiver.ROUNDS_PER_CYCLE, v -> v * 3);
    }
}

final class WeaponModUziSaturnite extends WeaponModBase {
    WeaponModUziSaturnite() {
        super(PRIORITY_ADDITIVE, Slot.FURNITURE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(GunConfig.DURABILITY, 5F).add(Receiver.BASE_DAMAGE, 3F);
    }
}

final class WeaponModNickel extends WeaponModBase {
    WeaponModNickel(Slot slot) {
        super(PRIORITY_SET, slot);
    }

    @Override
    protected void operations(Operations ops) {}
}

final class WeapnModG3SawedOff extends WeaponModBase {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_G3_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.EQUIP)
                    return new BusAnimation()
                            .addBus(
                                    "EQUIP",
                                    new AnimationSequence()
                                            .to(45, 0, 0, 0)
                                            .to(0, 0, 0, 250, IType.SIN_FULL));
                return XFactory556mm.LAMBDA_G3_ANIMS.apply(stack, type);
            };

    WeapnModG3SawedOff() {
        super(0, Slot.SHIELD);
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(GunConfig.DRAW_DURATION, 5).set(GunConfig.ANIMATIONS, LAMBDA_G3_ANIMS);
    }
}

final class WeaponModGreasegun extends WeaponModBase {
    WeaponModGreasegun() {
        super(PRIORITY_ADDITIVE, Slot.FURNITURE);
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(GunConfig.DURABILITY, 3F)
                .add(Receiver.BASE_DAMAGE, 2F)
                .set(Receiver.SPREAD_INNATE, 0F)
                .map(Receiver.DELAY_AFTER_FIRE, v -> v / 2)
                .set(GunConfig.ORCHESTRA, GunAnimationActions.GREASEGUN_CLEAN);
    }
}

final class WeaponModLiberatorSpeedloader extends WeaponModBase {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LIBERATOR_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case RELOAD -> {
                        return new BusAnimation()
                                .addBus("LATCH", new AnimationSequence().to(15, 0, 0, 100))
                                .addBus(
                                        "BREAK",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 100)
                                                .to(60, 0, 0, 350, IType.SIN_DOWN))
                                .addBus("SHELL1", reloadShell())
                                .addBus("SHELL2", reloadShell())
                                .addBus("SHELL3", reloadShell())
                                .addBus("SHELL4", reloadShell());
                    }
                    case RELOAD_END -> {
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new AnimationSequence()
                                                .to(15, 0, 0, 0)
                                                .to(15, 0, 0, 250)
                                                .to(0, 0, 0, 50))
                                .addBus(
                                        "BREAK",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 0)
                                                .to(0, 0, 0, 250, IType.SIN_UP));
                    }
                    case JAMMED -> {
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new AnimationSequence()
                                                .to(15, 0, 0, 0)
                                                .to(15, 0, 0, 250)
                                                .to(0, 0, 0, 50)
                                                .to(0, 0, 0, 550)
                                                .to(15, 0, 0, 100)
                                                .to(15, 0, 0, 600)
                                                .to(0, 0, 0, 50))
                                .addBus(
                                        "BREAK",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 0)
                                                .to(0, 0, 0, 250, IType.SIN_UP)
                                                .to(0, 0, 0, 600)
                                                .to(45, 0, 0, 250, IType.SIN_DOWN)
                                                .to(45, 0, 0, 300)
                                                .to(0, 0, 0, 150, IType.SIN_UP));
                    }
                }
                return XFactory12ga.LAMBDA_LIBERATOR_ANIMS.apply(stack, type);
            };

    WeaponModLiberatorSpeedloader() {
        super(0, Slot.SPEEDLOADER);
    }

    private static AnimationSequence reloadShell() {
        return new AnimationSequence()
                .to(2, -4, -2, 0)
                .to(2, -4, -2, 400)
                .to(0, 0, -2, 450, IType.SIN_FULL)
                .to(0, 0, 0, 50, IType.SIN_UP);
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(GunConfig.ANIMATIONS, LAMBDA_LIBERATOR_ANIMS)
                .map(
                        Receiver.MAGAZINE,
                        mag ->
                                mag instanceof MagazineSingleReload single
                                        ? DerivedMagazines.full(0, 4, single.acceptedBullets)
                                        : mag);
    }
}

final class WeaponModCaliber extends WeaponModBase {
    private final float baseDamage;
    private final List<BulletConfig> cfg;
    private final int count;

    WeaponModCaliber(int count, float baseDamage, BulletConfig... bullets) {
        super(PRIORITY_SET, Slot.CALIBER);
        this.cfg = List.of(bullets);
        this.count = count;
        this.baseDamage = baseDamage;
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(
                        Receiver.MAGAZINE,
                        mag ->
                                switch (mag) {
                                    case MagazineSingleReload single ->
                                            DerivedMagazines.single(single.index, count, cfg);
                                    case MagazineFullReload full ->
                                            DerivedMagazines.full(full.index, count, cfg);
                                    case MagazineBelt _ ->
                                            DerivedMagazines.belt(cfg);
                                    default -> mag;
                                })
                .set(Receiver.BASE_DAMAGE, baseDamage);
    }

    @Override
    protected boolean revalidatesMagazine() {
        return true;
    }
}

final class WeaponModCanisters extends WeaponModBase {
    WeaponModCanisters() {
        super(PRIORITY_MULT_FINAL, Slot.CANISTERS);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(
                Receiver.MAGAZINE,
                mag ->
                        switch (mag) {
                            case MagazineLiquidEngine liquid ->
                                    new MagazineLiquidEngine(
                                            liquid.index, liquid.capacity * 3, liquid::accepted);
                            case MagazineElectricEngine electric ->
                                    new MagazineElectricEngine(
                                            electric.index, electric.capacity * 3);
                            default -> mag;
                        });
    }

    @Override
    protected boolean revalidatesMagazine() {
        return true;
    }
}

final class WeaponModDrill extends WeaponModBase {
    private final float damage, dt, pierce;
    private final double reach;
    private final int aoe;
    private final int harvest;

    WeaponModDrill(float damage, float dt, float pierce, double reach, int aoe, int harvest) {
        super(PRIORITY_SET, Slot.DRILL);
        this.damage = damage;
        this.dt = dt;
        this.pierce = pierce;
        this.reach = reach;
        this.aoe = aoe;
        this.harvest = harvest;
    }

    @Override
    protected void operations(Operations ops) {
        ops.scale(Receiver.BASE_DAMAGE, damage)
                .scale(XFactoryDrill.REACH, reach)
                .set(XFactoryDrill.DT_NEGATION, dt)
                .set(XFactoryDrill.PIERCE, pierce)
                .set(XFactoryDrill.HARVEST, harvest);
        if (aoe >= 0) ops.set(XFactoryDrill.AOE, aoe);
    }
}

final class WeaponModDrillFortune extends WeaponModBase {
    private final int addFortune;

    WeaponModDrillFortune(Slot slot, int addFortune) {
        super(PRIORITY_ADDITIVE, slot);
        this.addFortune = addFortune;
    }

    private static void adjustFortune(ServerLevel level, ItemStack gun, int delta) {
        var fortune =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE);
        int result =
                gun.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                                .getLevel(fortune)
                        + delta;
        gun.update(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY,
                old -> {
                    var mutable = new ItemEnchantments.Mutable(old);
                    if (result > 0) mutable.set(fortune, result);
                    else mutable.removeIf(holder -> holder.is(Enchantments.FORTUNE));
                    return mutable.toImmutable();
                });
    }

    @Override
    protected void operations(Operations ops) {}

    @Override
    protected void onInstall(ServerLevel level, ItemStack gun) {
        adjustFortune(level, gun, addFortune);
    }

    @Override
    protected void onUninstall(ServerLevel level, ItemStack gun) {
        adjustFortune(level, gun, -addFortune);
    }
}

final class WeaponModEngine extends WeaponModBase {
    static final MagazineLiquidEngine ENGINE_DIESEL =
            new MagazineLiquidEngine(
                    0,
                    4_000,
                    () ->
                            new Fluid[] {
                                NTMFluids.DIESEL, NTMFluids.DIESEL_CRACK, NTMFluids.LIGHTOIL
                            });
    static final MagazineLiquidEngine ENGINE_AVIATION =
            new MagazineLiquidEngine(
                    0, 4_000, () -> new Fluid[] {NTMFluids.KEROSENE, NTMFluids.LPG});
    static final MagazineElectricEngine ENGINE_ELECTRIC = new MagazineElectricEngine(0, 1_000_000);
    static final MagazineLiquidEngine ENGINE_TURBO =
            new MagazineLiquidEngine(
                    0, 4_000, () -> new Fluid[] {NTMFluids.KEROSENE_REFORM, NTMFluids.REFORMATE});
    private final Magazine<?> mag;
    private final int delay;

    WeaponModEngine(Magazine<?> mag, int delay) {
        super(PRIORITY_SET, Slot.ENGINE);
        this.mag = mag;
        this.delay = delay;
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(Receiver.MAGAZINE, mag).set(Receiver.DELAY_AFTER_FIRE, delay);
    }

    @Override
    protected boolean revalidatesMagazine() {
        return true;
    }
}

final class WeaponModStackMag extends WeaponModBase {
    WeaponModStackMag() {
        super(0, Slot.MAG);
    }

    @Override
    protected void operations(Operations ops) {
        ops.map(
                Receiver.MAGAZINE,
                mag ->
                        switch (mag) {
                            case MagazineSingleReload single ->
                                    DerivedMagazines.single(
                                            single.index,
                                            single.capacity * 3 / 2,
                                            single.acceptedBullets);
                            case MagazineFullReload full ->
                                    DerivedMagazines.full(
                                            full.index,
                                            full.capacity * 3 / 2,
                                            full.acceptedBullets);
                            default -> mag;
                        });
    }

    @Override
    protected boolean revalidatesMagazine() {
        return true;
    }
}

final class WeaponModPanzerschreckSawedOff extends WeaponModBase {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PANZERSCHRECK_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.EQUIP)
                    return new BusAnimation()
                            .addBus(
                                    "EQUIP",
                                    new AnimationSequence()
                                            .to(60, 0, 0, 0)
                                            .to(0, 0, 0, 250, IType.SIN_DOWN));
                return XFactoryRocket.LAMBDA_PANZERSCHRECK_ANIMS.apply(stack, type);
            };
    static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE =
            (stack, ctx) -> {
                Lego.LAMBDA_STANDARD_FIRE.accept(stack, ctx);
                if (ctx.entity() != null) {
                    LivingHazards.getOrCreate(ctx.entity()).fire += 100;
                    if (ctx.entity().level() instanceof ServerLevel server) {
                        ctx.entity()
                                .hurtServer(
                                        server,
                                        ModifiedDamageSource.of(
                                                BulletConfig.getDamage(
                                                        ctx.entity(),
                                                        ctx.entity(),
                                                        ModDamageTypes.FIRE),
                                                true,
                                                0F,
                                                0F,
                                                0F),
                                        4F);
                    }
                }
            };

    WeaponModPanzerschreckSawedOff() {
        super(0, Slot.SHIELD);
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(GunConfig.DRAW_DURATION, 5)
                .set(Receiver.ON_FIRE, LAMBDA_FIRE)
                .set(GunConfig.ANIMATIONS, LAMBDA_PANZERSCHRECK_ANIMS);
    }
}

abstract class WeaponModBayonet extends WeaponModBase {
    WeaponModBayonet() {
        super(0, Slot.BAYONET);
    }

    private static void stab(Player player, LivingEntity entity) {
        var hit = EntityTargeting.getMouseOver(player, 3D);
        if (hit instanceof EntityHitResult entityHit) {
            var victim = entityHit.getEntity();
            victim.hurtServer(
                    (ServerLevel) entity.level(), player.damageSources().playerAttack(player), 15F);
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(2D, 1D, 2D));
            EntitySounds.play(
                    victim,
                    victim.getX(),
                    victim.getY(),
                    victim.getZ(),
                    ModSounds.GUN_STAB.get(),
                    SoundSource.PLAYERS,
                    1F,
                    0.9F + entity.getRandom().nextFloat() * 0.2F);
        } else if (hit instanceof BlockHitResult blockHit) {
            var state = entity.level().getBlockState(blockHit.getBlockPos());
            WorldSounds.play(
                    entity.level(),
                    null,
                    blockHit.getLocation().x,
                    blockHit.getLocation().y,
                    blockHit.getLocation().z,
                    state.getSoundType().getStepSound(),
                    SoundSource.BLOCKS,
                    2F,
                    0.9F + entity.getRandom().nextFloat() * 0.2F);
        }
    }

    abstract BiFunction<ItemStack, GunAnimation, BusAnimation> animation();

    abstract GunAnimationTimeline normalOrchestra();

    @Override
    protected void operations(Operations ops) {
        var orchestra =
                normalOrchestra()
                        .with(
                                t ->
                                        t.replace(GunAnimation.INSPECT)
                                                .at(
                                                        15,
                                                        (_, ctx) -> {
                                                            if (ctx.getPlayer() != null)
                                                                stab(ctx.getPlayer(), ctx.entity());
                                                        }));
        ops.set(GunConfig.ANIMATIONS, animation())
                .set(GunConfig.INSPECT_DURATION, 30)
                .set(GunConfig.PRESS_SECONDARY, XFactory44.SMACK_A_FUCKER)
                .set(GunConfig.ORCHESTRA, orchestra)
                .set(GunConfig.INSPECT_CANCEL, false);
    }
}

final class WeaponModCarbineBayonet extends WeaponModBayonet {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CARBINE_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.INSPECT) return stabAnimation();
                return XFactory762mm.LAMBDA_CARBINE_ANIMS.apply(stack, type);
            };

    static BusAnimation stabAnimation() {
        return new BusAnimation()
                .addBus(
                        "STAB",
                        new AnimationSequence()
                                .to(0, 1, -2, 250, IType.SIN_DOWN)
                                .stay(250)
                                .to(0, 1, 5, 250, IType.SIN_UP)
                                .stay(250)
                                .to(0, 0, 0, 500, IType.SIN_FULL));
    }

    @Override
    GunAnimationTimeline normalOrchestra() {
        return GunAnimationActions.CARBINE;
    }

    @Override
    BiFunction<ItemStack, GunAnimation, BusAnimation> animation() {
        return LAMBDA_CARBINE_ANIMS;
    }
}

final class WeaponModMASBayonet extends WeaponModBayonet {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MAS36_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.INSPECT) return WeaponModCarbineBayonet.stabAnimation();
                return XFactory762mm.LAMBDA_MAS36_ANIMS.apply(stack, type);
            };

    @Override
    GunAnimationTimeline normalOrchestra() {
        return GunAnimationActions.MAS36;
    }

    @Override
    BiFunction<ItemStack, GunAnimation, BusAnimation> animation() {
        return LAMBDA_MAS36_ANIMS;
    }
}

final class WeaponModSilencer extends WeaponModBase {
    WeaponModSilencer() {
        super(0, Slot.SILENCER);
    }

    @Override
    protected void operations(Operations ops) {
        ops.set(Receiver.FIRE_SOUND, ModSounds.GUN_RIFLE_SILENCER);
    }
}
