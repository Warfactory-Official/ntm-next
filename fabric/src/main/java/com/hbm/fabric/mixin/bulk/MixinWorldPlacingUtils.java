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

import com.hbm.fabric.integration.bulk.LitematicaBulkData;
import com.hbm.world.bulk.BulkSnapshot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.util.WorldPlacingUtils;
import fi.dy.masa.litematica.world.ProtoChunkSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WorldPlacingUtils.class, remap = false)
public abstract class MixinWorldPlacingUtils {
    @WrapOperation(
            method = "placeBlocksToProtoChunk",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lfi/dy/masa/litematica/world/ProtoChunkSchematic;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState hbm$data(
            ProtoChunkSchematic chunk,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<BlockState> original,
            @Local(name = "teNBT") CompoundData data,
            @Local(name = "mirrorMain") Mirror first,
            @Local(name = "mirrorSub") Mirror second,
            @Local(name = "rotationCombined") Rotation rotation) {
        BlockState previous = original.call(chunk, pos, state, flags);
        if (data != null && !state.is(Blocks.BARRIER)) {
            CompoundTag tag =
                    LitematicaBulkData.transform(
                            data,
                            state,
                            first,
                            second,
                            rotation,
                            chunk.getWrapped().getLevel().registryAccess());
            if (tag != null) {
                ((com.hbm.fabric.integration.bulk.BulkSchematicData) (chunk.getWrapped()))
                        .hbm$bulkTag(pos, tag);
            }
        }
        return previous;
    }

    @WrapOperation(
            method = "placeBlocksToProtoChunk",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private static void hbm$nativeData(
            BlockEntity be,
            ValueInput input,
            Operation<Void> original,
            @Local(argsOnly = true) ProtoChunkSchematic chunk) {
        var tag =
                ((com.hbm.fabric.integration.bulk.BulkSchematicData) (chunk.getWrapped()))
                        .hbm$bulkTag(be.getBlockPos());
        if (tag != null)
            input =
                    TagValueInput.create(
                            ProblemReporter.DISCARDING,
                            SchematicWorldHandler.INSTANCE.getRegistryManager(),
                            BulkSnapshot.forPlacement(tag, be.getBlockPos()));
        original.call(be, input);
    }
}
