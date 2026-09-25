// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(FlowSource.FluidHandler.class)
public abstract class MixinFlowSourceFluidHandler {

    @Shadow @Nullable Supplier<@Nullable FluidInventory> fluidHandlerCache;

    @Inject(method = "manageSource", at = @At("HEAD"), cancellable = true)
    private void hbm$attachWhereNoBlockEntitySits(
            Level world, BlockEntity networkBE, CallbackInfo ci) {
        if (fluidHandlerCache != null || !(world instanceof ServerLevel server)) return;

        BlockFace location = __asm__(BlockFace) {
            aload this;
            checkcast "com/zurrtum/create/content/fluids/FlowSource";
            getfield "com/zurrtum/create/content/fluids/FlowSource" "location"
                    "Lcom/zurrtum/create/catnip/math/BlockFace;";
        };
        BlockPos pos = location.getConnectedPos();
        if (world.getBlockEntity(pos) != null) return;
        Direction side = location.getOppositeFace();
        if (FluidHelper.getFluidInventory(world, pos, null, null, side) == null) return;
        fluidHandlerCache = FluidHelper.getFluidInventoryCache(server, pos, side);
        ci.cancel();
    }
}
