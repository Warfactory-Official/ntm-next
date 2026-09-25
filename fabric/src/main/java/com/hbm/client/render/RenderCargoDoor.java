// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class RenderCargoDoor implements DoorRenderer {
    public static final RenderCargoDoor INSTANCE = new RenderCargoDoor();
    private static final int DOOR_TOP = ResourceManager.pheo_cargo_door.partId("DoorTop");
    private static final int DOOR_BOT = ResourceManager.pheo_cargo_door.partId("DoorBot");

    private static void leaf(DoorFrame frame, Identifier texture, int part, float raise) {
        frame.push()
                .obj(ResourceManager.pheo_cargo_door, part)
                .texture(texture)
                .pose
                .translate(0F, raise, 0F);
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        double botMove = 0;
        double topMove = 0;
        if (state.doorState == BlockEntityDoorGeneric.STATE_OPEN) {
            botMove = 2.0;
            topMove = 1.0;
        }

        if (state.animation != null) {
            botMove = Mth.clamp(state.bus("BOT")[1], 0D, 1D) * 2.0;
            topMove = Mth.clamp(state.bus("TOP")[1], 0D, 1D);
        }

        Identifier texture = DoorDecl.CARGO_DOOR.getSkinFromIndex(state.skinIndex);
        leaf(frame, texture, DOOR_TOP, (float) topMove);
        leaf(frame, texture, DOOR_BOT, (float) botMove);
    }
}
