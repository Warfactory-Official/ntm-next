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
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockInput.class)
public abstract class MixinBlockInput {
    @Shadow @Final private CompoundTag tag;

    @Inject(method = "place", at = @At("RETURN"), cancellable = true)
    private void hbm$data(
            ServerLevel level, BlockPos pos, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!BulkAction.active() || tag == null || !tag.contains(BulkSnapshot.KEY)) return;
        BulkAction.record(level, pos, level.getBlockState(pos), level.getBlockState(pos));
        BulkAction.imported(level, pos, tag);
        cir.setReturnValue(true);
    }
}
