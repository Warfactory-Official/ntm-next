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
