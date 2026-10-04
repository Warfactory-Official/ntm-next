// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public final class SuitProtections {
    private static Reference2IntOpenHashMap<Item> head = emptyPiece(),
            chest = emptyPiece(),
            legs = emptyPiece(),
            feet = emptyPiece();
    private static long[] headBits = new long[0],
            chestBits = new long[0],
            legBits = new long[0],
            footBits = new long[0];
    private static int words;
    private static int[] kinds = new int[0];
    private static double[] maxHealth = new double[0];
    private static Holder<MobEffect>[] effects = new Holder[0];
    private static ReferenceOpenHashSet<Item>[] cladding = new ReferenceOpenHashSet[0];
    private static int[] effectOnly = new int[0];

    private SuitProtections() {}

    private static Reference2IntOpenHashMap<Item> emptyPiece() {
        var map = new Reference2IntOpenHashMap<Item>();
        map.defaultReturnValue(-1);
        return map;
    }

    public static void applyDataPack(RegistryAccess registries) {
        var rows = registries.lookupOrThrow(SuitProtection.REGISTRY).listElements().toList();
        int nextWords = Math.ceilDiv(rows.size(), Long.SIZE);
        Reference2IntOpenHashMap<Item>[] pieces =
                new Reference2IntOpenHashMap[] {
                    emptyPiece(), emptyPiece(), emptyPiece(), emptyPiece()
                };
        long[][] bits = new long[4][];
        int[] nextKinds = new int[rows.size()];
        double[] nextHealth = new double[rows.size()];
        Holder<MobEffect>[] nextEffects = new Holder[rows.size()];
        ReferenceOpenHashSet<Item>[] nextCladding = new ReferenceOpenHashSet[rows.size()];
        var onlyEffects = new ArrayList<Integer>();
        for (var holder : rows) {
            if (holder.value().armor().isEmpty()) continue;
            var armor = holder.value().armor().get();
            var selectors =
                    java.util.List.of(armor.head(), armor.chest(), armor.legs(), armor.feet());
            for (int slot = 0; slot < 4; slot++) {
                for (var item : selectors.get(slot)) {
                    if (item.value() == net.minecraft.world.item.Items.AIR)
                        throw new IllegalStateException(
                                "A suit selector cannot contain air: " + holder.key().identifier());
                    if (!pieces[slot].containsKey(item.value()))
                        pieces[slot].put(item.value(), pieces[slot].size());
                }
            }
        }
        for (int slot = 0; slot < 4; slot++)
            bits[slot] = new long[Math.multiplyExact(pieces[slot].size(), nextWords)];
        for (int index = 0; index < rows.size(); index++) {
            var rule = rows.get(index).value();
            for (var kind : rule.protections()) nextKinds[index] |= kind.bit();
            nextHealth[index] = rule.maxHealth();
            nextEffects[index] = rule.effect().orElse(null);
            if (rule.claddingIfModified().isPresent()) {
                nextCladding[index] = new ReferenceOpenHashSet<>();
                for (var item : rule.claddingIfModified().get())
                    nextCladding[index].add(item.value());
            }
            if (rule.armor().isEmpty()) {
                onlyEffects.add(index);
                continue;
            }
            var armor = rule.armor().get();
            var selectors =
                    java.util.List.of(armor.head(), armor.chest(), armor.legs(), armor.feet());
            for (int slot = 0; slot < 4; slot++) {
                for (var item : selectors.get(slot)) {
                    bits[slot][pieces[slot].getInt(item.value()) * nextWords + index / Long.SIZE] |=
                            1L << (index % Long.SIZE);
                }
            }
        }
        head = pieces[0];
        chest = pieces[1];
        legs = pieces[2];
        feet = pieces[3];
        headBits = bits[0];
        chestBits = bits[1];
        legBits = bits[2];
        footBits = bits[3];
        words = nextWords;
        kinds = nextKinds;
        maxHealth = nextHealth;
        effects = nextEffects;
        cladding = nextCladding;
        effectOnly = onlyEffects.stream().mapToInt(Integer::intValue).toArray();
    }

    public static boolean protects(LivingEntity entity, SuitProtection.Kind kind) {
        int h = head.getInt(entity.getItemBySlot(EquipmentSlot.HEAD).getItem());
        int c = chest.getInt(entity.getItemBySlot(EquipmentSlot.CHEST).getItem());
        int l = legs.getInt(entity.getItemBySlot(EquipmentSlot.LEGS).getItem());
        int f = feet.getInt(entity.getItemBySlot(EquipmentSlot.FEET).getItem());
        if (h >= 0 && c >= 0 && l >= 0 && f >= 0) {
            for (int word = 0; word < words; word++) {
                long candidates =
                        headBits[h * words + word]
                                & chestBits[c * words + word]
                                & legBits[l * words + word]
                                & footBits[f * words + word];
                while (candidates != 0L) {
                    int index = word * Long.SIZE + Long.numberOfTrailingZeros(candidates);
                    if ((kinds[index] & kind.bit()) != 0 && conditions(entity, index)) return true;
                    candidates &= candidates - 1L;
                }
            }
        }
        for (int index : effectOnly) {
            if ((kinds[index] & kind.bit()) != 0 && conditions(entity, index)) return true;
        }
        return false;
    }

    private static boolean conditions(LivingEntity entity, int index) {
        if (entity.getMaxHealth() >= maxHealth[index]) return false;
        if (effects[index] != null && !entity.hasEffect(effects[index])) return false;
        if (cladding[index] != null) {
            for (var slot : ArmorUtil.ARMOR_SLOTS) {
                var stack = entity.getItemBySlot(slot);
                if (ArmorModHandler.hasMods(stack)
                        && !cladding[index].contains(
                                ArmorModHandler.modItem(stack, ArmorModHandler.CLADDING)))
                    return false;
            }
        }
        return true;
    }

    public static void clear() {
        head = emptyPiece();
        chest = emptyPiece();
        legs = emptyPiece();
        feet = emptyPiece();
        headBits = new long[0];
        chestBits = new long[0];
        legBits = new long[0];
        footBits = new long[0];
        words = 0;
        kinds = new int[0];
        maxHealth = new double[0];
        effects = new Holder[0];
        cladding = new ReferenceOpenHashSet[0];
        effectOnly = new int[0];
    }
}
