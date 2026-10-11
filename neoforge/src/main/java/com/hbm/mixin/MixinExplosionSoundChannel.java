// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.sound.OriginalStereoExplosion;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.SOFTSourceSpatialize;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class MixinExplosionSoundChannel implements OriginalStereoExplosion {
    @Shadow @Final public int source;
    @Unique private boolean hbm$explosion;
    @Unique private boolean hbm$stereo;
    @Unique private float hbm$volume;
    @Unique private float hbm$distance = Float.POSITIVE_INFINITY;
    @Unique private double hbm$x, hbm$y, hbm$z;

    @Override
    public void hbm$originalStereoExplosion() {
        hbm$explosion = true;
    }

    @ModifyVariable(method = "setVolume", at = @At("HEAD"), argsOnly = true)
    private float hbm$volume(float volume) {
        if (!hbm$explosion) return volume;
        hbm$volume = volume;
        return hbm$stereo ? hbm$attenuatedVolume() : volume;
    }

    @Inject(method = "linearAttenuation", at = @At("TAIL"))
    private void hbm$distance(float maxDistance, CallbackInfo ci) {
        if (hbm$explosion) hbm$distance = maxDistance;
    }

    @Inject(method = "disableAttenuation", at = @At("TAIL"))
    private void hbm$noDistance(CallbackInfo ci) {
        if (hbm$explosion) hbm$distance = Float.POSITIVE_INFINITY;
    }

    @Inject(method = "setSelfPosition", at = @At("HEAD"))
    private void hbm$position(Vec3 position, CallbackInfo ci) {
        if (!hbm$explosion) return;
        hbm$x = position.x;
        hbm$y = position.y;
        hbm$z = position.z;
    }

    @Inject(method = "attachStaticBuffer", at = @At("HEAD"))
    private void hbm$staticBuffer(SoundBuffer buffer, CallbackInfo ci) {
        hbm$buffer(buffer.format().getChannels());
    }

    @Inject(method = "attachBufferStream", at = @At("HEAD"))
    private void hbm$stream(AudioStream stream, CallbackInfo ci) {
        hbm$buffer(stream.getFormat().getChannels());
    }

    @Unique
    private void hbm$buffer(int channels) {
        if (!hbm$explosion || channels != 2) return;
        hbm$stereo = true;
        if (AL.getCapabilities().AL_SOFT_source_spatialize) {
            AL10.alSourcei(source, SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT, AL10.AL_FALSE);
        }
        AL10.alSourcef(source, AL10.AL_GAIN, hbm$attenuatedVolume());
    }

    @Inject(method = "updateStream", at = @At("HEAD"))
    private void hbm$updateVolume(CallbackInfo ci) {
        if (hbm$stereo) AL10.alSourcef(source, AL10.AL_GAIN, hbm$attenuatedVolume());
    }

    @Unique
    private float hbm$attenuatedVolume() {
        if (hbm$distance == Float.POSITIVE_INFINITY) return hbm$volume;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var listener = stack.mallocFloat(3);
            AL10.alGetListenerfv(AL10.AL_POSITION, listener);
            double dx = hbm$x - listener.get(0);
            double dy = hbm$y - listener.get(1);
            double dz = hbm$z - listener.get(2);
            return hbm$volume
                    * (float)
                            Math.max(
                                    0D, 1D - Math.sqrt(dx * dx + dy * dy + dz * dz) / hbm$distance);
        }
    }
}
