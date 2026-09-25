// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
