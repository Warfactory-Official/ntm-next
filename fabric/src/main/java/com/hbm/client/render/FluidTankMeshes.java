// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

final class FluidTankMeshes {
    static final Identifier NONE = Library.id("textures/block/models/tank/tank_none.png");
    static final Identifier FRAME = Library.id("textures/block/models/machines/tank.png");
    static final Identifier INNER = Library.id("textures/block/models/tank/tank_inner.png");
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
        FT_Corrosive corrosive = NTMFluidProperties.getTrait(fluid, FT_Corrosive.class);
        boolean danger =
                NTMFluidProperties.hasTrait(fluid, FT_Amat.class)
                        || corrosive != null && corrosive.isHighlyCorrosive();
        String name = danger ? "danger" : NTMFluids.spritePath(fluid);
        Identifier texture = Library.id("textures/models/tank/tank_" + name + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
                ? texture
                : NONE;
    }
}
