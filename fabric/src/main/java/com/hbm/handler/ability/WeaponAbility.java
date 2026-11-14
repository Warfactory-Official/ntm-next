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

package com.hbm.handler.ability;

import com.hbm.blockentity.BobbleBlockEntity;
import com.hbm.blocks.generic.BobbleBlock;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.CloudEffect;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

public final class WeaponAbility extends BaseAbility {

    private static final int BASE_SORT_ORDER = 201;

    private static final float[] RADS = {15.0F, 50.0F, 500.0F};
    private static final float[] DRAIN = {2.0F, 3.0F, 5.0F, 10.0F, 50.0F};
    private static final int[] STUN_SECONDS = {2, 3, 5, 10, 15};
    private static final int[] PHOSPHORUS_SECONDS = {60, 90};
    private static final int[] FIRE_SECONDS = {5, 10};
    private static final int[] HEALTH_PER_CHARGE = {15, 10};
    private static final int AMPLIFIER = 4;
    private static final int MAX_CHARGES = 250;
    private static final Vec3 CHARGE_OFFSET = new Vec3(0.0D, 1.0D, 0.0D);

    public static final WeaponAbility RADIOACTIVE_BLADE =
            new WeaponAbility(
                    201,
                    "weapon.ability.radiation",
                    levelText(RADS),
                    (_, level, _, victim) ->
                            ContaminationUtil.contaminate(
                                    victim,
                                    ContaminationUtil.HazardType.RADIATION,
                                    ContaminationUtil.ContaminationType.CREATIVE,
                                    RADS[level]));
    public static final WeaponAbility VAMPIRE =
            new WeaponAbility(
                    202,
                    "weapon.ability.vampire",
                    levelText(DRAIN),
                    (world, level, attacker, victim) ->
                            drain(world, attacker, victim, DRAIN[level]));
    public static final WeaponAbility STUNNING =
            new WeaponAbility(
                    203,
                    "weapon.ability.stun",
                    levelText(STUN_SECONDS),
                    (_, level, _, victim) ->
                            stun(victim, STUN_SECONDS[level] * 20));
    public static final WeaponAbility PHOSPHORUS_TIP =
            new WeaponAbility(
                    204,
                    "weapon.ability.phosphorus",
                    levelText(PHOSPHORUS_SECONDS),
                    (_, level, _, victim) ->
                            victim.addEffect(
                                    new MobEffectInstance(
                                            HbmPotion.phosphorus(),
                                            PHOSPHORUS_SECONDS[level] * 20,
                                            AMPLIFIER)));
    public static final WeaponAbility FLAMING =
            new WeaponAbility(
                    206,
                    "weapon.ability.fire",
                    levelText(FIRE_SECONDS),
                    (_, level, _, victim) ->
                            victim.igniteForSeconds(FIRE_SECONDS[level]));
    public static final WeaponAbility PAINSAW =
            new WeaponAbility(
                    207,
                    "weapon.ability.chainsaw",
                    ratioText(HEALTH_PER_CHARGE),
                    (world, level, _, victim) ->
                            painsaw(world, victim, HEALTH_PER_CHARGE[level]));
    public static final WeaponAbility DECAPITATOR =
            new WeaponAbility(
                    208,
                    "weapon.ability.beheader",
                    oneLevel(),
                    (world, _, _, victim) ->
                            behead(world, victim));
    public static final WeaponAbility COLLECTORS_LUCK =
            new WeaponAbility(
                    209,
                    "weapon.ability.bobble",
                    oneLevel(),
                    (world, _, _, victim) ->
                            bobble(world, victim));

    private final Strike strike;

    private WeaponAbility(int sortOrder, String translationKey, String[] levelText, Strike strike) {
        super(translationKey, sortOrder, levelText, null);
        this.strike = strike;
    }

    @Override
    int index() {
        return sortOrder() - BASE_SORT_ORDER;
    }

    public void hit(ServerLevel world, int level, Player attacker, LivingEntity victim) {
        strike.hit(world, level, attacker, victim);
    }

    private static void drain(
            ServerLevel world, Player attacker, LivingEntity victim, float amount) {
        if (victim.isDeadOrDying()) return;
        if (victim.hurtServer(
                world,
                world.damageSources().source(ModDamageTypes.HEALTH_DRAIN, attacker),
                amount)) {
            attacker.heal(amount);
        }
    }

    private static void stun(LivingEntity victim, int ticks) {
        victim.addEffect(CloudEffect.local(MobEffects.SLOWNESS, ticks, AMPLIFIER));
        victim.addEffect(CloudEffect.local(MobEffects.WEAKNESS, ticks, AMPLIFIER));
    }

    private static void painsaw(ServerLevel world, LivingEntity victim, int healthPerCharge) {
        if (!victim.isDeadOrDying()) return;
        int charges = Math.min(Mth.ceil(victim.getMaxHealth() / healthPerCharge), MAX_CHARGES);
        Vec3 corpse = victim.position();
        for (int i = 0; i < charges; i++) {
            victim.spawnAtLocation(world, new ItemStack(ModItems.NITRA_SMALL.get()), CHARGE_OFFSET);
            ExperienceOrb.award(world, corpse, 1);
        }
        ConfettiUtil.gib(victim);
        EntitySounds.play(
                victim,
                victim.getX(),
                victim.getY() + victim.getBbHeight() / 2.0D,
                victim.getZ(),
                ModSounds.CHAINSAW.get(),
                SoundSource.PLAYERS,
                0.5F,
                1.0F);
    }

    private static void behead(ServerLevel world, LivingEntity victim) {
        if (!victim.isDeadOrDying()) return;
        switch (victim) {
            case WitherSkeleton _ -> {
                if (world.getRandom().nextInt(20) == 0)
                    drop(world, victim, Items.WITHER_SKELETON_SKULL, 1);
                else drop(world, victim, Items.COAL, 3);
            }
            case Skeleton _ -> drop(world, victim, Items.SKELETON_SKULL, 1);
            case Zombie _ -> drop(world, victim, Items.ZOMBIE_HEAD, 1);
            case Creeper _ -> drop(world, victim, Items.CREEPER_HEAD, 1);
            case MagmaCube _ -> drop(world, victim, Items.MAGMA_CREAM, 3);
            case Slime _ -> drop(world, victim, Items.SLIME_BALL, 3);
            case Player player -> {
                var head = new ItemStack(Items.PLAYER_HEAD);
                head.set(
                        DataComponents.PROFILE,
                        ResolvableProfile.createResolved(player.getGameProfile()));
                victim.spawnAtLocation(world, head);
            }
            default -> {
                drop(world, victim, Items.ROTTEN_FLESH, 3);
                drop(world, victim, Items.BONE, 2);
            }
        }
    }

    private static void drop(ServerLevel world, LivingEntity victim, Item item, int count) {
        victim.spawnAtLocation(world, new ItemStack(item, count));
    }

    private static void bobble(ServerLevel world, LivingEntity victim) {
        if (!(victim instanceof Monster) || !victim.isDeadOrDying()) return;
        var random = world.getRandom();
        if (random.nextInt(victim.getMaxHealth() > 20.0F ? 750 : 1000) != 0) return;
        var kinds = BobbleBlock.TYPES.values();
        victim.spawnAtLocation(
                world, BobbleBlockEntity.stackOf(kinds.get(1 + random.nextInt(kinds.size() - 1))));
    }

    @FunctionalInterface
    private interface Strike {

        void hit(ServerLevel world, int level, Player attacker, LivingEntity victim);
    }
}
