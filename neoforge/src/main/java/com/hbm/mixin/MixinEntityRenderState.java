// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.VanishedRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class MixinEntityRenderState implements VanishedRenderState {

    @Unique private boolean hbm$vanished;

    @Override
    public boolean hbm$vanished() {
        return hbm$vanished;
    }

    @Override
    public void hbm$setVanished(boolean vanished) {
        hbm$vanished = vanished;
    }
}
