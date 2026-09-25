// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mods;

import com.hbm.config.GunVisualConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.*;
import com.hbm.items.weapon.sedna.mags.*;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.ModSounds;
import com.hbm.util.DamageClass;
import com.hbm.util.EntityDamageUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

final class WeaponModChoke extends WeaponModBase {
    WeaponModChoke(int id) {
        super(id, "BARREL");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return key == Receiver.F_SPREADAMMO ? cast((Float) base * 0.5F, base) : base;
    }
}

final class WeaponModGenericDamage extends WeaponModBase {
    WeaponModGenericDamage(int id) {
        super(id, "GENERIC_DAMAGE");
        setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof Receiver && key == Receiver.F_BASEDAMAGE && base instanceof Float
                ? cast((Float) base * 1.15F, base)
                : base;
    }
}

final class WeaponModGenericDurability extends WeaponModBase {
    WeaponModGenericDurability(int id) {
        super(id, "GENERIC_DURABILITY");
        setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof GunConfig && key == GunConfig.F_DURABILITY && base instanceof Float
                ? cast((Float) base * 2F, base)
                : base;
    }
}

final class WeaponModLasAuto extends WeaponModBase {
    WeaponModLasAuto(int id) {
        super(id, "RECEIVER");
        setPriority(PRIORITY_SET);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.F_BASEDAMAGE) return cast((Float) base * 0.66F, base);
        if (key == Receiver.B_REFIREONHOLD) return cast(true, base);
        if (key == Receiver.I_DELAYAFTERFIRE) return cast(5, base);
        if (key == GunConfig.O_SCOPETEXTURE) return cast(null, base);
        return base;
    }
}

final class WeaponModLasCapacitor extends WeaponModBase {
    WeaponModLasCapacitor(int id) {
        super(id, "UNDERBARREL");
        setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.F_BASEDAMAGE) return cast((Float) base * 1.05F, base);
        if (key == Receiver.O_MAGAZINE) {
            MagazineFullReload original = (MagazineFullReload) base;
            WeaponModStackMag.DUMMY_FULL.acceptedBullets = original.acceptedBullets;
            WeaponModStackMag.DUMMY_FULL.capacity = original.capacity * 3 / 2;
            WeaponModStackMag.DUMMY_FULL.index = original.index;
            return cast(WeaponModStackMag.DUMMY_FULL, base);
        }
        return base;
    }
}

final class WeaponModLasShotgun extends WeaponModBase {
    WeaponModLasShotgun(int id) {
        super(id, "BARREL");
        setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.F_BASEDAMAGE) return cast((Float) base * 0.35F, base);
        if (key == Receiver.F_SPLITPROJECTILES) return cast((Float) base * 3F, base);
        if (key == Receiver.F_SPREADINNATE) return cast((Float) base + 3F, base);
        if (key == Receiver.F_SPREADHIPFIRE) return cast(0F, base);
        if (key == GunConfig.O_CROSSHAIR) return cast(Crosshair.L_CIRCLE, base);
        return base;
    }
}

final class WeaponModMinigunSpeedup extends WeaponModBase {
    WeaponModMinigunSpeedup(int id) {
        super(id, "SPEED");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.I_ROUNDSPERCYCLE) return cast((Integer) base * 3, base);
        if (key == Receiver.F_SPREADINNATE) return cast((Float) base * 1.5F, base);
        return base;
    }
}

final class WeaponModOverride extends WeaponModBase {
    private final float baseDamage;

    WeaponModOverride(int id, float baseDamage, String... slots) {
        super(id, slots);
        this.baseDamage = baseDamage;
        setPriority(PRIORITY_SET);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return key == Receiver.F_BASEDAMAGE ? cast(baseDamage, base) : base;
    }
}

final class WeaponModPolymerFurniture extends WeaponModBase {
    static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> {
                Player player = ctx.getPlayer();
                if (player != null)
                    ItemGunBaseNT.setupRecoil(
                            (float) (player.getRandom().nextGaussian() * 0.125),
                            (float) (player.getRandom().nextGaussian() * 0.125));
            };

