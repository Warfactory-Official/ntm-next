// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.ManlyPlayerLayer;
import com.hbm.client.render.PlayerCapes;
import com.hbm.main.Polaroid;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
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
        state.hbm$setAppearance(
                avatar instanceof LocalPlayer local
                        ? PlayerAppearancePayload.fromEffects(local)
                        : avatar instanceof Player player ? player.hbm$appearance() : (byte) 0);
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
}
