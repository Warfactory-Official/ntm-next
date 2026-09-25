// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.api.entity.IResistanceProvider;
import com.hbm.entity.mob.EntityCreeperNuclear;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class DamageResistanceHandler {

    public static final String CATEGORY_EXPLOSION = "EXPL";
    public static final String CATEGORY_FIRE = "FIRE";
    public static final String CATEGORY_PHYSICAL = "PHYS";
    public static final String CATEGORY_ENERGY = "EN";

    public static final String EXACT_FALL = "fall";

    private static final HashMap<Item, ResistanceStats> itemStats = new HashMap<>();
    private static final HashMap<ArmorSet, ResistanceStats> setStats = new HashMap<>();
    private static final HashMap<Class<? extends Entity>, ResistanceStats> entityStats =
            new HashMap<>();

    private DamageResistanceHandler() {}

    public static void init() {
        clearSystem();
        initDefaults();
    }

    private static void clearSystem() {
        itemStats.clear();
        setStats.clear();
        entityStats.clear();
    }

    private static void initDefaults() {
        entityStats.put(
                Creeper.class, new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        entityStats.put(
                EntityCreeperNuclear.class,
                new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 5F, 0.35F));
    }

    public static void registerItem(Item item, ResistanceStats stats) {
        itemStats.put(item, stats);
    }

    public static ResistanceStats getItemResistance(Item item) {
        return itemStats.get(item);
    }

    public static void registerSet(
            Item helmet, Item plate, Item legs, Item boots, ResistanceStats stats) {
        setStats.put(new ArmorSet(helmet, plate, legs, boots), stats);
    }

    public static ResistanceStats getSetResistanceContaining(Item item) {
        for (Map.Entry<ArmorSet, ResistanceStats> entry : setStats.entrySet()) {
            ArmorSet set = entry.getKey();
            if (item == set.helmet()
                    || item == set.plate()
                    || item == set.legs()
                    || item == set.boots()) return entry.getValue();
        }
        return null;
    }

    public static Item[] getSetItemsContaining(Item item) {
        for (ArmorSet set : setStats.keySet()) {
            if (item == set.helmet()
                    || item == set.plate()
                    || item == set.legs()
                    || item == set.boots())
                return new Item[] {set.helmet(), set.plate(), set.legs(), set.boots()};
        }
        return null;
    }

    public static void addInfo(ItemStack stack, Consumer<Component> adder) {
        Item item = stack.getItem();
        List<Component> setLines = resistLines(getSetResistanceContaining(item));
        if (!setLines.isEmpty()) {
            adder.accept(
                    Component.translatable("damage.inset").withStyle(ChatFormatting.DARK_PURPLE));
            Item[] siblings = getSetItemsContaining(item);
            if (siblings != null) {
                for (Item sibling : siblings) {
                    if (sibling == null) continue;
                    adder.accept(
                            Component.literal("  ")
                                    .append(sibling.getDefaultInstance().getHoverName())
                                    .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            setLines.forEach(adder);
        }

        List<Component> itemLines = resistLines(getItemResistance(item));
        if (!itemLines.isEmpty()) {
            adder.accept(
                    Component.translatable("damage.item").withStyle(ChatFormatting.DARK_PURPLE));
            itemLines.forEach(adder);
        }
    }

    private static List<Component> resistLines(@Nullable ResistanceStats stats) {
        List<Component> lines = new ArrayList<>();
        if (stats == null) return lines;
        stats.categories()
                .forEach((key, res) -> lines.add(resistLine("damage.category." + key, res)));
        stats.exacts().forEach((key, res) -> lines.add(resistLine("damage.exact." + key, res)));
        if (stats.other() != null) lines.add(resistLine("damage.other", stats.other()));
        return lines;
    }

    private static Component resistLine(String key, Resistance resistance) {
        return Component.translatable(key)
                .append(
                        ": "
                                + resistance.threshold()
                                + "/"
                                + (int) (resistance.resistance() * 100F)
                                + "%")
                .withStyle(ChatFormatting.GRAY);
    }

    public static void registerEntity(Class<? extends Entity> clazz, ResistanceStats stats) {
        entityStats.put(clazz, stats);
    }

    public static float calculateDamage(
            LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {

        if (damage.is(DamageTypeTags.BYPASSES_EFFECTS)) return amount;

        float[] vals = getDTDR(entity, damage, amount, pierceDT, pierce);
        float dt = vals[0];
        float dr = vals[1];

        dt = Math.max(0F, dt - pierceDT);
        if (dt >= amount) return 0F;
        amount -= dt;

        dr *= Mth.clamp(1F - pierce, 0F, 2F);

        return amount * (1F - dr);
    }

    public static float[] getDTDR(
            LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {

        float dt = 0;
        float dr = 0;

        if (entity instanceof IResistanceProvider irp) {
            float[] res = irp.getCurrentDTDR(damage, amount, pierceDT, pierce);
            dt += res[0];
            dr += res[1];
        }

        ArmorSet wornSet =
                new ArmorSet(
                        itemOrNull(entity, EquipmentSlot.HEAD),
                        itemOrNull(entity, EquipmentSlot.CHEST),
                        itemOrNull(entity, EquipmentSlot.LEGS),
                        itemOrNull(entity, EquipmentSlot.FEET));

        ResistanceStats setResistance = setStats.get(wornSet);
        if (setResistance != null) {
            Resistance res = setResistance.getResistance(damage);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }

        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            ResistanceStats stats = itemStats.get(armor.getItem());
            if (stats == null) continue;
            Resistance res = stats.getResistance(damage);
            if (res == null) continue;
            dt += res.threshold;
            dr += res.resistance;
        }

        ResistanceStats innateResistance = entityStats.get(entity.getClass());
        if (innateResistance != null) {
            Resistance res = innateResistance.getResistance(damage);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }

        return new float[] {dt, dr};
    }

    private static Item itemOrNull(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        return stack.isEmpty() ? null : stack.getItem();
    }

    public static String typeToCategory(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return CATEGORY_EXPLOSION;
        if (source.is(DamageTypeTags.IS_FIRE)) return CATEGORY_FIRE;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return CATEGORY_PHYSICAL;
        if (source.is(ModDamageTypes.EXPLOSIVE)) return CATEGORY_EXPLOSION;
        if (source.is(ModDamageTypes.FIRE)) return CATEGORY_FIRE;
        if (source.is(ModDamageTypes.PHYSICAL)) return CATEGORY_PHYSICAL;
        if (source.is(ModDamageTypes.LASER)) return CATEGORY_ENERGY;
        if (source.is(ModDamageTypes.PLASMA)) return CATEGORY_ENERGY;
        if (source.is(ModDamageTypes.MICROWAVE)) return CATEGORY_ENERGY;
        if (source.is(ModDamageTypes.SUBATOMIC)) return CATEGORY_ENERGY;
        if (source.is(ModDamageTypes.ELECTRIC)) return CATEGORY_ENERGY;
        if (source.is(DamageTypes.CACTUS)) return CATEGORY_PHYSICAL;
        if (source.is(ModDamageTypes.SPIKES)) return CATEGORY_PHYSICAL;
        if (source.is(ModDamageTypes.ELECTRICITY)) return CATEGORY_ENERGY;

        if (source.getEntity() != null || source.getDirectEntity() != null)
            return CATEGORY_PHYSICAL;
        return source.getMsgId();
    }

    private record ArmorSet(Item helmet, Item plate, Item legs, Item boots) {}

    public static final class ResistanceStats {

        final Map<String, Resistance> exactResistances = new LinkedHashMap<>();
        final Map<String, Resistance> categoryResistances = new LinkedHashMap<>();
        Resistance otherResistance;

        Resistance getResistance(DamageSource source) {
            Resistance exact = exactResistances.get(source.getMsgId().toLowerCase(Locale.US));
            if (exact != null) return exact;
            Resistance category = categoryResistances.get(typeToCategory(source));
            if (category != null) return category;
            return source.is(DamageTypeTags.BYPASSES_ARMOR) ? null : otherResistance;
        }

        public ResistanceStats addExact(String type, float threshold, float resistance) {
            exactResistances.put(
                    type.toLowerCase(Locale.US), new Resistance(threshold, resistance));
            return this;
        }

        public ResistanceStats addCategory(String type, float threshold, float resistance) {
            categoryResistances.put(type, new Resistance(threshold, resistance));
            return this;
        }

        public ResistanceStats setOther(float threshold, float resistance) {
            otherResistance = new Resistance(threshold, resistance);
            return this;
        }

        public Map<String, Resistance> categories() {
            return categoryResistances;
        }

        public Map<String, Resistance> exacts() {
            return exactResistances;
        }

        public Resistance fire() {
            return categoryResistances.get(CATEGORY_FIRE);
        }

        public Resistance physical() {
            return categoryResistances.get(CATEGORY_PHYSICAL);
        }

        public Resistance explosive() {
            return categoryResistances.get(CATEGORY_EXPLOSION);
        }

        public Resistance fall() {
            return exactResistances.get(EXACT_FALL);
        }

        public Resistance other() {
            return otherResistance;
        }
    }

    public record Resistance(float threshold, float resistance) {}
}
