// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.blockentity.machine.fusion.BlockEntityFusionPlasmaForge;
import com.hbm.inventory.material.MatDistribution;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import com.hbm.inventory.recipes.crafting.*;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.NeoForgeRegistrar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;
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

    public static void addon(SerializableRecipe table) {
        table.registerType();
        SerializableRecipe.recipeHandlers.add(table);
    }

    public static void register() {
        registerAllHandlers();
        for (SerializableRecipe table : SerializableRecipe.recipeHandlers) table.registerType();

        registerCustomCrafting("rbmk_fuel_disassembly", RBMKFuelDisassemblyRecipe.SERIALIZER);
        registerCustomCrafting("duct_retype", DuctRetypeRecipe.SERIALIZER);
        registerCustomCrafting("duct_untype", DuctUntypeRecipe.SERIALIZER);
        registerCustomCrafting("mku", MkuRecipe.SERIALIZER);
        registerCustomCrafting("grenade_assembly", GrenadeCraftingRecipe.SERIALIZER);
        registerCustomCrafting("cargo_shell", CargoShellRecipe.SERIALIZER);
        registerCustomCrafting("scraps_split", ScrapsSplittingRecipe.SERIALIZER);
        NeoForgeRegistrar.registerRecipeSerializer("crafting_shaped", StateShapedRecipe.SERIALIZER);
        registerListedCrafting("crafting_shapeless", StateShapelessRecipe.SERIALIZER);
        registerListedCrafting("tag_result", TagResultRecipe.SERIALIZER);
    }

    private static <T extends HbmCustomRecipe> void registerCustomCrafting(
            String name, RecipeSerializer<T> serializer) {
        NeoForgeRegistrar.registerRecipeSerializer(name, serializer);
    }

    private static <T extends HbmNormalCraftingRecipe> void registerListedCrafting(
            String name, RecipeSerializer<T> serializer) {
        NeoForgeRegistrar.registerRecipeSerializer(name, serializer);
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
