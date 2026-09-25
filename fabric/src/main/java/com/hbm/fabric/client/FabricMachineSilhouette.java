// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.client.render.MachinePlacementOutline;
import com.hbm.client.render.MachineSilhouette;
import com.hbm.client.render.MultiblockOutline;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.world.phys.BlockHitResult;

final class FabricMachineSilhouette {
    private static final RenderStateDataKey<MachineSilhouette.State> KEY =
            RenderStateDataKey.create(() -> "hbm:machine_silhouette");
    private static final RenderStateDataKey<MachinePlacementOutline.State> PLACEMENT_KEY =
            RenderStateDataKey.create(() -> "hbm:machine_placement_outline");

    private FabricMachineSilhouette() {}

    static void register() {
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register(
                (context, hit) -> {
                    BlockOutlineRenderState outline = context.levelState().blockOutlineRenderState;
                    if (outline == null || !(hit instanceof BlockHitResult blockHit)) return;
                    var placement =
                            MachinePlacementOutline.extract(
                                    context.level(), blockHit, Minecraft.getInstance().player);
                    if (placement != null) {
                        outline.setData(PLACEMENT_KEY, placement);
                        return;
                    }
                    var state =
                            MultiblockOutline.silhouette(
                                    context.level(),
                                    blockHit.getBlockPos(),
                                    context.camera().position());
                    if (state != null) outline.setData(KEY, state);
                });
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(
                (context, outline) -> {
                    MachinePlacementOutline.State placement = outline.getData(PLACEMENT_KEY);
                    if (placement != null) {
                        MachinePlacementOutline.submit(
                                placement,
                                outline,
                                context.submitNodeCollector(),
                                context.poseStack(),
                                context.levelState());
                        return false;
                    }
                    MachineSilhouette.State state = outline.getData(KEY);
                    if (state == null) return true;
                    float width =
                            context.gameRenderer()
                                    .gameRenderState()
                                    .windowRenderState
                                    .appropriateLineWidth;
                    MachineSilhouette.submit(
                            state,
                            outline,
                            context.submitNodeCollector(),
                            context.poseStack(),
                            context.levelState(),
                            width);
                    return false;
                });
    }
}
