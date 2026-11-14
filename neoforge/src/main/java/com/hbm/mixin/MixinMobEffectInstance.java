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

import com.hbm.potion.CloudEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MixinMobEffectInstance implements CloudEffect {
    @Shadow @Final @Mutable public static Codec<MobEffectInstance> CODEC;
    @Shadow private @Nullable MobEffectInstance hiddenEffect;

    @Unique private boolean hbm$local;
    @Unique private @Nullable MobEffectInstance hbm$cloudEffect;

    @Override
    public void hbm$excludeFromCloud() {
        MobEffectInstance self = (MobEffectInstance) (Object) this;
        assert self.is(MobEffects.SLOWNESS) || self.is(MobEffects.WEAKNESS);
        hbm$local = true;
        hbm$cloudEffect = null;
    }

    @Override
    public @Nullable MobEffectInstance hbm$cloudEffect() {
        return hbm$local ? hbm$cloudEffect : (MobEffectInstance) (Object) this;
    }

    @Unique
    private static MobEffectInstance hbm$copyCloudEffect(MobEffectInstance source) {
        MobEffectInstance hidden = ((MixinMobEffectInstance) (Object) source).hiddenEffect;
        return new MobEffectInstance(
                source.getEffect(),
                source.getDuration(),
                source.getAmplifier(),
                source.isAmbient(),
                source.isVisible(),
                source.showIcon(),
                hidden == null ? null : hbm$copyCloudEffect(hidden));
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void hbm$mergeCloudEffect(
            MobEffectInstance incoming, CallbackInfoReturnable<Boolean> cir) {
        MixinMobEffectInstance source = (MixinMobEffectInstance) (Object) incoming;
        if (!hbm$local && !source.hbm$local) return;
        MobEffectInstance contribution = source.hbm$cloudEffect();
        if (!hbm$local) {
            hbm$cloudEffect = hbm$copyCloudEffect((MobEffectInstance) (Object) this);
            hbm$local = true;
        }
        if (contribution != null) {
            if (hbm$cloudEffect == null) hbm$cloudEffect = hbm$copyCloudEffect(contribution);
            else hbm$cloudEffect.update(contribution);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("RETURN"))
    private void hbm$copyCloudProjection(MobEffectInstance original, CallbackInfo ci) {
        MixinMobEffectInstance source = (MixinMobEffectInstance) (Object) original;
        hbm$local = source.hbm$local;
        if (source.hbm$cloudEffect != null)
            hbm$cloudEffect = hbm$copyCloudEffect(source.hbm$cloudEffect);
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void hbm$tickCloudEffect(
            ServerLevel level,
            LivingEntity target,
            Runnable onEffectUpdate,
            CallbackInfoReturnable<Boolean> cir) {
        if (hbm$cloudEffect != null && !hbm$cloudEffect.tickServer(level, target, () -> {})) {
            hbm$cloudEffect = null;
        }
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void hbm$cloudCodec(CallbackInfo ci) {
        Codec<MobEffectInstance> vanilla = CODEC;
        MapCodec<MobEffectInstance> vanillaMap =
                ((MapCodec.MapCodecCodec<MobEffectInstance>) vanilla).codec();
        CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                vanillaMap.forGetter(
                                                        (MobEffectInstance effect) -> effect),
                                                Codec.BOOL
                                                        .optionalFieldOf("hbm_local", false)
                                                        .forGetter(
                                                                effect ->
                                                                        ((MixinMobEffectInstance)
                                                                                        (Object)
                                                                                                effect)
                                                                                .hbm$local),
                                                vanilla.optionalFieldOf("hbm_cloud_effect")
                                                        .forGetter(
                                                                effect ->
                                                                        Optional.ofNullable(
                                                                                ((MixinMobEffectInstance)
                                                                                                (Object)
                                                                                                        effect)
                                                                                        .hbm$cloudEffect)))
                                        .apply(
                                                instance,
                                                (effect, local, contribution) -> {
                                                    MixinMobEffectInstance state =
                                                            (MixinMobEffectInstance)
                                                                    (Object) effect;
                                                    state.hbm$local = local;
                                                    state.hbm$cloudEffect =
                                                            contribution.orElse(null);
                                                    assert contribution.isEmpty()
                                                            || local
                                                                    && effect.getEffect()
                                                                            .equals(
                                                                                    contribution
                                                                                            .get()
                                                                                            .getEffect());
                                                    return effect;
                                                }));
    }
}
