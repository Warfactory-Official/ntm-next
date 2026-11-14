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
import com.hbm.fabric.integration.bulk.LitematicaBulkData;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkSnapshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PasteLayerBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.SchematicPlacingUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SchematicPlacingUtils.class, remap = false)
public abstract class MixinSchematicPlacingUtils {
    @WrapMethod(method = "placeToWorldWithinChunk")
    private static boolean hbm$bulk(
            Level world,
            ChunkPos chunk,
            SchematicPlacement placement,
            ReplaceBehavior replace,
            PasteLayerBehavior layers,
            boolean notify,
            Operation<Boolean> original) {
        if (!(world instanceof ServerLevel))
            return original.call(world, chunk, placement, replace, layers, notify);
        try (var bulk = BulkAction.begin()) {
            return original.call(world, chunk, placement, replace, layers, notify);
        }
    }

    @WrapOperation(
            method = "placeBlocksWithinChunk",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean hbm$data(
            Level world,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original,
            @Local(name = "teNBT") CompoundData data,
            @Local(name = "mirrorMain") Mirror first,
            @Local(name = "mirrorSub") Mirror second,
            @Local(name = "rotationCombined") Rotation rotation) {
        boolean result = original.call(world, pos, state, flags);
        if (data != null && !state.is(Blocks.BARRIER)) {
            CompoundTag tag =
                    LitematicaBulkData.transform(
                            data, state, first, second, rotation, world.registryAccess());
            if (tag != null) {
                if (world instanceof ServerLevel level) {
                    BulkAction.record(level, pos, state, state);
                    BulkAction.imported(level, pos, tag);
                } else if (world instanceof BulkSchematicData schematic)
                    schematic.hbm$bulkTag(pos, tag);
            }
        }
        return result;
    }

    @WrapOperation(
            method = "placeBlocksWithinChunk",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private static void hbm$nativeData(
            BlockEntity be,
            ValueInput input,
            Operation<Void> original,
            @Local(argsOnly = true) Level world) {
        if (world instanceof BulkSchematicData schematic) {
            var tag = schematic.hbm$bulkTag(be.getBlockPos());
            if (tag != null)
                input =
                        TagValueInput.create(
                                ProblemReporter.DISCARDING,
                                world.registryAccess(),
                                BulkSnapshot.forPlacement(tag, be.getBlockPos()));
        }
        original.call(be, input);
    }
}
