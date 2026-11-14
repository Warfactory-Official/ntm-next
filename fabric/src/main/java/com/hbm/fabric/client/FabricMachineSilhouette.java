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
