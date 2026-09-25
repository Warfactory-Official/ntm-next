// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.material.MatDistribution;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import com.hbm.inventory.recipes.crafting.CargoShellRecipe;
import com.hbm.inventory.recipes.crafting.ContainerUpgradeRecipe;
import com.hbm.inventory.recipes.crafting.DuctRetypeRecipe;
import com.hbm.inventory.recipes.crafting.DuctUntypeRecipe;
import com.hbm.inventory.recipes.crafting.GrenadeCraftingRecipe;
import com.hbm.inventory.recipes.crafting.MkuRecipe;
import com.hbm.inventory.recipes.crafting.TagResultRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.registration.IRegistrar;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {

    private ModRecipes() {}

    private static void registerAllHandlers() {
        List<SerializableRecipe> handlers = SerializableRecipe.recipeHandlers;
        handlers.clear();

        handlers.add(CentrifugeRecipes.INSTANCE);

        handlers.add(GasCentrifugeRecipes.INSTANCE);
        handlers.add(BlastFurnaceRecipesNT.INSTANCE);
        handlers.add(RockMillRecipes.INSTANCE);
        handlers.add(SuperComputerRecipes.INSTANCE);
        handlers.add(SpaceAssemblerRecipes.INSTANCE);
        handlers.add(ReformingRecipes.INSTANCE);
        handlers.add(HydrotreatingRecipes.INSTANCE);
        handlers.add(OutgasserRecipes.INSTANCE);
        handlers.add(CrucibleRecipes.INSTANCE);
        handlers.add(AssemblyMachineRecipes.INSTANCE);
        handlers.add(PrecAssRecipes.INSTANCE);
        handlers.add(ChemicalPlantRecipes.INSTANCE);
        handlers.add(PUREXRecipes.INSTANCE);
        handlers.add(FusionRecipes.INSTANCE);
        handlers.add(PlasmaForgeRecipes.INSTANCE);
        handlers.add(RefineryRecipes.INSTANCE);
        handlers.add(VacuumRefineryRecipes.INSTANCE);
        handlers.add(FractionRecipes.INSTANCE);
        handlers.add(CrackingRecipes.INSTANCE);
        handlers.add(RadiolysisRecipes.INSTANCE);
        handlers.add(FluidBreederRecipes.INSTANCE);
        handlers.add(CompressorRecipes.INSTANCE);
        handlers.add(ElectrolyserFluidRecipes.INSTANCE);
        handlers.add(ElectrolyserMetalRecipes.INSTANCE);
        handlers.add(CombinationRecipes.INSTANCE);
        handlers.add(MixerRecipes.INSTANCE);
        handlers.add(CrystallizerRecipes.INSTANCE);
        handlers.add(PressRecipes.INSTANCE);
        handlers.add(ShredderRecipes.INSTANCE);
        handlers.add(BreederRecipes.INSTANCE);
        handlers.add(CyclotronRecipes.INSTANCE);
        handlers.add(FuelPoolRecipes.INSTANCE);
        handlers.add(RotaryFurnaceRecipes.INSTANCE);
        handlers.add(ArcWelderRecipes.INSTANCE);
        handlers.add(ExposureChamberRecipes.INSTANCE);
        handlers.add(ParticleAcceleratorRecipes.INSTANCE);
        handlers.add(CokerRecipes.INSTANCE);
        handlers.add(LiquefactionRecipes.INSTANCE);
        handlers.add(SolidificationRecipes.INSTANCE);
        handlers.add(PyroOvenRecipes.INSTANCE);
        handlers.add(MagicRecipes.INSTANCE);
        handlers.add(LemegetonRecipes.INSTANCE);
        handlers.add(SolderingRecipes.INSTANCE);
        handlers.add(CustomMachineRecipes.INSTANCE);
        handlers.add(AmmoPressRecipes.INSTANCE);
        handlers.add(PedestalRecipes.INSTANCE);
        handlers.add(SILEXRecipes.INSTANCE);
        handlers.add(AnvilConstructionRecipes.INSTANCE);
        handlers.add(AnvilSmithingRecipes.INSTANCE);
        handlers.add(AnnihilatorRecipes.INSTANCE);

        handlers.add(MatDistribution.INSTANCE);

        handlers.add(ArcFurnaceRecipes.INSTANCE);
    }

    public static void register(IRegistrar r) {
        registerAllHandlers();
        for (SerializableRecipe table : SerializableRecipe.recipeHandlers) table.registerType(r);

        r.registerRecipeSerializer("rbmk_fuel_disassembly", RBMKFuelDisassemblyRecipe.SERIALIZER);
        r.registerRecipeSerializer("duct_retype", DuctRetypeRecipe.SERIALIZER);
        r.registerRecipeSerializer("duct_untype", DuctUntypeRecipe.SERIALIZER);
        r.registerRecipeSerializer("mku", MkuRecipe.SERIALIZER);
        r.registerRecipeSerializer("grenade_assembly", GrenadeCraftingRecipe.SERIALIZER);
        r.registerRecipeSerializer("cargo_shell", CargoShellRecipe.SERIALIZER);
        r.registerListedRecipeSerializer("container_upgrade", ContainerUpgradeRecipe.SERIALIZER);
        r.registerListedRecipeSerializer("tag_result", TagResultRecipe.SERIALIZER);
    }

    public static List<RecipeType<?>> datapackBackedTypes() {
        List<RecipeType<?>> types = new ArrayList<>();
        for (SerializableRecipe table : SerializableRecipe.recipeHandlers)
            types.add(table.datapackType());
        return types;
    }

    public static void bootstrap() {

        BlockEntityFusionPlasmaForge.registerBoosters();
    }

    public static void bootstrapClientRecipes() {
        BlockEntityFusionPlasmaForge.registerBoosters();
    }
}
