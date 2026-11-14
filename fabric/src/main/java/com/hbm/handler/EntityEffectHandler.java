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

package com.hbm.handler;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.environment.Environment;
import com.hbm.config.BalanceConfig;
import com.hbm.data.ItemData;
import com.hbm.data.RadiationData;
import com.hbm.data.WorldData;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.BiomeRadiation;
import com.hbm.handler.radiation.IrradiationTransformations;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.Exposure;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.EffectKinds.VomitMode;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.BiomeUtil;
import com.hbm.util.ContagionUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.LcgRandom;
import com.hbm.util.TickPhase;
import com.hbm.world.biome.NtmBiomes;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jspecify.annotations.Nullable;

public final class EntityEffectHandler {
    public static final int BLACK_FIRE_DAMAGE_PERIOD = 10;

    private static final int SECOND = SharedConstants.TICKS_PER_SECOND;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    public static final int CONTAGION_DURATION = 3 * HOUR;

    public static @Nullable IntConsumer CLIENT_RADIATION_FX;
    public static @Nullable Consumer<Player> CLIENT_CRATER_AURA;

    private static final Sickness[] RADIATION_SICKNESS = {
        new Sickness(
                800D,
                false,
                new Chance(300, MobEffects.NAUSEA, 150, 0),
                new Chance(300, MobEffects.SLOWNESS, 10 * SECOND, 2),
                new Chance(300, MobEffects.WEAKNESS, 10 * SECOND, 2),
                new Chance(500, MobEffects.POISON, 3 * SECOND, 2),
                new Chance(700, MobEffects.WITHER, 3 * SECOND, 1)),
        new Sickness(
                600D,
                false,
                new Chance(300, MobEffects.NAUSEA, 150, 0),
                new Chance(300, MobEffects.SLOWNESS, 10 * SECOND, 2),
                new Chance(300, MobEffects.WEAKNESS, 10 * SECOND, 2),
                new Chance(500, MobEffects.POISON, 3 * SECOND, 1)),
        new Sickness(
                400D,
                false,
                new Chance(300, MobEffects.NAUSEA, 150, 0),
                new Chance(500, MobEffects.SLOWNESS, 5 * SECOND, 0),
                new Chance(300, MobEffects.WEAKNESS, 5 * SECOND, 1)),
        new Sickness(
                200D,
                true,
                new Chance(300, MobEffects.NAUSEA, 5 * SECOND, 0),
                new Chance(500, MobEffects.WEAKNESS, 5 * SECOND, 0)),
    };

    private static final Flare[] CONTAGION_FLARES = {
        new Flare(2 * HOUR, 1000, 0F, new Affliction(MobEffects.NAUSEA, SECOND, 0)),
        new Flare(
                HOUR,
                100,
                0F,
                new Affliction(MobEffects.NAUSEA, 3 * SECOND, 0),
                new Affliction(MobEffects.WEAKNESS, 15 * SECOND, 4)),
        new Flare(30 * MINUTE, 400, 1F),
        new Flare(5 * MINUTE, 100, 2F),
    };

    private static final Retching SEVERE_RETCHING = new Retching(600, VomitMode.BLOOD, 25);
    private static final Retching MILD_RETCHING = new Retching(1200, VomitMode.NORMAL, 15);
    private static final int SWEAT_PERIOD = 10;
    private static final int RETCH_WINDOW = 20;

    private EntityEffectHandler() {}