    WeaponModPolymerFurniture(int id) {
        super(id, "FURNITURE");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return key == Receiver.CON_ONRECOIL ? cast(LAMBDA_RECOIL_G3, base) : base;
    }
}

final class WeaponModSawedOff extends WeaponModBase {
    WeaponModSawedOff(int id) {
        super(id, "BARREL");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.F_SPREADINNATE) return cast(Math.max(0.025F, (Float) base), base);
        if (key == Receiver.F_SPREADAMMO) return cast((Float) base * 1.5F, base);
        if (key == Receiver.F_BASEDAMAGE) return cast((Float) base * 1.35F, base);
        if (gun.is(ModItems.GUN_MARESLEG.get())) {
            if (key == GunConfig.FUN_ANIMNATIONS)
                return cast(XFactory12ga.LAMBDA_MARESLEG_SHORT_ANIMS, base);
            if (key == GunConfig.I_DRAWDURATION) return cast(5, base);
        }
        return base;
    }
}

final class WeaponModScope extends WeaponModBase {
    WeaponModScope(int id) {
        super(id, "SCOPE");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.O_SCOPETEXTURE) {
            if (gun.is(ModItems.GUN_HEAVY_REVOLVER.get()))
                return cast(XFactory44.scope_lilmac, base);
            if (((ItemGunBaseNT) gun.getItem()).quality == WeaponQuality.UTILITY)
                return cast(XFactoryTool.scope, base);
            return cast(XFactory556mm.scope, base);
        }
        if (key == GunConfig.B_HIDECROSSHAIR) return cast(true, base);
        return base;
    }
}

final class WeaponModShredderSpeedup extends WeaponModBase {
    WeaponModShredderSpeedup(int id) {
        super(id, "SPEED");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.I_DELAYAFTERFIRE || key == Receiver.I_DELAYAFTERDRYFIRE)
            return cast((Integer) base / 2, base);
        return base;
    }
}

final class WeaponModSlowdown extends WeaponModBase {
    WeaponModSlowdown(int id) {
        super(id, "SPEED");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.I_DELAYAFTERFIRE) return cast((Integer) base * 2, base);
        if (key == Receiver.F_SPREADINNATE) return cast(0F, base);
        return base;
    }
}

final class WeaponModTestDamage extends WeaponModBase {
    WeaponModTestDamage(int id, String... slots) {
        super(id, slots);
        setPriority(PRIORITY_MULT_FINAL);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof Receiver && key == Receiver.F_BASEDAMAGE && base instanceof Float
                ? cast((Float) base * 1.5F, base)
                : base;
    }
}

final class WeaponModTestFirerate extends WeaponModBase {
    WeaponModTestFirerate(int id, String... slots) {
        super(id, slots);
        setPriority(PRIORITY_MULT_FINAL);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof Receiver
                        && key == Receiver.I_DELAYAFTERFIRE
                        && base instanceof Integer
                ? cast(Math.max((Integer) base / 2, 1), base)
                : base;
    }
}

final class WeaponModTestMulti extends WeaponModBase {
    WeaponModTestMulti(int id, String... slots) {
        super(id, slots);
        setPriority(PRIORITY_MULT_FINAL);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof Receiver
                        && key == Receiver.I_ROUNDSPERCYCLE
                        && base instanceof Integer
                ? cast((Integer) base * 3, base)
                : base;
    }
}

final class WeaponModUziSaturnite extends WeaponModBase {
    WeaponModUziSaturnite(int id) {
        super(id, "FURNITURE");
        setPriority(PRIORITY_ADDITIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.F_DURABILITY) return cast((Float) base * 5F, base);
        if (key == Receiver.F_BASEDAMAGE) return cast((Float) base + 3F, base);
        return base;
    }
}

