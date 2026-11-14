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

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class RubbleModel {

    private RubbleModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        shape(root, "shape1", 14, 6, 6, -7F, 1F, 2F, 0F, 0F);
        shape(root, "shape2", 6, 13, 5, -7F, -6F, -5F, 0F, 0F);
        shape(root, "shape3", 6, 6, 6, 1F, 1F, -5F, 0F, 0F);
        shape(root, "shape4", 14, 7, 4, -7F, -7F, 2F, 0.4363323F, 0F);
        shape(root, "shape5", 6, 6, 11, 0F, -6F, -5F, 0F, 0F);
        shape(root, "shape6", 8, 8, 8, -4F, -4F, -4F, 0F, 0F);
        shape(root, "shape7", 6, 5, 7, -7F, -5F, 1F, 0F, 0F);
        shape(root, "shape8", 12, 6, 4, -6F, -1F, 3F, 0F, -0.3490659F);
        shape(root, "shape9", 12, 6, 6, -6F, 2F, -3F, -0.2094395F, 0F);
        shape(root, "shape10", 6, 10, 4, -5F, -3F, -6F, 0F, -0.3490659F);

        return LayerDefinition.create(mesh, 16, 16);
    }

    private static void shape(
            PartDefinition root,
            String name,
            int w,
            int h,
            int d,
            float x,
            float y,
            float z,
            float yRot,
            float zRot) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, w, h, d),
                PartPose.offsetAndRotation(x, y, z, 0F, yRot, zRot));
    }
}
