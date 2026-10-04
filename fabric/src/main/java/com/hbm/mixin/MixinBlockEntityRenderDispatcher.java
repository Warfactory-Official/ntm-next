// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.CrumblingCollector;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void hbm$skipRemoved(
            BlockEntity entity,
            float partialTick,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay,
            boolean offscreen,
            CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (entity.isRemoved()) cir.setReturnValue(null);
    }

    @ModifyArg(
            method = "submit",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"),
            index = 2)
    private SubmitNodeCollector hbm$crumbling(
            SubmitNodeCollector collector, @Local(argsOnly = true) BlockEntityRenderState state) {
        return CrumblingCollector.wrap(collector, state);
    }
}
