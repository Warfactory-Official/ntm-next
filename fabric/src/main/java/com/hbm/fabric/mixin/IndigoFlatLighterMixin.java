// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.FlatLighter;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlatLighter.class)
public class IndigoFlatLighterMixin {
    @Shadow @Final private BlockModelLighter.Cache lightCache;

    @Inject(method = "light", at = @At("HEAD"), cancellable = true)
    private void hbm$ownBlockLight(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            QuadViewImpl quad,
            CallbackInfoReturnable<Integer> cir) {
        if (quad.tag() == QuadLighting.OWN_BLOCK)
            cir.setReturnValue(lightCache.getLightCoords(state, level, pos));
    }

    @ModifyExpressionValue(
            method = "light",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$cellFaces(boolean fullBlock, @Local(argsOnly = true) QuadViewImpl quad) {
        return fullBlock && !QuadLighting.isCell(quad.tag());
    }
}
