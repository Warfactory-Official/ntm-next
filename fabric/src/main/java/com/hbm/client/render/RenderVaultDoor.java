// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;

public final class RenderVaultDoor implements DoorRenderer {
    public static final RenderVaultDoor INSTANCE = new RenderVaultDoor();
    private static final int DOOR = ResourceManager.pheo_vault_door.partId("Door");
    private static final int LABEL = ResourceManager.pheo_vault_door.partId("Label");

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double pull = 0;
        double slide = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) {
            pull = 1;
            slide = 1;
        }

        if (state.animation != null) {
            pull = state.bus("PULL")[2];
            slide = state.bus("SLIDE")[0];
        }

        double diameter = 4.25D;
        double circumference = diameter * Math.PI;
        slide *= 5D;
        double roll = 360D * slide / circumference;

        DoorDecl decl = DoorDecl.VAULT_DOOR;
        DoorFrame.Part door =
                frame.push()
                        .obj(ResourceManager.pheo_vault_door, DOOR)
                        .texture(decl.getTextureForPart(state.skinIndex, "Door"));
        door.pose
                .translate((float) -pull, 0F, 0F)
                .translate(0F, 0F, (float) slide)
                .translate(0F, 2.5F, 0F)
                .rotateX(DoorRenderer.rad(roll))
                .translate(0F, -2.5F, 0F);

        frame.push()
                .obj(ResourceManager.pheo_vault_door, LABEL)
                .texture(decl.getTextureForPart(state.skinIndex, "Label"))
                .pose
                .set(door.pose);
    }
}
