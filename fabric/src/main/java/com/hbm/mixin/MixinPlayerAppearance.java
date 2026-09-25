// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayerAppearance implements PlayerAppearance {

    @Unique private byte hbm$appearance;

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$refreshAppearance(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        PlayerAppearancePayload.update(player);
    }

    @Override
    public byte hbm$appearance() {
        return hbm$appearance;
    }

    @Override
    public void hbm$setAppearance(byte flags) {
        hbm$appearance = flags;
    }
}
