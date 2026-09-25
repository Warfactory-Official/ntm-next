// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import net.minecraft.world.item.ItemStack;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class SyncBindings {
    private static final ClassValue<long[]> FIELDS =
            new ClassValue<>() {
                @Override
                protected long[] computeValue(Class<?> type) {
                    LongArrayList fields = new LongArrayList();
                    for (Class<?> current = type;
                            current != null;
                            current = current.getSuperclass()) {
                        for (var field : current.getDeclaredFields()) {
                            SyncField declaration = field.getAnnotation(SyncField.class);
                            if (declaration == null) continue;
                            int mask = declaration.value();
                            if (mask < 1 || mask > 3)
                                throw new IllegalArgumentException("Invalid sync mask " + mask);
                            if (field.getType().isPrimitive()) continue;
                            assert !Modifier.isStatic(field.getModifiers());
                            fields.add(U.objectFieldOffset(field));
                            fields.add(mask);
                            fields.add(declaration.units());
                        }
                    }
                    return fields.toLongArray();
                }
            };

    private SyncBindings() {}

    public static void bindFields(SyncSource owner) {
        bindFields(owner, null);
    }

    public static void bindFields(SyncSource owner, SyncSource parent) {
        long[] fields = FIELDS.get(owner.getClass());
        for (int i = 0; i < fields.length; i += 3) {
            owner.bindSyncValue(
                    U.getReference(owner, fields[i]), (int) fields[i + 1], fields[i + 2]);
        }
        if (owner instanceof SyncInventory inventory) inventory.bindItems();
        else if (owner instanceof SyncList<?> list) {
            for (int i = 0; i < list.size(); i++) bind(owner, list.get(i), 3);
        } else if ((Object) owner instanceof ItemStack stack) {

            if (!(parent instanceof SyncInventory inventory) || inventory.components())
                bind(owner, stack.components, 3);
        }
    }

    public static void unbindFields(SyncSource owner) {
        unbindFields(owner, null);
    }

    public static void unbindFields(SyncSource owner, SyncSource parent) {
        long[] fields = FIELDS.get(owner.getClass());
        for (int i = 0; i < fields.length; i += 3) {
            unbind(owner, U.getReference(owner, fields[i]));
        }
        if (owner instanceof SyncInventory inventory) inventory.unbindItems();
        else if (owner instanceof SyncList<?> list) {
            for (int i = 0; i < list.size(); i++) unbind(owner, list.get(i));
        } else if ((Object) owner instanceof ItemStack stack) {
            if (!(parent instanceof SyncInventory inventory) || inventory.components())
                unbind(owner, stack.components);
        }
    }

    public static void bind(SyncSource owner, Object value, int mask) {
        bindUnits(owner, value, mask, 0);
    }

    public static void bindUnits(SyncSource owner, Object value, int mask, long units) {
        if (value == ItemStack.EMPTY) return;
        if (value instanceof SyncInventory inventory && inventory.units() != 0)
            units = inventory.units();
        if (value instanceof SyncSource child) child.bindSyncUnits(owner, mask, units);
        else if (value instanceof SyncEnvelope envelope) envelope.bindSyncChildren(owner, mask);
        else if (value instanceof Object[] children) {
            for (Object child : children) bindUnits(owner, child, mask, units);
        }
    }

    public static void bindIndexed(
            SyncSource owner, Object value, int mask, int unit, int count, long also) {
        if (unit < 0 || count < 1 || unit > Long.SIZE - count)
            throw new IllegalArgumentException("Invalid sync unit range");
        int length = Array.getLength(value);
        int width = groupWidth(length, count);
        if (value instanceof Object[] children) {
            for (int i = 0; i < length; i++)
                bindUnits(owner, children[i], mask, 1L << (unit + i / width) | also);
        }
    }

    public static int groupWidth(int length, int count) {
        if (count <= 0 || length < count || length % count != 0)
            throw new IllegalArgumentException(
                    "Sync array length " + length + " cannot form " + count + " groups");
        return length / count;
    }

    public static void unbind(SyncSource owner, Object value) {
        if (value == ItemStack.EMPTY) return;
        if (value instanceof SyncSource child) child.unbindSync(owner);
        else if (value instanceof SyncEnvelope envelope) envelope.unbindSyncChildren(owner);
        else if (value instanceof Object[] children) {
            for (Object child : children) unbind(owner, child);
        }
    }
}
