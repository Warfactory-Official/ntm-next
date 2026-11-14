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
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkSnapshot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LitematicaSchematic.class, remap = false)
public abstract class MixinLitematicaSchematic {
    @Unique
    private static @Nullable CompoundTag hbm$snapshot(Level world, BlockPos pos) {
        if (world instanceof BulkSchematicData schematic)
            return schematic.hbm$bulkSnapshot(world, pos, world.getBlockEntity(pos));
        return BulkSnapshot.capture(world, pos);
    }

    @WrapOperation(
            method = {"takeBlocksFromWorld", "takeBlocksFromWorldWithinChunk"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lfi/dy/masa/litematica/schematic/container/LitematicaBlockStateContainer;set(IIILnet/minecraft/world/level/block/state/BlockState;)V"))
    private void hbm$data(
            LitematicaBlockStateContainer container,
            int x,
            int y,
            int z,
            BlockState state,
            Operation<Void> original,
            @Local(argsOnly = true) Level world,
            @Local(name = "posMutable") BlockPos.MutableBlockPos pos,
            @Local(name = "tileEntityMap") Map<BlockPos, CompoundData> tags) {
        original.call(container, x, y, z, state);
        if (!BulkAction.owns(state)) return;
        CompoundTag tag = hbm$snapshot(world, pos);
        if (tag == null || !tag.contains(BulkSnapshot.KEY)) return;
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        tags.put(new BlockPos(x, y, z), DataConverterNbt.fromVanillaCompound(tag));
    }

    @WrapOperation(
            method = {"takeBlocksFromWorld", "takeBlocksFromWorldWithinChunk"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/BlockEntity;saveWithFullMetadata(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag hbm$controller(
            BlockEntity be,
            HolderLookup.Provider registries,
            Operation<CompoundTag> original,
            @Local(argsOnly = true) Level world) {
        CompoundTag tag = hbm$snapshot(world, be.getBlockPos());
        return tag != null && tag.contains(BulkSnapshot.KEY) ? tag : original.call(be, registries);
    }
}
