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

package com.hbm.extprop;

import com.hbm.advancement.HbmCriteria;
import com.hbm.data.ItemData;
import com.hbm.data.RadiationData;
import com.hbm.entity.mob.DuckEntity;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.HbmPlayerSyncLayout;
import com.hbm.packet.layout.PlayerInformLayout;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public final class LivingHazards {

    public static final double RADIATION_CAP = 2500D;
    public static final int ASBESTOS_LIMIT = 72000;
    public static final int BLACK_LUNG_LIMIT = 144000;
    public static final double DIGAMMA_LIMIT = 10D;
    public static final Identifier DIGAMMA_HEALTH = RegistryUtil.id("digamma_health");

    private static final Component ASBESTOS_WARNING =
            Component.translatable("info.asbestos").withStyle(ChatFormatting.RED);
    private static final Component COAL_WARNING =
            Component.translatable("info.coaldust").withStyle(ChatFormatting.RED);

    public static final MapCodec<LivingHazards> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .optionalFieldOf("radiation", 0D)
                                                    .forGetter(h -> h.radiation),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("digamma", 0D)
                                                    .forGetter(h -> h.digamma),
                                            Codec.INT
                                                    .optionalFieldOf("asbestos", 0)
                                                    .forGetter(h -> h.asbestos),
                                            Codec.INT
                                                    .optionalFieldOf("blacklung", 0)
                                                    .forGetter(h -> h.blackLung),
                                            Codec.INT
                                                    .optionalFieldOf("bombTimer", 0)
                                                    .forGetter(h -> h.bombTimer),
                                            Codec.INT
                                                    .optionalFieldOf("contagion")
                                                    .forGetter(
                                                            h ->
                                                                    h.contagion() != 0
                                                                            ? Optional.of(
                                                                                    h.contagion)
                                                                            : Optional.empty()),
                                            Codec.INT
                                                    .optionalFieldOf("oil", 0)
                                                    .forGetter(h -> h.oil),
                                            Codec.INT
                                                    .optionalFieldOf("fire", 0)
                                                    .forGetter(h -> h.fire),
                                            Codec.INT
                                                    .optionalFieldOf("phosphorus", 0)
                                                    .forGetter(h -> h.phosphorus),
                                            Codec.INT
                                                    .optionalFieldOf("balefire", 0)
                                                    .forGetter(h -> h.balefire),
                                            Codec.INT
                                                    .optionalFieldOf("blackFire", 0)
                                                    .forGetter(h -> h.blackFire),
                                            Codec.BOOL
                                                    .optionalFieldOf("hfr_defused", false)
                                                    .forGetter(h -> h.defused),
                                            ContaminationEffect.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("contamination", List.of())
                                                    .forGetter(h -> h.doseView))
                                    .apply(i, LivingHazards::decoded));

    public double radiation;
    public double digamma;
    public int asbestos;
    public int blackLung;
    public int bombTimer;
    private int contagion;
    public int oil;
    public int fire, phosphorus, balefire, blackFire;
    private boolean defused;
    private @Nullable ArrayList<ContaminationEffect> doseStore;
    private List<ContaminationEffect> doseView = List.of();
    public double fieldThisSecond, fieldLastSecond;
    public double neutron;
    private @Nullable Mirror mirror;

    private static LivingHazards decoded(
            double radiation,
            double digamma,
            int asbestos,
            int blackLung,
            int bombTimer,
            Optional<Integer> contagion,
            int oil,
            int fire,
            int phosphorus,
            int balefire,
            int blackFire,
            boolean defused,
            List<ContaminationEffect> doses) {
        var hazards = new LivingHazards();
        hazards.radiation = radiation;
        hazards.digamma = digamma;
        hazards.asbestos = asbestos;
        hazards.blackLung = blackLung;
        hazards.bombTimer = bombTimer;
        if (ItemData.ENABLE_MKU.get()) hazards.contagion = contagion.orElse(0);
        hazards.oil = oil;
        hazards.fire = fire;
        hazards.phosphorus = phosphorus;
        hazards.balefire = balefire;
        hazards.blackFire = blackFire;
        hazards.defused = defused;
        for (ContaminationEffect dose : doses) hazards.addDose(dose);
        return hazards;
    }

    public static LivingHazards getOrCreate(LivingEntity entity) {
        return ((Entity) entity).getData(ModEntityData.LIVING_PROPS.get());
    }

    public static @Nullable LivingHazards getIfPresent(LivingEntity entity) {
        return ((Entity) entity).getExistingDataOrNull(ModEntityData.LIVING_PROPS.get());
    }

    public double radiation() {
        return RadiationData.ENABLE_CONTAMINATION.get() ? radiation : 0D;
    }

    public static double radiation(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0D : hazards.radiation();
    }

    public static double storedRadiation(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0D : hazards.radiation;
    }

    public static void setRadiation(LivingEntity entity, double rad) {
        if (RadiationData.ENABLE_CONTAMINATION.get()) getOrCreate(entity).radiation = rad;
    }

    public void addRadiation(double rad) {
        if (RadiationData.ENABLE_CONTAMINATION.get())
            radiation = Mth.clamp(radiation + rad, 0D, RADIATION_CAP);
    }

    public static void addRadiation(LivingEntity entity, double rad) {
        if (RadiationData.ENABLE_CONTAMINATION.get()) getOrCreate(entity).addRadiation(rad);
    }

    public static double digamma(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0D : hazards.digamma;
    }

    public static void setDigamma(LivingEntity entity, double digamma) {
        if (entity.level() instanceof ServerLevel level)
            applyDigamma(level, entity, getOrCreate(entity), digamma);
    }

    public static void addDigamma(LivingEntity entity, double digamma) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var hazards = getOrCreate(entity);
        applyDigamma(
                level, entity, hazards, Mth.clamp(hazards.digamma + digamma, 0D, DIGAMMA_LIMIT));
    }

    private static void applyDigamma(
            ServerLevel level, LivingEntity entity, LivingHazards hazards, double digamma) {
        if (entity instanceof DuckEntity) digamma = 0D;
        hazards.digamma = digamma;
        var maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.addOrReplacePermanentModifier(
                    new AttributeModifier(
                            DIGAMMA_HEALTH,
                            Math.pow(0.5D, digamma) - 1D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        float max = entity.getMaxHealth();
        if (entity.getHealth() > max && max > 0F) entity.setHealth(max);
        if (entity.isAlive() && (max <= 0F || digamma >= DIGAMMA_LIMIT)) {
            entity.hurtServer(level, level.damageSources().source(ModDamageTypes.DIGAMMA), 500F);
            ParticleCreators.sweat(level, entity, Blocks.SOUL_SAND.defaultBlockState(), 50, 50);
        }
        if (entity instanceof ServerPlayer player && digamma > 0D)
            HbmCriteria.digamma(player, digamma);
    }

    public int asbestos() {
        return RadiationData.DISABLE_ASBESTOS.get() ? 0 : asbestos;
    }

    public static int asbestos(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0 : hazards.asbestos();
    }

    public static void setAsbestos(LivingEntity entity, int ticks) {
        if (!RadiationData.DISABLE_ASBESTOS.get())
            storeAsbestos(entity, getOrCreate(entity), ticks);
    }

    public static void addAsbestos(LivingEntity entity, int ticks) {
        if (RadiationData.DISABLE_ASBESTOS.get()) return;
        var hazards = getOrCreate(entity);
        storeAsbestos(entity, hazards, hazards.asbestos + ticks);
        warn(entity, ASBESTOS_WARNING);
    }

    private static void storeAsbestos(LivingEntity entity, LivingHazards hazards, int ticks) {
        hazards.asbestos = ticks < ASBESTOS_LIMIT ? ticks : 0;
        if (ticks >= ASBESTOS_LIMIT) choke(entity, ModDamageTypes.ASBESTOS);
    }

    public int blackLung() {
        return RadiationData.DISABLE_COAL.get() ? 0 : blackLung;
    }

    public static int blackLung(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0 : hazards.blackLung();
    }

    public static void setBlackLung(LivingEntity entity, int ticks) {
        if (!RadiationData.DISABLE_COAL.get()) storeBlackLung(entity, getOrCreate(entity), ticks);
    }

    public static void addBlackLung(LivingEntity entity, int ticks) {
        if (RadiationData.DISABLE_COAL.get()) return;
        var hazards = getOrCreate(entity);
        storeBlackLung(entity, hazards, hazards.blackLung + ticks);
        warn(entity, COAL_WARNING);
    }

    private static void storeBlackLung(LivingEntity entity, LivingHazards hazards, int ticks) {
        hazards.blackLung = ticks < BLACK_LUNG_LIMIT ? ticks : 0;
        if (ticks >= BLACK_LUNG_LIMIT) choke(entity, ModDamageTypes.BLACKLUNG);
    }

    private static void choke(LivingEntity entity, ResourceKey<DamageType> type) {
        if (entity.level() instanceof ServerLevel level) {
            entity.hurtServer(level, level.damageSources().source(type), 1000F);
        }
    }

    private static void warn(LivingEntity entity, Component warning) {
        if (entity instanceof ServerPlayer player) {
            PacketWire.sendTo(
                    PlayerInformLayout.encode(
                            warning, ClientEventConstants.PLAYER_INFORM_ID_GAS_HAZARD, 3000),
                    player);
        }
    }

    public int contagion() {
        return ItemData.ENABLE_MKU.get() ? contagion : 0;
    }

    public void setContagion(int ticks) {
        contagion = ticks;
    }

    public List<ContaminationEffect> doses() {
        return doseView;
    }

    public static List<ContaminationEffect> doses(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? List.of() : hazards.doseView;
    }

    public void addDose(ContaminationEffect dose) {
        var store = doseStore;
        if (store == null) {
            doseStore = store = new ArrayList<>();
            doseView = Collections.unmodifiableList(store);
        }
        store.add(dose);
    }

    public static void addDose(LivingEntity entity, ContaminationEffect dose) {
        getOrCreate(entity).addDose(dose);
    }

    public void clearDoses() {
        if (doseStore != null) doseStore.clear();
    }

    public void dropSpentDoses() {
        if (doseStore != null) doseStore.removeIf(ContaminationEffect::isExpired);
    }

    public static double fieldThisSecond(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0D : hazards.fieldThisSecond;
    }

    public static void setFieldThisSecond(LivingEntity entity, double rad) {
        getOrCreate(entity).fieldThisSecond = rad;
    }

    public static double fieldLastSecond(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0D : hazards.fieldLastSecond;
    }

    public static void setFieldLastSecond(LivingEntity entity, double rad) {
        getOrCreate(entity).fieldLastSecond = rad;
    }

    public static double neutron(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards == null ? 0D : hazards.neutron;
    }

    public static void setNeutron(LivingEntity entity, double activation) {
        getOrCreate(entity).neutron = Math.max(activation, 0D);
    }

    public static boolean defused(LivingEntity entity) {
        var hazards = getIfPresent(entity);
        return hazards != null && hazards.defused;
    }

    public static void setDefused(LivingEntity entity, boolean defused) {
        getOrCreate(entity).defused = defused;
    }

    public void mirrorTo(ServerPlayer owner) {
        var last = mirror;
        if (last == null) mirror = last = new Mirror();
        if (!last.refresh(this, owner)) return;
        PacketWire.sendTo(
                HbmPlayerSyncLayout.encode(
                        radiation,
                        digamma,
                        fieldLastSecond,
                        asbestos,
                        bombTimer,
                        contagion,
                        blackLung,
                        oil,
                        doseView),
                owner);
    }

    private static final class Mirror {
        private @Nullable ServerPlayer owner;
        private @Nullable Level level;
        private double radiation, digamma, fieldLastSecond;
        private int asbestos, bombTimer, contagion, blackLung, oil;
        private boolean doses;

        boolean refresh(LivingHazards hazards, ServerPlayer to) {
            boolean dosing = !hazards.doseView.isEmpty();
            if (to == owner
                    && to.level() == level
                    && !dosing
                    && !doses
                    && to.tickCount % SharedConstants.TICKS_PER_SECOND != 0
                    && radiation == hazards.radiation
                    && digamma == hazards.digamma
                    && fieldLastSecond == hazards.fieldLastSecond
                    && asbestos == hazards.asbestos
                    && bombTimer == hazards.bombTimer
                    && contagion == hazards.contagion
                    && blackLung == hazards.blackLung
                    && oil == hazards.oil) {
                return false;
            }
            owner = to;
            level = to.level();
            radiation = hazards.radiation;
            digamma = hazards.digamma;
            fieldLastSecond = hazards.fieldLastSecond;
            asbestos = hazards.asbestos;
            bombTimer = hazards.bombTimer;
            contagion = hazards.contagion;
            blackLung = hazards.blackLung;
            oil = hazards.oil;
            doses = dosing;
            return true;
        }
    }
}
