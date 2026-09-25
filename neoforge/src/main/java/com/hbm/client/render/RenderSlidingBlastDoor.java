// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import net.minecraft.util.Mth;

public final class RenderSlidingBlastDoor implements DoorRenderer {
    public static final RenderSlidingBlastDoor INSTANCE = new RenderSlidingBlastDoor();
    private static final int LEFT_DOOR = ResourceManager.pheo_blast_door.partId("LeftDoor");
    private static final int RIGHT_DOOR = ResourceManager.pheo_blast_door.partId("RightDoor");
    private static final int LEFT_LOCK = ResourceManager.pheo_blast_door.partId("LeftLock");
    private static final int RIGHT_LOCK = ResourceManager.pheo_blast_door.partId("RightLock");

    private static void leaf(DoorFrame frame, float travel, int door, int lockPart, double lock) {
        frame.push()
                .obj(ResourceManager.pheo_blast_door, door)
                .texture(ResourceManager.pheo_blast_door_tex)
                .slide(0F, 0F, travel)
                .slab(0F, 0F, 1F, 2.5F);

        float swing = DoorRenderer.rad(90D + lock);
        float side = travel < 0F ? -1F : 1F;
        DoorFrame.Part pin =
                frame.push()
                        .obj(ResourceManager.pheo_blast_door, lockPart)
                        .texture(ResourceManager.pheo_blast_door_tex)
                        .slide(0F, -1.8125F, 0F)
                        .halfspace(
                                0F,
                                side * (float) Math.sin(swing),
                                side * (float) Math.cos(swing),
                                2.5F - Math.abs(travel));
        pin.pose.translate(0F, 0F, travel).translate(0F, 1.8125F, 0F).rotateX(swing);
    }

    @Override
    public boolean culls() {
        return false;
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double maxOpen = 2.125;
        double open = 0;
        double lock = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) {
            open = maxOpen;
            lock = 90;
        }

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
            lock = state.bus("LOCK")[0] * 90;
        }

        float travel = (float) Mth.clamp(open, 0D, maxOpen);
        leaf(frame, travel, LEFT_DOOR, RIGHT_LOCK, lock);
        leaf(frame, -travel, RIGHT_DOOR, LEFT_LOCK, lock);
    }
}
