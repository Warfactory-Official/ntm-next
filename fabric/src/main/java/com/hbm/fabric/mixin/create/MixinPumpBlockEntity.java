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
import com.zurrtum.create.content.fluids.pump.PumpBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpBlockEntity.class)
public abstract class MixinPumpBlockEntity {

    @Inject(method = "hasReachedValidEndpoint", at = @At("RETURN"), cancellable = true)
    private void hbm$endAtABlockEntitylessProvider(
            LevelAccessor world,
            BlockFace blockFace,
            boolean pull,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !(world instanceof Level level)) return;
        BlockPos connected = blockFace.getConnectedPos();
        if (level.getBlockEntity(connected) != null) return;
        if (FluidHelper.hasFluidInventory(
                level,
                connected,
                level.getBlockState(connected),
                null,
                blockFace.getOppositeFace())) {
            cir.setReturnValue(true);
        }
    }
}
