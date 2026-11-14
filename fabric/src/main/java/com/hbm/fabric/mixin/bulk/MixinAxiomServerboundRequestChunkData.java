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

import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkSnapshot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.packets.AxiomServerboundRequestChunkData;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.io.ByteArrayOutputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AxiomServerboundRequestChunkData.class, remap = false)
public abstract class MixinAxiomServerboundRequestChunkData {
    @WrapOperation(
            method = "handle",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"))
    private boolean hbm$capture(
            BlockState state,
            Operation<Boolean> original,
            @Local(name = "level") ServerLevel level,
            @Local(name = "blockEntityMap") Long2ObjectMap<CompressedBlockEntity> data,
            @Local(name = "baos") ByteArrayOutputStream bytes,
            @Local(name = "sx") int sx,
            @Local(name = "sy") int sy,
            @Local(name = "sz") int sz,
            @Local(name = "x") int x,
            @Local(name = "y") int y,
            @Local(name = "z") int z) {
        if (!BulkAction.owns(state)) return original.call(state);
        BlockPos pos = new BlockPos((sx << 4) + x, (sy << 4) + y, (sz << 4) + z);
        CompoundTag tag = BulkSnapshot.capture(level, pos);
        if (tag != null) data.put(pos.asLong(), CompressedBlockEntity.compress(tag, bytes));
        return false;
    }
}
