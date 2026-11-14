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

package com.hbm.fabric.client;

import com.hbm.client.render.AssemblyMarkers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

final class FabricAssemblyMarkers {
    private static final RenderStateDataKey<AssemblyMarkers.State> KEY =
            RenderStateDataKey.create(() -> "hbm:assembly_markers");

    private FabricAssemblyMarkers() {}

    static void register() {
        LevelExtractionEvents.END_EXTRACTION.register(
                context ->
                        context.levelState()
                                .setData(
                                        KEY,
                                        AssemblyMarkers.extract(
                                                context.levelState().cameraRenderState.pos,
                                                context.camera()
                                                        .getCameraEntityPartialTicks(
                                                                context.deltaTracker()))));
        LevelRenderEvents.COLLECT_SUBMITS.register(
                context ->
                        AssemblyMarkers.submit(
                                context.levelState().getData(KEY),
                                context.poseStack(),
                                context.submitNodeCollector(),
                                context.levelState().cameraRenderState,
                                Minecraft.getInstance().font));
        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> AssemblyMarkers.clear());
    }
}
