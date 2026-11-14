/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin;

import com.hbm.client.gui.DeferredGuiDraws;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor implements DeferredGuiDraws {

    @Unique private @Nullable List<Consumer<GuiGraphicsExtractor>> hbm$deferred;

    @Shadow
    public abstract void nextStratum();

    @Override
    public void hbm$defer(Consumer<GuiGraphicsExtractor> draw) {
        if (hbm$deferred == null) hbm$deferred = new ArrayList<>(2);
        hbm$deferred.add(draw);
    }

    @Inject(method = "extractDeferredElements", at = @At("TAIL"))
    private void hbm$extractDeferredDraws(int mouseX, int mouseY, float a, CallbackInfo ci) {
        List<Consumer<GuiGraphicsExtractor>> deferred = hbm$deferred;
        if (deferred == null) return;
        hbm$deferred = null;
        for (Consumer<GuiGraphicsExtractor> draw : deferred) {
            nextStratum();
            draw.accept((GuiGraphicsExtractor) (Object) this);
        }
    }
}
