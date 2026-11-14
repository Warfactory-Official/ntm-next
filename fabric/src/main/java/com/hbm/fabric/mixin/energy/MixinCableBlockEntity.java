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

package com.hbm.fabric.mixin.energy;

import com.hbm.fabric.energy.TechRebornCable;
import com.hbm.fabric.energy.TechRebornEnergy;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import techreborn.blockentity.cable.CableBlockEntity;
import techreborn.init.TRContent;

@Mixin(value = CableBlockEntity.class, remap = false)
public abstract class MixinCableBlockEntity implements TechRebornCable {
    @Unique private TechRebornEnergy.@Nullable Component hbm$component;

    @Shadow
    abstract TRContent.Cables getCableType();

    @Override
    public TechRebornEnergy.@Nullable Component hbm$component() {
        return hbm$component;
    }

    @Override
    public void hbm$component(TechRebornEnergy.Component component) {
        hbm$component = component;
    }

    @Override
    public long hbm$transferRate() {
        return getCableType().transferRate;
    }
}