final class WeaponModNickel extends WeaponModBase {
    WeaponModNickel(int id, String name) {
        super(id, name);
        setPriority(PRIORITY_SET);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return base;
    }
}

final class WeapnModG3SawedOff extends WeaponModBase {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_G3_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.EQUIP)
                    return new BusAnimation()
                            .addBus(
                                    "EQUIP",
                                    new BusAnimationSequence()
                                            .addPos(45, 0, 0, 0)
                                            .addPos(0, 0, 0, 250, IType.SIN_FULL));
                return XFactory556mm.LAMBDA_G3_ANIMS.apply(stack, type);
            };

    WeapnModG3SawedOff(int id) {
        super(id, "SHIELD");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.I_DRAWDURATION) return cast(5, base);
        if (key == GunConfig.FUN_ANIMNATIONS) return cast(LAMBDA_G3_ANIMS, base);
        return base;
    }
}

final class WeaponModGreasegun extends WeaponModBase {
    static final BiConsumer<ItemStack, LambdaContext> ORCHESTRA_GREASEGUN =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);
                if (type == GunAnimation.CYCLE) {
                    if (timer == 1) {
                        SpentCasing casing =
                                ctx.config()
                                        .getReceivers(stack)[0]
                                        .getMagazine(stack)
                                        .getCasing(stack, ctx.inventory());
                        if (casing != null)
                            CasingCreator.composeEffect(
                                    entity.level(),
                                    entity,
                                    0.55,
                                    aiming ? 0 : -0.125,
                                    aiming ? 0 : -0.25D,
                                    0,
                                    0.18,
                                    -0.12,
                                    0.01,
                                    -7.5F + (float) entity.getRandom().nextGaussian() * 5F,
                                    12F + (float) entity.getRandom().nextGaussian() * 5F,
                                    casing.getName());
                    }
                    return;
                }
                Orchestras.ORCHESTRA_GREASEGUN.accept(stack, ctx);
            };

    WeaponModGreasegun(int id) {
        super(id, "FURNITURE");
        setPriority(PRIORITY_ADDITIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.F_DURABILITY) return cast((Float) base * 3F, base);
        if (key == Receiver.F_BASEDAMAGE) return cast((Float) base + 2F, base);
        if (key == Receiver.F_SPREADINNATE) return cast(0F, base);
        if (key == Receiver.I_DELAYAFTERFIRE) return cast((Integer) base / 2, base);
        if (key == GunConfig.CON_ORCHESTRA) return cast(ORCHESTRA_GREASEGUN, base);
        return base;
    }
}

final class WeaponModLiberatorSpeedloader extends WeaponModBase {
    static final MagazineFullReload MAG = new MagazineFullReload(0, 4);
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LIBERATOR_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case RELOAD -> {
                        return new BusAnimation()
                                .addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
                                .addBus(
                                        "BREAK",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 100)
                                                .addPos(60, 0, 0, 350, IType.SIN_DOWN))
                                .addBus("SHELL1", reloadShell())
                                .addBus("SHELL2", reloadShell())
                                .addBus("SHELL3", reloadShell())
                                .addBus("SHELL4", reloadShell());
                    }
                    case RELOAD_END -> {
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
                                                .addPos(0, 0, 0, 250, IType.SIN_UP));
                    }
                    case JAMMED -> {
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
                                                .addPos(0, 0, 0, 150, IType.SIN_UP));
                    }
                }
                return XFactory12ga.LAMBDA_LIBERATOR_ANIMS.apply(stack, type);
            };

    WeaponModLiberatorSpeedloader(int id) {
        super(id, "SPEEDLOADER");
    }

    private static BusAnimationSequence reloadShell() {
        return new BusAnimationSequence()
                .addPos(2, -4, -2, 0)
                .addPos(2, -4, -2, 400)
                .addPos(0, 0, -2, 450, IType.SIN_FULL)
                .addPos(0, 0, 0, 50, IType.SIN_UP);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.FUN_ANIMNATIONS) return cast(LAMBDA_LIBERATOR_ANIMS, base);
        if (parent instanceof Receiver
                && base instanceof IMagazine<?>
                && key == Receiver.O_MAGAZINE) {
            MagazineSingleReload original = (MagazineSingleReload) base;
            if (MAG.acceptedBullets.isEmpty()) MAG.acceptedBullets.addAll(original.acceptedBullets);
            return cast(MAG, base);
        }
        return base;
    }
}

