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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float hbm$interpolateViewPitch(float rotation, float partialTick) {
        return ((LocalPlayer) (Object) this).getXRot(partialTick);
    }

    @ModifyReturnValue(method = "getViewYRot", at = @At("RETURN"))
    private float hbm$interpolateViewYaw(float rotation, float partialTick) {
        var player = (LocalPlayer) (Object) this;
        return player.isPassenger() ? rotation : player.getYRot(partialTick);
    }
}
