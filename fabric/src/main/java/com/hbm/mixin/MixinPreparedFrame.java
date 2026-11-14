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

import com.hbm.client.render.OrderedBlendFrame;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class MixinPreparedFrame implements OrderedBlendFrame {
    @Shadow private FeatureFrameContext context;
    @Shadow private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private Map<FeatureRenderPhase<?>, List<?>> groupsByPhase;

    @Shadow
    protected abstract void executePhase(
            FeatureRenderPhase<?> phase, FeatureFrameContext context, RenderPass pass);

    @Override
    public boolean hbm$hasOrderedBlends() {
        for (var collection : submitNodeStorage.getSubmitsPerOrder().values()) {
            var phase =
                    ((com.hbm.client.render.OrderedBlendCollection) (collection))
                            .hbm$orderedBlends();
            if (phase != null && !groupsByPhase.getOrDefault(phase, List.of()).isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public void hbm$drawOrderedBlends(RenderPass pass) {
        for (var collection : submitNodeStorage.getSubmitsPerOrder().values()) {
            var phase =
                    ((com.hbm.client.render.OrderedBlendCollection) (collection))
                            .hbm$orderedBlends();
            if (phase != null) executePhase(phase, context, pass);
        }
    }
}