final class WeaponModCaliber extends WeaponModBase {
    static final MagazineSingleReload DUMMY_SINGLE = new MagazineSingleReload(0, 0);
    static final MagazineFullReload DUMMY_FULL = new MagazineFullReload(0, 0);
    static final MagazineBelt DUMMY_BELT = new MagazineBelt();
    private final List<BulletConfig> cfg = new ArrayList<>();
    private final int count;
    private final float baseDamage;

    WeaponModCaliber(int id, int count, float baseDamage, BulletConfig... cfg) {
        super(id, "CALIBER");
        setPriority(PRIORITY_SET);
        Collections.addAll(this.cfg, cfg);
        this.count = count;
        this.baseDamage = baseDamage;
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.O_MAGAZINE) {
            if (base instanceof MagazineSingleReload original) {
                DUMMY_SINGLE.acceptedBullets = cfg;
                DUMMY_SINGLE.capacity = count;
                DUMMY_SINGLE.index = original.index;
                return cast(DUMMY_SINGLE, base);
            }
            if (base instanceof MagazineFullReload original) {
                DUMMY_FULL.acceptedBullets = cfg;
                DUMMY_FULL.capacity = count;
                DUMMY_FULL.index = original.index;
                return cast(DUMMY_FULL, base);
            }
            if (base instanceof MagazineBelt) {
                DUMMY_BELT.acceptedBullets = cfg;
                return cast(DUMMY_BELT, base);
            }
        }
        return key == Receiver.F_BASEDAMAGE ? cast(baseDamage, base) : base;
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }
}

final class WeaponModCanisters extends WeaponModBase {
    private static final MagazineElectricEngine DUMMY_ELECTRIC = new MagazineElectricEngine(0, 0);

    WeaponModCanisters(int id) {
        super(id, "CANISTERS");
        setPriority(PRIORITY_MULT_FINAL);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.O_MAGAZINE) {
            if (base instanceof MagazineLiquidEngine original) {
                return cast(
                        new MagazineLiquidEngine(
                                original.index, original.capacity * 3, original::accepted),
                        base);
            }
            if (base instanceof MagazineElectricEngine original) {
                DUMMY_ELECTRIC.capacity = original.capacity * 3;
                DUMMY_ELECTRIC.index = original.index;
                return cast(DUMMY_ELECTRIC, base);
            }
        }
        return base;
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }
}

final class WeaponModDrill extends WeaponModBase {
    private float damage = 1F, dt = -1F, pierce = -1F;
    private double reach = 1D;
    private int aoe = -1, harvest = -1;

    WeaponModDrill(int id) {
        super(id, "DRILL");
        setPriority(PRIORITY_SET);
    }

    WeaponModDrill damage(float v) {
        damage = v;
        return this;
    }

    WeaponModDrill reach(double v) {
        reach = v;
        return this;
    }

    WeaponModDrill dt(float v) {
        dt = v;
        return this;
    }

    WeaponModDrill pierce(float v) {
        pierce = v;
        return this;
    }

    WeaponModDrill aoe(int v) {
        aoe = v;
        return this;
    }

    WeaponModDrill harvest(int v) {
        harvest = v;
        return this;
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key.equals(Receiver.F_BASEDAMAGE)) return cast((Float) base * damage, base);
        if (key.equals(XFactoryDrill.D_REACH)) return cast((Double) base * reach, base);
        if (key.equals(XFactoryDrill.F_DTNEG) && dt >= 0) return cast(dt, base);
        if (key.equals(XFactoryDrill.F_PIERCE) && pierce >= 0) return cast(pierce, base);
        if (key.equals(XFactoryDrill.I_AOE) && aoe >= 0) return cast(aoe, base);
        if (key.equals(XFactoryDrill.I_HARVEST) && harvest >= 0) return cast(harvest, base);
        return base;
    }
}

