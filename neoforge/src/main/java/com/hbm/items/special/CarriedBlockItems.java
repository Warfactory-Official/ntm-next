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

package com.hbm.items.special;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarriedBlockItems {
    private static final Logger LOGGER = LoggerFactory.getLogger("NTM");

    private CarriedBlockItems() {}

    public static @Nullable BlockEntity load(
            BlockState state, BlockPos pos, CompoundTag data, HolderLookup.Provider registries) {
        String owner = state.getBlock().getClass().getName();
        if (!owner.startsWith("net.minecraft.") && !owner.startsWith("com.hbm.")) return null;
        BlockEntity blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER)) {
            blockEntity.loadWithComponents(TagValueInput.create(reporter, registries, data));
        }
        return blockEntity;
    }
}
