// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

public abstract class FluidTrait {

    public static final List<Class<? extends FluidTrait>> traitList = new ArrayList<>();
    public static final Map<String, Class<? extends FluidTrait>> traitNameMap = new HashMap<>();

    static {
        registerTrait("corrosive", FT_Corrosive.class);
        registerTrait("flammable", FT_Flammable.class);
        registerTrait("combustible", FT_Combustible.class);
        registerTrait("polluting", FT_Polluting.class);
        registerTrait("heatable", FT_Heatable.class);
        registerTrait("coolable", FT_Coolable.class);
        registerTrait("pwrmoderator", FT_PWRModerator.class);
        registerTrait("poison", FT_Poison.class);
        registerTrait("toxin", FT_Toxin.class);
        registerTrait("ventradiation", FT_VentRadiation.class);
        registerTrait("pheromone", FT_Pheromone.class);
        registerTrait("gaseous", FT_Gaseous.class);
        registerTrait("gaseous_art", FT_Gaseous_ART.class);
        registerTrait("liquid", FT_Liquid.class);
        registerTrait("viscous", FT_Viscous.class);
        registerTrait("plasma", FT_Plasma.class);
        registerTrait("amat", FT_Amat.class);
        registerTrait("leadcontainer", FT_LeadContainer.class);
        registerTrait("delicious", FT_Delicious.class);
        registerTrait("noid", FT_NoID.class);
        registerTrait("nocontainer", FT_NoContainer.class);
        registerTrait("unsiphonable", FT_Unsiphonable.class);
        registerTrait("canister", CD_Canister.class);
        registerTrait("gastank", CD_Gastank.class);
    }

    private static void registerTrait(String name, Class<? extends FluidTrait> clazz) {
        traitNameMap.put(name, clazz);
        traitList.add(clazz);
    }

    public void addInfo(List<String> info) {}

    public void addInfoHidden(List<String> info) {}

    public void onFluidRelease(
            Level level,
            BlockPos pos,
            FluidTankNTM tank,
            int overflowAmount,
            FluidReleaseType type) {}

    public static void onRelease(
            Level level,
            BlockPos pos,
            Fluid fluid,
            FluidTankNTM tank,
            FluidReleaseType release,
            int mB) {
        NTMFluidProperty property = NTMFluidProperties.get(fluid);
        if (property == null) return;
        for (FluidTrait trait : property.traits())
            trait.onFluidRelease(level, pos, tank, mB, release);
    }

    public enum FluidReleaseType {
        VOID,
        BURN,
        SPILL
    }
}
