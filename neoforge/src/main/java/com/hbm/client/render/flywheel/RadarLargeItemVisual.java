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

import com.hbm.client.render.RadarLargeItemRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class RadarLargeItemVisual extends HbmItemVisual {
    private final TransformedInstance radar;
    private final TransformedInstance dish;
    private final Matrix4f world = new Matrix4f();

    public RadarLargeItemVisual(VisualizationContext ctx) {
        super(ctx);
        Material material = ItemMaterials.cutout(ResourceManager.radar_large_tex, true);
        radar =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.radar_large,
                                RadarLargeItemRenderer.RADAR,
                                material));
        dish =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.radar_large,
                                RadarLargeItemRenderer.DISH,
                                material));
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        write(radar, pose, -1, light);
        write(
                dish,
                world.set(pose)
                        .rotate(
                                Axis.YP.rotationDegrees(
                                        RadarLargeItemRenderer.dishDegrees(GameTime.now()))),
                -1,
                light);
    }
}
