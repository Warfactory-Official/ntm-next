/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin;

import com.hbm.blocks.PlayerStepListener;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.items.armor.ArmorDamageHandler;
import com.hbm.packet.PlayerAppearanceSync;
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
        PlayerAppearanceSync.update(player);
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
        if (player.level().getBlockState(pos).getBlock() instanceof PlayerStepListener step) {
            step.onPlayerStep(player.level(), pos, player);
        }
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float hbm$acceptedDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isInvulnerableTo(level, source)) return amount;
        float result = ArmorDamageHandler.modifyAcceptedDamage(self, source, amount);
        self.damageContainers.peek().setNewDamage(result);
        return result;
    }
}
