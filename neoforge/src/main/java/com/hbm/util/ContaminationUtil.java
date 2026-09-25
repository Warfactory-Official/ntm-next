// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.config.RadiationConfig;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.HazardSystem;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.ModDataComponents;
import com.hbm.potion.HbmPotion;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
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
    private static final Set<Class<?>> immuneEntities = new HashSet<>();

    static {
        immuneEntities.add(MushroomCow.class);
        immuneEntities.add(Zombie.class);

        immuneEntities.add(AbstractSkeleton.class);
        immuneEntities.add(Ocelot.class);
        immuneEntities.add(IRadiationImmune.class);
        immuneEntities.add(ZombieHorse.class);
        immuneEntities.add(SkeletonHorse.class);
        immuneEntities.add(ArmorStand.class);
    }

    private ContaminationUtil() {}

    public static boolean isRadImmune(Entity e) {
        Class<?> cls = e.getClass();
        for (Class<?> c : immuneEntities) {
            if (c.isAssignableFrom(cls)) return true;
        }
        return false;
    }

    public static double getRads(Entity e) {
        if (!(e instanceof LivingEntity living)) return 0D;
        if (isRadImmune(e)) return 0D;
        return HbmLivingProps.getRadiation(living);
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

        if (entity instanceof Ocelot || entity instanceof EntityDuck) return;
        if (entity instanceof Player player) {
            if (player.isSpectator() || player.isCreative()) return;
            if (player.tickCount < 200) return;
            if (ArmorUtil.checkForDigamma(player)) return;
        }
        if (entity.hasEffect(HbmPotion.stability())) return;
        HbmLivingProps.incrementDigamma(entity, f);
    }

    public static double calculateRadiationMod(LivingEntity entity) {
        if (!(entity instanceof Player)) return 1D;
        return Math.pow(10D, -HazmatRegistry.getResistance(entity));
    }

    public static double getPlayerRads(LivingEntity entity) {
        double rads = HbmLivingProps.getRadBuf(entity);

        if (entity instanceof Player) {
            rads += HbmLivingProps.getNeutron(entity) * 20D / RadiationConfig.hazardRate;
        }
        return rads;
    }

    public static double getActualPlayerRads(LivingEntity entity) {
        return getPlayerRads(entity) * calculateRadiationMod(entity);
    }

    public static double getNoNeutronPlayerRads(LivingEntity entity) {
        return HbmLivingProps.getRadBuf(entity) * calculateRadiationMod(entity);
    }

    private static final double DOSIMETER_CEILING = 3.6D;

    public static void printGeigerData(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        double eRad = HbmLivingProps.getRadiation(player);
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
        double env = HbmLivingProps.getRadBuf(player);
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
        double digamma = HbmLivingProps.getDigamma(player);
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
        if (rads == 0D) return ChatFormatting.GREEN;
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

        if (hazard == HazardType.RADIATION) {
            HbmLivingProps.setRadEnv(entity, HbmLivingProps.getRadEnv(entity) + amount);
        }

        if (entity instanceof Player player) {
            if (player.isSpectator()) return false;
            switch (cont) {
                case FARADAY -> {
                    if (ArmorUtil.checkForFaraday(player)) return false;
                }
                case HAZMAT -> {
                    if (ArmorUtil.checkForHazmat(player)) return false;
                }
                case HAZMAT2 -> {
                    if (ArmorUtil.checkForHaz2(player)) return false;
                }
                case DIGAMMA -> {
                    if (ArmorUtil.checkForDigamma(player) || ArmorUtil.checkForDigamma2(player))
                        return false;
                }
                case DIGAMMA2 -> {
                    if (ArmorUtil.checkForDigamma2(player)) return false;
                }
                default -> {}
            }
            if (player.isCreative()
                    && cont != ContaminationType.NONE
                    && cont != ContaminationType.DIGAMMA2) {

                if (hazard == HazardType.NEUTRON) HbmLivingProps.setNeutron(entity, amount);
                return false;
            }
            if (player.tickCount < 200) return false;
        }

        if ((hazard == HazardType.RADIATION || hazard == HazardType.NEUTRON)
                && isRadImmune(entity)) {
            return false;
        }

        switch (hazard) {
            case RADIATION ->
                    HbmLivingProps.incrementRadiation(
                            entity,
                            amount
                                    * (cont == ContaminationType.RAD_BYPASS
                                            ? 1D
                                            : calculateRadiationMod(entity)));
            case DIGAMMA -> HbmLivingProps.incrementDigamma(entity, amount);
            case NEUTRON -> {
                HbmLivingProps.incrementRadiation(
                        entity,
                        amount
                                * (cont == ContaminationType.RAD_BYPASS
                                        ? 1D
                                        : calculateRadiationMod(entity)));
                HbmLivingProps.setNeutron(entity, amount);
            }
            case MONOXIDE -> {}
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
                res += level.getBlockState(pos).getBlock().getExplosionResistance();
            }
            if (res < 1.0D) res = 1.0D;
            double dose = rad / res / (len * len);
            contaminate(e, HazardType.RADIATION, cont, dose);
        }
    }

    public enum HazardType {
        MONOXIDE,
        RADIATION,
        NEUTRON,
        DIGAMMA
    }

    public enum ContaminationType {
        FARADAY,
        HAZMAT,
        HAZMAT2,
        DIGAMMA,
        DIGAMMA2,
        CREATIVE,
        RAD_BYPASS,
        NONE
    }
}
