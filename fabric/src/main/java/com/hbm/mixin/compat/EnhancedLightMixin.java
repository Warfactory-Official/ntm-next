// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat;

import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.neoforged.neoforge.client.model.ao.EnhancedBlockModelLighter", remap = false)
public abstract class EnhancedLightMixin extends BlockModelLighter {

    @ModifyVariable(method = "prepareQuadFlat", at = @At("HEAD"), argsOnly = true)
    private int hbm$ownFlatLight(
            int original,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int lightCoords,
            BakedQuad quad,
            QuadInstance output) {
        return QuadLighting.usesOwnBlock(quad.materialInfo())
                ? getLightCoords(state, level, pos)
                : original;
    }

    @Inject(method = "prepareQuadAmbientOcclusion", at = @At("RETURN"))
    private void hbm$ownLight(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            BakedQuad quad,
            QuadInstance output,
            CallbackInfo ci) {
        if (QuadLighting.usesOwnBlock(quad.materialInfo()))
            output.setLightCoords(getLightCoords(state, level, pos));
    }

    @ModifyExpressionValue(
            method = "calculateIrregular",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$cellFaces(boolean fullBlock, @Local(argsOnly = true) BakedQuad quad) {
        return fullBlock && !QuadLighting.usesCell(quad.materialInfo());
    }
}
