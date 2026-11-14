/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public class CrabModel extends EntityModel<LivingEntityRenderState> {

    private static final float[] LEG_YAW = {0.78539816F, -0.78539816F, -2.35619449F, 2.35619449F};
    private static final int[] LEG_SWING = {1, -1, -1, 1};

    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];

    public CrabModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
            feet[i] = root.getChild("foot" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        shell(root, "shell0", 1, 1, 4, 1, 4, -2F, -3F, -2F);
        shell(root, "shell1", 17, 1, 4, 1, 6, -2F, -4F, -3F);
        shell(root, "shell2", 33, 1, 3, 1, 3, -1.5F, -5F, -1.5F);
        shell(root, "shell3", 49, 1, 4, 1, 2, -2F, -4.5F, -1F);
        shell(root, "shell4", 1, 9, 6, 1, 4, -3F, -4F, -2F);
        shell(root, "shell17", 1, 25, 2, 1, 4, -1F, -4.5F, -2F);
        shell(root, "shell18", 17, 25, 5, 1, 3, -2.5F, -3.5F, -1.5F);
        shell(root, "shell19", 33, 25, 3, 1, 5, -1.5F, -3.5F, -2.5F);

        int[][] legUv = {{25, 9}, {41, 9}, {1, 17}, {17, 17}};
        int[][] footUv = {{33, 17}, {57, 9}, {41, 17}, {49, 17}};
        int[][] fangUv = {{17, 1}, {33, 9}, {49, 9}, {9, 17}};
        float[] fangYaw = {-0.6981317F, 0.87266463F, -2.26892803F, 2.44346095F};

        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild(
                    "leg" + i,
                    CubeListBuilder.create()
                            .texOffs(legUv[i][0], legUv[i][1])
                            .addBox(-0.5F, 0F, 2F, 1, 1, 3),
                    PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, LEG_YAW[i], 0F));
            root.addOrReplaceChild(
                    "foot" + i,
                    CubeListBuilder.create()
                            .texOffs(footUv[i][0], footUv[i][1])
                            .addBox(-0.5F, 1F, 4F, 1, 3, 1),
                    PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, LEG_YAW[i], 0F));
            root.addOrReplaceChild(
                    "fang" + i,
                    CubeListBuilder.create()
                            .texOffs(fangUv[i][0], fangUv[i][1])
                            .addBox(-0.5F, 0F, 1.5F, 1, 1, 1),
                    PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, fangYaw[i], 0F));
        }

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void shell(
            PartDefinition root,
            String name,
            int u,
            int v,
            int w,
            int h,
            int d,
            float x,
            float y,
            float z) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(u, v).addBox(0F, 0F, 0F, w, h, d),
                PartPose.offset(x, y, z));
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing =
                -(Mth.cos(state.walkAnimationPos * 0.6662F * 2F) * 0.4F)
                        * state.walkAnimationSpeed
                        * 1.5F;
        for (int i = 0; i < 4; i++) {
            legs[i].yRot = feet[i].yRot = LEG_YAW[i] + swing * LEG_SWING[i];
        }
    }
}
