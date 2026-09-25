// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class ForeignItems {

    private ForeignItems() {}

    public static boolean present(Level level, BlockPos pos, @Nullable Direction face) {
        return handler(level, pos, face) != null;
    }

    public static ItemStack insert(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;

        ResourceHandler<ItemResource> handler = handler(level, pos, face);
        if (handler == null) return stack;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return rest(stack, moved);
        }
    }

    public static ItemStack merge(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;

        ResourceHandler<ItemResource> handler = handler(level, pos, face);
        if (handler == null) return stack;
        ItemResource resource = ItemResource.of(stack);
        int moved = 0;
        try (Transaction tx = Transaction.openRoot()) {
            for (int index = 0; index < handler.size() && moved < stack.getCount(); index++) {
                if (handler.getResource(index).equals(resource)) {
                    moved += handler.insert(index, resource, stack.getCount() - moved, tx);
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

    public static ItemStack extract(
            Level level,
            BlockPos pos,
            @Nullable Direction face,
            Predicate<ItemStack> accept,
            int max) {
        if (max <= 0) return ItemStack.EMPTY;

        ResourceHandler<ItemResource> handler = handler(level, pos, face);
        if (handler == null) return ItemStack.EMPTY;
        for (int index = 0; index < handler.size(); index++) {
            ItemResource resource = handler.getResource(index);
            int amount = handler.getAmountAsInt(index);
            if (resource.isEmpty() || amount <= 0) continue;
            if (!accept.test(resource.toStack(amount))) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.extract(index, resource, Math.min(max, amount), tx);
                tx.commit();
                if (moved > 0) return resource.toStack(moved);
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<ItemStack> contents(Level level, BlockPos pos, @Nullable Direction face) {
        List<ItemStack> out = new ArrayList<>();

        ResourceHandler<ItemResource> handler = handler(level, pos, face);
        if (handler == null) return out;
        for (int index = 0; index < handler.size(); index++) {
            ItemResource resource = handler.getResource(index);
            int amount = handler.getAmountAsInt(index);
            if (resource.isEmpty() || amount <= 0) continue;
            out.add(resource.toStack(amount));
        }

        return out;
    }

    private static @Nullable ResourceHandler<ItemResource> handler(
            Level level, BlockPos pos, @Nullable Direction face) {
        ResourceHandler<ItemResource> handler =
                level.getCapability(Capabilities.Item.BLOCK, pos, face);
        if (handler != null) return handler;
        Container container = InventoryUtil.containerAt(level, pos);
        if (container == null) return null;
        return face != null && container instanceof WorldlyContainer worldly
                ? new WorldlyContainerWrapper(worldly, face)
                : VanillaContainerWrapper.of(container);
    }
}
