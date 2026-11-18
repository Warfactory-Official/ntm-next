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

package com.hbm.util.datafix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

final class FallingMultiblockCellFix extends EntityTagFix {

    FallingMultiblockCellFix(Schema outputSchema) {
        super(outputSchema);
    }

    @Override
    protected Dynamic<?> fixEntity(Dynamic<?> input) {
        if (!input.get("id").asString("").equals("hbm:entity_falling_multiblock")) return input;
        var entity = (CompoundTag) input.convert(NbtOps.INSTANCE).getValue();
        var states = BlockStateEncoding14.canonicalList(entity.getListOrEmpty("states"));
        int[] cells =
                IntStream.range(0, states.size())
                        .filter(
                                i ->
                                        MultiblockCellLayoutFix.shared(
                                                states.getCompoundOrEmpty(i)
                                                        .getStringOr("Name", "")))
                        .toArray();
        if (cells.length == 0) return input;
        int[] offsets = entity.getIntArray("offsets").orElseThrow();
        int coreIndex =
                IntStream.range(0, Math.min(offsets.length, states.size()))
                        .filter(i -> (offsets[i] & 0x3FFFFF) == 0)
                        .findFirst()
                        .orElseThrow();
        var core = states.getCompoundOrEmpty(coreIndex);
        var fixed = entity.copy();
        var replacements = fixed.getListOrEmpty("states");
        for (int i : cells) {
            int offset = offsets[i];
            int dx = (offset << 26) >> 26;
            int dy = (offset << 16) >> 22;
            int dz = (offset << 10) >> 26;
            var state =
                    CellLayouts14.has(core.getStringOr("Name", ""))
                            ? CellLayouts14.reconstruct(
                                    core,
                                    dx,
                                    dy,
                                    dz,
                                    MultiblockCellLayoutFix.flags(states.getCompoundOrEmpty(i)),
                                    entity.getCompoundOrEmpty("be"))
                            : null;
            replacements.set(
                    i, BlockStateEncoding14.current(state == null ? CellChunk14.air() : state));
        }
        return new Dynamic<>(NbtOps.INSTANCE, fixed).convert(input.getOps());
    }
}
