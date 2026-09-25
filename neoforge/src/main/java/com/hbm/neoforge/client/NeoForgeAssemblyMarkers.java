// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.client.render.AssemblyMarkers;
import com.hbm.lib.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeAssemblyMarkers {
    private static final ContextKey<AssemblyMarkers.State> KEY =
            new ContextKey<>(Library.id("assembly_markers"));

    private NeoForgeAssemblyMarkers() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(
                (ExtractLevelRenderStateEvent event) ->
                        event.getRenderState()
                                .setRenderData(
                                        KEY,
                                        AssemblyMarkers.extract(
                                                event.getRenderState().cameraRenderState.pos)));
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
