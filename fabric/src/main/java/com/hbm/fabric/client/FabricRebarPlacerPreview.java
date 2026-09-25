// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.client.render.RebarPlacerPreview;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.world.phys.AABB;

final class FabricRebarPlacerPreview {
    private static final RenderStateDataKey<AABB> KEY =
            RenderStateDataKey.create(() -> "hbm:rebar_placer_preview");

    private FabricRebarPlacerPreview() {}

    static void register() {
        LevelExtractionEvents.END_EXTRACTION.register(
                context -> context.levelState().setData(KEY, RebarPlacerPreview.extract()));
        LevelRenderEvents.COLLECT_SUBMITS.register(
                context ->
                        RebarPlacerPreview.submit(
                                context.levelState().getData(KEY),
                                context.poseStack(),
                                context.submitNodeCollector(),
                                context.levelState().cameraRenderState));
    }
}
