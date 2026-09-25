// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.util.Mth;

public final class RenderContainmentDoor implements DoorRenderer {
    public static final RenderContainmentDoor INSTANCE = new RenderContainmentDoor();
    private static final int DOOR = ResourceManager.pheo_containment_door.partId("Door");

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxRaise = 2.25;
        double raise = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        float travel = (float) Mth.clamp(raise, 0D, maxRaise);
        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_containment_door, DOOR)
                        .texture(DoorDecl.QE_CONTAINMENT.getSkinFromIndex(state.skinIndex))
                        .slide(0F, travel, 0F)
                        .halfspace(0F, 1F, 0F, 3F);
        door.pose.translate(0.25F, 0F, 0F);
    }
}
