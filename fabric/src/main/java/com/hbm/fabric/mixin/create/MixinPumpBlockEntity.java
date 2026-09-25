// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.create;

import com.zurrtum.create.catnip.math.BlockFace;
import com.zurrtum.create.content.fluids.pump.PumpBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
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
