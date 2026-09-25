// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class RenderWaterDoor implements DoorRenderer {
    public static final RenderWaterDoor INSTANCE = new RenderWaterDoor();
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
                .rotateZ(DoorRenderer.rad(bolt * 360D))
                .translate(-0.40625F, -pivotY, 0F);
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxRot = 120;
        double rot = 0;
        double bolt = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) {
            rot = maxRot;
            bolt = 1D;
        }

        if (state.animation != null) {
            rot = state.bus("DOOR")[1] * maxRot;
            bolt = state.bus("BOLT")[2];
        }

        Identifier texture = DoorDecl.WATER_DOOR.getSkinFromIndex(state.skinIndex);

        DoorFrame.Part leaf =
                frame.push().obj(ResourceManager.pheo_water_door, DOOR).texture(texture);
        Matrix4f swing =
                leaf.pose
                        .translate(0.375F, 0F, 0F)
                        .rotateY(DoorRenderer.rad(90))
                        .translate(-1.1875F, 0F, 0F)
                        .rotateY(DoorRenderer.rad(-rot))
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