final class WeaponModDrillFortune extends WeaponModBase {
    private final int addFortune;

    WeaponModDrillFortune(int id, String slot, int fortune) {
        super(id, slot);
        setPriority(PRIORITY_ADDITIVE);
        addFortune = fortune;
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
                    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(old);
                    if (result > 0) mutable.set(fortune, result);
                    else mutable.removeIf(holder -> holder.is(Enchantments.FORTUNE));
                    return mutable.toImmutable();
                });
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return base;
    }

    @Override
    public void onInstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) {
        adjustFortune(level, gun, addFortune);
    }

    @Override
    public void onUninstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) {
        adjustFortune(level, gun, -addFortune);
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        throw new IllegalStateException("Drill fortune upgrades require a ServerLevel");
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        throw new IllegalStateException("Drill fortune upgrades require a ServerLevel");
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
    private IMagazine<?> mag;
    private int delay;

    WeaponModEngine(int id) {
        super(id, "ENGINE");
        setPriority(PRIORITY_SET);
    }

    WeaponModEngine mag(IMagazine<?> v) {
        mag = v;
        return this;
    }

    WeaponModEngine delay(int v) {
        delay = v;
        return this;
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.O_MAGAZINE && mag != null) return cast(mag, base);
        if (key == Receiver.I_DELAYAFTERFIRE) return cast(delay, base);
        return base;
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }
}

final class WeaponModStackMag extends WeaponModBase {
    static final MagazineSingleReload DUMMY_SINGLE = new MagazineSingleReload(0, 0);
    static final MagazineFullReload DUMMY_FULL = new MagazineFullReload(0, 0);

    WeaponModStackMag(int id) {
        super(id, "MAG");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.O_MAGAZINE) {
            if (base instanceof MagazineSingleReload original) {
                DUMMY_SINGLE.acceptedBullets = original.acceptedBullets;
                DUMMY_SINGLE.capacity = original.capacity * 3 / 2;
                DUMMY_SINGLE.index = original.index;
                return cast(DUMMY_SINGLE, base);
            }
            if (base instanceof MagazineFullReload original) {
                DUMMY_FULL.acceptedBullets = original.acceptedBullets;
                DUMMY_FULL.capacity = original.capacity * 3 / 2;
                DUMMY_FULL.index = original.index;
                return cast(DUMMY_FULL, base);
            }
        }
        return base;
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }
}

final class WeaponModPanzerschreckSawedOff extends WeaponModBase {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PANZERSCHRECK_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.EQUIP)
                    return new BusAnimation()
                            .addBus(
                                    "EQUIP",
                                    new BusAnimationSequence()
                                            .addPos(60, 0, 0, 0)
                                            .addPos(0, 0, 0, 250, IType.SIN_DOWN));
                return XFactoryRocket.LAMBDA_PANZERSCHRECK_ANIMS.apply(stack, type);
            };
    static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE =
            (stack, ctx) -> {
                Lego.LAMBDA_STANDARD_FIRE.accept(stack, ctx);
                if (ctx.entity() != null) {
                    HbmLivingProps.getData(ctx.entity()).fire += 100;
                    EntityDamageUtil.attackEntityFromNT(
                            ctx.entity(),
                            BulletConfig.getDamage(ctx.entity(), ctx.entity(), DamageClass.FIRE),
                            4F,
                            true,
                            false,
                            0F,
                            0F,
                            0F);
                }
            };

    WeaponModPanzerschreckSawedOff(int id) {
        super(id, "SHIELD");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.I_DRAWDURATION) return cast(5, base);
        if (key == Receiver.CON_ONFIRE) return cast(LAMBDA_FIRE, base);
        if (key == GunConfig.FUN_ANIMNATIONS) return cast(LAMBDA_PANZERSCHRECK_ANIMS, base);
        return base;
    }
}

