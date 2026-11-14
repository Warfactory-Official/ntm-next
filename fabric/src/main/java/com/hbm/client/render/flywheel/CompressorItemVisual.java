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

package com.hbm.client.render.flywheel;

import com.hbm.client.render.CompressorItemRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import org.joml.Matrix4fc;

public final class CompressorItemVisual extends HbmItemVisual {
    private final TransformedInstance body;
    private final TransformedInstance pump;
    private final TransformedInstance fan;
    private final PoseStack.Pose part = new PoseStack.Pose();

    public CompressorItemVisual(VisualizationContext ctx) {
        super(ctx);
        Material material = ItemMaterials.cutout(ResourceManager.compressor_tex, false);
        body =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.compressor, CompressorItemRenderer.BODY, material));
        pump =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.compressor, CompressorItemRenderer.PUMP, material));
        fan =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.compressor, CompressorItemRenderer.FAN, material));
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        long now = GameTime.now();
        write(body, pose, -1, light);
        part.pose().set(pose);
        CompressorItemRenderer.pumpPose(part, now);
        write(pump, part.pose(), -1, light);
        part.pose().set(pose);
        CompressorItemRenderer.fanPose(part, now);
        write(fan, part.pose(), -1, light);
    }
}
