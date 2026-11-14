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

package com.hbm.handler.resistance;

import com.hbm.api.entity.DamageResistanceProvider.Protection;
import com.hbm.api.entity.DamageResistanceProvider;
import com.hbm.entity.mob.CreeperNuclearEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class DamageResistanceHandler {

    private static volatile Tables tables = Tables.EMPTY;
    private static final ConcurrentHashMap<String, String> LOWER_CASE = new ConcurrentHashMap<>();

    private DamageResistanceHandler() {}

    public static synchronized void init() {
        tables = Tables.EMPTY;
        registerEntity(
                Creeper.class,
                DamageResistanceProfile.builder().category(DamageCategory.EXPL, 2F, 0.25F).build());
        registerEntity(
                CreeperNuclearEntity.class,
                DamageResistanceProfile.builder().category(DamageCategory.EXPL, 5F, 0.35F).build());
    }

    public static synchronized void registerSet(
            @Nullable Item head,
            @Nullable Item chest,
            @Nullable Item legs,
            @Nullable Item feet,
            DamageResistanceProfile profile) {
        var row = new SetRow(worn(head), worn(chest), worn(legs), worn(feet), profile);
        var sets = new ArrayList<>(tables.sets);
        int at = 0;
        while (at < sets.size() && !sets.get(at).sameKey(row)) at++;
        if (at < sets.size()) sets.set(at, row);
        else sets.add(row);
        tables = tables.withSets(sets);
    }

    public static synchronized void registerItem(Item item, DamageResistanceProfile profile) {
        var items = new HashMap<>(tables.items);
        items.put(item, profile);
        tables = tables.withItems(items);
    }

    public static synchronized void registerEntity(
            Class<? extends Entity> type, DamageResistanceProfile profile) {
        var entities = new HashMap<>(tables.entities);
        entities.put(type, profile);
        tables = tables.withEntities(entities);
    }

    public static boolean refuses(
            LivingEntity entity,
            DamageSource source,
            float amount,
            float pierceThreshold,
            float pierceResistance) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) return false;
        long sum = sum(entity, source, amount, pierceThreshold, pierceResistance);
        float threshold = threshold(sum) - pierceThreshold;
        float resistance = resistance(sum) - pierceResistance;
        return threshold > 0F && threshold >= amount || resistance >= 1F;
    }

    public static float reduce(
            LivingEntity entity,
            DamageSource source,
            float amount,
            float pierceThreshold,
            float pierceResistance) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) return amount;
        long sum = sum(entity, source, amount, pierceThreshold, pierceResistance);
        float threshold = Math.max(0F, threshold(sum) - pierceThreshold);
        if (threshold >= amount) return 0F;
        float resistance = resistance(sum) * Mth.clamp(1F - pierceResistance, 0F, 2F);
        return (amount - threshold) * (1F - resistance);
    }

    public static float reduceAndNotify(
            LivingEntity entity,
            DamageSource source,
            float amount,
            float pierceThreshold,
            float pierceResistance) {
        float result = reduce(entity, source, amount, pierceThreshold, pierceResistance);
        if (entity instanceof DamageResistanceProvider own) own.onDamageTaken(source, result);
        return result;
    }

    public static Protection protection(
            LivingEntity entity,
            DamageSource source,
            float amount,
            float pierceThreshold,
            float pierceResistance) {
        long sum = sum(entity, source, amount, pierceThreshold, pierceResistance);
        return new Protection(threshold(sum), resistance(sum));
    }

    public static void appendTooltip(ItemStack stack, Consumer<Component> out) {
        TooltipRow row = tooltip(stack.getItem());
        if (row == null) return;
        SetRow set = row.set();
        if (set != null && set.profile().lineCount() > 0) {
            out.accept(
                    Component.translatable("damage.inset").withStyle(ChatFormatting.DARK_PURPLE));
            for (int slot = 0; slot < 4; slot++) {
                Item member = set.member(slot);
                if (member == Items.AIR) continue;
                out.accept(
                        Component.literal("  ")
                                .append(member.getDefaultInstance().getHoverName())
                                .withStyle(ChatFormatting.DARK_PURPLE));
            }
            appendTooltipLines(set.profile(), out);
        }
        DamageResistanceProfile own = row.item();
        if (own != null && own.lineCount() > 0) {
            out.accept(Component.translatable("damage.item").withStyle(ChatFormatting.DARK_PURPLE));
            appendTooltipLines(own, out);
        }
    }

    private static void appendTooltipLines(
            DamageResistanceProfile profile, Consumer<Component> out) {
        for (int line = 0; line < profile.lineCount(); line++) {
            out.accept(
                    Component.translatable(profile.lineKey(line))
                            .append(profile.lineValue(line))
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    static @Nullable TooltipRow tooltip(Item item) {
        return tables.tooltips.get(item);
    }

    private static long sum(
            LivingEntity entity,
            DamageSource source,
            float amount,
            float pierceThreshold,
            float pierceResistance) {
        float threshold = 0F;
        float resistance = 0F;
        if (entity instanceof DamageResistanceProvider own) {
            Protection innate =
                    own.protectionAgainst(source, amount, pierceThreshold, pierceResistance);
            threshold += innate.threshold();
            resistance += innate.resistance();
        }

        Tables current = tables;
        Item head = entity.getItemBySlot(EquipmentSlot.HEAD).getItem();
        Item chest = entity.getItemBySlot(EquipmentSlot.CHEST).getItem();
        Item legs = entity.getItemBySlot(EquipmentSlot.LEGS).getItem();
        Item feet = entity.getItemBySlot(EquipmentSlot.FEET).getItem();
        DamageResistanceProfile set = current.set(head, chest, legs, feet);
        DamageResistanceProfile onFeet = current.items.get(feet);
        DamageResistanceProfile onLegs = current.items.get(legs);
        DamageResistanceProfile onChest = current.items.get(chest);
        DamageResistanceProfile onHead = current.items.get(head);
        DamageResistanceProfile innate = current.entities.get(entity.getClass());
        if (set == null
                && onFeet == null
                && onLegs == null
                && onChest == null
                && onHead == null
                && innate == null) {
            return pack(threshold, resistance);
        }

        String id = lowerCase(source.getMsgId());
        DamageCategory category = DamageCategory.of(source);
        boolean bypassesArmor = source.is(DamageTypeTags.BYPASSES_ARMOR);
        for (int i = 0; i < 6; i++) {
            DamageResistanceProfile profile =
                    switch (i) {
                        case 0 -> set;
                        case 1 -> onFeet;
                        case 2 -> onLegs;
                        case 3 -> onChest;
                        case 4 -> onHead;
                        default -> innate;
                    };
            if (profile == null) continue;
            Protection entry = profile.against(id, category, bypassesArmor);
            if (entry == null) continue;
            threshold += entry.threshold();
            resistance += entry.resistance();
        }
        return pack(threshold, resistance);
    }

    private static String lowerCase(String messageId) {
        String lower = LOWER_CASE.get(messageId);
        return lower != null
                ? lower
                : LOWER_CASE.computeIfAbsent(messageId, id -> id.toLowerCase(Locale.ROOT));
    }

    private static long pack(float threshold, float resistance) {
        return (long) Float.floatToRawIntBits(threshold) << 32
                | Float.floatToRawIntBits(resistance) & 0xFFFFFFFFL;
    }

    private static float threshold(long sum) {
        return Float.intBitsToFloat((int) (sum >>> 32));
    }

    private static float resistance(long sum) {
        return Float.intBitsToFloat((int) sum);
    }

    private static Item worn(@Nullable Item item) {
        return item != null ? item : Items.AIR;
    }

    record SetRow(Item head, Item chest, Item legs, Item feet, DamageResistanceProfile profile) {
        boolean sameKey(SetRow other) {
            return head == other.head
                    && chest == other.chest
                    && legs == other.legs
                    && feet == other.feet;
        }

        Item member(int slot) {
            return switch (slot) {
                case 0 -> head;
                case 1 -> chest;
                case 2 -> legs;
                default -> feet;
            };
        }
    }

    record TooltipRow(@Nullable SetRow set, @Nullable DamageResistanceProfile item) {}

    private record Tables(
            List<SetRow> sets,
            Map<Item, SetRow[]> byHead,
            Map<Item, DamageResistanceProfile> items,
            Map<Class<?>, DamageResistanceProfile> entities,
            Map<Item, TooltipRow> tooltips) {
        static final Tables EMPTY = new Tables(List.of(), Map.of(), Map.of(), Map.of(), Map.of());

        @Nullable DamageResistanceProfile set(Item head, Item chest, Item legs, Item feet) {
            SetRow[] rows = byHead.get(head);
            if (rows == null) return null;
            for (SetRow row : rows) {
                if (row.chest == chest && row.legs == legs && row.feet == feet) return row.profile;
            }
            return null;
        }

        Tables withSets(List<SetRow> sets) {
            var byHead = new HashMap<Item, List<SetRow>>();
            for (SetRow row : sets)
                byHead.computeIfAbsent(row.head, _ -> new ArrayList<>())
                        .add(row);
            var index = new HashMap<Item, SetRow[]>();
            byHead.forEach((item, rows) -> index.put(item, rows.toArray(new SetRow[0])));
            return new Tables(
                    List.copyOf(sets), Map.copyOf(index), items, entities, tooltips(sets, items));
        }

        Tables withItems(Map<Item, DamageResistanceProfile> items) {
            return new Tables(sets, byHead, Map.copyOf(items), entities, tooltips(sets, items));
        }

        Tables withEntities(Map<Class<?>, DamageResistanceProfile> entities) {
            return new Tables(sets, byHead, items, Map.copyOf(entities), tooltips);
        }

        private static Map<Item, TooltipRow> tooltips(
                List<SetRow> sets, Map<Item, DamageResistanceProfile> items) {
            var firstSet = new HashMap<Item, SetRow>();
            for (SetRow row : sets) {
                for (int slot = 0; slot < 4; slot++) {
                    Item member = row.member(slot);
                    if (member != Items.AIR) firstSet.putIfAbsent(member, row);
                }
            }
            var rows = new HashMap<Item, TooltipRow>();
            firstSet.forEach((item, row) -> rows.put(item, new TooltipRow(row, items.get(item))));
            items.forEach((item, profile) -> rows.putIfAbsent(item, new TooltipRow(null, profile)));
            return Map.copyOf(rows);
        }
    }
}
