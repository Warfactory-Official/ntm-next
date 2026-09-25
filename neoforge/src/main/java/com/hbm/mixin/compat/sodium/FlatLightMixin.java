// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
@Mixin(value = FlatLightPipeline.class, remap = false)
public abstract class FlatLightMixin {
    @Shadow @Final protected LightDataAccess lightCache;

    @Shadow
    abstract float getShade(BlockAndTintGetter level, Direction face, boolean shade);

    @Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
    private void hbm$ownLight(
            ModelQuadView quad,
            BlockPos pos,
            QuadLightData out,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            boolean enhanced,
            CallbackInfo ci) {
        if (!(quad instanceof QuadViewImpl view) || view.getTag() != QuadLighting.OWN_BLOCK) return;
        int word = lightCache.get(pos);
        boolean aligned = cullFace != null;
        if (!aligned) {
            int flags = quad.getFlags();
            aligned =
                    (flags & ModelQuadFlags.IS_ALIGNED) != 0
                            || (flags & ModelQuadFlags.IS_PARALLEL) != 0
                                    && LightDataAccess.unpackFC(word);
        }
        Arrays.fill(out.lm, LightDataAccess.getEmissiveLightmap(word));
        Arrays.fill(
                out.br,
                enhanced && !aligned
                        ? PlatformBlockAccess.getInstance()
                                .getNormalVectorShade(quad, lightCache.getLevel(), shade)
                        : getShade(lightCache.getLevel(), lightFace, shade));
        ci.cancel();
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
