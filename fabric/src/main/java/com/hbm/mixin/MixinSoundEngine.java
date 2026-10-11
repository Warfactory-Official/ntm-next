// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.compat.SoundPhysicsOcclusion;
import com.hbm.client.sound.OriginalStereoExplosion;
import com.hbm.config.AudioConfig;
import com.hbm.registration.Reg;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Channel;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.SOFTSourceSpatialize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @WrapOperation(
            method = "play",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
                            ordinal = 0))
    private void hbm$spatialize(
            ChannelAccess.ChannelHandle handle,
            Consumer<Channel> setup,
            Operation<Void> original,
            @Local(argsOnly = true) SoundInstance instance) {
        if (!instance.isRelative()
                && Reg.isNtmNamespace(instance.getIdentifier().getNamespace())
                && !(instance instanceof EntityBoundSoundInstance bound
                        && bound.entity == Minecraft.getInstance().getCameraEntity())) {
            if (AudioConfig.originalStereoExplosions && hbm$isExplosion(instance)) {
                Consumer<Channel> prepare =
                        channel ->
                                ((OriginalStereoExplosion) (Object) channel)
                                        .hbm$originalStereoExplosion();
                setup = prepare.andThen(setup);
            }
            if (AL.getCapabilities().AL_SOFT_source_spatialize) {
                setup =
                        setup.andThen(
                                channel ->
                                        AL10.alSourcei(
                                                channel.source,
                                                SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT,
                                                AL10.AL_TRUE));
            }
        }
        var emitter = SoundPhysicsOcclusion.capture(instance);
        if (emitter != null)
            setup = setup.andThen(channel -> SoundPhysicsOcclusion.bind(channel.source, emitter));
        original.call(handle, setup);
    }

    @Unique
    private static boolean hbm$isExplosion(SoundInstance instance) {
        return instance.getIdentifier().getNamespace().equals("hbm")
                && switch (instance.getIdentifier().getPath()) {
                    case "entity.old_explosion",
                            "nuclear_explosion",
                            "rbmk_explosion",
                            "weapon.explosion_medium",
                            "weapon.explosion_large_near",
                            "weapon.explosion_large_far",
                            "weapon.explosion_small_near",
                            "weapon.explosion_small_far",
                            "weapon.explosion_tiny",
                            "weapon.muke_explosion",
                            "weapon.robin_explosion" ->
                            true;
                    default -> false;
                };
    }
}
