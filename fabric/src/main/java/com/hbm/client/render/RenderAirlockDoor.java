// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class RenderAirlockDoor implements DoorRenderer {
    public static final RenderAirlockDoor INSTANCE = new RenderAirlockDoor();
    private static final int LEFT = ResourceManager.pheo_airlock_door.partId("Left");
    private static final int RIGHT = ResourceManager.pheo_airlock_door.partId("Right");

    private static void leaf(DoorFrame frame, Identifier texture, int part, float travel) {
        DoorFrame.Part leaf =
                frame.push()
                        .obj(ResourceManager.pheo_airlock_door, part)
                        .texture(texture)
                        .slide(0F, 0F, travel)
                        .slab(0F, 0F, 1F, 1.999F);
        leaf.pose.translate(0F, 0F, 0.5F);
    }

    @Override
    public boolean culls() {
        return false;
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxOpen = 1.5;
        double open = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
        }

        float travel = (float) Mth.clamp(open, 0D, maxOpen);
        Identifier texture = DoorDecl.ROUND_AIRLOCK_DOOR.getSkinFromIndex(state.skinIndex);

        leaf(frame, texture, LEFT, travel);
        leaf(frame, texture, RIGHT, -travel);
    }
}
