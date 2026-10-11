// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.platform.NeoForgeCaps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public final class ForeignItems {

    private ForeignItems() {}

    public static boolean present(Level level, BlockPos pos, @Nullable Direction face) {
        return NeoForgeCaps.findItemStore(level, pos, face) != null;
    }

    public static ItemStack insert(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        ResourceHandler<ItemResource> handler = NeoForgeCaps.findItemStore(level, pos, face);
        return handler == null ? stack : NeoForgeCaps.insert(handler, stack);
    }

    public static int move(
            Level level,
            BlockPos sourcePos,
            @Nullable Direction sourceFace,
            BlockPos destPos,
            @Nullable Direction destFace,
            ItemStack stack,
            int amount) {
        ResourceHandler<ItemResource> source =
                NeoForgeCaps.findItemStore(level, sourcePos, sourceFace);
        if (source == null) return 0;
        ResourceHandler<ItemResource> dest = NeoForgeCaps.findItemStore(level, destPos, destFace);
        return dest == null ? 0 : NeoForgeCaps.move(source, dest, stack, amount);
    }

    public static ItemStack extract(
            Level level, BlockPos pos, @Nullable Direction face, ToIntFunction<ItemStack> request) {
        ResourceHandler<ItemResource> handler = NeoForgeCaps.findItemStore(level, pos, face);
        return handler == null ? ItemStack.EMPTY : NeoForgeCaps.extract(handler, request);
    }

    public static List<ItemStack> contents(Level level, BlockPos pos, @Nullable Direction face) {
        ResourceHandler<ItemResource> handler = NeoForgeCaps.findItemStore(level, pos, face);
        return handler == null ? new ArrayList<>() : NeoForgeCaps.contents(handler);
    }
}
