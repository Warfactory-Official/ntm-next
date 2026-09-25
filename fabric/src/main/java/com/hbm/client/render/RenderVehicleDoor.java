// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import net.minecraft.util.Mth;

public final class RenderVehicleDoor implements DoorRenderer {
    public static final RenderVehicleDoor INSTANCE = new RenderVehicleDoor();
    private static final int LEFT = ResourceManager.pheo_vehicle_door.partId("Left");
    private static final int RIGHT = ResourceManager.pheo_vehicle_door.partId("Right");

    private static void leaf(DoorFrame frame, int part, float travel) {
        DoorFrame.Part leaf =
                frame.push()
                        .obj(ResourceManager.pheo_vehicle_door, part)
                        .texture(ResourceManager.pheo_vehicle_door_tex)
                        .slide(travel, 0F, 0F)
                        .slab(1F, 0F, 0F, 3.4375F);
        leaf.pose.rotateY(DoorRenderer.rad(90));
    }

    @Override
    public boolean culls() {
        return false;
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxOpen = 3;
        double open = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
        }

        float travel = (float) Mth.clamp(open, 0D, maxOpen);
        leaf(frame, LEFT, -travel);
        leaf(frame, RIGHT, travel);
    }
}
