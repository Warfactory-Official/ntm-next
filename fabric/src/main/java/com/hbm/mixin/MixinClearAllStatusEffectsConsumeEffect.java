// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
