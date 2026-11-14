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

package com.hbm.util;

import com.hbm.config.RadiationConfig;
import com.hbm.entity.mob.DuckEntity;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModDataComponents;
import com.hbm.potion.HbmPotion;
import com.hbm.tags.HbmEntityTypeTags;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ContaminationUtil {

    public static final float ACTIVATION_FLOOR = 0.0001F;
    private static final float DECONTAMINATION_FLOOR = 0.001F;
    private static final int ACTIVATION_SLOT_END = Inventory.INVENTORY_SIZE + 4;

    private ContaminationUtil() {}

    public static boolean isRadImmune(Entity e) {
        if (e instanceof LivingEntity living && living.hasEffect(HbmPotion.mutation())) return true;
        return e.getType().builtInRegistryHolder().is(HbmEntityTypeTags.RADIATION_IMMUNE);
    }

    public static double getRads(Entity e) {
        if (!(e instanceof LivingEntity living)) return 0D;
        if (isRadImmune(e)) return 0D;
        return LivingHazards.radiation(living);
    }

    public static float getNeutronRads(ItemStack stack) {
        Float stored = stack.get(ModDataComponents.NEUTRON_ACTIVATION.get());
        if (stored == null || stored <= 0F) return 0F;
        if (HazardSystem.getRawRadsFromStack(stack) > 0D) return 0F;
        return stored * stack.getCount();
    }

    public static boolean isContaminated(ItemStack stack) {
        Float stored = stack.get(ModDataComponents.NEUTRON_ACTIVATION.get());
        return stored != null && stored > 0F;
    }

    public static boolean neutronActivateInventory(Player player, float rad, float decay) {
        Inventory inv = player.getInventory();
        int held = inv.getSelectedSlot();
        boolean changed = false;
        for (int i = 0; i < ACTIVATION_SLOT_END; i++) {
            if (i == held) continue;
            if (neutronActivateItem(inv.getItem(i), rad, decay)) changed = true;
        }
        return changed;
    }

    public static boolean neutronActivateItem(ItemStack stack, float rad, float decay) {
        if (stack.isEmpty()) return false;

        Float stored = stack.get(ModDataComponents.NEUTRON_ACTIVATION.get());
        float prev = stored == null ? 0F : stored;
        if (stored == null && rad <= 0F) return false;
        if (rad > 0F && HazardSystem.getRawRadsFromStack(stack) > 0D) return false;

        float next = prev * decay + rad / stack.getCount();
        float total = next * stack.getCount();
        if (total < ACTIVATION_FLOOR || (rad <= 0F && total < DECONTAMINATION_FLOOR)) {
            if (stored == null) return false;
            stack.remove(ModDataComponents.NEUTRON_ACTIVATION.get());
            return true;
        }
        if (stored != null && next == prev) return false;
        stack.set(ModDataComponents.NEUTRON_ACTIVATION.get(), next);
        return true;
    }

    public static void applyDigammaData(LivingEntity entity, double f) {
        if (entity == null) return;
        if (entity.level().isClientSide()) return;
        if (entity instanceof Ocelot || entity instanceof DuckEntity) return;
        if (entity instanceof Player player) {
            if (player.isSpectator() || player.isCreative()) return;
            if (player.tickCount < 200) return;
            if (ArmorUtil.checkForDigamma(player)) return;
        }
        if (entity.hasEffect(HbmPotion.stability())) return;
        LivingHazards.addDigamma(entity, f);
    }

    public static double calculateRadiationMod(LivingEntity entity) {
        if (!(entity instanceof Player)) return 1D;
        return Math.pow(10D, -HazmatRegistry.getResistance(entity));
    }

    public static double getPlayerRads(LivingEntity entity) {
        double rads = LivingHazards.fieldLastSecond(entity);
        if (entity instanceof Player) {
            rads += LivingHazards.neutron(entity) * 20D / RadiationConfig.hazardRate;
        }
        return rads;
    }

    public static double getActualPlayerRads(LivingEntity entity) {
        return getPlayerRads(entity) * calculateRadiationMod(entity);
    }

    public static double getNoNeutronPlayerRads(LivingEntity entity) {
        return LivingHazards.fieldLastSecond(entity) * calculateRadiationMod(entity);
    }

    private static final double DOSIMETER_CEILING = 3.6D;

    public static void printGeigerData(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        double eRad = LivingHazards.radiation(player);
        double chunkRad = RadiationSystemNT.getRadForCoord(level, player.blockPosition());
        double radMod = calculateRadiationMod(player);
        double env = getPlayerRads(player);
        double received = env * radMod;
        double resPercent = (1D - radMod) * 100D;

        player.sendSystemMessage(
                Component.literal("===== ☢ ")
                        .append(Component.translatable("geiger.title"))
                        .append(" ☢ =====")
                        .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(radLine("geiger.chunkRad", chunkRad));
        player.sendSystemMessage(radLine("geiger.envRad", env));
        player.sendSystemMessage(radLine("geiger.recievedRad", received));
        player.sendSystemMessage(
                Component.translatable("geiger.playerRad")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(
                                Component.literal(" " + fmt(eRad) + " RAD")
                                        .withStyle(radTier(eRad))));
        player.sendSystemMessage(
                Component.translatable("geiger.playerRes")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(
                                Component.literal(" " + String.format("%.2f", resPercent) + "%")
                                        .withStyle(
                                                resPercent > 0D
                                                        ? ChatFormatting.GREEN
                                                        : ChatFormatting.WHITE)));
    }

    public static void printDosimeterData(Player player) {
        double env = LivingHazards.fieldLastSecond(player);
        boolean limit = env > DOSIMETER_CEILING;
        if (limit) env = DOSIMETER_CEILING;

        player.sendSystemMessage(
                Component.literal("===== ☢ ")
                        .append(Component.translatable("geiger.title.dosimeter"))
                        .append(" ☢ =====")
                        .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(
                Component.translatable("geiger.envRad")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(
                                Component.literal(" " + (limit ? ">" : "") + fmt(env) + " RAD/s")
                                        .withStyle(prefixFromRad(env))));
    }

    public static void printDiagnosticData(Player player) {
        double digamma = LivingHazards.digamma(player);
        double halflife = (1D - Math.pow(0.5D, digamma)) * 100D;

        player.sendSystemMessage(
                Component.literal("===== \u03dc ")
                        .append(Component.translatable("digamma.title"))
                        .append(" \u03dc =====")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(
                Component.translatable("digamma.playerDigamma")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(
                                Component.literal(" " + fmt(digamma) + " DRX")
                                        .withStyle(ChatFormatting.RED)));
        player.sendSystemMessage(
                Component.translatable("digamma.playerHealth")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(
                                Component.literal(" " + fmt(halflife) + "%")
                                        .withStyle(ChatFormatting.RED)));
        player.sendSystemMessage(
                Component.translatable("digamma.playerRes")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(" N/A").withStyle(ChatFormatting.BLUE)));
    }

    private static Component radLine(String key, double rate) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.YELLOW)
                .append(
                        Component.literal(" " + fmt(rate) + " RAD/s")
                                .withStyle(prefixFromRad(rate)));
    }

    private static String fmt(double v) {
        double a = Math.abs(v);
        return (a >= 1.0e6 || (a > 0D && a < 1.0e-3))
                ? String.format("%.3e", v)
                : String.format("%.3f", v);
    }

    private static ChatFormatting radTier(double eRad) {
        if (eRad < 200D) return ChatFormatting.GREEN;
        if (eRad < 400D) return ChatFormatting.YELLOW;
        if (eRad < 600D) return ChatFormatting.GOLD;
        if (eRad < 800D) return ChatFormatting.RED;
        if (eRad < 1000D) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    private static ChatFormatting prefixFromRad(double rads) {
        if (rads < 1.0e-3D) return ChatFormatting.GREEN;
        if (rads < 1D) return ChatFormatting.YELLOW;
        if (rads < 10D) return ChatFormatting.GOLD;
        if (rads < 100D) return ChatFormatting.RED;
        if (rads < 1000D) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    public static boolean contaminate(
            LivingEntity entity, HazardType hazard, ContaminationType cont, double amount) {
        if (entity == null) return false;
        if (entity.level().isClientSide()) return false;

        LivingHazards hazards = null;
        if (hazard == HazardType.RADIATION) {
            hazards = LivingHazards.getOrCreate(entity);
            hazards.fieldThisSecond += amount;
        }

        if (entity instanceof Player player) {
            if (player.isSpectator() || cont.protects(player)) return false;
            if (cont.spares(player)) {
                if (hazard == HazardType.NEUTRON) LivingHazards.setNeutron(entity, amount);
                return false;
            }
            if (player.tickCount < 200) return false;
        }

        if ((hazard == HazardType.RADIATION || hazard == HazardType.NEUTRON)
                && isRadImmune(entity)) {
            return false;
        }

        switch (hazard) {
            case RADIATION -> hazards.addRadiation(amount * cont.radiationMod(entity));
            case DIGAMMA -> LivingHazards.addDigamma(entity, amount);
            case NEUTRON -> {
                LivingHazards.addRadiation(entity, amount * cont.radiationMod(entity));
                LivingHazards.setNeutron(entity, amount);
            }
        }
        return true;
    }

    public static void radiate(
            Level level, double x, double y, double z, double range, double rad) {
        radiate(level, x, y, z, range, rad, ContaminationType.RAD_BYPASS);
    }

    public static void radiate(
            Level level,
            double x,
            double y,
            double z,
            double range,
            double rad,
            ContaminationType cont) {
        if (level.isClientSide()) return;
        AABB aabb = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        var reader = new SectionAccess.LoadingReader(level);
        for (LivingEntity e : entities) {
            if (isRadImmune(e)) continue;
            double dx = e.getX() - x;
            double dy = (e.getY() + e.getEyeHeight()) - y;
            double dz = e.getZ() - z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double invLen = 1.0D / Math.max(len, 1.0E-4D);
            Vec3 dir = new Vec3(dx * invLen, dy * invLen, dz * invLen);
            double res = 0D;
            for (int i = 1; i < len; i++) {
                int ix = Mth.floor(x + dir.x * i);
                int iy = Mth.floor(y + dir.y * i);
                int iz = Mth.floor(z + dir.z * i);
                pos.set(ix, iy, iz);
                res += reader.read(pos).getBlock().getExplosionResistance();
            }
            if (res < 1.0D) res = 1.0D;
            double dose = rad / res / (len * len);
            contaminate(e, HazardType.RADIATION, cont, dose);
        }
    }

    public enum HazardType {
        RADIATION,
        NEUTRON,
        DIGAMMA
    }

    public enum ContaminationType {
        FARADAY(ArmorUtil::checkForFaraday, true, false),
        HAZMAT(ArmorUtil::checkForHazmat, true, false),
        HAZMAT2(ArmorUtil::checkForHaz2, true, false),
        DIGAMMA(
                player -> ArmorUtil.checkForDigamma(player) || ArmorUtil.checkForDigamma2(player),
                true,
                false),
        DIGAMMA2(ArmorUtil::checkForDigamma2, false, false),
        CREATIVE(_ -> false, true, false),
        RAD_BYPASS(_ -> false, true, true),
        NONE(_ -> false, false, false);

        private final Predicate<Player> protection;
        private final boolean sparesCreative;
        private final boolean bypassesArmor;

        ContaminationType(
                Predicate<Player> protection, boolean sparesCreative, boolean bypassesArmor) {
            this.protection = protection;
            this.sparesCreative = sparesCreative;
            this.bypassesArmor = bypassesArmor;
        }

        public boolean protects(Player player) {
            return protection.test(player);
        }

        public boolean spares(Player player) {
            return sparesCreative && player.isCreative();
        }

        public double radiationMod(LivingEntity entity) {
            return bypassesArmor ? 1D : calculateRadiationMod(entity);
        }
    }
}
