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

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;

final class StructureCellLayoutFix extends DataFix {
    StructureCellLayoutFix(Schema schema) {
        super(schema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "StructureCellLayoutFix",
                getInputSchema().getType(HbmDataFixers.STRUCTURE),
                typed -> typed.update(DSL.remainderFinder(), StructureCellLayoutFix::fix));
    }

    private static <T> Dynamic<T> fix(Dynamic<T> input) {
        var source = (CompoundTag) input.convert(NbtOps.INSTANCE).getValue();
        var palettes =
                source.contains("palettes")
                        ? source.getListOrEmpty("palettes").stream()
                                .map(tag -> BlockStateEncoding14.canonicalList((ListTag) tag))
                                .toList()
                        : List.of(
                                BlockStateEncoding14.canonicalList(
                                        source.getListOrEmpty("palette")));
        boolean shared =
                palettes.stream()
                        .flatMap(ListTag::compoundStream)
                        .anyMatch(
                                state ->
                                        MultiblockCellLayoutFix.shared(
                                                state.getStringOr("Name", "")));
        if (!shared) return input;
        var fixed = source.copy();
        var blocks = source.getListOrEmpty("blocks").compoundStream().toList();
        var positions =
                blocks.stream()
                        .map(
                                block -> {
                                    var p = block.getListOrEmpty("pos");
                                    return new BlockPos(
                                            p.getIntOr(0, 0), p.getIntOr(1, 0), p.getIntOr(2, 0));
                                })
                        .toList();
        var converted = new ArrayList<List<CompoundTag>>();
        for (var palette : palettes) {
            var owners = new Long2ObjectOpenHashMap<Owner>();
            for (int i = 0; i < blocks.size(); i++) {
                var state = palette.getCompoundOrEmpty(blocks.get(i).getInt("state").orElseThrow());
                if (!CellLayouts14.has(state.getStringOr("Name", ""))) continue;
                var core = positions.get(i);
                var owner = new Owner(core, state, blocks.get(i).getCompoundOrEmpty("nbt"));
                CellLayouts14.visit(
                        state,
                        offset -> {
                            var cell =
                                    core.offset(
                                            BlockPos.getX(offset),
                                            BlockPos.getY(offset),
                                            BlockPos.getZ(offset));
                            if (owners.putIfAbsent(cell.asLong(), owner) != null)
                                throw new IllegalArgumentException(
                                        "overlapping multiblocks in saved structure at " + cell);
                        });
            }
            var states = new ArrayList<CompoundTag>();
            for (int i = 0; i < blocks.size(); i++) {
                var state = palette.getCompoundOrEmpty(blocks.get(i).getInt("state").orElseThrow());
                if (MultiblockCellLayoutFix.shared(state.getStringOr("Name", ""))) {
                    var pos = positions.get(i);
                    var owner = owners.get(pos.asLong());
                    var cell =
                            owner == null
                                    ? null
                                    : CellLayouts14.reconstruct(
                                            owner.state(),
                                            pos.getX() - owner.pos().getX(),
                                            pos.getY() - owner.pos().getY(),
                                            pos.getZ() - owner.pos().getZ(),
                                            MultiblockCellLayoutFix.flags(state),
                                            owner.be());
                    state = cell == null ? CellChunk14.air() : cell;
                }
                states.add(state);
            }
            converted.add(states);
        }
        var newPalettes = new ArrayList<ListTag>();
        for (int p = 0; p < converted.size(); p++) newPalettes.add(new ListTag());
        var tuples = new HashMap<List<CompoundTag>, Integer>();
        var newBlocks = fixed.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            var tuple = new ArrayList<CompoundTag>();
            for (var palette : converted) tuple.add(palette.get(i));
            Integer id = tuples.get(tuple);
            if (id == null) {
                id = tuples.size();
                tuples.put(tuple, id);
                for (int p = 0; p < tuple.size(); p++)
                    newPalettes.get(p).add(BlockStateEncoding14.current(tuple.get(p)));
            }
            newBlocks.getCompoundOrEmpty(i).putInt("state", id);
        }
        if (source.contains("palettes")) {
            var list = new ListTag();
            list.addAll(newPalettes);
            fixed.put("palettes", list);
        } else fixed.put("palette", newPalettes.getFirst());
        return new Dynamic<>(NbtOps.INSTANCE, fixed).convert(input.getOps());
    }

    private record Owner(BlockPos pos, CompoundTag state, CompoundTag be) {}
}
