// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.PlayerAppearance;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class MixinAvatarRenderState implements PlayerAppearance {

    @Unique private byte hbm$appearance;

    @Override
    public byte hbm$appearance() {
        return hbm$appearance;
    }

    @Override
    public void hbm$setAppearance(byte flags) {
        hbm$appearance = flags;
    }
}
