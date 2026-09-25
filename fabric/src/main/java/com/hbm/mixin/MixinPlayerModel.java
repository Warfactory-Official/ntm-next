// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.ArmorWorldRenderer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class MixinPlayerModel {

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL"))
    private void hbm$hideHatUnderObjHelmet(AvatarRenderState state, CallbackInfo ci) {
        if (ArmorWorldRenderer.hidesSkin(state.headEquipment))
            ((PlayerModel) (Object) this).hat.visible = false;
        if (ArmorWorldRenderer.hidesSkin(state.chestEquipment)) {
            ((PlayerModel) (Object) this).jacket.visible = false;
            ((PlayerModel) (Object) this).leftSleeve.visible = false;
            ((PlayerModel) (Object) this).rightSleeve.visible = false;
        }
        if (ArmorWorldRenderer.hidesSkin(state.legsEquipment)
                || ArmorWorldRenderer.hidesSkin(state.feetEquipment)) {
            ((PlayerModel) (Object) this).leftPants.visible = false;
            ((PlayerModel) (Object) this).rightPants.visible = false;
        }
    }
}
