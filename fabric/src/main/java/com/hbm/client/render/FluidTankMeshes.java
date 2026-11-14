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

package com.hbm.client.render;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CorrosiveTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

final class FluidTankMeshes {
    static final Identifier NONE = RegistryUtil.id("textures/block/models/tank/tank_none.png");
    static final Identifier FRAME = RegistryUtil.id("textures/block/models/machines/tank.png");
    static final Identifier INNER = RegistryUtil.id("textures/block/models/tank/tank_inner.png");
    private static final int INTACT_FRAME = ResourceManager.fluidtank.partId("Frame");
    private static final int INTACT_TANK = ResourceManager.fluidtank.partId("Tank");
    private static final int DAMAGED_FRAME = ResourceManager.fluidtank_exploded.partId("Frame");
    private static final int DAMAGED_TANK = ResourceManager.fluidtank_exploded.partId("Tank");
    static final int TANK_INNER = ResourceManager.fluidtank_exploded.partId("TankInner");

    private FluidTankMeshes() {}

    static HFRWavefrontObject mesh(boolean damaged) {
        return damaged ? ResourceManager.fluidtank_exploded : ResourceManager.fluidtank;
    }

    static int frame(boolean damaged) {
        return damaged ? DAMAGED_FRAME : INTACT_FRAME;
    }

    static int tank(boolean damaged) {
        return damaged ? DAMAGED_TANK : INTACT_TANK;
    }

    static Identifier texture(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return NONE;
        CorrosiveTrait corrosive = NTMFluidProperties.getTrait(fluid, CorrosiveTrait.class);
        boolean danger =
                NTMFluidProperties.hasTrait(fluid, FT_Amat.class)
                        || corrosive != null && corrosive.severe();
        Identifier texture =
                danger
                        ? RegistryUtil.id("textures/models/tank/tank_danger.png")
                        : NTMFluids.sprite(fluid, "textures/models/tank/tank_", ".png");
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
                ? texture
                : NONE;
    }
}
