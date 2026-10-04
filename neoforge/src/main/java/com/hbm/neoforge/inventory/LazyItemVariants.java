// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LazyItemVariants extends AbstractQueue<ItemStack> {
    private ItemStack[] prototypes = new ItemStack[16];
    private int[] damageRanges = new int[16];
    private int count;
    private int itemIndex;
    private int damage;
    private long available;
    private ItemStack polled;

    public LazyItemVariants() {}

    public void register(Item item) {
        if (item == null || item == Items.AIR) return;
        assert polled == null;
        if (count == prototypes.length) {
            prototypes = Arrays.copyOf(prototypes, count * 2);
            damageRanges = Arrays.copyOf(damageRanges, count * 2);
        }
        ItemStack prototype = item.getDefaultInstance();
        int range = prototype.isDamageableItem() ? prototype.getMaxDamage() : 0;
        prototypes[count] = prototype;
        damageRanges[count++] = range;
        available = Math.addExact(available, Math.max(1, range));
    }

    @Override
    public int size() {
        return (int) Math.min(available, Integer.MAX_VALUE);
    }

    @Override
    public ItemStack poll() {
        if (available == 0) return null;
        assert polled == null;
        polled = variant(itemIndex, damage);
        available--;
        int range = damageRanges[itemIndex];
        if (range > 0 && damage + 1 < range) damage++;
        else {
            damage = 0;
            itemIndex = (itemIndex + 1) % count;
        }
        return polled;
    }

    @Override
    public boolean offer(ItemStack item) {
        assert polled != null && item == polled;
        available++;
        polled = null;
        return true;
    }

    @Override
    public ItemStack peek() {
        return available == 0 ? null : variant(itemIndex, damage);
    }

    private ItemStack variant(int index, int damage) {
        ItemStack prototype = prototypes[index];
        if (damageRanges[index] == 0) return prototype;
        ItemStack variant = prototype.copy();
        variant.setDamageValue(damage);
        return variant;
    }

    @Override
    public void clear() {
        Arrays.fill(prototypes, 0, count, null);
        Arrays.fill(damageRanges, 0, count, 0);
        count = itemIndex = damage = 0;
        available = 0;
        polled = null;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private int index = itemIndex;
            private int nextDamage = damage;
            private long remaining = available;

            @Override
            public boolean hasNext() {
                return remaining != 0;
            }

            @Override
            public ItemStack next() {
                if (remaining == 0) throw new NoSuchElementException();
                ItemStack item = variant(index, nextDamage);
                remaining--;
                int range = damageRanges[index];
                if (range > 0 && nextDamage + 1 < range) nextDamage++;
                else {
                    nextDamage = 0;
                    index = (index + 1) % count;
                }
                return item;
            }
        };
    }
}
