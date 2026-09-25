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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class ModelHunterChopper extends EntityModel<EntityRenderState> {

    private static final float ROTOR_SPEED = 1.5F;

    private final ModelPart mainRotor;
    private final ModelPart torsoRotor;
    private final ModelPart tailRotor;

    public ModelHunterChopper(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.mainRotor = root.getChild("rotor_blades");
        this.torsoRotor = root.getChild("torso_rotor_blades");
        this.tailRotor = root.getChild("tail_rotor_blades");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        box(root, "rotor_pivot_stem", 40, 22, 0F, 0F, 0F, 1F, 4F, 1F, -0.5F, 0F, -0.5F);
        box(root, "rotor_pivot_top", 40, 27, 0F, 0F, 0F, 3F, 1F, 3F, -1.5F, -1F, -1.5F);
        box(root, "rotor_pivot_plate", 40, 31, 0F, 0F, 0F, 6F, 1F, 6F, -3F, 1.5F, -3F);

        box(root, "torso_base_center", 70, 0, 0F, 0F, 0F, 14F, 4F, 2F, -8F, 4F, -1F);
        box(
                root,
                "torso_plate_left",
                70,
                6,
                0F,
                -4F,
                0F,
                14F,
                4F,
                1F,
                -8F,
                8F,
                -2F,
                -0.2268928F,
                0F,
                0F);
        box(root, "torso_base_bottom", 70, 11, 0F, 0F, 0F, 7F, 2F, 4F, -4F, 8F, -2F);
        box(
                root,
                "torso_plate_right",
                70,
                17,
                0F,
                -4F,
                -1F,
                14F,
                4F,
                1F,
                -8F,
                8F,
                2F,
                0.2268928F,
                0F,
                0F);
        box(
                root,
                "torso_plate_bottom",
                70,
                22,
                -5F,
                -2F,
                0F,
                5F,
                2F,
                4F,
                -4F,
                10F,
                -2F,
                0F,
                0F,
                0.2094395F);

        box(
                root,
                "wing_left_plate",
                110,
                0,
                0F,
                -3F,
                0F,
                9F,
                3F,
                1F,
                -8F,
                9F,
                -3F,
                -0.2268928F,
                0F,
                0F);
        box(
                root,
                "wing_right_plate",
                130,
                0,
                0F,
                -3F,
                0F,
                9F,
                3F,
                1F,
                -8F,
                9F,
                2F,
                0.2268928F,
                0F,
                0F);
        box(root, "wing_left", 110, 4, 0F, 0F, 0F, 3F, 1F, 6F, -3F, 10F, -8F, 0.3490659F, 0F, 0F);
        box(
                root,
                "wing_left_front",
                110,
                11,
                0F,
                0F,
                0F,
                2F,
                1F,
                7F,
                -3F,
                10F,
                -8F,
                0.3490659F,
                -0.3490659F,
                -0.1745329F);
        box(root, "wing_left_tip", 110, 19, 0F, 0F, 0F, 5F, 2F, 1F, -4F, 9F, -8F);
        box(root, "wing_right", 130, 4, 0F, 0F, -6F, 3F, 1F, 6F, -3F, 10F, 8F, -0.3490659F, 0F, 0F);
        box(
                root,
                "wing_right_front",
                130,
                11,
                0F,
                0F,
                -7F,
                2F,
                1F,
                7F,
                -3F,
                10F,
                8F,
                -0.3490659F,
                0.3490659F,
                -0.1745329F);
        box(root, "wing_right_tip", 130, 19, 0F, 0F, 0F, 5F, 2F, 1F, -4F, 9F, 7F);

        box(root, "torso_base_back", 70, 28, 0F, 0F, 0F, 3F, 2F, 3F, 3F, 7.5F, -1.5F);
        box(
                root,
                "torso_box_bottom",
                70,
                33,
                0F,
                -2F,
                0F,
                7F,
                2F,
                2F,
                -3F,
                10F,
                -1F,
                0F,
                0F,
                0.1570796F);
        box(
                root,
                "torso_plate_back",
                70,
                37,
                0F,
                0F,
                0F,
                3F,
                1F,
                2F,
                6F,
                4F,
                -1F,
                0F,
                0F,
                0.2268928F);
        box(root, "torso_box_back", 70, 40, 0F, 0F, 0F, 2F, 4F, 2F, 6F, 5F, -1F);
        box(
                root,
                "torso_plate_left_back",
                70,
                46,
                0F,
                -4F,
                -1F,
                3F,
                4F,
                1F,
                6F,
                8.5F,
                -1F,
                -0.2268928F,
                0F,
                0F);
        box(
                root,
                "torso_plate_right_back",
                70,
                51,
                0F,
                -4F,
                0F,
                3F,
                4F,
                1F,
                6F,
                8.5F,
                1F,
                0.2268928F,
                0F,
                0F);

        box(root, "tail_front_base", 24, 54, 0F, 0F, 0F, 5F, 2F, 2F, 8F, 6F, -1F);
        box(
                root,
                "tail_front_plate",
                24,
                58,
                -5F,
                0F,
                0F,
                5F,
                1F,
                2F,
                13F,
                6F,
                -1F,
                0F,
                0F,
                0.2268928F);
        box(root, "tail_back_base", 24, 61, 0F, 0F, 0F, 4F, 2F, 1F, 13F, 6F, -0.5F);
        box(
                root,
                "tail_rotor_front",
                24,
                64,
                0F,
                0F,
                0F,
                1F,
                3F,
                1F,
                15.5F,
                8F,
                -0.5F,
                0F,
                0F,
                -0.2268928F);
        box(root, "tail_rotor_top", 24, 68, 0F, 0F, 0F, 3F, 1F, 1F, 17F, 6F, -0.5F);
        box(root, "tail_rotor_back", 24, 70, 0F, 0F, 0F, 1F, 4F, 1F, 20F, 6F, -0.5F);
        box(root, "tail_rotor_bottom", 24, 75, 0F, 0F, 0F, 3F, 1F, 1F, 18F, 10F, -0.5F);
        box(root, "tail_rotor_blades", 120, 120, -1.5F, -1.5F, 0F, 3F, 3F, 0F, 18.5F, 8.5F, 0F);
        box(root, "tail_rotor_pivot", 24, 77, 0F, 0F, 0F, 1F, 2F, 1F, 18F, 8F, -0.5F);

        box(root, "head_neck", 0, 40, -1F, 0F, 0F, 1F, 6F, 3F, -7F, 4F, -1.5F, 0F, 0F, 0.2268928F);
        box(root, "head_back", 0, 49, 0F, 0F, 0F, 1F, 7F, 4F, -8.5F, 3.5F, -2F, 0F, 0F, 0.2268928F);
        box(
                root,
                "head_base",
                0,
                60,
                -2F,
                1F,
                0F,
                2F,
                6F,
                4F,
                -8.5F,
                3.5F,
                -2F,
                0F,
                0F,
                0.2268928F);
        box(
                root,
                "head_top",
                0,
                70,
                -2F,
                0F,
                0F,
                2F,
                2F,
                4F,
                -8.5F,
                3.5F,
                -2F,
                0F,
                0F,
                -0.2268928F);
        box(root, "head_front", 0, 76, 0F, 0F, 0F, 2F, 4F, 2F, -13F, 5F, -1F);
        box(root, "head_left", 0, 82, -3F, 0F, 0F, 3F, 4F, 1F, -10F, 5F, -2F, 0F, 0.3490659F, 0F);
        box(root, "head_right", 0, 87, -3F, 0F, -1F, 3F, 4F, 1F, -10F, 5F, 2F, 0F, -0.3490659F, 0F);
        box(
                root,
                "head_front_top",
                0,
                92,
                -3F,
                0F,
                0F,
                3F,
                1F,
                2F,
                -10.5F,
                4F,
                -1F,
                0F,
                0F,
                -0.3490659F);

        box(root, "torso_rotor_bottom", 0, 0, 0F, 0F, 0F, 3F, 1F, 1F, -7F, 11.5F, -0.5F);
        box(root, "torso_rotor_front", 0, 2, 0F, 0F, 0F, 1F, 3F, 1F, -8F, 9F, -0.5F);
        box(root, "torso_rotor_back", 0, 6, 0F, 0F, 0F, 1F, 2F, 1F, -4F, 10F, -0.5F);
        box(root, "torso_rotor_blades", 112, 120, -1.5F, -1.5F, 0F, 3F, 3F, 0F, -5.5F, 10F, 0F);
        box(root, "torso_rotor_pivot", 0, 9, 0F, 0F, 0F, 1F, 2F, 1F, -6F, 8.5F, -0.5F);

        box(root, "rotor_blades", 76, 68, -30F, 0F, -30F, 60F, 0F, 60F, 0F, 1.5F, 0F);

        box(root, "antenna_1", 0, 95, 0F, 0F, 0F, 4F, 1F, 1F, -14F, 4F, 0.5F);
        box(root, "antenna_2", 0, 97, 0F, 0F, 0F, 2F, 1F, 1F, -15F, 7F, 0F);

        return LayerDefinition.create(mesh, 256, 128);
    }

    private static void box(
            PartDefinition root,
            String name,
            int u,
            int v,
            float boxX,
            float boxY,
            float boxZ,
            float width,
            float height,
            float depth,
            float pivotX,
            float pivotY,
            float pivotZ) {
        box(
                root, name, u, v, boxX, boxY, boxZ, width, height, depth, pivotX, pivotY, pivotZ,
                0F, 0F, 0F);
    }

    private static void box(
            PartDefinition root,
            String name,
            int u,
            int v,
            float boxX,
            float boxY,
            float boxZ,
            float width,
            float height,
            float depth,
            float pivotX,
            float pivotY,
            float pivotZ,
            float xRot,
            float yRot,
            float zRot) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create()
                        .texOffs(u, v)
                        .addBox(boxX, boxY, boxZ, width, height, depth),
                PartPose.offsetAndRotation(pivotX, pivotY, pivotZ, xRot, yRot, zRot));
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        super.setupAnim(state);
        float spin = state.ageInTicks * ROTOR_SPEED;
        mainRotor.yRot = spin;
        torsoRotor.zRot = spin;
        tailRotor.zRot = spin;
    }
}
