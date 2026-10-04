// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ForeignItems {

    private ForeignItems() {}

    public static boolean present(Level level, BlockPos pos, @Nullable Direction face) {
        return handler(level, pos, face) != null;
    }

    public static ItemStack insert(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        Storage<ItemVariant> storage = handler(level, pos, face);
        if (storage == null) return stack;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
            tx.commit();
            return rest(stack, moved);
        }
    }

    public static ItemStack merge(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        Storage<ItemVariant> storage = handler(level, pos, face);
        if (storage == null) return stack;
        ItemVariant variant = ItemVariant.of(stack);
        Iterable<? extends StorageView<ItemVariant>> views =
                storage instanceof SlottedStorage<ItemVariant> slotted
                        ? slotted.getSlots()
                        : storage.nonEmptyViews();
        long moved = 0;
        try (Transaction tx = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : views) {
                if (moved >= stack.getCount()) break;
                if (view instanceof SingleSlotStorage<ItemVariant> slot
                        && slot.getResource().equals(variant)) {
                    moved += slot.insert(variant, stack.getCount() - moved, tx);
                }
            }
            tx.commit();
        }
        return rest(stack, moved);
    }

    private static ItemStack rest(ItemStack stack, long moved) {
        return moved >= stack.getCount()
                ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - (int) moved);
    }

    public static int move(
            Level level,
            BlockPos sourcePos,
            @Nullable Direction sourceFace,
            BlockPos destPos,
            @Nullable Direction destFace,
            ItemStack stack,
            int amount) {
        ItemVariant resource = ItemVariant.of(stack);
        return (int)
                StorageUtil.move(
                        handler(level, sourcePos, sourceFace),
                        handler(level, destPos, destFace),
                        candidate -> candidate.equals(resource),
                        amount,
                        null);
    }

    public static ItemStack extract(
            Level level, BlockPos pos, @Nullable Direction face, ToIntFunction<ItemStack> request) {
        Storage<ItemVariant> storage = handler(level, pos, face);
        if (storage == null) return ItemStack.EMPTY;
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            ItemVariant resource = view.getResource();
            ItemStack held = resource.toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
            int wanted = request.applyAsInt(held);
            if (wanted == 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long moved = view.extract(resource, Math.min(wanted, held.getCount()), tx);
                if (moved > 0) {
                    tx.commit();
                    return resource.toStack((int) moved);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<ItemStack> contents(Level level, BlockPos pos, @Nullable Direction face) {
        List<ItemStack> out = new ArrayList<>();
        Storage<ItemVariant> storage = handler(level, pos, face);
        if (storage == null) return out;
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            out.add(
                    view.getResource()
                            .toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE)));
        }
        return out;
    }

    private static @Nullable Storage<ItemVariant> handler(
            Level level, BlockPos pos, @Nullable Direction face) {
        return ItemStorage.SIDED.find(level, pos, face);
    }
}
