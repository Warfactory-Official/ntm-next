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

package com.hbm.mixin.compat.bulk;

import com.hbm.blocks.multiblock.MultiblockGeometryCellBlock;
import com.hbm.world.bulk.BulkSnapshot;
import com.hbm.world.bulk.BulkTransform;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.sk89q.worldedit.coremc.internal.NBTConverter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockTransformExtent.class, remap = false)
public abstract class MixinBlockTransformExtent {
    @ModifyReturnValue(method = "transform", at = @At("RETURN"))
    private static BlockStateHolder<?> hbm$transform(
            BlockStateHolder<?> result, BlockStateHolder<?> source, Transform transform) {
        Vector3 origin = transform.apply(Vector3.ZERO);
        BulkTransform offsets =
                offset -> {
                    Vector3 transformed =
                            transform
                                    .apply(Vector3.at(offset.getX(), offset.getY(), offset.getZ()))
                                    .subtract(origin);
                    int x = (int) Math.round(transformed.x());
                    int y = (int) Math.round(transformed.y());
                    int z = (int) Math.round(transformed.z());
                    if (Math.abs(transformed.x() - x) > 1e-8
                            || Math.abs(transformed.y() - y) > 1e-8
                            || Math.abs(transformed.z() - z) > 1e-8)
                        throw new IllegalArgumentException(
                                "NTM multiblocks and graph links require a grid-aligned transform");
                    return new BlockPos(x, y, z);
                };
        var nativeState =
                Block.stateById(BlockStateIdAccess.getBlockStateId(source.toImmutableState()));
        if (nativeState.getBlock() instanceof MultiblockGeometryCellBlock cell) {
            var turned = cell.transform(nativeState, offsets);
            var states = new HashMap<>(result.getStates());
            for (var property : turned.getProperties())
                if (property instanceof IntegerProperty integer)
                    states.put(
                            result.getBlockType().getProperty(integer.getName()),
                            turned.getValue(integer));
            result =
                    result.getBlockType()
                            .getState(states)
                            .toBaseBlock(
                                    result instanceof BaseBlock b ? b.getNbtReference() : null);
        }
        if (!(source instanceof BaseBlock block) || block.getNbt() == null) return result;
        CompoundTag tag = NBTConverter.toNative(block.getNbt());
        if (!tag.contains(BulkSnapshot.KEY)) return result;
        return result.toImmutableState()
                .toBaseBlock(NBTConverter.fromNative(BulkSnapshot.transform(tag, offsets)));
    }
}