    public static void onLivingTick(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level && !entity.isSpectator())
            ImpactWorldHandler.livingTick(level, entity);
        HazardSystem.tickMobEquipment(entity);
        onUpdate(entity);
    }

    public static void onUpdate(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            if (entity instanceof Player player) handleClientFX(player);
            return;
        }
        if (entity instanceof ServerPlayer player) PlayerShield.tick(player);

        var props = LivingHazards.getIfPresent(entity);
        if (props != null) {
            if (entity.tickCount % SECOND == 0) closeExposureWindow(props);
            if (entity instanceof ServerPlayer player) props.mirrorTo(player);
        }
        if (entity.isSpectator()) {
            handleBiomeRadiation(entity);
            handleRadiation(entity);
            return;
        }

        if (props != null) handleBombTimer(entity, props);
        if (burnsInNether(entity)) entity.igniteForSeconds(5);
        handleBiomeRadiation(entity);
        Environment.tickLiving(entity);
        if (props != null) handleContamination(entity, props);
        props = handleContagion(entity, props);
        if (props != null) handleRadiationEffect(entity, props);
        handleRadiation(entity);
        if (props != null) {
            handleRadiationFX(entity, props);
            handleDigamma(entity, props);
        }
        handleLungDisease(entity, props);
        if (props != null) handleOil(entity, props);
        if (TickPhase.every(entity, 60)) handlePollution(entity);
        if (props != null) handleTemperature(entity, props);
    }

    private static void closeExposureWindow(LivingHazards props) {
        props.fieldLastSecond = props.fieldThisSecond;
        props.fieldThisSecond = 0D;
    }

    private static boolean burnsInNether(LivingEntity entity) {
        return BalanceConfig.enable528
                && WorldData.NETHER_BURN.get()
                && entity instanceof Player
                && !entity.fireImmune()
                && entity.level().dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER);
    }

    private static void handleBombTimer(LivingEntity entity, LivingHazards props) {
        if (props.bombTimer > 0 && --props.bombTimer == 0) {
            MiniNukeExplosion.MEDIUM.detonate(
                    entity.level(), entity.getX(), entity.getY(), entity.getZ());
        }
    }

    private static void handleContamination(LivingEntity entity, LivingHazards props) {
        var doses = props.doses();
        if (doses.isEmpty()) return;
        for (int i = 0; i < doses.size(); i++) {
            var dose = doses.get(i);
            irradiate(
                    entity,
                    dose.bypassesArmor()
                            ? ContaminationType.RAD_BYPASS
                            : ContaminationType.CREATIVE,
                    dose.getCurrentDose());
            dose.tick();
        }
        props.dropSpentDoses();
    }

    private static void handleBiomeRadiation(LivingEntity entity) {
        var biome = BiomeUtil.biomeAt((ServerLevel) entity.level(), entity.blockPosition());
        irradiatePerSecond(entity, BiomeRadiation.dose(biome, entity.isInWaterOrRain()));
    }

    private static void handleRadiation(LivingEntity entity) {
        if (ContaminationUtil.isRadImmune(entity)
                || !(entity.level() instanceof ServerLevel server)) return;
        var pos = entity.blockPosition();
        irradiatePerSecond(
                entity,
                Environment.skyRadiation(server, pos, RadiationSystemNT.doseAt(server, pos)));
    }

    private static void irradiatePerSecond(LivingEntity entity, double perSecond) {
        if (perSecond > 0D) irradiate(entity, ContaminationType.CREATIVE, perSecond / SECOND);
    }

    private static void irradiate(LivingEntity entity, ContaminationType type, double amount) {
        ContaminationUtil.contaminate(entity, HazardType.RADIATION, type, amount);
    }

    private static void handleRadiationEffect(LivingEntity entity, LivingHazards props) {
        if (!RadiationData.ENABLE_CONTAMINATION.get()
                || ContaminationUtil.isRadImmune(entity)
                || entity.isDeadOrDying()
                || !(entity.level() instanceof ServerLevel world)
                || isCreative(entity)) {
            return;
        }
        double dose = props.radiation();
        if (dose <= 0D) return;
        var source = world.damageSources().source(ModDamageTypes.RADIATION);
        if (entity.isInvulnerableTo(world, source)
                || IrradiationTransformations.apply(entity, dose)) return;

        if (dose >= 1000D) {
            entity.hurtServer(world, source, 1000F);
            LivingHazards.setRadiation(entity, 0D);
            if (entity instanceof ServerPlayer player && entity.getHealth() <= 0F) {
                HbmCriteria.radiation(player, dose, true);
            }
            return;
        }
        for (var sickness : RADIATION_SICKNESS) {
            if (dose < sickness.from) continue;
            for (var chance : sickness.chances) chance.roll(world.getRandom(), entity);
            if (sickness.poisoning && entity instanceof ServerPlayer player)
                HbmCriteria.radiation(player, dose, false);
            return;
        }
    }

    private static void handleRadiationFX(LivingEntity entity, LivingHazards props) {
        if (ContaminationUtil.isRadImmune(entity)
                || !(entity.level() instanceof ServerLevel world)
                || isCreative(entity)) {
            return;
        }
        double dose = props.radiation();
        if (dose <= 200D) return;

        long time = world.getGameTime();
        long severe = LcgRandom.afterInt(LcgRandom.seeded(entity.getId()), SEVERE_RETCHING.period);
        long mild = LcgRandom.afterInt(severe, MILD_RETCHING.period);
        if (dose > 600D) SEVERE_RETCHING.tick(world, entity, time, severe);
        else MILD_RETCHING.tick(world, entity, time, mild);

        if (dose > 900D) {
            long sweat = LcgRandom.afterInt(mild, SWEAT_PERIOD);
            if ((time + LcgRandom.intDrawn(sweat, SWEAT_PERIOD)) % SWEAT_PERIOD == 0) {
                ParticleCreators.sweat(world, entity, Blocks.REDSTONE_BLOCK.defaultBlockState(), 1);
            }
        }
    }

    private static void handleDigamma(LivingEntity entity, LivingHazards props) {
        if (props.digamma < 0.01D) return;
        int oneIn = 10 - (int) props.digamma;
        if (oneIn <= 1 || entity.getRandom().nextInt(oneIn) == 0) {
            ParticleCreators.sweat(entity.level(), entity, Blocks.SOUL_SAND.defaultBlockState(), 1);
        }
    }

    private static void handleClientFX(Player player) {
        if (CLIENT_CRATER_AURA != null) {
            Holder<Biome> biome = player.level().getBiome(player.blockPosition());
            if (biome.is(NtmBiomes.CRATER) || biome.is(NtmBiomes.CRATER_INNER))
                CLIENT_CRATER_AURA.accept(player);
        }
        var props = LivingHazards.getIfPresent(player);
        if (props == null || CLIENT_RADIATION_FX == null || props.radiation <= 600D) return;
        CLIENT_RADIATION_FX.accept(props.radiation > 900D ? 4 : props.radiation > 800D ? 2 : 1);
    }

    private static @Nullable LivingHazards handleContagion(
            LivingEntity entity, @Nullable LivingHazards props) {
        if (!ItemData.ENABLE_MKU.get() || !(entity.level() instanceof ServerLevel world))
            return props;
        var random = entity.getRandom();
        int remaining = props != null ? props.contagion() : 0;
        if (entity instanceof Player player
                && exchangeWithCarriedItem(player, random, remaining > 0)) {
            props = LivingHazards.getOrCreate(player);
        }
        if (remaining > 0) progressContagion(world, entity, props, remaining, random);
        return props;
    }

    private static boolean exchangeWithCarriedItem(
            Player player, RandomSource random, boolean infected) {
        int slot = random.nextInt(Inventory.INVENTORY_SIZE);
        var stack =
                random.nextInt(100) == 0
                        ? player.getItemBySlot(
                                ArmorUtil.ARMOR_SLOTS[random.nextInt(ArmorUtil.ARMOR_SLOTS.length)])
                        : player.getInventory().getItem(slot);
        if (stack.isEmpty() || stack.getMaxStackSize() != 1) return false;
        if (infected) {
            ContagionUtil.taint(stack);
            return false;
        }
        if (!ContagionUtil.isContagious(stack) || ContagionUtil.isProtected(player)) return false;
        LivingHazards.getOrCreate(player).setContagion(CONTAGION_DURATION);
        return true;
    }

    private static void progressContagion(
            ServerLevel world,
            LivingEntity entity,
            LivingHazards props,
            int remaining,
            RandomSource random) {
        props.setContagion(remaining - 1);
        if (remaining < CONTAGION_DURATION - 5 * MINUTE && remaining % SECOND == 0)
            spreadContagion(world, entity);
        for (var flare : CONTAGION_FLARES) {
            if (remaining >= flare.below || random.nextInt(flare.oneIn) != 0) continue;
            for (var affliction : flare.afflictions) affliction.apply(entity);
            if (flare.damage > 0F) entity.hurtServer(world, mku(world), flare.damage);
        }
        int retch = (remaining + entity.getId()) % 200;
        if (remaining < 30 * MINUTE && retch < RETCH_WINDOW && canVomit(entity)) {
            ParticleCreators.vomit(world, entity, VomitMode.BLOOD, 25);
            if (retch == RETCH_WINDOW - 1) playVomit(entity);
        }
        if (props.contagion() == 0) entity.hurtServer(world, mku(world), 1000F);
    }

    private static void spreadContagion(ServerLevel world, LivingEntity carrier) {
        double range = carrier.isInWaterOrRain() ? 16D : 2D;
        for (Entity other : world.getEntities(carrier, carrier.getBoundingBox().inflate(range))) {
            if (other instanceof LivingEntity living) {
                var record = LivingHazards.getIfPresent(living);
                if ((record == null || record.contagion() <= 0)
                        && !ContagionUtil.isProtected(living)) {
                    LivingHazards.getOrCreate(living).setContagion(CONTAGION_DURATION);
                }
            } else if (other instanceof ItemEntity item) {
                ContagionUtil.taint(item.getItem());
            }
        }
    }

    private static DamageSource mku(ServerLevel world) {
        return world.damageSources().source(ModDamageTypes.MKU);
    }

    private static boolean canVomit(LivingEntity entity) {
        return entity.getType().getCategory() != MobCategory.WATER_CREATURE;
    }

    private static boolean isCreative(LivingEntity entity) {
        return entity instanceof Player player && player.isCreative();
    }

    private static void playVomit(LivingEntity entity) {
        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.PLAYER_VOMIT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    private static BlockPos eyeBlock(LivingEntity entity) {
        return BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    private static void handleLungDisease(LivingEntity entity, @Nullable LivingHazards props) {
        if (!(entity.level() instanceof ServerLevel world)
                || props == null && !(entity instanceof Player)) return;
        if (isCreative(entity)) {
            if (props != null) {
                LivingHazards.setBlackLung(entity, 0);
                LivingHazards.setAsbestos(entity, 0);
            }
            return;
        }
        if (props != null) {
            int stored = props.blackLung();
            if (stored > 0 && stored < LivingHazards.BLACK_LUNG_LIMIT * 0.5D) {
                LivingHazards.setBlackLung(entity, stored - 1);
            }
        }

        double coalShare =
                props == null
                        ? 0D
                        : Math.min(props.blackLung(), LivingHazards.BLACK_LUNG_LIMIT)
                                / (double) LivingHazards.BLACK_LUNG_LIMIT;
        double fibreShare =
                props == null
                        ? 0D
                        : Math.min(props.asbestos(), LivingHazards.ASBESTOS_LIMIT)
                                / (double) LivingHazards.ASBESTOS_LIMIT;
        double soot =
                entity instanceof Player
                                && !ArmorRegistry.hasProtection(
                                        entity, EquipmentSlot.HEAD, Exposure.PARTICLE_COARSE)
                        ? PollutionHandler.read(world, eyeBlock(entity), PollutionType.SOOT)
                        : 0D;
        if (coalShare <= 0.25D && fibreShare <= 0.25D && soot <= 30) return;

        double clearLungs = (1D - coalShare) * (1D - fibreShare);
        double damage = 1D - clearLungs;
        if (damage > 0.75D) entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
        if (damage > 0.95D) entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));

        double irritation = 1D - clearLungs * (1D - Math.min(soot / 100, 1D));
        int coughEvery = Math.max((int) (1000 - 950 * irritation), 20);
        if (world.getGameTime() % coughEvery != entity.getId() % coughEvery) return;
        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.PLAYER_COUGH.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        if (fibreShare > 0.75D || coalShare > 0.75D)
            ParticleCreators.vomit(world, entity, VomitMode.BLOOD, 5);
        if (coalShare > 0.5D)
            ParticleCreators.vomit(world, entity, VomitMode.SMOKE, coalShare > 0.8D ? 50 : 10);
    }

    private static void handleOil(LivingEntity entity, LivingHazards props) {
        if (props.oil <= 0) return;
        boolean ignites = entity.isOnFire();
        props.oil = ignites ? 0 : props.oil - 1;
        if (ignites) {
            entity.level()
                    .explode(
                            null,
                            entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2,
                            entity.getZ(),
                            3F,
                            false,
                            Level.ExplosionInteraction.BLOCK);
        }
        if (entity.tickCount % 5 == 0) {
            ParticleCreators.sweat(
                    entity.level(), entity, Blocks.COAL_BLOCK.defaultBlockState(), 1);
        }
    }

    public static void handlePollution(LivingEntity entity) {
        var level = entity.level();
        var eye = eyeBlock(entity);
        if (RadiationData.ENABLE_POISON.get()
                && !ArmorRegistry.hasProtection(
                        entity, EquipmentSlot.HEAD, Exposure.GAS_BLISTERING)) {
            float poison = PollutionHandler.read(level, eye, PollutionType.POISON);
            if (poison > 10) {
                entity.addEffect(
                        poison >= 50
                                ? new MobEffectInstance(MobEffects.WITHER, 100, 2)
                                : new MobEffectInstance(
                                        MobEffects.POISON, 100, poison < 25 ? 0 : 1));
            }
        }
        if (RadiationData.ENABLE_LEAD_POISONING.get()
                && !ArmorRegistry.hasProtection(
                        entity, EquipmentSlot.HEAD, Exposure.PARTICLE_FINE)) {
            float metal = PollutionHandler.read(level, eye, PollutionType.HEAVYMETAL);
            if (metal > 25)
                entity.addEffect(new MobEffectInstance(HbmPotion.lead(), 100, metal < 50 ? 0 : 2));
        }
    }

    private static void handleTemperature(LivingEntity entity, LivingHazards props) {
        if (!entity.isAlive() || !(entity.level() instanceof ServerLevel server)) return;
        int phase = entity.tickCount + entity.getId();
        for (var burn : Burn.VALUES) burn.tick(server, entity, props, phase);
        if (!entity.isAlive() && Burn.anyLit(props))
            ConfettiUtil.decideConfetti(entity, entity.damageSources().onFire());
    }

    private record Affliction(Holder<MobEffect> effect, int ticks, int amplifier) {
        void apply(LivingEntity entity) {
            entity.addEffect(new MobEffectInstance(effect, ticks, amplifier));
        }
    }

    private record Chance(int oneIn, Affliction affliction) {
        Chance(int oneIn, Holder<MobEffect> effect, int ticks, int amplifier) {
            this(oneIn, new Affliction(effect, ticks, amplifier));
        }

        void roll(RandomSource random, LivingEntity entity) {
            if (random.nextInt(oneIn) == 0) affliction.apply(entity);
        }
    }

    private record Sickness(double from, boolean poisoning, Chance... chances) {}

    private record Flare(int below, int oneIn, float damage, Affliction... afflictions) {}

    private record Retching(int period, VomitMode mode, int count) {
        void tick(ServerLevel world, LivingEntity entity, long time, long offsetDrawn) {
            long phase = (time + LcgRandom.intDrawn(offsetDrawn, period)) % period;
            if (phase >= RETCH_WINDOW || !canVomit(entity)) return;
            ParticleCreators.vomit(world, entity, mode, count);
            if (phase == 1) {
                playVomit(entity);
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
            }
        }
    }

    private enum Burn {
        FIRE(15, 40, 2F, 0D, FlameCreator.META_FIRE, true, true),
        PHOSPHORUS(15, 40, 5F, 0D, FlameCreator.META_FIRE, true, false),
        BALEFIRE(15, 20, 5F, 5D, FlameCreator.META_BALEFIRE, false, false),
        BLACK_FIRE(10, BLACK_FIRE_DAMAGE_PERIOD, 10F, 5D, FlameCreator.META_BLACK, false, false);

        static final Burn[] VALUES = values();

        final int fizzPeriod;
        final int damagePeriod;
        final float damage;
        final double radiation;
        final int flameMeta;
        final boolean quenchedByImmunity;
        final boolean quenchedByWater;

        Burn(
                int fizzPeriod,
                int damagePeriod,
                float damage,
                double radiation,
                int flameMeta,
                boolean quenchedByImmunity,
                boolean quenchedByWater) {
            this.fizzPeriod = fizzPeriod;
            this.damagePeriod = damagePeriod;
            this.damage = damage;
            this.radiation = radiation;
            this.flameMeta = flameMeta;
            this.quenchedByImmunity = quenchedByImmunity;
            this.quenchedByWater = quenchedByWater;
        }

        static boolean anyLit(LivingHazards props) {
            for (var burn : VALUES) if (burn.left(props) > 0) return true;
            return false;
        }

        int left(LivingHazards props) {
            return switch (this) {
                case FIRE -> props.fire;
                case PHOSPHORUS -> props.phosphorus;
                case BALEFIRE -> props.balefire;
                case BLACK_FIRE -> props.blackFire;
            };
        }

        void setLeft(LivingHazards props, int ticks) {
            switch (this) {
                case FIRE -> props.fire = ticks;
                case PHOSPHORUS -> props.phosphorus = ticks;
                case BALEFIRE -> props.balefire = ticks;
                case BLACK_FIRE -> props.blackFire = ticks;
            }
        }

        void tick(ServerLevel server, LivingEntity entity, LivingHazards props, int phase) {
            if (quenchedByImmunity && entity.fireImmune()
                    || quenchedByWater && entity.isInWaterOrRain()) {
                setLeft(props, 0);
            }
            int left = left(props);
            if (left <= 0) return;
            setLeft(props, left - 1);
            var random = entity.getRandom();
            if (phase % fizzPeriod == 0) {
                EntitySounds.play(
                        entity,
                        entity.getX(),
                        entity.getY() + entity.getBbHeight() / 2D,
                        entity.getZ(),
                        SoundEvents.FIRE_EXTINGUISH,
                        entity.getSoundSource(),
                        1F,
                        1.5F + random.nextFloat() * 0.5F);
            }
            if (radiation > 0D) irradiate(entity, ContaminationType.CREATIVE, radiation);
            if (phase % damagePeriod == 0)
                entity.hurtServer(server, entity.damageSources().onFire(), damage);
            double width = entity.getBbWidth();
            double x = entity.getX() - width / 2D + width * random.nextDouble();
            double y = entity.getY() + random.nextDouble() * entity.getBbHeight();
            double z = entity.getZ() - width / 2D + width * random.nextDouble();
            FlameCreator.composeEffect(server, x, y, z, flameMeta);
        }
    }
}
