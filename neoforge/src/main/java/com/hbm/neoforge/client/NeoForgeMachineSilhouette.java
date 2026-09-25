// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.client.render.MachinePlacementOutline;
import com.hbm.client.render.MachineSilhouette;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeMachineSilhouette {
    private NeoForgeMachineSilhouette() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(
                (ExtractBlockOutlineRenderStateEvent event) -> {
                    var placement =
                            MachinePlacementOutline.extract(
                                    event.getLevel(),
                                    event.getHitResult(),
                                    Minecraft.getInstance().player);
                    if (placement != null) {
                        event.addCustomRenderer(
                                (outline, collector, poseStack, levelState) -> {
                                    MachinePlacementOutline.submit(
                                            placement, outline, collector, poseStack, levelState);
                                    return true;
                                });
                        return;
                    }
                    var state =
                            MachineSilhouette.extract(
                                    event.getLevel(),
                                    event.getBlockPos(),
                                    event.getBlockState(),
                                    event.getCamera().position());
                    if (state == null) return;
                    event.addCustomRenderer(
                            (outline, collector, poseStack, levelState) -> {
                                float width =
                                        Minecraft.getInstance()
                                                .gameRenderer
                                                .gameRenderState()
                                                .windowRenderState
                                                .appropriateLineWidth;
                                MachineSilhouette.submit(
                                        state, outline, collector, poseStack, levelState, width);
                                return true;
                            });
                });
    }
}
