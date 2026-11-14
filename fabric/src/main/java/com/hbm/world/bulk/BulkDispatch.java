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

package com.hbm.world.bulk;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BulkDispatch {
    public static boolean relevant(int kind, BlockState state) {
        return BulkRegistry.bindings(kind).relevant(BulkRegistry.local(kind), state);
    }

    public static boolean cell(int kind, BlockState state) {
        return BulkRegistry.bindings(kind).cell(BulkRegistry.local(kind), state);
    }

    public static boolean capture(
            int kind,
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            CompoundTag data,
            Consumer<BlockPos> cells) {
        return BulkRegistry.bindings(kind)
                .capture(BulkRegistry.local(kind), level, pos, state, data, cells);
    }

    public static void relocate(int kind, CompoundTag tag, BulkTransform transform) {
        BulkRegistry.bindings(kind).relocate(BulkRegistry.local(kind), tag, transform);
    }

    public static void transform(int kind, CompoundTag tag, BulkTransform transform) {
        BulkRegistry.bindings(kind).transform(BulkRegistry.local(kind), tag, transform);
    }

    public static CompoundTag publicData(int kind, CompoundTag tag) {
        return BulkRegistry.bindings(kind).publicData(BulkRegistry.local(kind), tag);
    }

    public static void blockPublished(
            int kind, ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {
        BulkRegistry.bindings(kind)
                .blockPublished(BulkRegistry.local(kind), level, pos, state, data);
    }

    public static boolean entityCells(
            int kind, BlockEntity be, ServerLevel level, Consumer<BlockPos> cells) {
        return BulkRegistry.bindings(kind).entityCells(BulkRegistry.local(kind), be, level, cells);
    }

    public static void clearInventory(int kind, BlockEntity be) {
        BulkRegistry.bindings(kind).clearInventory(BulkRegistry.local(kind), be);
    }

    public static void forgetDrops(int kind, BlockEntity be) {
        BulkRegistry.bindings(kind).forgetDrops(BulkRegistry.local(kind), be);
    }

    public static void entityPublished(int kind, BlockEntity be) {
        BulkRegistry.bindings(kind).entityPublished(BulkRegistry.local(kind), be);
    }
}
