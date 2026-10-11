// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.platform.FabricCaps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ForeignItems {

    private ForeignItems() {}

    public static boolean present(Level level, BlockPos pos, @Nullable Direction face) {
        return ItemStorage.SIDED.find(level, pos, face) != null;
    }

    public static ItemStack insert(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        Storage<ItemVariant> handler = ItemStorage.SIDED.find(level, pos, face);
        return handler == null ? stack : FabricCaps.insert(handler, stack);
    }

    public static int move(
            Level level,
            BlockPos sourcePos,
            @Nullable Direction sourceFace,
            BlockPos destPos,
            @Nullable Direction destFace,
            ItemStack stack,
            int amount) {
        Storage<ItemVariant> source = ItemStorage.SIDED.find(level, sourcePos, sourceFace);
        if (source == null) return 0;
        Storage<ItemVariant> dest = ItemStorage.SIDED.find(level, destPos, destFace);
        return dest == null ? 0 : FabricCaps.move(source, dest, stack, amount);
    }

    public static ItemStack extract(
            Level level, BlockPos pos, @Nullable Direction face, ToIntFunction<ItemStack> request) {
        Storage<ItemVariant> handler = ItemStorage.SIDED.find(level, pos, face);
        return handler == null ? ItemStack.EMPTY : FabricCaps.extract(handler, request);
    }

    public static List<ItemStack> contents(Level level, BlockPos pos, @Nullable Direction face) {
        Storage<ItemVariant> handler = ItemStorage.SIDED.find(level, pos, face);
        return handler == null ? new ArrayList<>() : FabricCaps.contents(handler);
    }
}
