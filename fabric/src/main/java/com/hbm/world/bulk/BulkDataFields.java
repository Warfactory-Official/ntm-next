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

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class BulkDataFields {
    private BulkDataFields() {}

    public static CompoundTag copy(CompoundTag source, String... keys) {
        var target = new CompoundTag();
        for (var key : keys) {
            if (source.contains(key)) target.put(key, source.get(key).copy());
        }
        return target;
    }

    public static void positions(CompoundTag tag, String key, BulkTransform transform) {
        if (!tag.contains(key)) return;
        var points = tag.read(key, BlockPos.CODEC.listOf()).orElseThrow();
        tag.store(key, BlockPos.CODEC.listOf(), points.stream().map(transform::apply).toList());
    }

    public static void bounds(CompoundTag tag, String key, BulkTransform transform) {
        if (!tag.contains(key)) return;
        var box = tag.read(key, BoundingBox.CODEC).orElseThrow();
        var first = transform.apply(new BlockPos(box.minX(), box.minY(), box.minZ()));
        var opposite = transform.apply(new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
        tag.store(key, BoundingBox.CODEC, BoundingBox.fromCorners(first, opposite));
    }
}
