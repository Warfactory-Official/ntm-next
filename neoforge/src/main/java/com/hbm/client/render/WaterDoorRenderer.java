/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.main.ResourceManager;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class WaterDoorRenderer implements DoorMotion {
    public static final WaterDoorRenderer INSTANCE = new WaterDoorRenderer();
    private static final int DOOR = ResourceManager.pheo_water_door.partId("Door_Cube.003");
    private static final int BOLTS = ResourceManager.pheo_water_door.partId("Bolts");
    private static final int TOP = ResourceManager.pheo_water_door.partId("Top");
    private static final int BOTTOM = ResourceManager.pheo_water_door.partId("Bottom");

    private static void wheel(
            DoorFrame frame,
            Identifier texture,
            Matrix4f swing,
            double bolt,
            float pivotY,
            int part) {
        frame.push()
                .obj(ResourceManager.pheo_water_door, part)
                .texture(texture)
                .pose
                .set(swing)
                .translate(0.40625F, pivotY, 0F)
                .rotateZ(DoorMotion.radians(bolt * 360D))
                .translate(-0.40625F, -pivotY, 0F);
    }

    @Override
    public void placeParts(DoorState state, DoorFrame frame) {
        double maxRot = 120;
        double rot = 0;
        double bolt = 0;
        if (state.doorState == DoorBlockEntity.OPEN) {
            rot = maxRot;
            bolt = 1D;
        }

        if (state.animation != null) {
            rot = state.bus("DOOR", 1) * maxRot;
            bolt = state.bus("BOLT", 2);
        }

        Identifier texture = state.look.getSkin(state.skinIndex);

        DoorFrame.Part leaf =
                frame.push().obj(ResourceManager.pheo_water_door, DOOR).texture(texture);
        Matrix4f swing =
                leaf.pose
                        .translate(0.375F, 0F, 0F)
                        .rotateY(DoorMotion.radians(90))
                        .translate(-1.1875F, 0F, 0F)
                        .rotateY(DoorMotion.radians(-rot))
                        .translate(1.1875F, 0F, 0F);

        frame.push()
                .obj(ResourceManager.pheo_water_door, BOLTS)
                .texture(texture)
                .pose
                .set(swing)
                .translate((float) (-0.4D * bolt), 0F, 0F);

        wheel(frame, texture, swing, bolt, 2.28125F, TOP);
        wheel(frame, texture, swing, bolt, 0.71875F, BOTTOM);
    }
}
