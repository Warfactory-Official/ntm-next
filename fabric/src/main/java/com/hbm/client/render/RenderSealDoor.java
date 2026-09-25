// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.util.Mth;

public final class RenderSealDoor implements DoorRenderer {
    public static final RenderSealDoor INSTANCE = new RenderSealDoor();
    private static final int DOOR = ResourceManager.pheo_seal_door.partId("Door");

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxRaise = 1;
        double raise = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        float travel = (float) (DoorDecl.smoothstep((float) Mth.clamp(raise, 0D, maxRaise)) * 0.9D);
        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_seal_door, DOOR)
                        .texture(ResourceManager.pheo_seal_door_tex)
                        .slide(0F, 0F, travel)
                        .halfspace(0F, 0F, 1F, 0.5001F);
        door.pose.translate(0.5F, 0F, 0F);
    }
}
