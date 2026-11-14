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

package com.hbm.fabric.mixin.bulk;

import com.hbm.fabric.integration.bulk.BulkSchematicData;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = WorldSchematic.class, remap = false)
public abstract class MixinWorldSchematic implements BulkSchematicData {
    private BulkSchematicData hbm$chunk(BlockPos pos) {
        return ((com.hbm.fabric.integration.bulk.BulkSchematicData)
                (((WorldSchematic) (Object) this).getChunk(pos.getX() >> 4, pos.getZ() >> 4)));
    }

    @Override
    public void hbm$bulkTag(BlockPos pos, CompoundTag tag) {
        hbm$chunk(pos).hbm$bulkTag(pos, tag);
    }

    @Override
    public @Nullable CompoundTag hbm$bulkTag(BlockPos pos) {
        return hbm$chunk(pos).hbm$bulkTag(pos);
    }
}
