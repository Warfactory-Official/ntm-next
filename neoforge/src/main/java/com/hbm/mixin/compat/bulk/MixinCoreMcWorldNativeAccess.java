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

import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkSnapshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sk89q.worldedit.coremc.internal.CoreMcWorldNativeAccess;
import com.sk89q.worldedit.coremc.internal.NBTConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CoreMcWorldNativeAccess.class, remap = false)
public abstract class MixinCoreMcWorldNativeAccess {
    @Shadow
    private ServerLevel getWorld() {
        throw new AssertionError();
    }

    @WrapMethod(method = "updateTileEntity")
    private boolean hbm$restore(BlockPos pos, LinCompoundTag encoded, Operation<Boolean> original) {
        CompoundTag tag = NBTConverter.toNative(encoded);
        if (!tag.contains(BulkSnapshot.KEY)) return original.call(pos, encoded);
        try (var bulk = BulkAction.begin()) {
            BulkAction.record(
                    getWorld(), pos, getWorld().getBlockState(pos), getWorld().getBlockState(pos));
            BulkAction.imported(getWorld(), pos, tag);
            return true;
        }
    }

    @WrapMethod(method = "updateNeighbors")
    private void hbm$recordNotification(
            BlockPos pos,
            BlockState old,
            BlockState state,
            int recursion,
            Operation<Void> original) {
        BulkAction.record(getWorld(), pos, getWorld().getBlockState(pos), state);
        original.call(pos, old, state, recursion);
    }
}
