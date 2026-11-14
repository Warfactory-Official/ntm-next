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
