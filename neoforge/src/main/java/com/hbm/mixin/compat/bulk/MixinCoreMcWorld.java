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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.sk89q.worldedit.coremc.internal.CoreMcWorld;
import com.sk89q.worldedit.coremc.internal.NBTConverter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CoreMcWorld.class, remap = false)
public abstract class MixinCoreMcWorld {
    private ServerLevel hbm$world() {
        return ((CoreMcWorld) (Object) this).getWorld();
    }

    @ModifyReturnValue(method = "getFullBlock", at = @At("RETURN"))
    private BaseBlock hbm$capture(BaseBlock original, BlockVector3 position) {
        BlockPos pos = new BlockPos(position.x(), position.y(), position.z());
        if (!BulkAction.owns(hbm$world().getBlockState(pos))) return original;
        CompoundTag tag = BulkSnapshot.capture(hbm$world(), pos);
        return tag == null
                ? original
                : original.toImmutableState().toBaseBlock(NBTConverter.fromNative(tag));
    }
}
