// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.IStepTickReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayerStepTick {

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$stepTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getAbilities().flying) return;

        BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.01D, player.getZ());
        if (player.level().getBlockState(pos).getBlock() instanceof IStepTickReceiver step) {
            step.onPlayerStep(player.level(), pos, player);
        }
    }
}
