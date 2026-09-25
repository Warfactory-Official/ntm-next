// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.client.render.RebarPlacerPreview;
import com.hbm.lib.Library;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeRebarPlacerPreview {
    private static final ContextKey<AABB> KEY =
            new ContextKey<>(Library.id("rebar_placer_preview"));

    private NeoForgeRebarPlacerPreview() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(
                (ExtractLevelRenderStateEvent event) ->
                        event.getRenderState().setRenderData(KEY, RebarPlacerPreview.extract()));
        NeoForge.EVENT_BUS.addListener(
                (SubmitCustomGeometryEvent event) ->
                        RebarPlacerPreview.submit(
                                event.getLevelRenderState().getRenderData(KEY),
                                event.getPoseStack(),
                                event.getSubmitNodeCollector(),
                                event.getLevelRenderState().cameraRenderState));
    }
}
