// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinRemovedBlockEntity {

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void hbm$skipRemoved(
            BlockEntity entity,
            float partialTick,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay,
            boolean offscreen,
            CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (entity.isRemoved()) cir.setReturnValue(null);
    }
}
