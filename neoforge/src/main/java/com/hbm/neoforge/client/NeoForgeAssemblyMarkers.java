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

package com.hbm.neoforge.client;

import com.hbm.client.render.AssemblyMarkers;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeAssemblyMarkers {
    private static final ContextKey<AssemblyMarkers.State> KEY =
            new ContextKey<>(RegistryUtil.id("assembly_markers"));

    private NeoForgeAssemblyMarkers() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(
                (ExtractLevelRenderStateEvent event) ->
                        event.getRenderState()
                                .setRenderData(
                                        KEY,
                                        AssemblyMarkers.extract(
                                                event.getRenderState().cameraRenderState.pos,
                                                event.getCamera()
                                                        .getCameraEntityPartialTicks(
                                                                event.getDeltaTracker()))));
        NeoForge.EVENT_BUS.addListener(
                (SubmitCustomGeometryEvent event) ->
                        AssemblyMarkers.submit(
                                event.getLevelRenderState().getRenderDataOrThrow(KEY),
                                event.getPoseStack(),
                                event.getSubmitNodeCollector(),
                                event.getLevelRenderState().cameraRenderState,
                                Minecraft.getInstance().font));
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> AssemblyMarkers.clear());
    }
}