abstract class WeaponModBayonet extends WeaponModBase {
    WeaponModBayonet(int id) {
        super(id, "BAYONET");
    }

    private static void stab(Player player, LivingEntity entity) {
        HitResult hit = EntityDamageUtil.getMouseOver(player, 3D);
        if (hit instanceof EntityHitResult entityHit) {
            Entity victim = entityHit.getEntity();
            victim.hurtServer(
                    (ServerLevel) entity.level(), player.damageSources().playerAttack(player), 15F);
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(2D, 1D, 2D));
            entity.level()
                    .playSound(
                            null,
                            victim.getX(),
                            victim.getY(),
                            victim.getZ(),
                            ModSounds.GUN_STAB.get(),
                            SoundSource.PLAYERS,
                            1F,
                            0.9F + entity.getRandom().nextFloat() * 0.2F);
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockState state = entity.level().getBlockState(blockHit.getBlockPos());
            entity.level()
                    .playSound(
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

    abstract BiConsumer<ItemStack, LambdaContext> normalOrchestra();

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.FUN_ANIMNATIONS) return cast(animation(), base);
        if (key == GunConfig.I_INSPECTDURATION) return cast(30, base);
        if (key == GunConfig.CON_ONPRESSSECONDARY) return cast(XFactory44.SMACK_A_FUCKER, base);
        if (key == GunConfig.CON_ORCHESTRA) return cast(orchestra(), base);
        if (key == GunConfig.I_INSPECTCANCEL) return cast(false, base);
        return base;
    }

    private BiConsumer<ItemStack, LambdaContext> orchestra() {
        return (stack, ctx) -> {
            LivingEntity entity = ctx.entity();
            if (entity.level().isClientSide()) return;
            if (ItemGunBaseNT.getLastAnim(stack, ctx.configIndex()) == GunAnimation.INSPECT) {
                if (ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex()) == 15
                        && ctx.getPlayer() != null) stab(ctx.getPlayer(), entity);
                return;
            }
            normalOrchestra().accept(stack, ctx);
        };
    }
}

final class WeaponModCarbineBayonet extends WeaponModBayonet {
    static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CARBINE_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.INSPECT) return stabAnimation();
                return XFactory762mm.LAMBDA_CARBINE_ANIMS.apply(stack, type);
            };

    WeaponModCarbineBayonet(int id) {
        super(id);
    }

    static BusAnimation stabAnimation() {
        return new BusAnimation()
                .addBus(
                        "STAB",
                        new BusAnimationSequence()
                                .addPos(0, 1, -2, 250, IType.SIN_DOWN)
                                .hold(250)
                                .addPos(0, 1, 5, 250, IType.SIN_UP)
                                .hold(250)
                                .addPos(0, 0, 0, 500, IType.SIN_FULL));
    }

    @Override
    BiConsumer<ItemStack, LambdaContext> normalOrchestra() {
        return Orchestras.ORCHESTRA_CARBINE;
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

    WeaponModMASBayonet(int id) {
        super(id);
    }

    @Override
    BiConsumer<ItemStack, LambdaContext> normalOrchestra() {
        return Orchestras.ORCHESTRA_MAS36;
    }

    @Override
    BiFunction<ItemStack, GunAnimation, BusAnimation> animation() {
        return LAMBDA_MAS36_ANIMS;
    }
}

final class WeaponModSilencer extends WeaponModBase {
    WeaponModSilencer(int id) {
        super(id, "SILENCER");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.S_FIRESOUND) {
            Supplier<SoundEvent> sound =
                    gun.is(ModItems.GUN_AMAT.get())
                            ? ModSounds.GUN_AMAT_SILENCER::get
                            : ModSounds.GUN_RIFLE_SILENCER::get;
            return cast(sound, base);
        }
        return base;
    }
}
