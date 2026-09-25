// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.util.Mth;

public final class RenderSecureDoor implements DoorRenderer {
    public static final RenderSecureDoor INSTANCE = new RenderSecureDoor();
    private static final int DOOR = ResourceManager.pheo_secure_door.partId("Door");

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxRaise = 3.5;
        double raise = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_secure_door, DOOR)
                        .texture(DoorDecl.SECURE_ACCESS_DOOR.getSkinFromIndex(state.skinIndex));
        door.pose.translate(0F, 1F, 0F).translate(0F, (float) Mth.clamp(raise, 0D, maxRaise), 0F);
    }
}
