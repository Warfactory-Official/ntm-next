// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.client.WeatherTints;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {
    private static final String RENDER =
            "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V";

    @Unique private WeatherTints.@Nullable Tint hbm$tint;
    @Unique private @Nullable List<WeatherEffectRenderer.ColumnInstance> hbm$rain;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void hbm$claimTint(CallbackInfo ci, @Local(argsOnly = true) WeatherRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        hbm$tint = level == null ? null : WeatherTints.tint(level);
        hbm$rain = state.rainColumns;
    }

    @ModifyVariable(method = RENDER, at = @At("STORE"))
    private RenderPipeline hbm$tintPipeline(RenderPipeline pipeline) {
        WeatherTints.Tint tint = hbm$tint;
        if (tint == null) return pipeline;
        return pipeline == RenderPipelines.WEATHER_DEPTH_WRITE
                ? tint.depthWrite()
                : tint.noDepthWrite();
    }

    @ModifyExpressionValue(
            method = "renderInstances",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;white(F)I"))
    private int hbm$tintColumn(
            int white, @Local(argsOnly = true) List<WeatherEffectRenderer.ColumnInstance> columns) {
        WeatherTints.Tint tint = hbm$tint;
        if (tint == null) return white;
        return ARGB.color(ARGB.alpha(white), columns == hbm$rain ? tint.rain() : tint.snow());
    }
}
