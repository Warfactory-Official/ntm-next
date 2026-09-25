// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.MotionRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public abstract class MixinHumanoidRenderState implements MotionRenderState {

    @Unique private boolean hbm$onGround;
    @Unique private double hbm$motionY;

    @Override
    public boolean hbm$onGround() {
        return hbm$onGround;
    }

    @Override
    public double hbm$motionY() {
        return hbm$motionY;
    }

    @Override
    public void hbm$setMotion(boolean onGround, double motionY) {
        hbm$onGround = onGround;
        hbm$motionY = motionY;
    }
}
