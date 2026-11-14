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

package com.hbm.fabric.mixin.create;

import com.zurrtum.create.catnip.math.BlockFace;
import com.zurrtum.create.content.fluids.FlowSource;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowSource.FluidHandler.class)
public abstract class MixinFluidHandler {

    @Shadow @Nullable Supplier<@Nullable FluidInventory> fluidHandlerCache;

    @Inject(method = "manageSource", at = @At("HEAD"), cancellable = true)
    private void hbm$attachWhereNoBlockEntitySits(
            Level world, BlockEntity networkBE, CallbackInfo ci) {
        if (fluidHandlerCache != null || !(world instanceof ServerLevel server)) return;
        BlockFace location = ((AccessorFlowSource) (Object) this).hbm$location();
        BlockPos pos = location.getConnectedPos();
        if (world.getBlockEntity(pos) != null) return;
        Direction side = location.getOppositeFace();
        if (FluidHelper.getFluidInventory(world, pos, null, null, side) == null) return;
        fluidHandlerCache = FluidHelper.getFluidInventoryCache(server, pos, side);
        ci.cancel();
    }
}
