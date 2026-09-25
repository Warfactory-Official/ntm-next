// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.util.Mth;

public final class RenderFireDoor implements DoorRenderer {
    public static final RenderFireDoor INSTANCE = new RenderFireDoor();
    private static final int DOOR = ResourceManager.pheo_fire_door.partId("Door");

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxRaise = 2.75;
        double raise = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_fire_door, DOOR)
                        .texture(DoorDecl.FIRE_DOOR.getSkinFromIndex(state.skinIndex));
        door.pose
                .rotateY(DoorRenderer.rad(90))
                .translate(-0.5F, 0F, 0F)
                .translate(0F, (float) Mth.clamp(raise, 0D, maxRaise), 0F);
    }
}
