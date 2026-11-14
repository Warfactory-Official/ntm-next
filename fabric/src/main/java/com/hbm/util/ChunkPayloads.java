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

import com.hbm.entity.item.PendingFallers;
import com.hbm.explosion.FalloutReplay;
import com.hbm.interfaces.injected.ChunkExtension;
import com.hbm.util.datafix.HbmDataFixers;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class ChunkPayloads {
    private ChunkPayloads() {}

    public static void write(ChunkExtension snapshot, BiConsumer<String, Tag> out) {
        out.accept(HbmDataFixers.VERSION_KEY, IntTag.valueOf(HbmDataFixers.CURRENT));
        long appliedThrough = snapshot.hbm$falloutAppliedThrough();
        if (appliedThrough != 0)
            out.accept(
                    ChunkExtension.FALLOUT_APPLIED_THROUGH_NBT_KEY,
                    LongTag.valueOf(appliedThrough));
        FalloutReplay replay = snapshot.hbm$falloutReplay();
        if (replay != null) out.accept(ChunkExtension.FALLOUT_REPLAY_NBT_KEY, replay.write());
        long[] coreIndex = snapshot.hbm$coreIndex();
        int coreIndexSize = snapshot.hbm$coreIndexSize();
        if (coreIndex != null && coreIndexSize != 0) {
            out.accept(
                    ChunkExtension.CORE_INDEX_NBT_KEY,
                    new LongArrayTag(
                            coreIndexSize == coreIndex.length
                                    ? coreIndex
                                    : Arrays.copyOf(coreIndex, coreIndexSize)));
        }
        int fallers = snapshot.hbm$pendingFallerSize();
        if (fallers != 0) {
            out.accept(
                    ChunkExtension.PENDING_FALLERS_NBT_KEY,
                    PendingFallers.save(
                            snapshot.hbm$pendingFallerPositions(),
                            snapshot.hbm$pendingFallerStates(),
                            fallers));
        }
        Long2ObjectOpenHashMap<String> fluids = snapshot.hbm$ductFluids();
        if (fluids != null) out.accept(ChunkExtension.DUCT_FLUIDS_NBT_KEY, ductFluids(fluids));
    }

    private static CompoundTag ductFluids(Long2ObjectOpenHashMap<String> fluids) {
        long[] positions = new long[fluids.size()];
        var ids = new ListTag();
        int i = 0;
        for (var entry : fluids.long2ObjectEntrySet()) {
            positions[i++] = entry.getLongKey();
            ids.add(StringTag.valueOf(entry.getValue()));
        }
        var tag = new CompoundTag();
        tag.putLongArray("pos", positions);
        tag.put("fluid", ids);
        return tag;
    }
}
