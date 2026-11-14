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

import com.hbm.client.render.ManlyPlayerLayer;
import com.hbm.client.render.PlayerCapes;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.main.Polaroid;
import com.hbm.packet.PlayerAppearanceSync;
import com.hbm.render.util.RiderTilt;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer
        extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {

    protected MixinAvatarRenderer(
            EntityRendererProvider.Context context, PlayerModel model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$addManlyLayer(
            EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        addLayer(new ManlyPlayerLayer(this));
    }

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
                            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void hbm$extractAppearance(
            Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        ((com.hbm.interfaces.injected.PlayerAppearance) (state))
                .hbm$setAppearance(
                        avatar instanceof LocalPlayer local
                                ? PlayerAppearanceSync.fromPlayer(local)
                                : avatar instanceof Player player
                                        ? ((com.hbm.interfaces.injected.PlayerAppearance) (player))
                                                .hbm$appearance()
                                        : (byte) 0);
        if (!state.isFallFlying
                && (((com.hbm.interfaces.injected.PlayerAppearance) (state)).hbm$appearance()
                                & PlayerAppearance.ARMOR_FLIGHT)
                        != 0) {
            state.isFallFlying = true;
            state.pose = Pose.FALL_FLYING;
            state.fallFlyingTimeInTicks = 10F;
            state.isCrouching = false;
            state.walkAnimationSpeed = 0F;
            state.capeLean = 0F;
        }
        if (avatar instanceof Player player) {
            ClientAsset.ResourceTexture cape =
                    PlayerCapes.forPlayer(
                            player.getUUID(),
                            player.getDisplayName().getString(),
                            Polaroid.isBalefireDay());
            if (cape != null) {
                PlayerSkin skin = state.skin;
                state.skin =
                        new PlayerSkin(
                                skin.body(), cape, skin.elytra(), skin.model(), skin.secure());
            }
        }
    }

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
                            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void hbm$extractRiderTilt(
            Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        Entity vehicle = avatar.getVehicle();
        ((com.hbm.client.render.RiderTiltRenderState) (state))
                .hbm$setRiderTilt(
                        vehicle == null
                                ? 0F
                                : RiderTilt.POINT.get().degrees(vehicle, partialTicks));
    }
}
