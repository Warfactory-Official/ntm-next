// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.ability;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityBobble;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public enum WeaponAbility implements BaseAbility {
    RADIATION("weapon.ability.radiation", 201) {
        private static final float[] RADS = {15F, 50F, 500F};

        @Override
        public int levels() {
            return RADS.length;
        }

        @Override
        public String extension(int level) {
            return " (" + RADS[level] + ")";
        }

        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            ContaminationUtil.contaminate(
                    victim, HazardType.RADIATION, ContaminationType.CREATIVE, RADS[abilityLevel]);
        }
    },
    VAMPIRE("weapon.ability.vampire", 202) {
        private static final float[] AMOUNTS = {2F, 3F, 5F, 10F, 50F};

        @Override
        public int levels() {
            return AMOUNTS.length;
        }

        @Override
        public String extension(int level) {
            return " (" + AMOUNTS[level] + ")";
        }

        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            float amount = AMOUNTS[abilityLevel];
            if (victim.getHealth() <= 0.0F) return;

            victim.setHealth(victim.getHealth() - amount);

            if (victim.getHealth() <= 0.0F) victim.die(level.damageSources().magic());
            attacker.heal(amount);
        }
    },
    STUN("weapon.ability.stun", 203) {
        private static final int[] DURATIONS = {2, 3, 5, 10, 15};

        @Override
        public int levels() {
            return DURATIONS.length;
        }

        @Override
        public String extension(int level) {
            return " (" + DURATIONS[level] + ")";
        }

        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            int duration = DURATIONS[abilityLevel] * 20;
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 4));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 4));
        }
    },
    PHOSPHORUS("weapon.ability.phosphorus", 204) {
        private static final int[] DURATIONS = {60, 90};

        @Override
        public int levels() {
            return DURATIONS.length;
        }

        @Override
        public String extension(int level) {
            return " (" + DURATIONS[level] + ")";
        }

        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            victim.addEffect(
                    new MobEffectInstance(HbmPotion.phosphorus(), DURATIONS[abilityLevel] * 20, 4));
        }
    },
    FIRE("weapon.ability.fire", 206) {
        private static final int[] DURATIONS = {5, 10};

        @Override
        public int levels() {
            return DURATIONS.length;
        }

        @Override
        public String extension(int level) {
            return " (" + DURATIONS[level] + ")";
        }

        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            victim.igniteForSeconds(DURATIONS[abilityLevel]);
        }
    },
    CHAINSAW("weapon.ability.chainsaw", 207) {
        private static final int[] DIVIDERS = {15, 10};

        private static final int MAX_NITRA = 250;

        @Override
        public int levels() {
            return DIVIDERS.length;
        }

        @Override
        public String extension(int level) {
            return " (1:" + DIVIDERS[level] + ")";
        }

        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            if (victim.getHealth() > 0.0F) return;

            int count =
                    Math.min(Mth.ceil(victim.getMaxHealth() / DIVIDERS[abilityLevel]), MAX_NITRA);
            for (int i = 0; i < count; i++) {
                victim.spawnAtLocation(level, new ItemStack(ModItems.NITRA_SMALL));
                ExperienceOrb.award(level, victim.position(), 1);
            }

            ConfettiUtil.gib(victim);
            level.playSound(
                    null,
                    victim.getX(),
                    victim.getY() + victim.getBbHeight() * 0.5,
                    victim.getZ(),
                    ModSounds.CHAINSAW.get(),
                    SoundSource.PLAYERS,
                    0.5F,
                    1.0F);
        }
    },
    BEHEADER("weapon.ability.beheader", 208) {
        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            if (victim.getHealth() > 0.0F) return;

            if (victim instanceof WitherSkeleton) {
                if (level.getRandom().nextInt(20) == 0) {
                    drop(victim, level, new ItemStack(Items.WITHER_SKELETON_SKULL));
                } else {
                    drop(victim, level, new ItemStack(Items.COAL, 3));
                }
            } else if (victim instanceof Skeleton) {
                drop(victim, level, new ItemStack(Items.SKELETON_SKULL));
            } else if (victim instanceof Zombie) {
                drop(victim, level, new ItemStack(Items.ZOMBIE_HEAD));
            } else if (victim instanceof Creeper) {
                drop(victim, level, new ItemStack(Items.CREEPER_HEAD));
            } else if (victim instanceof MagmaCube) {
                drop(victim, level, new ItemStack(Items.MAGMA_CREAM, 3));
            } else if (victim instanceof Slime) {
                drop(victim, level, new ItemStack(Items.SLIME_BALL, 3));
            } else if (victim instanceof Player player) {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(
                        DataComponents.PROFILE,
                        ResolvableProfile.createResolved(player.getGameProfile()));
                drop(victim, level, head);
            } else {
                drop(victim, level, new ItemStack(Items.ROTTEN_FLESH, 3));
                drop(victim, level, new ItemStack(Items.BONE, 2));
            }
        }
    },
    BOBBLE("weapon.ability.bobble", 209) {
        @Override
        public void onHit(
                ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim) {
            if (victim.getHealth() > 0.0F || !(victim instanceof Monster monster)) return;

            int chance = monster.getMaxHealth() > 20.0F ? 750 : 1_000;
            RandomSource random = level.getRandom();
            if (random.nextInt(chance) != 0) return;

            BobbleType[] types = BobbleType.values();
            ItemStack bobble =
                    BlockEntityBobble.stackOf(types[random.nextInt(types.length - 1) + 1]);
            drop(monster, level, bobble);
        }
    };

    public static final WeaponAbility[] VALUES = values();

    private final String translationKey;
    private final int sortOrder;

    WeaponAbility(String translationKey, int sortOrder) {
        this.translationKey = translationKey;
        this.sortOrder = sortOrder;
    }

    private static void drop(LivingEntity victim, ServerLevel level, ItemStack stack) {
        victim.spawnAtLocation(level, stack, 0.0F);
    }

    public abstract void onHit(
            ServerLevel level, int abilityLevel, Player attacker, LivingEntity victim);

    @Override
    public String translationKey() {
        return translationKey;
    }

    @Override
    public int sortOrder() {
        return sortOrder;
    }
}
