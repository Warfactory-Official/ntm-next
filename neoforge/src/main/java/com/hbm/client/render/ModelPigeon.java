// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class ModelPigeon extends EntityModel<PigeonRenderState> {

    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart body;
    private final ModelPart bodyFat;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart ass;
    private final ModelPart feathers;
    private final ModelPart[] leftWings;
    private final ModelPart[] rightWings;

    public ModelPigeon(ModelPart root) {
        super(root);
        head = root.getChild("head");
        beak = root.getChild("beak");
        body = root.getChild("body");
        bodyFat = root.getChild("body_fat");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        ass = root.getChild("ass");
        feathers = root.getChild("feathers");
        leftWings = new ModelPart[] {body.getChild("left_wing"), bodyFat.getChild("left_wing")};
        rightWings = new ModelPart[] {body.getChild("right_wing"), bodyFat.getChild("right_wing")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -6F, -2F, 4, 6, 4),
                PartPose.offset(0F, 16F, -2F));
        root.addOrReplaceChild(
                "beak",
                CubeListBuilder.create().texOffs(14, 0).addBox(-1F, -4F, -4F, 2, 2, 2),
                PartPose.offset(0F, 16F, -2F));

        body(root, "body", CubeDeformation.NONE);
        body(root, "body_fat", new CubeDeformation(1F));

        root.addOrReplaceChild(
                "ass",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2F, -2F, -2F, 4, 4, 4),
                PartPose.offset(0F, 20F, 4F));
        root.addOrReplaceChild(
                "feathers",
                CubeListBuilder.create().texOffs(16, 24).addBox(-1F, -0.5F, -2F, 2, 1, 4),
                PartPose.offset(0F, 21.5F, 7.5F));
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(20, 0).addBox(-1F, 0F, 0F, 2, 4, 2),
                PartPose.offset(1F, 20F, -1F));
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(20, 0).addBox(-1F, 0F, 0F, 2, 4, 2),
                PartPose.offset(-1F, 20F, -1F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void body(PartDefinition root, String name, CubeDeformation deformation) {
        PartDefinition body =
                root.addOrReplaceChild(
                        name,
                        CubeListBuilder.create()
                                .texOffs(0, 10)
                                .addBox(-3F, -3F, -4F, 6, 6, 8, deformation),
                        PartPose.offset(0F, 17F, 0F));
        body.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create().texOffs(28, 0).addBox(0F, 0F, -3F, 1, 4, 6),
                PartPose.offset(3F, -2F, 0F));
        body.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create().texOffs(28, 10).addBox(-1F, 0F, -3F, 1, 4, 6),
                PartPose.offset(-3F, -2F, 0F));
    }

    @Override
    public void setupAnim(PigeonRenderState state) {
        super.setupAnim(state);
        head.xRot = beak.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = beak.yRot = state.yRot * Mth.DEG_TO_RAD;
        body.xRot = bodyFat.xRot = ass.xRot = -((float) Math.PI / 4F);
        feathers.xRot = -((float) Math.PI / 8F);
        rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
        leftLeg.xRot =
                Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI)
                        * 1.4F
                        * state.walkAnimationSpeed;

        for (ModelPart wing : leftWings) {
            wing.zRot = -state.flap;
            wing.x = state.fat ? 4F : 3F;
        }
        for (ModelPart wing : rightWings) {
            wing.zRot = state.flap;
            wing.x = state.fat ? -4F : -3F;
        }

        head.z = beak.z = state.fat ? -4F : -2F;
        ass.z = state.fat ? 5F : 4F;
        feathers.z = state.fat ? 8.5F : 7.5F;
        body.visible = !state.fat;
        bodyFat.visible = state.fat;
    }
}
