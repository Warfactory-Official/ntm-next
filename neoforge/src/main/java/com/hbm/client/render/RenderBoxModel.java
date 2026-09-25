// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class RenderBoxModel {

    public static final ModelPart CHOPPER_MINE = cube(32, 16, -4F, -4F, -4F, 8, 8, 8);

    public static final ModelPart SHRAPNEL = cube(16, 8, 1F, -0.5F, -0.5F, 4, 4, 4);

    public static final ModelPart BULLET = cube(8, 4, 1F, -0.5F, -0.5F, 2, 1, 1);

    private RenderBoxModel() {}

    private static ModelPart cube(
            int texWidth, int texHeight, float x, float y, float z, int w, int h, int d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot()
                .addOrReplaceChild(
                        "box",
                        CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, w, h, d),
                        PartPose.offset(x, y, z));
        return LayerDefinition.create(mesh, texWidth, texHeight).bakeRoot();
    }

    public static void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            RenderType type,
            ModelPart part,
            int light) {
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) -> {
                    PoseStack local = new PoseStack();
                    local.last().set(pose);
                    part.render(local, buffer, light, OverlayTexture.NO_OVERLAY);
                });
    }
}
