// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.extprop;

import com.hbm.advancement.HbmCriteria;
import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

public final class HbmLivingProps {

    public static final double RAD_CAP = 2500D;
    public static final int maxAsbestos = 60 * SharedConstants.TICKS_PER_MINUTE;
    public static final int maxBlacklung = 120 * SharedConstants.TICKS_PER_MINUTE;
    public static final MapCodec<HbmLivingProps> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .optionalFieldOf("radiation", 0D)
                                                    .forGetter(p -> p.radiation),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("digamma", 0D)
                                                    .forGetter(p -> p.digamma),
                                            Codec.INT
                                                    .optionalFieldOf("asbestos", 0)
                                                    .forGetter(p -> p.asbestos),
                                            Codec.INT
                                                    .optionalFieldOf("blacklung", 0)
                                                    .forGetter(p -> p.blacklung),
                                            Codec.INT
                                                    .optionalFieldOf("bombTimer", 0)
                                                    .forGetter(p -> p.bombTimer),
                                            Codec.INT
                                                    .optionalFieldOf("contagion", 0)
                                                    .forGetter(p -> p.contagion),
                                            Codec.INT
                                                    .optionalFieldOf("oil", 0)
                                                    .forGetter(p -> p.oil),
                                            Codec.INT
                                                    .optionalFieldOf("fire", 0)
                                                    .forGetter(p -> p.fire),
                                            Codec.INT
                                                    .optionalFieldOf("phosphorus", 0)
                                                    .forGetter(p -> p.phosphorus),
                                            Codec.INT
                                                    .optionalFieldOf("balefire", 0)
                                                    .forGetter(p -> p.balefire),
                                            Codec.INT
                                                    .optionalFieldOf("blackFire", 0)
                                                    .forGetter(p -> p.blackFire),
                                            Codec.BOOL
                                                    .optionalFieldOf("hfr_defused", false)
                                                    .forGetter(p -> p.defused),
                                            ContaminationEffect.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("contamination", List.of())
                                                    .forGetter(HbmLivingProps::getCont))
                                    .apply(i, HbmLivingProps::create));

    private static final Identifier DIGAMMA_MODIFIER_ID = Library.id("digamma_health");

    private @Nullable List<ContaminationEffect> contamination;
    public double radiation;
    public double digamma;
    public int asbestos;
    public int blacklung;
    public int bombTimer;
    public int contagion;
    public int oil;
    public int fire;
    public int phosphorus;
    public int balefire;
    public int blackFire;

    public boolean defused;
    public double radEnv;
    public double radBuf;
    public double neutron;

    public HbmLivingProps() {}

    private static HbmLivingProps create(
            double radiation,
            double digamma,
            int asbestos,
            int blacklung,
            int bombTimer,
            int contagion,
            int oil,
            int fire,
            int phosphorus,
            int balefire,
            int blackFire,
            boolean defused,
            List<ContaminationEffect> contamination) {
        HbmLivingProps p = new HbmLivingProps();
        p.radiation = radiation;
        p.digamma = digamma;
        p.asbestos = asbestos;
        p.blacklung = blacklung;
        p.bombTimer = bombTimer;
        p.contagion = contagion;
        p.oil = oil;
        p.fire = fire;
        p.phosphorus = phosphorus;
        p.balefire = balefire;
        p.blackFire = blackFire;
        p.defused = defused;
        if (!contamination.isEmpty()) p.contamination = new ArrayList<>(contamination);
        return p;
    }

    public static HbmLivingProps getData(LivingEntity entity) {
        return Services.ENTITY_DATA.get(entity, ModEntityData.LIVING_PROPS);
    }

    public static @Nullable HbmLivingProps peek(LivingEntity entity) {
        return Services.ENTITY_DATA.getOrNull(entity, ModEntityData.LIVING_PROPS);
    }

    public boolean isDefault() {
        return radiation == 0D
                && digamma == 0D
                && asbestos == 0
                && blacklung == 0
                && bombTimer == 0
                && contagion == 0
                && oil == 0
                && fire == 0
                && phosphorus == 0
                && balefire == 0
                && blackFire == 0
                && !defused
                && radEnv == 0D
                && radBuf == 0D
                && neutron == 0D
                && (contamination == null || contamination.isEmpty());
    }

    public double radiation() {
        return RadiationData.ENABLE_CONTAMINATION.get() ? radiation : 0D;
    }

    public int asbestos() {
        return RadiationData.DISABLE_ASBESTOS.get() ? 0 : asbestos;
    }

    public int blackLung() {
        return RadiationData.DISABLE_COAL.get() ? 0 : blacklung;
    }

    public List<ContaminationEffect> getCont() {
        return contamination == null ? List.of() : contamination;
    }

    public void addCont(ContaminationEffect cont) {
        if (contamination == null) contamination = new ArrayList<>(2);
        contamination.add(cont);
    }

    public void setCont(ContaminationEffect[] cont) {
        if (cont.length == 0) {
            if (contamination != null) contamination.clear();
            return;
        }
        if (contamination == null) contamination = new ArrayList<>(cont.length);
        else contamination.clear();
        Collections.addAll(contamination, cont);
    }

    public static boolean isDefused(LivingEntity entity) {
        return getData(entity).defused;
    }

    public static void setDefused(LivingEntity entity, boolean defused) {
        getData(entity).defused = defused;
    }

    public static double getRadiation(LivingEntity entity) {
        return getData(entity).radiation();
    }

    public static void setRadiation(LivingEntity entity, double rad) {
        if (RadiationData.ENABLE_CONTAMINATION.get()) getData(entity).radiation = rad;
    }

    public static void incrementRadiation(LivingEntity entity, double rad) {
        if (!RadiationData.ENABLE_CONTAMINATION.get()) return;
        HbmLivingProps p = getData(entity);
        p.radiation = Mth.clamp(p.radiation + rad, 0D, RAD_CAP);
    }

    public static double getRadEnv(LivingEntity entity) {
        return getData(entity).radEnv;
    }

    public static void setRadEnv(LivingEntity entity, double rad) {
        getData(entity).radEnv = rad;
    }

    public static double getRadBuf(LivingEntity entity) {
        return getData(entity).radBuf;
    }

    public static void setRadBuf(LivingEntity entity, double rad) {
        getData(entity).radBuf = rad;
    }

    public static double getNeutron(LivingEntity entity) {
        return getData(entity).neutron;
    }

    public static void setNeutron(LivingEntity entity, double rad) {
        getData(entity).neutron = Math.max(rad, 0D);
    }

    public static double getDigamma(LivingEntity entity) {
        return getData(entity).digamma;
    }

    public static void setDigamma(LivingEntity entity, double digamma) {

        if (entity instanceof EntityDuck) digamma = 0D;
        digamma = Mth.clamp(digamma, 0D, 1000D);
        getData(entity).digamma = digamma;

        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        double healthMod = Math.pow(0.5D, digamma) - 1D;
        attr.addOrUpdateTransientModifier(
                new AttributeModifier(
                        DIGAMMA_MODIFIER_ID,
                        healthMod,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        if (entity.getHealth() > entity.getMaxHealth() && entity.getMaxHealth() > 0F) {
            entity.setHealth(entity.getMaxHealth());
        }
        if ((entity.getMaxHealth() <= 0F || digamma >= 10D)
                && entity.isAlive()
                && entity.level() instanceof ServerLevel server) {
            entity.setAbsorptionAmount(0F);
            entity.hurtServer(server, server.damageSources().source(ModDamageTypes.DIGAMMA), 500F);
            entity.setHealth(0F);
        }

        if (entity instanceof ServerPlayer player) {
            double di = getData(entity).digamma;

            if (di > 0D) HbmCriteria.digamma(player, di);
        }
    }

    public static void incrementDigamma(LivingEntity entity, double digamma) {
        setDigamma(entity, getDigamma(entity) + digamma);
    }

    public static int getAsbestos(LivingEntity entity) {
        return getData(entity).asbestos();
    }

    public static void setAsbestos(LivingEntity entity, int asbestos) {
        if (RadiationData.DISABLE_ASBESTOS.get()) return;
        HbmLivingProps p = getData(entity);
        p.asbestos = asbestos;
        if (asbestos >= maxAsbestos) {
            p.asbestos = 0;
            if (entity.level() instanceof ServerLevel server) {
                entity.hurtServer(
                        server, server.damageSources().source(ModDamageTypes.ASBESTOS), 1000F);
            }
        }
    }

    public static void incrementAsbestos(LivingEntity entity, int asbestos) {
        if (RadiationData.DISABLE_ASBESTOS.get()) return;
        setAsbestos(entity, getAsbestos(entity) + asbestos);
        informGasHazard(entity, "info.asbestos");
    }

    private static void informGasHazard(LivingEntity entity, String key) {
        if (entity instanceof ServerPlayer player) {
            Services.NETWORK.sendTo(
                    new PlayerInformPayload(
                            Component.translatable(key).withStyle(ChatFormatting.RED),
                            PlayerInformPayload.ID_GAS_HAZARD,
                            3_000),
                    player);
        }
    }

    public static int getBlackLung(LivingEntity entity) {
        return getData(entity).blackLung();
    }

    public static void setBlackLung(LivingEntity entity, int bl) {
        if (RadiationData.DISABLE_COAL.get()) return;
        HbmLivingProps p = getData(entity);
        p.blacklung = bl;
        if (bl >= maxBlacklung) {
            p.blacklung = 0;
            if (entity.level() instanceof ServerLevel server) {
                entity.hurtServer(
                        server, server.damageSources().source(ModDamageTypes.BLACKLUNG), 1000F);
            }
        }
    }

    public static void incrementBlackLung(LivingEntity entity, int bl) {
        if (RadiationData.DISABLE_COAL.get()) return;
        setBlackLung(entity, getBlackLung(entity) + bl);
        informGasHazard(entity, "info.coaldust");
    }

    public static List<ContaminationEffect> getCont(LivingEntity entity) {
        return getData(entity).getCont();
    }

    public static void addCont(LivingEntity entity, ContaminationEffect cont) {
        getData(entity).addCont(cont);
    }
}
