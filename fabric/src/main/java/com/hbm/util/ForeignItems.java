/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.util;

import com.hbm.platform.Caps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
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
        return Caps.findItemStore(level, pos, face) != null;
    }

    public static ItemStack insert(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        Storage<ItemVariant> handler = Caps.findItemStore(level, pos, face);
        return handler == null ? stack : Caps.insert(handler, stack);
    }

    public static int move(
            Level level,
            BlockPos sourcePos,
            @Nullable Direction sourceFace,
            BlockPos destPos,
            @Nullable Direction destFace,
            ItemStack stack,
            int amount) {
        Storage<ItemVariant> source = Caps.findItemStore(level, sourcePos, sourceFace);
        if (source == null) return 0;
        Storage<ItemVariant> dest = Caps.findItemStore(level, destPos, destFace);
        return dest == null ? 0 : Caps.move(source, dest, stack, amount);
    }

    public static ItemStack extract(
            Level level, BlockPos pos, @Nullable Direction face, ToIntFunction<ItemStack> request) {
        Storage<ItemVariant> handler = Caps.findItemStore(level, pos, face);
        return handler == null ? ItemStack.EMPTY : Caps.extract(handler, request);
    }

    public static List<ItemStack> contents(Level level, BlockPos pos, @Nullable Direction face) {
        Storage<ItemVariant> handler = Caps.findItemStore(level, pos, face);
        return handler == null ? new ArrayList<>() : Caps.contents(handler);
    }
}
