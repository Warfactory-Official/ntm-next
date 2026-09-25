// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.data.MobData;
import com.hbm.entity.mob.ai.EntityAIFireGun;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class MobUtil {

    private static final Random RAND = new Random();

    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_COMMON =
            Map.of(
                    EquipmentSlot.HEAD,
                    List.of(
                            w(ModItems.GAS_MASK_M65, 16),
                            w(ModItems.GAS_MASK_OLDE, 12),
                            w(ModItems.MASK_OF_INFAMY, 8),
                            w(ModItems.GAS_MASK_MONO, 8),
                            w(ModItems.ROBES_HELMET, 32),
                            w(ModItems.NO9, 16),
                            w(ModItems.COBALT_HELMET, 2),
                            w(ModItems.RAG_PISS, 1),
                            w(ModItems.NOSSY_HAT, 1),
                            w(ModItems.ALLOY_HELMET, 2),
                            w(ModItems.TITANIUM_HELMET, 4),
                            w(ModItems.STEEL_HELMET, 8)),
                    EquipmentSlot.CHEST,
                    List.of(
                            w(ModItems.STARMETAL_PLATE, 1),
                            w(ModItems.COBALT_PLATE, 2),
                            w(ModItems.ROBES_PLATE, 32),
                            w(ModItems.JACKT, 32),
                            w(ModItems.JACKT2, 32),
                            w(ModItems.ALLOY_PLATE, 2),
                            w(ModItems.STEEL_PLATE, 2),
                            none(10)),
                    EquipmentSlot.LEGS,
                    List.of(
                            w(ModItems.ZIRCONIUM_LEGS, 1),
                            w(ModItems.COBALT_LEGS, 2),
                            w(ModItems.STEEL_LEGS, 16),
                            w(ModItems.TITANIUM_LEGS, 8),
                            w(ModItems.ROBES_LEGS, 32),
                            w(ModItems.ALLOY_LEGS, 2),
                            none(20)),
                    EquipmentSlot.FEET,
                    List.of(
                            w(ModItems.ROBES_BOOTS, 32),
                            w(ModItems.STEEL_BOOTS, 16),
                            w(ModItems.COBALT_BOOTS, 2),
                            w(ModItems.ALLOY_BOOTS, 2),
                            none(10)),
                    EquipmentSlot.MAINHAND,
                    List.of(
                            w(ModItems.WEAPON_PIPE_LEAD, 30),
                            w(ModItems.CROWBAR, 25),
                            w(ModItems.GEIGER_COUNTER, 20),
                            w(ModItems.REER_GRAAR, 16),
                            w(ModItems.STEEL_PICKAXE, 12),
                            w(ModItems.STOPSIGN, 10),
                            w(ModItems.SOPSIGN, 8),
                            w(ModItems.CHERNOBYLSIGN, 6),
                            w(ModItems.STEEL_SWORD, 15),
                            w(ModItems.TITANIUM_SWORD, 8),
                            w(ModItems.LEAD_GAVEL, 4),
                            w(ModItems.WRENCH_FLIPPED, 2),
                            w(ModItems.WRENCH, 20),
                            none(1000)));

    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_MASKS = Map.of();
    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_RANGED =
            Map.of(
                    EquipmentSlot.HEAD,
                    List.of(
                            w(ModItems.GAS_MASK_M65, 16),
                            w(ModItems.GAS_MASK_OLDE, 12),
                            w(ModItems.MASK_OF_INFAMY, 8),
                            w(ModItems.GAS_MASK_MONO, 8),
                            w(ModItems.ROBES_HELMET, 32),
                            w(ModItems.NO9, 16),
                            w(ModItems.RAG_PISS, 1),
                            w(ModItems.GOGGLES, 1),
                            w(ModItems.ALLOY_HELMET, 2),
                            w(ModItems.TITANIUM_HELMET, 4),
                            w(ModItems.STEEL_HELMET, 8)),
                    EquipmentSlot.CHEST,
                    List.of(
                            w(ModItems.STARMETAL_PLATE, 1),
                            w(ModItems.COBALT_PLATE, 2),
                            w(ModItems.ALLOY_PLATE, 2),
                            w(ModItems.STEEL_PLATE, 8),
                            w(ModItems.TITANIUM_PLATE, 4),
                            none(10)),
                    EquipmentSlot.LEGS,
                    List.of(
                            w(ModItems.ZIRCONIUM_LEGS, 1),
                            w(ModItems.COBALT_LEGS, 2),
                            w(ModItems.STEEL_LEGS, 16),
                            w(ModItems.TITANIUM_LEGS, 8),
                            w(ModItems.ROBES_LEGS, 32),
                            w(ModItems.ALLOY_LEGS, 2),
                            none(10)),
                    EquipmentSlot.FEET,
                    List.of(
                            w(ModItems.ROBES_BOOTS, 32),
                            w(ModItems.STEEL_BOOTS, 16),
                            w(ModItems.COBALT_BOOTS, 2),
                            w(ModItems.ALLOY_BOOTS, 2),
                            w(ModItems.TITANIUM_BOOTS, 6),
                            none(10)));
    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_ADV_RANGED =
            Map.of(
                    EquipmentSlot.HEAD,
                    List.of(
                            w(ModItems.SECURITY_HELMET, 10),
                            w(ModItems.T51_HELMET, 4),
                            w(ModItems.ASBESTOS_HELMET, 12),
                            w(ModItems.LIQUIDATOR_HELMET, 4),
                            w(ModItems.NO9, 12),
                            w(ModItems.HAZMAT_HELMET, 6)),
                    EquipmentSlot.CHEST,
                    List.of(
                            w(ModItems.LIQUIDATOR_PLATE, 4),
                            w(ModItems.SECURITY_PLATE, 8),
                            w(ModItems.ASBESTOS_PLATE, 12),
                            w(ModItems.T51_PLATE, 4),
                            w(ModItems.HAZMAT_PLATE, 6),
                            w(ModItems.STEEL_PLATE, 8)),
                    EquipmentSlot.LEGS,
                    List.of(
                            w(ModItems.LIQUIDATOR_LEGS, 4),
                            w(ModItems.SECURITY_LEGS, 8),
                            w(ModItems.ASBESTOS_LEGS, 12),
                            w(ModItems.T51_LEGS, 4),
                            w(ModItems.HAZMAT_LEGS, 6),
                            w(ModItems.STEEL_LEGS, 8)),
                    EquipmentSlot.FEET,
                    List.of(
                            w(ModItems.LIQUIDATOR_BOOTS, 4),
                            w(ModItems.SECURITY_BOOTS, 8),
                            w(ModItems.ASBESTOS_BOOTS, 12),
                            w(ModItems.T51_BOOTS, 4),
                            w(ModItems.HAZMAT_BOOTS, 6),
                            w(ModItems.ROBES_BOOTS, 8)));

    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_ADV =
            withMainHand(
                    SLOT_POOL_ADV_RANGED,
                    List.of(
                            w(ModItems.WEAPON_PIPE_LEAD, 20),
                            w(ModItems.CROWBAR, 10),
                            w(ModItems.GEIGER_COUNTER, 10),
                            w(ModItems.REER_GRAAR, 20),
                            w(ModItems.WRENCH_FLIPPED, 20),
                            w(ModItems.STOPSIGN, 16),
                            w(ModItems.SOPSIGN, 4),
                            w(ModItems.CHERNOBYLSIGN, 16),
                            w(ModItems.TITANIUM_SWORD, 18),
                            w(ModItems.LEAD_GAVEL, 8),
                            w(ModItems.WRENCH, 20),
                            none(500)));

    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_COMMON_SOOT =
            soot(SLOT_POOL_COMMON, 10000, 7000, 7000, 7000, 8000);
    public static final Map<EquipmentSlot, List<WeightedItem>> SLOT_POOL_RANGED_SOOT =
            soot(SLOT_POOL_RANGED, 0, 10000, 7000, 7000, 8000);

    public static final Map<Double, List<WeightedItem>> SLOT_POOL_GUNS =
            Map.of(
                    0.3D,
                            List.of(
                                    w(ModItems.GUN_LIGHT_REVOLVER, 16),
                                    w(ModItems.GUN_GREASEGUN, 8),
                                    w(ModItems.GUN_MARESLEG, 2)),
                    1D,
                            List.of(
                                    w(ModItems.GUN_LIGHT_REVOLVER, 6),
                                    w(ModItems.GUN_GREASEGUN, 8),
                                    w(ModItems.GUN_MARESLEG, 4),
                                    w(ModItems.GUN_HENRY, 6)),
                    3D,
                            List.of(
                                    w(ModItems.GUN_UZI, 10),
                                    w(ModItems.GUN_MARESLEG, 8),
                                    w(ModItems.GUN_HENRY, 12),
                                    w(ModItems.GUN_HEAVY_REVOLVER, 4),
                                    w(ModItems.GUN_FLAREGUN, 2)),
                    5D,
                            List.of(
                                    w(ModItems.GUN_AM180, 6),
                                    w(ModItems.GUN_UZI, 10),
                                    w(ModItems.GUN_SPAS12, 8),
                                    w(ModItems.GUN_HENRY_LINCOLN, 2),
                                    w(ModItems.GUN_HEAVY_REVOLVER, 12),
                                    w(ModItems.GUN_FLAREGUN, 4),
                                    w(ModItems.GUN_FLAMER, 2)));

    private MobUtil() {}

    public static void decorateNaturalSpawn(Mob mob) {
        decorateMob(mob, RAND);
    }

    public static void decorateMob(Mob mob, Random random) {
        if (!MobData.ENABLE_MOB_GEAR.get() || mob.isBaby()) return;
        float soot =
                PollutionHandler.getPollution(mob.level(), mob.blockPosition(), PollutionType.SOOT);
        if (mob instanceof Zombie) {
            if (random.nextFloat() < 0.005F && soot > 2) {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HAZMAT_HELMET.get()));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HAZMAT_PLATE.get()));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HAZMAT_LEGS.get()));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.HAZMAT_BOOTS.get()));
                return;
            }
            equipArmorPool(mob, SLOT_POOL_COMMON_SOOT, random);
        } else if (mob instanceof AbstractSkeleton) {
            Item gun = skelegun(soot, random);
            Map<EquipmentSlot, List<WeightedItem>> pool = new EnumMap<>(SLOT_POOL_RANGED_SOOT);
            pool.put(
                    EquipmentSlot.MAINHAND,
                    gun == null ? List.of(none(50)) : List.of(none(50), new WeightedItem(gun, 1)));
            equipArmorPool(mob, pool, random);
            if (gun != null && mob.getMainHandItem().is(gun)) addFireTask(mob);
        }
    }

    public static @Nullable Item skelegun(float soot, Random random) {
        if (!MobData.ENABLE_MOB_WEAPONS.get()) return null;
        soot -= (float) MobData.WEAPON_SOOT_REDUCTION.get().doubleValue();
        if (random.nextDouble() > Math.log(soot) * 0.25) return null;
        List<WeightedItem> pool;
        if (soot < 0.3) pool = List.of(w(ModItems.GUN_PEPPERBOX, 5), none(20));
        else if (soot > 0.3 && soot < 1) pool = SLOT_POOL_GUNS.get(0.3D);
        else if (soot < 3) pool = SLOT_POOL_GUNS.get(1D);
        else if (soot < 5) pool = SLOT_POOL_GUNS.get(3D);
        else pool = SLOT_POOL_GUNS.get(5D);
        return choose(pool, random);
    }

    public static void onEntityLoad(Entity entity) {
        if (entity instanceof Mob mob
                && !mob.level().isClientSide()
                && mob.getMainHandItem().getItem() instanceof ItemGunBaseNT) addFireTask(mob);
    }

    public static void equipArmorPool(
            Mob entity, Map<EquipmentSlot, List<WeightedItem>> pool, Random random) {
        for (Map.Entry<EquipmentSlot, List<WeightedItem>> entry : pool.entrySet()) {
            Item item = choose(entry.getValue(), random);
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);

            if (item == ModItems.GAS_MASK_M65.get()
                    || item == ModItems.GAS_MASK_OLDE.get()
                    || item == ModItems.GAS_MASK_MONO.get()) {
                ArmorUtil.installGasMaskFilter(
                        stack, new ItemStack(ModItems.GAS_MASK_FILTER.get()));
            }
            entity.setItemSlot(entry.getKey(), stack);
        }
    }

    public static void equipGunTier(Mob entity, int tier, Random random) {
        Item[] guns =
                switch (tier) {
                    case 1 ->
                            new Item[] {
                                ModItems.GUN_LIGHT_REVOLVER.get(),
                                ModItems.GUN_GREASEGUN.get(),
                                ModItems.GUN_MARESLEG.get(),
                                ModItems.GUN_FLAREGUN.get()
                            };
                    case 2 ->
                            new Item[] {
                                ModItems.GUN_UZI.get(),
                                ModItems.GUN_MARESLEG.get(),
                                ModItems.GUN_HENRY.get(),
                                ModItems.GUN_HEAVY_REVOLVER.get(),
                                ModItems.GUN_FLAREGUN.get(),
                                ModItems.GUN_STAR_F.get()
                            };
                    case 3 ->
                            new Item[] {
                                ModItems.GUN_G3.get(),
                                ModItems.GUN_SPAS12.get(),
                                ModItems.GUN_CARBINE.get(),
                                ModItems.GUN_STAR_F.get(),
                                ModItems.GUN_AM180.get(),
                                ModItems.GUN_AMAT.get()
                            };
                    default ->
                            throw new IllegalArgumentException(
                                    "Unknown logic-block gun tier: " + tier);
                };
        int[] weights =
                switch (tier) {
                    case 1 -> new int[] {16, 8, 2, 1};
                    case 2 -> new int[] {12, 8, 12, 8, 4, 8};
                    case 3 -> new int[] {25, 20, 15, 20, 6, 5};
                    default ->
                            throw new IllegalArgumentException(
                                    "Unknown logic-block gun tier: " + tier);
                };
        int total = 0;
        for (int weight : weights) total += weight;
        int pick = random.nextInt(total);
        for (int index = 0; index < guns.length; index++) {
            pick -= weights[index];
            if (pick < 0) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(guns[index]));
                return;
            }
        }
        throw new IllegalStateException("Weighted gun selection exhausted");
    }

    public static void addFireTask(Mob entity) {
        addFireTask(entity, new EntityAIFireGun(entity));
    }

    public static void addFireTask(Mob entity, EntityAIFireGun gunTask) {
        entity.setDropChance(EquipmentSlot.MAINHAND, 0F);

        GoalSelector goalSelector = entity.getGoalSelector();
        if (goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof EntityAIFireGun)) return;
        goalSelector.addGoal(3, gunTask);
    }

    public static double oldAiWalkSpeed(double legacy) {
        return Math.sqrt(0.1D * Math.min(0.98D * legacy, 1D));
    }

    private static WeightedItem w(RegistryHandle<? extends Item> item, int weight) {
        return new WeightedItem(item.get(), weight);
    }

    private static Map<EquipmentSlot, List<WeightedItem>> withMainHand(
            Map<EquipmentSlot, List<WeightedItem>> armor, List<WeightedItem> mainHand) {
        Map<EquipmentSlot, List<WeightedItem>> pool = new EnumMap<>(armor);
        pool.put(EquipmentSlot.MAINHAND, mainHand);
        return Map.copyOf(pool);
    }

    private static Map<EquipmentSlot, List<WeightedItem>> soot(
            Map<EquipmentSlot, List<WeightedItem>> gob,
            int mainHand,
            int feet,
            int legs,
            int chest,
            int head) {
        Map<EquipmentSlot, List<WeightedItem>> pool = new EnumMap<>(EquipmentSlot.class);
        int[] nulls = {mainHand, feet, legs, chest, head};
        EquipmentSlot[] slots = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
        };
        for (int i = 0; i < slots.length; i++) {
            List<WeightedItem> items = gob.get(slots[i]);
            if (items == null) continue;
            List<WeightedItem> choices =
                    new ArrayList<>(items.stream().filter(c -> c.item() != null).toList());
            choices.add(none(nulls[i]));
            pool.put(slots[i], List.copyOf(choices));
        }
        return Collections.unmodifiableMap(pool);
    }

    private static WeightedItem none(int weight) {
        return new WeightedItem(null, weight);
    }

    private static Item choose(List<WeightedItem> choices, Random random) {
        int sum = choices.stream().mapToInt(WeightedItem::weight).sum();
        int pick = random.nextInt(sum);
        for (WeightedItem choice : choices) if ((pick -= choice.weight()) < 0) return choice.item();
        throw new IllegalStateException("Weighted armor selection exhausted");
    }

    public record WeightedItem(Item item, int weight) {}
}
