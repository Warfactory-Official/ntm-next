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

import com.hbm.interfaces.injected.QuadLightOriginAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedQuad.MaterialInfo.class)
public class MixinMaterialInfo implements QuadLightOriginAccess {
    @Unique private int hbm$lightOrigin;

    @Override
    public int hbm$lightOrigin() {
        return hbm$lightOrigin;
    }

    @Override
    public void hbm$setLightOrigin(int origin) {
        hbm$lightOrigin = origin;
    }

    @ModifyReturnValue(method = "equals", at = @At("RETURN"))
    private boolean hbm$compareLighting(boolean equal, Object other) {
        return equal && hbm$lightOrigin == ((QuadLightOriginAccess) other).hbm$lightOrigin();
    }

    @ModifyReturnValue(method = "hashCode", at = @At("RETURN"))
    private int hbm$hashLighting(int hash) {
        return 31 * hash + hbm$lightOrigin;
    }
}
