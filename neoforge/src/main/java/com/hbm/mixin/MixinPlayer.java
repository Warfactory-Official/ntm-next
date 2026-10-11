// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.IStepTickReceiver;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayer implements PlayerAppearance {

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

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$stepTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getAbilities().flying) return;

        BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.01D, player.getZ());
        if (player.level().getBlockState(pos).getBlock() instanceof IStepTickReceiver step) {
            step.onPlayerStep(player.level(), pos, player);
        }
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float hbm$acceptedDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isInvulnerableTo(level, source)) return amount;
        float result = EntityDamageUtil.modifyAcceptedDamage(self, source, amount);
        self.damageContainers.peek().setNewDamage(result);
        return result;
    }
}
