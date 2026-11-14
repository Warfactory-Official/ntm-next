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

package com.hbm.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

public class BoatRubberModel extends EntityModel<BoatRenderState> {

    private static final float QUARTER_TURN = (float) (Math.PI / 2D);

    public BoatRubberModel(ModelPart root) {
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
