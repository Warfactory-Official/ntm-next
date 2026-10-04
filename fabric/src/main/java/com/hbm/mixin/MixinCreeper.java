// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.potion.CloudEffect;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Creeper.class)
public abstract class MixinCreeper {
    @ModifyExpressionValue(
            method = "spawnLingeringCloud",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/monster/Creeper;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> hbm$cloudEffects(Collection<MobEffectInstance> effects) {
        boolean changed = false;
        for (MobEffectInstance effect : effects) {
            if (((CloudEffect) effect).hbm$cloudEffect() != effect) {
                changed = true;
                break;
            }
        }
        if (!changed) return effects;
        ArrayList<MobEffectInstance> result = new ArrayList<>(effects.size());
        for (MobEffectInstance effect : effects) {
            MobEffectInstance contribution = ((CloudEffect) effect).hbm$cloudEffect();
            if (contribution != null) result.add(contribution);
        }
        return result;
    }
}
