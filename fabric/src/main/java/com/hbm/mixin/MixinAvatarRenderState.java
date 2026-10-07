// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.RiderTiltRenderState;
import com.hbm.interfaces.injected.PlayerAppearance;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class MixinAvatarRenderState implements PlayerAppearance, RiderTiltRenderState {

    @Unique private byte hbm$appearance;
    @Unique private float hbm$riderTilt;

    @Override
    public byte hbm$appearance() {
        return hbm$appearance;
    }

    @Override
    public void hbm$setAppearance(byte flags) {
        hbm$appearance = flags;
    }

    @Override
    public float hbm$riderTilt() {
        return hbm$riderTilt;
    }

    @Override
    public void hbm$setRiderTilt(float degrees) {
        hbm$riderTilt = degrees;
    }
}
