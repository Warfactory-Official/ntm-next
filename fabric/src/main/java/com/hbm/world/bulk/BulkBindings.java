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
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface BulkBindings {
    Class<?>[] types();

    default boolean relevant(int kind, BlockState state) {
        return state.hasBlockEntity();
    }

    default boolean cell(int kind, BlockState state) {
        return false;
    }

    default boolean capture(
            int kind,
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            CompoundTag data,
            Consumer<BlockPos> cells) {
        return false;
    }

    default void relocate(int kind, CompoundTag tag, BulkTransform transform) {}

    default void transform(int kind, CompoundTag tag, BulkTransform transform) {
        relocate(kind, tag, transform);
    }

    default CompoundTag publicData(int kind, CompoundTag tag) {
        return new CompoundTag();
    }

    default void blockPublished(
            int kind, ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {}

    default boolean entityCells(
            int kind, BlockEntity be, ServerLevel level, Consumer<BlockPos> cells) {
        return false;
    }

    default void clearInventory(int kind, BlockEntity be) {
        if (be instanceof RandomizableContainer loot) loot.setLootTable(null);
        if (be instanceof Container inventory) inventory.clearContent();
    }

    default void forgetDrops(int kind, BlockEntity be) {
        clearInventory(kind, be);
    }

    default void entityPublished(int kind, BlockEntity be) {}
}
