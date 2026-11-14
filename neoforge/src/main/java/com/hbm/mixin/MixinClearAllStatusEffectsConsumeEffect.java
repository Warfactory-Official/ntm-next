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

import com.hbm.potion.UncurableEffectInstance;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClearAllStatusEffectsConsumeEffect.class)
public abstract class MixinClearAllStatusEffectsConsumeEffect {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void hbm$preserveUncurableEffects(
            Level level,
            ItemStack stack,
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir) {
        if (!stack.is(Items.MILK_BUCKET)) return;
        if (entity.getActiveEffects().stream().noneMatch(UncurableEffectInstance.class::isInstance))
            return;
        boolean removed = false;
        for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
            if (!(effect instanceof UncurableEffectInstance))
                removed |= entity.removeEffect(effect.getEffect());
        }
        cir.setReturnValue(removed);
    }
}
