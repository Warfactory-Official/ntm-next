// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
