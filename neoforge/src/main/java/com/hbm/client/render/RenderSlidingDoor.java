// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import net.minecraft.util.Mth;

public final class RenderSlidingDoor implements DoorRenderer {
    public static final RenderSlidingDoor INSTANCE = new RenderSlidingDoor();
    private static final int LEFT = ResourceManager.pheo_sliding_door.partId("Left");
    private static final int RIGHT = ResourceManager.pheo_sliding_door.partId("Right");

    private static void leaf(DoorFrame frame, int part, float slide) {
        DoorFrame.Part leaf =
                frame.push()
                        .obj(ResourceManager.pheo_sliding_door, part)
                        .texture(ResourceManager.pheo_sliding_door_tex);
        leaf.pose.translate(0.53125F, 0.001F, 0.5F).translate(0F, 0F, slide);
    }

    @Override
    public boolean culls() {
        return false;
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxOpen = 0.95;
        double open = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
        }
        float slide = (float) Mth.clamp(open, 0D, maxOpen);

        leaf(frame, LEFT, slide);
        leaf(frame, RIGHT, -slide);
    }
}
