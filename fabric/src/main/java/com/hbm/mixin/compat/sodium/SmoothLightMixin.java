// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = SmoothLightPipeline.class, remap = false)
public class SmoothLightMixin {
    @Shadow @Final private LightDataAccess lightCache;

    @Inject(method = "calculate", at = @At("RETURN"))
    private void hbm$ownLight(
            ModelQuadView quad,
            BlockPos pos,
            QuadLightData out,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            boolean enhanced,
            CallbackInfo ci) {
        if (quad instanceof QuadViewImpl view && view.getTag() == QuadLighting.OWN_BLOCK) {
            Arrays.fill(out.lm, LightDataAccess.getEmissiveLightmap(lightCache.get(pos)));
        }
    }

    @ModifyExpressionValue(
            method = "calculate",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/model/light/data/LightDataAccess;unpackFC(I)Z"))
    private boolean hbm$cellFaces(boolean fullBlock, @Local(argsOnly = true) ModelQuadView quad) {
        return fullBlock
                && !(quad instanceof QuadViewImpl view && QuadLighting.isCell(view.getTag()));
    }
}
