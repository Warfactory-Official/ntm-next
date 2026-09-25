// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.animloader.AnimatedModel;
import com.hbm.animloader.Animation;
import com.hbm.animloader.AnimationWrapper;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityDoorGeneric;

public final class RenderTransitionSeal implements DoorRenderer {

    public static final RenderTransitionSeal INSTANCE = new RenderTransitionSeal();

    @Override
    public boolean blends() {
        return true;
    }

    @Override
    public void emit(DoorState state, DoorFrame frame) {
        long time = state.animMillis;

        AnimationWrapper wrapper = state.wrapper(Seal.ANIM);
        wrapper.startTime =
                state.doorState > BlockEntityDoorGeneric.STATE_OPEN ? state.animStartTime : time;
        wrapper.reverse =
                state.doorState == BlockEntityDoorGeneric.STATE_OPEN
                        || state.doorState == BlockEntityDoorGeneric.STATE_CLOSING;
        wrapper.prevFrame = 0;

        state.walkArmature(
                Seal.MODEL, time, wrapper, ResourceManager.transition_seal_tex, 0F, 0F, 0.5F);
    }

    private static final class Seal {
        static final AnimatedModel MODEL = ResourceManager.transition_seal();
        static final Animation ANIM = ResourceManager.transition_seal_anim();
    }
}
