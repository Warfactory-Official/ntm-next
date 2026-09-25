// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderPipeline.class, priority = 900)
public class VertexBindingsMixin {
    @Shadow @Final private VertexFormat[] vertexFormatPerBuffer;
    @Unique private VertexFormat[] hbm$irisBindings;

    @Inject(method = "getVertexFormatBindings", at = @At("HEAD"), cancellable = true)
    private void hbm$keepBindingSlots(CallbackInfoReturnable<VertexFormat[]> cir) {
        if (!Iris.isPackInUseQuick() || !ImmediateState.isRenderingLevel) return;
        VertexFormat first = ((RenderPipeline) (Object) this).getVertexFormatBinding(0);
        if (first == vertexFormatPerBuffer[0]) {
            cir.setReturnValue(vertexFormatPerBuffer);
        } else {
            if (hbm$irisBindings == null || hbm$irisBindings[0] != first) {
                hbm$irisBindings = vertexFormatPerBuffer.clone();
                hbm$irisBindings[0] = first;
            }
            cir.setReturnValue(hbm$irisBindings);
        }
    }
}
