// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.potion;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.data.ItemData;
import com.hbm.entity.mob.EntityCreeperTainted;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HbmPotion {

    public static RegistryHandle<MobEffect> potionsickness;
    public static RegistryHandle<MobEffect> radiation;
    public static RegistryHandle<MobEffect> radaway;

    public static RegistryHandle<MobEffect> death;
    public static RegistryHandle<MobEffect> radx;
    public static RegistryHandle<MobEffect> stability;
    public static RegistryHandle<MobEffect> lead;
    public static RegistryHandle<MobEffect> taint;

    public static RegistryHandle<MobEffect> mutation;
    public static RegistryHandle<MobEffect> phosphorus;

    public static RegistryHandle<MobEffect> bang;

    private static Holder<MobEffect> potionsicknessHolder;
    private static Holder<MobEffect> radiationHolder;
    private static Holder<MobEffect> radawayHolder;
    private static Holder<MobEffect> deathHolder;
    private static Holder<MobEffect> radxHolder;
    private static Holder<MobEffect> stabilityHolder;
    private static Holder<MobEffect> leadHolder;
    private static Holder<MobEffect> taintHolder;
    private static Holder<MobEffect> mutationHolder;
    private static Holder<MobEffect> phosphorusHolder;
    private static Holder<MobEffect> bangHolder;

    private HbmPotion() {}

    public static void register(IRegistrar r) {
        potionsickness =
                r.registerMobEffect(
                        "potionsickness",
                        () -> new HbmMobEffect(MobEffectCategory.NEUTRAL, 0xff8080));
        radiation = r.registerMobEffect("radiation", RadiationEffect::new);
        radaway = r.registerMobEffect("radaway", RadawayEffect::new);
        death =
                r.registerMobEffect(
                        "death", () -> new HbmMobEffect(MobEffectCategory.NEUTRAL, 0x111111));
        radx =
                r.registerMobEffect(
                        "radx", () -> new HbmMobEffect(MobEffectCategory.BENEFICIAL, 0xBB4B00));
        stability =
                r.registerMobEffect(
                        "stability",
                        () -> new HbmMobEffect(MobEffectCategory.BENEFICIAL, 0xD0D0D0));
        lead = r.registerMobEffect("lead", LeadEffect::new);
        taint = r.registerMobEffect("taint", TaintEffect::new);
        mutation =
                r.registerMobEffect(
                        "mutation", () -> new HbmMobEffect(MobEffectCategory.NEUTRAL, 0x800080));
        phosphorus = r.registerMobEffect("phosphorus", PhosphorusEffect::new);
        bang = r.registerMobEffect("bang", BangEffect::new);
    }

    public static Holder<MobEffect> potionsickness() {
        if (potionsicknessHolder == null)
            potionsicknessHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(potionsickness.get());
        return potionsicknessHolder;
    }

    public static Holder<MobEffect> radiation() {
        if (radiationHolder == null)
            radiationHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(radiation.get());
        return radiationHolder;
    }

    public static Holder<MobEffect> radaway() {
        if (radawayHolder == null)
            radawayHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(radaway.get());
        return radawayHolder;
    }

    public static Holder<MobEffect> death() {
        if (deathHolder == null)
            deathHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(death.get());
        return deathHolder;
    }

    public static Holder<MobEffect> radx() {
        if (radxHolder == null) radxHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(radx.get());
        return radxHolder;
    }

    public static Holder<MobEffect> stability() {
        if (stabilityHolder == null)
            stabilityHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(stability.get());
        return stabilityHolder;
    }

    public static Holder<MobEffect> lead() {
        if (leadHolder == null) leadHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(lead.get());
        return leadHolder;
    }

    public static Holder<MobEffect> taint() {
        if (taintHolder == null)
            taintHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(taint.get());
        return taintHolder;
    }

    public static Holder<MobEffect> mutation() {
        if (mutationHolder == null)
            mutationHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mutation.get());
        return mutationHolder;
    }

    public static Holder<MobEffect> phosphorus() {
        if (phosphorusHolder == null)
            phosphorusHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(phosphorus.get());
        return phosphorusHolder;
    }

    public static Holder<MobEffect> bang() {
        if (bangHolder == null) bangHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(bang.get());
        return bangHolder;
    }

    private static final class BangEffect extends HbmMobEffect {
        private BangEffect() {
            super(MobEffectCategory.HARMFUL, 0x111111);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            entity.hurtServer(level, level.damageSources().source(ModDamageTypes.BANG), 1000F);
            entity.setHealth(0.0F);
            if (!(entity instanceof Player)) entity.discard();

            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.LASER_BANG.get(),
                    SoundSource.PLAYERS,
                    100.0F,
                    1.0F);
            ExplosionLarge.spawnParticles(level, entity.getX(), entity.getY(), entity.getZ(), 10);

            if (entity instanceof Cow cow) {
                cow.spawnAtLocation(
                        level, new ItemStack(ModItems.CHEESE.get(), cow.isBaby() ? 10 : 3), 1.0F);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return true;
        }
    }

    private static final class PhosphorusEffect extends HbmMobEffect {
        private PhosphorusEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFFF00);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            entity.igniteForSeconds(1F);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return true;
        }
    }

    private static final class RadiationEffect extends HbmMobEffect {
        private RadiationEffect() {
            super(MobEffectCategory.HARMFUL, 0x84c128);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            ContaminationUtil.contaminate(
                    entity,
                    ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE,
                    (amplifier + 1) * 0.05D);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return true;
        }
    }

    private static final class RadawayEffect extends HbmMobEffect {
        private RadawayEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xbb4b00);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            HbmLivingProps.incrementRadiation(entity, -(amplifier + 1));
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return true;
        }
    }

    private static final class LeadEffect extends HbmMobEffect {
        private LeadEffect() {
            super(MobEffectCategory.HARMFUL, 0x767682);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            entity.hurtServer(
                    level, level.damageSources().source(ModDamageTypes.LEAD), amplifier + 1.0F);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return tickCount % 60 == 0;
        }
    }

    private static final class TaintEffect extends HbmMobEffect {
        private TaintEffect() {
            super(MobEffectCategory.HARMFUL, 0x800080);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {

            if (!(entity instanceof EntityCreeperTainted)
                    && !(entity instanceof EntityTaintCrab)
                    && level.getRandom().nextInt(40) == 0) {
                entity.hurtServer(
                        level,
                        level.damageSources().source(ModDamageTypes.TAINT),
                        amplifier + 1.0F);
            }

            if (ItemData.TAINT_TRAILS.get()) {
                BlockPos below = entity.blockPosition().below();
                BlockState state = level.getBlockState(below);
                if (below.getY() > level.getMinY() && state.isSolidRender() && !state.isAir()) {

                    level.setBlock(
                            below,
                            ModBlocks.TAINT.get().defaultBlockState().setValue(BlockTaint.AGE, 14),
                            Block.UPDATE_CLIENTS);
                }
            }

            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return tickCount % 2 == 0;
        }
    }

    private static class HbmMobEffect extends MobEffect {
        private HbmMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
