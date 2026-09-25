// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
                                                context.levelState().cameraRenderState.pos)));
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
