// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

public class ModelBoatRubber extends EntityModel<BoatRenderState> {

    private static final float QUARTER_TURN = (float) (Math.PI / 2D);

    public ModelBoatRubber(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root =
                mesh.getRoot()
                        .addOrReplaceChild(
                                "body",
                                CubeListBuilder.create(),
                                PartPose.rotation(0F, -QUARTER_TURN, 0F));

        root.addOrReplaceChild(
                "bottom",
                CubeListBuilder.create().texOffs(0, 8).addBox(-12F, -8F, -3F, 24, 16, 4),
                PartPose.offsetAndRotation(0F, 4F, 0F, QUARTER_TURN, 0F, 0F));

        wall(root, "bow", -11F, 0F, -QUARTER_TURN);
        wall(root, "stern", 11F, 0F, QUARTER_TURN);
        wall(root, "port", 0F, -9F, (float) Math.PI);
        wall(root, "starboard", 0F, 9F, 0F);

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void wall(PartDefinition root, String name, float x, float z, float yaw) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(0, 0).addBox(-10F, -7F, -1F, 20, 6, 2),
                PartPose.offsetAndRotation(x, 4F, z, 0F, yaw, 0F));
    }
}
