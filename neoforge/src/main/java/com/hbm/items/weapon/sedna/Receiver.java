/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mods.GunAttribute;
import com.hbm.items.weapon.sedna.mods.InstalledWeaponMods;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class Receiver {

    public static final GunAttribute.OfFloat BASE_DAMAGE = new GunAttribute.OfFloat();
    public static final GunAttribute.OfInt DELAY_AFTER_FIRE = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt DELAY_AFTER_DRY_FIRE = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt ROUNDS_PER_CYCLE = new GunAttribute.OfInt();
    public static final GunAttribute.OfFloat SPLIT_PROJECTILES = new GunAttribute.OfFloat();
    public static final GunAttribute.OfFloat SPREAD_INNATE = new GunAttribute.OfFloat();
    public static final GunAttribute.OfFloat SPREAD_AMMO = new GunAttribute.OfFloat();
    public static final GunAttribute.OfFloat SPREAD_HIPFIRE = new GunAttribute.OfFloat();
    public static final GunAttribute.OfFloat SPREAD_DURABILITY = new GunAttribute.OfFloat();
    public static final GunAttribute.OfBool REFIRE_ON_HOLD = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool REFIRE_AFTER_DRY = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool DOES_DRY_FIRE = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool DOES_DRY_FIRE_AFTER_AUTO = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool EJECT_ON_FIRE = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool RELOAD_ON_EMPTY = new GunAttribute.OfBool();
    public static final GunAttribute.OfInt RELOAD_BEGIN_DURATION = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt RELOAD_CYCLE_DURATION = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt RELOAD_END_DURATION = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt RELOAD_COCK_ON_EMPTY_PRE = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt RELOAD_COCK_ON_EMPTY_POST = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt JAM_DURATION = new GunAttribute.OfInt();
    public static final GunAttribute.Ref<Supplier<SoundEvent>> FIRE_SOUND =
            new GunAttribute.Ref<>();
    public static final GunAttribute.OfFloat FIRE_VOLUME = new GunAttribute.OfFloat();
    public static final GunAttribute.OfFloat FIRE_PITCH = new GunAttribute.OfFloat();
    public static final GunAttribute.Ref<Magazine<?>> MAGAZINE = new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<Vec3> PROJECTILE_OFFSET = new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<Vec3> PROJECTILE_OFFSET_SCOPED = new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiFunction<ItemStack, LambdaContext, Boolean>> CAN_FIRE =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> ON_FIRE =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> ON_RECOIL =
            new GunAttribute.Ref<>();

    private final int config;
    private final Values declared;

    private Receiver(int config, Values declared) {
        this.config = config;
        this.declared = declared;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float getBaseDamage(ItemStack stack) {
        return resolved(stack).baseDamage();
    }

    public int getDelayAfterFire(ItemStack stack) {
        return resolved(stack).delayAfterFire();
    }

    public int getDelayAfterDryFire(ItemStack stack) {
        return resolved(stack).delayAfterDryFire();
    }

    public int getRoundsPerCycle(ItemStack stack) {
        return resolved(stack).roundsPerCycle();
    }

    public float getSplitProjectiles(ItemStack stack) {
        return resolved(stack).splitProjectiles();
    }

    public float getInnateSpread(ItemStack stack) {
        return resolved(stack).spreadInnate();
    }

    public float getAmmoSpread(ItemStack stack) {
        return resolved(stack).spreadAmmo();
    }

    public float getHipfireSpread(ItemStack stack) {
        return resolved(stack).spreadHipfire();
    }

    public float getDurabilitySpread(ItemStack stack) {
        return resolved(stack).spreadDurability();
    }

    public boolean getRefireOnHold(ItemStack stack) {
        return resolved(stack).refireOnHold();
    }

    public boolean getRefireAfterDry(ItemStack stack) {
        return resolved(stack).refireAfterDry();
    }

    public boolean getDoesDryFire(ItemStack stack) {
        return resolved(stack).doesDryFire();
    }

    public boolean getDoesDryFireAfterAuto(ItemStack stack) {
        return resolved(stack).doesDryFireAfterAuto();
    }

    public boolean getEjectOnFire(ItemStack stack) {
        return resolved(stack).ejectOnFire();
    }

    public boolean getReloadOnEmpty(ItemStack stack) {
        return resolved(stack).reloadOnEmpty();
    }

    public int getReloadBeginDuration(ItemStack stack) {
        return resolved(stack).reloadBegin();
    }

    public int getReloadCycleDuration(ItemStack stack) {
        return resolved(stack).reloadCycle();
    }

    public int getReloadEndDuration(ItemStack stack) {
        return resolved(stack).reloadEnd();
    }

    public int getReloadCockOnEmptyPre(ItemStack stack) {
        return resolved(stack).cockOnEmptyPre();
    }

    public int getReloadCockOnEmptyPost(ItemStack stack) {
        return resolved(stack).cockOnEmptyPost();
    }

    public int getJamDuration(ItemStack stack) {
        return resolved(stack).jamDuration();
    }

    public Supplier<SoundEvent> getFireSound(ItemStack stack) {
        return resolved(stack).fireSound();
    }

    public float getFireVolume(ItemStack stack) {
        return resolved(stack).fireVolume();
    }

    public float getFirePitch(ItemStack stack) {
        return resolved(stack).firePitch();
    }

    public Magazine getMagazine(ItemStack stack) {
        return resolved(stack).magazine();
    }

    public Vec3 getProjectileOffset(ItemStack stack) {
        return resolved(stack).offset();
    }

    public Vec3 getProjectileOffsetScoped(ItemStack stack) {
        return resolved(stack).offsetScoped();
    }

    public BiFunction<ItemStack, LambdaContext, Boolean> getCanFire(ItemStack stack) {
        return resolved(stack).canFire();
    }

    public BiConsumer<ItemStack, LambdaContext> getOnFire(ItemStack stack) {
        return resolved(stack).onFire();
    }

    public BiConsumer<ItemStack, LambdaContext> getRecoil(ItemStack stack) {
        return resolved(stack).onRecoil();
    }

    private Values resolved(ItemStack stack) {
        return InstalledWeaponMods.of(stack, config).resolved(this, Receiver::resolve);
    }

    private static Values resolve(Receiver spec, InstalledWeaponMods mods) {
        var d = spec.declared;
        return new Values(
                BASE_DAMAGE.resolve(d.baseDamage, mods),
                DELAY_AFTER_FIRE.resolve(d.delayAfterFire, mods),
                DELAY_AFTER_DRY_FIRE.resolve(d.delayAfterDryFire, mods),
                ROUNDS_PER_CYCLE.resolve(d.roundsPerCycle, mods),
                SPLIT_PROJECTILES.resolve(d.splitProjectiles, mods),
                SPREAD_INNATE.resolve(d.spreadInnate, mods),
                SPREAD_AMMO.resolve(d.spreadAmmo, mods),
                SPREAD_HIPFIRE.resolve(d.spreadHipfire, mods),
                SPREAD_DURABILITY.resolve(d.spreadDurability, mods),
                REFIRE_ON_HOLD.resolve(d.refireOnHold, mods),
                REFIRE_AFTER_DRY.resolve(d.refireAfterDry, mods),
                DOES_DRY_FIRE.resolve(d.doesDryFire, mods),
                DOES_DRY_FIRE_AFTER_AUTO.resolve(d.doesDryFireAfterAuto, mods),
                EJECT_ON_FIRE.resolve(d.ejectOnFire, mods),
                RELOAD_ON_EMPTY.resolve(d.reloadOnEmpty, mods),
                RELOAD_BEGIN_DURATION.resolve(d.reloadBegin, mods),
                RELOAD_CYCLE_DURATION.resolve(d.reloadCycle, mods),
                RELOAD_END_DURATION.resolve(d.reloadEnd, mods),
                RELOAD_COCK_ON_EMPTY_PRE.resolve(d.cockOnEmptyPre, mods),
                RELOAD_COCK_ON_EMPTY_POST.resolve(d.cockOnEmptyPost, mods),
                JAM_DURATION.resolve(d.jamDuration, mods),
                FIRE_SOUND.resolve(d.fireSound, mods),
                FIRE_VOLUME.resolve(d.fireVolume, mods),
                FIRE_PITCH.resolve(d.firePitch, mods),
                MAGAZINE.resolve(d.magazine, mods),
                PROJECTILE_OFFSET.resolve(d.offset, mods),
                PROJECTILE_OFFSET_SCOPED.resolve(d.offsetScoped, mods),
                CAN_FIRE.resolve(d.canFire, mods),
                ON_FIRE.resolve(d.onFire, mods),
                ON_RECOIL.resolve(d.onRecoil, mods));
    }

    private record Values(
            float baseDamage,
            int delayAfterFire,
            int delayAfterDryFire,
            int roundsPerCycle,
            float splitProjectiles,
            float spreadInnate,
            float spreadAmmo,
            float spreadHipfire,
            float spreadDurability,
            boolean refireOnHold,
            boolean refireAfterDry,
            boolean doesDryFire,
            boolean doesDryFireAfterAuto,
            boolean ejectOnFire,
            boolean reloadOnEmpty,
            int reloadBegin,
            int reloadCycle,
            int reloadEnd,
            int cockOnEmptyPre,
            int cockOnEmptyPost,
            int jamDuration,
            Supplier<SoundEvent> fireSound,
            float fireVolume,
            float firePitch,
            Magazine<?> magazine,
            Vec3 offset,
            Vec3 offsetScoped,
            BiFunction<ItemStack, LambdaContext, Boolean> canFire,
            BiConsumer<ItemStack, LambdaContext> onFire,
            BiConsumer<ItemStack, LambdaContext> onRecoil) {}

    public static final class Builder {
        private float baseDamage;
        private int delayAfterFire;
        private int delayAfterDryFire;
        private int roundsPerCycle = 1;
        private float splitProjectiles = 1F;
        private float spreadInnate;
        private float spreadAmmo = 1F;
        private float spreadHipfire = 0.025F;
        private float spreadDurability = 0.125F;
        private boolean refireOnHold;
        private boolean refireAfterDry;
        private boolean doesDryFire = true;
        private boolean doesDryFireAfterAuto;
        private boolean ejectOnFire = true;
        private boolean reloadOnEmpty;
        private int reloadBegin;
        private int reloadCycle;
        private int reloadEnd;
        private int cockOnEmptyPre;
        private int cockOnEmptyPost;
        private int jamDuration;
        private Supplier<SoundEvent> fireSound;
        private float fireVolume = 1F;
        private float firePitch = 1F;
        private Magazine<?> magazine;
        private Vec3 offset = Vec3.ZERO;
        private Vec3 offsetScoped = Vec3.ZERO;
        private BiFunction<ItemStack, LambdaContext, Boolean> canFire;
        private BiConsumer<ItemStack, LambdaContext> onFire;
        private BiConsumer<ItemStack, LambdaContext> onRecoil;

        private Builder() {}

        public Builder dmg(float damage) {
            this.baseDamage = damage;
            return this;
        }

        public Builder delay(int ticks) {
            this.delayAfterFire = ticks;
            this.delayAfterDryFire = ticks;
            return this;
        }

        public Builder dry(int ticks) {
            this.delayAfterDryFire = ticks;
            return this;
        }

        public Builder rounds(int rounds) {
            this.roundsPerCycle = rounds;
            return this;
        }

        public Builder split(float projectiles) {
            this.splitProjectiles = projectiles;
            return this;
        }

        public Builder spread(float spread) {
            this.spreadInnate = spread;
            return this;
        }

        public Builder spreadAmmo(float multiplier) {
            this.spreadAmmo = multiplier;
            return this;
        }

        public Builder spreadHipfire(float spread) {
            this.spreadHipfire = spread;
            return this;
        }

        public Builder spreadDurability(float spread) {
            this.spreadDurability = spread;
            return this;
        }

        public Builder auto(boolean refire) {
            this.refireOnHold = refire;
            return this;
        }

        public Builder autoAfterDry(boolean refire) {
            this.refireAfterDry = refire;
            return this;
        }

        public Builder dryfire(boolean dryFire) {
            this.doesDryFire = dryFire;
            return this;
        }

        public Builder dryfireAfterAuto(boolean dryFire) {
            this.doesDryFireAfterAuto = dryFire;
            return this;
        }

        public Builder ejectOnFire(boolean eject) {
            this.ejectOnFire = eject;
            return this;
        }

        public Builder reloadOnEmpty(boolean reload) {
            this.reloadOnEmpty = reload;
            return this;
        }

        public Builder mag(Magazine<?> magazine) {
            this.magazine = magazine;
            return this;
        }

        public Builder offset(double forward, double up, double side) {
            this.offset = new Vec3(forward, up, side);
            this.offsetScoped = new Vec3(forward, up, 0);
            return this;
        }

        public Builder offsetScoped(double forward, double up, double side) {
            this.offsetScoped = new Vec3(forward, up, side);
            return this;
        }

        public Builder jam(int ticks) {
            this.jamDuration = ticks;
            return this;
        }

        public Builder reload(int ticks) {
            return reload(0, ticks, ticks, 0, 0);
        }

        public Builder reload(int begin, int cycle, int end, int cockPost) {
            return reload(0, begin, cycle, end, cockPost);
        }

        public Builder reload(int cockPre, int begin, int cycle, int end, int cockPost) {
            this.cockOnEmptyPre = cockPre;
            this.reloadBegin = begin;
            this.reloadCycle = cycle;
            this.reloadEnd = end;
            this.cockOnEmptyPost = cockPost;
            return this;
        }

        public Builder canFire(BiFunction<ItemStack, LambdaContext, Boolean> hook) {
            this.canFire = hook;
            return this;
        }

        public Builder fire(BiConsumer<ItemStack, LambdaContext> hook) {
            this.onFire = hook;
            return this;
        }

        public Builder recoil(BiConsumer<ItemStack, LambdaContext> hook) {
            this.onRecoil = hook;
            return this;
        }

        public Builder sound(Supplier<SoundEvent> sound, float volume, float pitch) {
            this.fireSound = sound;
            this.fireVolume = volume;
            this.firePitch = pitch;
            return this;
        }

        public Builder setupStandardFire() {
            return canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE);
        }

        public Builder setupLockonFire() {
            return canFire(Lego.LAMBDA_LOCKON_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE);
        }

        Receiver build(int config) {
            return new Receiver(
                    config,
                    new Values(
                            baseDamage,
                            delayAfterFire,
                            delayAfterDryFire,
                            roundsPerCycle,
                            splitProjectiles,
                            spreadInnate,
                            spreadAmmo,
                            spreadHipfire,
                            spreadDurability,
                            refireOnHold,
                            refireAfterDry,
                            doesDryFire,
                            doesDryFireAfterAuto,
                            ejectOnFire,
                            reloadOnEmpty,
                            reloadBegin,
                            reloadCycle,
                            reloadEnd,
                            cockOnEmptyPre,
                            cockOnEmptyPost,
                            jamDuration,
                            fireSound,
                            fireVolume,
                            firePitch,
                            magazine,
                            offset,
                            offsetScoped,
                            canFire,
                            onFire,
                            onRecoil));
        }
    }
}
