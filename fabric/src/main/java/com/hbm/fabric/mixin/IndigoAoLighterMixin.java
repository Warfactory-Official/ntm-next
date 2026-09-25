// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AoCalculator.class)
public class IndigoAoLighterMixin {
    @Shadow @Final private BlockModelLighter.Cache lightCache;
    @Shadow @Final private BakedQuad.MaterialInfo vanillaMaterialInfo;
    @Shadow private BlockAndTintGetter level;
    @Shadow private BlockState state;
    @Shadow private BlockPos pos;
    @Shadow @Final public int[] light;

    @Inject(method = "compute", at = @At("RETURN"))
    private void hbm$ownLight(QuadViewImpl quad, boolean vanillaShade, CallbackInfo ci) {
        if (quad.tag() == QuadLighting.OWN_BLOCK)
            Arrays.fill(light, lightCache.getLightCoords(state, level, pos));
    }

    @ModifyExpressionValue(
            method = "calcFastVanilla",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$cellFaces(boolean fullBlock, @Local(argsOnly = true) QuadViewImpl quad) {
        return fullBlock && !QuadLighting.isCell(quad.tag());
    }

    @Inject(
            method =
                    "calcVanilla(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/QuadViewImpl;[F[I)V",
            at = @At("HEAD"))
    private void hbm$cellMaterial(
            QuadViewImpl quad, float[] aoDest, int[] lightDest, CallbackInfo ci) {
        vanillaMaterialInfo.hbm$setLightOrigin(
                QuadLighting.isCell(quad.tag()) ? quad.tag() : QuadLighting.VANILLA);
    }
}
