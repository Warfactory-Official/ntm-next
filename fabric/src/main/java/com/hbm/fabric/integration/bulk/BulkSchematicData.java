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

package com.hbm.fabric.integration.bulk;

import com.hbm.world.bulk.BulkSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface BulkSchematicData {
    void hbm$bulkTag(BlockPos pos, CompoundTag tag);

    @Nullable CompoundTag hbm$bulkTag(BlockPos pos);

    default @Nullable CompoundTag hbm$bulkSnapshot(
            Level level, BlockPos pos, @Nullable BlockEntity be) {
        CompoundTag stored = hbm$bulkTag(pos);
        if (be == null) return stored;
        CompoundTag current = be.saveWithFullMetadata(level.registryAccess());
        if (stored != null && stored.contains(BulkSnapshot.KEY)) {
            BulkSnapshot.relativeData(current, pos, stored);
        }
        return current;
    }
}
