// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.gui.*;
import com.hbm.config.InteractionConfig;
import com.hbm.integration.recipeviewer.GenericMachinePage.Extras;
import com.hbm.integration.recipeviewer.GenericMachinePage.FluidView;
import com.hbm.integration.recipeviewer.GenericMachinePage.Layout;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBattery;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class RecipePages {

    public static final Identifier VANILLA_SMELTING = Identifier.withDefaultNamespace("smelting");

    public static final AnvilConstructionPage ANVIL_CONSTRUCTION = new AnvilConstructionPage();
    public static final CentrifugePage CENTRIFUGE = new CentrifugePage();
    public static final PressPage PRESS = new PressPage();
    public static final ShredderPage SHREDDER = new ShredderPage();
    public static final CyclotronPage CYCLOTRON = new CyclotronPage();
    public static final ExposureChamberPage EXPOSURE_CHAMBER = new ExposureChamberPage();
    public static final SolidificationPage SOLIDIFICATION = new SolidificationPage();
    public static final CokerPage COKER = new CokerPage();
    public static final RotaryFurnacePage ROTARY_FURNACE = new RotaryFurnacePage();
    public static final SolderingStationPage SOLDERING = new SolderingStationPage();
    public static final GrenadeAssemblyPage GRENADE_ASSEMBLY = new GrenadeAssemblyPage();
    public static final ParticleAcceleratorPage PARTICLE_ACCELERATOR =
            new ParticleAcceleratorPage();
    public static final GasCentrifugePage GAS_CENTRIFUGE = new GasCentrifugePage();
    public static final ZirnoxPage ZIRNOX = new ZirnoxPage();
    public static final PWRPage PWR = new PWRPage();
    public static final WatzFuelPage WATZ_FUEL = new WatzFuelPage();
    public static final RTGPage RTG = new RTGPage();
    public static final DeuteriumPage DEUTERIUM = new DeuteriumPage();
    public static final CastingPage CASTING = new CastingPage();
    public static final CrucibleAlloyingPage CRUCIBLE_ALLOYING = new CrucibleAlloyingPage();
    public static final CrucibleSmeltingPage CRUCIBLE_SMELTING = new CrucibleSmeltingPage();
    public static final RadiolysisPage RADIOLYSIS = new RadiolysisPage();
    public static final AnvilSmithingPage ANVIL_SMITHING = new AnvilSmithingPage();
    public static final MagicPage MAGIC = new MagicPage();
    public static final RefineryPage REFINERY = new RefineryPage();
    public static final SilexPage SILEX = new SilexPage();
    public static final FuelPoolPage FUEL_POOL = new FuelPoolPage();
    public static final RBMKDisassemblyPage RBMK_DISASSEMBLY = new RBMKDisassemblyPage();
    public static final WasteDecayPage WASTE_DECAY = new WasteDecayPage();
    public static final SatellitePage SATELLITE = new SatellitePage();
    public static final ToolingPage TOOLING = new ToolingPage();
    public static final ConstructionPage CONSTRUCTION = new ConstructionPage();
    public static final AnnihilatorPage ANNIHILATOR = new AnnihilatorPage();
    public static final OreSlopperPage ORE_SLOPPER = new OreSlopperPage();
    public static final OreGenerationPage ORE_GENERATION = new OreGenerationPage();
    public static final BoilingPage BOILING = new BoilingPage();
    public static final SawmillPage SAWMILL = new SawmillPage();
    public static final AshpitPage ASHPIT = new AshpitPage();

    public static final GenericMachinePage<GenericRecipe> ASSEMBLY =
            GenericMachinePage.<GenericRecipe>builder(
                            "assembly_machine",
                            GenericRecipe.class,
                            AssemblyMachineRecipes.INSTANCE,
                            "block.hbm.machine_assembly_machine",
                            ModBlocks.MACHINE_ASSEMBLY_MACHINE.get(),
                            Layout.ASSEMBLY)
                    .secretRows()
                    .build();
    public static final GenericMachinePage<SpaceAssemblerRecipe> SPACE_ASSEMBLER =
            GenericMachinePage.builder(
                            "space_assembler",
                            SpaceAssemblerRecipe.class,
                            SpaceAssemblerRecipes.INSTANCE,
                            "item.hbm.satellite_science_assembler",
                            ModItems.SATELLITE_SCIENCE_ASSEMBLER.get(),
                            Layout.GENERIC)
                    .secretRows()
                    .build();
    public static final GenericMachinePage<ChemicalPlantRecipe> CHEMICAL_PLANT =
            GenericMachinePage.builder(
                            "chemical_plant",
                            ChemicalPlantRecipe.class,
                            ChemicalPlantRecipes.INSTANCE,
                            "block.hbm.machine_chemical_plant",
                            ModBlocks.MACHINE_CHEMICAL_PLANT.get(),
                            Layout.GENERIC)
                    .catalysts(ModBlocks.MACHINE_CHEMICAL_FACTORY.get())
                    .secretRows()
                    .build();
    public static final GenericMachinePage<RockMillRecipe> ROCK_MILL =
            GenericMachinePage.builder(
                            "rock_mill",
                            RockMillRecipe.class,
                            RockMillRecipes.INSTANCE,
                            "block.hbm.machine_rockmill",
                            ModBlocks.MACHINE_ROCK_MILL.get(),
                            Layout.GENERIC)
                    .build();
    public static final GenericMachinePage<GenericRecipe> PRECISION_ASSEMBLER =
            GenericMachinePage.<GenericRecipe>builder(
                            "precision_assembler",
                            GenericRecipe.class,
                            PrecAssRecipes.INSTANCE,
                            "block.hbm.machine_precass",
                            ModBlocks.MACHINE_PRECASS.get(),
                            Layout.GENERIC)
                    .secretRows()
                    .build();
    public static final GenericMachinePage<PUREXRecipe> PUREX =
            GenericMachinePage.builder(
                            "purex",
                            PUREXRecipe.class,
                            PUREXRecipes.INSTANCE,
                            "block.hbm.machine_purex",
                            ModBlocks.MACHINE_PUREX.get(),
                            Layout.GENERIC)
                    .extras(Extras.ONE_GREEN_LINE)
                    .secretRows()
                    .build();
    public static final GenericMachinePage<BlastFurnaceRecipe> BLAST_FURNACE =
            GenericMachinePage.builder(
                            "blast_furnace",
                            BlastFurnaceRecipe.class,
                            BlastFurnaceRecipesNT.INSTANCE,
                            "block.hbm.machine_blast_furnace",
                            ModBlocks.MACHINE_BLAST_FURNACE.get(),
                            Layout.GENERIC)
                    .extras(Extras.DURATION_ONLY)
                    .secretRows()
                    .build();
    public static final GenericMachinePage<SuperComputerRecipe> SUPERCOMPUTER =
            GenericMachinePage.builder(
                            "supercomputer",
                            SuperComputerRecipe.class,
                            SuperComputerRecipes.INSTANCE,
                            "block.hbm.machine_supercomputer",
                            ModBlocks.MACHINE_SUPERCOMPUTER.get(),
                            Layout.GENERIC)
                    .build();
    public static final GenericMachinePage<FusionRecipe> FUSION =
            GenericMachinePage.builder(
                            "fusion",
                            FusionRecipe.class,
                            FusionRecipes.INSTANCE,
                            "block.hbm.fusion_torus",
                            ModBlocks.FUSION_TORUS.get(),
                            Layout.GENERIC)
                    .extras(Extras.IGNITION)
                    .secretRows()
                    .build();
    public static final GenericMachinePage<PlasmaForgeRecipe> PLASMA_FORGE =
            GenericMachinePage.builder(
                            "plasma_forge",
                            PlasmaForgeRecipe.class,
                            PlasmaForgeRecipes.INSTANCE,
                            "block.hbm.fusion_plasma_forge",
                            ModBlocks.FUSION_PLASMA_FORGE.get(),
                            Layout.ASSEMBLY)
                    .extras(Extras.IGNITION)
                    .build();
    public static final GenericMachinePage<ReformingRecipe> REFORMING =
            universal(
                            "catalytic_reformer",
                            ReformingRecipe.class,
                            ReformingRecipes.INSTANCE,
                            "block.hbm.machine_catalytic_reformer",
                            ModBlocks.MACHINE_CATALYTIC_REFORMER.get())
                    .fluidView(FluidView.batch(1000, 10))
                    .build();
    public static final GenericMachinePage<HydrotreatingRecipe> HYDROTREATING =
            universal(
                            "hydrotreater",
                            HydrotreatingRecipe.class,
                            HydrotreatingRecipes.INSTANCE,
                            "block.hbm.machine_hydrotreater",
                            ModBlocks.MACHINE_HYDROTREATER.get())
                    .fluidView(FluidView.batch(1000, 10))
                    .build();
    public static final GenericMachinePage<OutgasserRecipe> OUTGASSER =
            universal(
                            "outgasser",
                            OutgasserRecipe.class,
                            OutgasserRecipes.INSTANCE,
                            "block.hbm.rbmk_outgasser",
                            ModBlocks.RBMK_OUTGASSER.get())
                    .build();
    public static final GenericMachinePage<AmmoPressRecipe> AMMO_PRESS =
            universal(
                            "ammo_press",
                            AmmoPressRecipe.class,
                            AmmoPressRecipes.INSTANCE,
                            "block.hbm.machine_ammo_press",
                            ModBlocks.MACHINE_AMMO_PRESS.get())
                    .build();

    public static final GenericMachinePage<ArcFurnaceRecipe> ARC_FURNACE_SOLID =
            universal(
                            "arc_furnace_solid",
                            ArcFurnaceRecipe.class,
                            ArcFurnaceRecipes.INSTANCE,
                            "block.hbm.machine_arc_furnace",
                            ModBlocks.MACHINE_ARC_FURNACE.get())
                    .rows(
                            () ->
                                    ArcFurnaceRecipes.INSTANCE.pageRows().stream()
                                            .filter(r -> r.solidOutput() != null)
                                            .toList())
                    .build();
    public static final ArcFurnaceMoltenPage ARC_FURNACE_FLUID = new ArcFurnaceMoltenPage();
    public static final GenericMachinePage<ArcWelderRecipe> ARC_WELDER =
            GenericMachinePage.builder(
                            "arc_welder",
                            ArcWelderRecipe.class,
                            ArcWelderRecipes.INSTANCE,
                            "block.hbm.machine_arc_welder",
                            ModBlocks.MACHINE_ARC_WELDER.get(),
                            Layout.UNIVERSAL)
                    .build();
    public static final GenericMachinePage<CombinationRecipe> COMBINATION =
            universal(
                            "combination_oven",
                            CombinationRecipe.class,
                            CombinationRecipes.INSTANCE,
                            "block.hbm.furnace_combination",
                            ModBlocks.FURNACE_COMBINATION.get())
                    .build();
    public static final GenericMachinePage<CompressorRecipe> COMPRESSOR =
            universal(
                            "compressor",
                            CompressorRecipe.class,
                            CompressorRecipes.INSTANCE,
                            "block.hbm.machine_compressor",
                            ModBlocks.MACHINE_COMPRESSOR.get())
                    .catalysts(ModBlocks.MACHINE_COMPRESSOR_COMPACT.get())
                    .build();
    public static final GenericMachinePage<CrackingRecipe> CRACKING =
            GenericMachinePage.builder(
                            "cracking_tower",
                            CrackingRecipe.class,
                            CrackingRecipes.INSTANCE,
                            "block.hbm.machine_cracking_tower",
                            ModBlocks.MACHINE_CRACKING_TOWER.get(),
                            Layout.UNIVERSAL)
                    .extras(Extras.NONE)
                    .fluidView(
                            new FluidView(
                                    FluidView.append(() -> new FluidStackNTM(NTMFluids.STEAM, 200)),
                                    FluidView.append(
                                            () -> new FluidStackNTM(NTMFluids.SPENTSTEAM, 2))))
                    .build();
    public static final GenericMachinePage<CrystallizerRecipe> CRYSTALLIZER =
            GenericMachinePage.builder(
                            "crystallizer",
                            CrystallizerRecipe.class,
                            CrystallizerRecipes.INSTANCE,
                            "block.hbm.machine_crystallizer",
                            ModBlocks.MACHINE_CRYSTALLIZER.get(),
                            Layout.UNIVERSAL)
                    .extras(Extras.EFFECTIVENESS)
                    .fluidInputsFirst()
                    .build();
    public static final GenericMachinePage<ElectrolyserFluidRecipe> ELECTROLYSER_FLUID =
            GenericMachinePage.builder(
                            "electrolyser_fluid",
                            ElectrolyserFluidRecipe.class,
                            ElectrolyserFluidRecipes.INSTANCE,
                            "block.hbm.machine_electrolyser",
                            ModBlocks.MACHINE_ELECTROLYSER.get(),
                            Layout.UNIVERSAL)
                    .extras(Extras.NONE)
                    .fluidInputsFirst()
                    .fluidOutputsFirst()
                    .build();
    public static final GenericMachinePage<ElectrolyserMetalRecipe> ELECTROLYSER_METAL =
            GenericMachinePage.builder(
                            "electrolyser_metal",
                            ElectrolyserMetalRecipe.class,
                            ElectrolyserMetalRecipes.INSTANCE,
                            "block.hbm.machine_electrolyser",
                            ModBlocks.MACHINE_ELECTROLYSER.get(),
                            Layout.UNIVERSAL)
                    .extras(Extras.NONE)
                    .fluidView(
                            new FluidView(
                                    FluidView.append(
                                            () -> new FluidStackNTM(NTMFluids.NITRIC_ACID, 100)),
                                    UnaryOperator.identity()))
                    .build();
    public static final GenericMachinePage<FluidBreederRecipe> FLUID_BREEDER =
            universal(
                            "fluid_breeder",
                            FluidBreederRecipe.class,
                            FluidBreederRecipes.INSTANCE,
                            "block.hbm.fusion_breeder",
                            ModBlocks.FUSION_BREEDER.get())
                    .build();
    public static final GenericMachinePage<FractionRecipe> FRACTION =
            universal(
                            "fraction_tower",
                            FractionRecipe.class,
                            FractionRecipes.INSTANCE,
                            "block.hbm.machine_fraction_tower",
                            ModBlocks.MACHINE_FRACTION_TOWER.get())
                    .build();
    public static final GenericMachinePage<LiquefactionRecipe> LIQUEFACTION =
            universal(
                            "liquefaction",
                            LiquefactionRecipe.class,
                            LiquefactionRecipes.INSTANCE,
                            "block.hbm.machine_liquefactor",
                            ModBlocks.MACHINE_LIQUEFACTOR.get())
                    .build();
    public static final GenericMachinePage<MixerRecipe> MIXER =
            GenericMachinePage.builder(
                            "mixer",
                            MixerRecipe.class,
                            MixerRecipes.INSTANCE,
                            "block.hbm.machine_mixer",
                            ModBlocks.MACHINE_MIXER.get(),
                            Layout.UNIVERSAL)
                    .extras(Extras.NONE)
                    .fluidInputsFirst()
                    .build();
    public static final GenericMachinePage<PyroOvenRecipe> PYRO_OVEN =
            GenericMachinePage.builder(
                            "pyro_oven",
                            PyroOvenRecipe.class,
                            PyroOvenRecipes.INSTANCE,
                            "block.hbm.machine_pyrooven",
                            ModBlocks.MACHINE_PYROOVEN.get(),
                            Layout.UNIVERSAL)
                    .extras(Extras.NONE)
                    .fluidInputsFirst()
                    .build();
    public static final GenericMachinePage<VacuumRefineryRecipe> VACUUM_REFINERY =
            universal(
                            "vacuum_refinery",
                            VacuumRefineryRecipe.class,
                            VacuumRefineryRecipes.INSTANCE,
                            "block.hbm.machine_vacuum_distill",
                            ModBlocks.MACHINE_VACUUM_DISTILL.get())
                    .fluidView(FluidView.batch(1000, 2, 10))
                    .build();

    private static final List<RecipePage<?>> FIXED =
            List.of(
                    ANVIL_CONSTRUCTION,
                    CENTRIFUGE,
                    PRESS,
                    SHREDDER,
                    CYCLOTRON,
                    EXPOSURE_CHAMBER,
                    SOLIDIFICATION,
                    COKER,
                    ROTARY_FURNACE,
                    SOLDERING,
                    GRENADE_ASSEMBLY,
                    PARTICLE_ACCELERATOR,
                    GAS_CENTRIFUGE,
                    ZIRNOX,
                    PWR,
                    WATZ_FUEL,
                    RTG,
                    DEUTERIUM,
                    ROCK_MILL,
                    SUPERCOMPUTER,
                    SPACE_ASSEMBLER,
                    CASTING,
                    CRUCIBLE_ALLOYING,
                    CRUCIBLE_SMELTING,
                    RADIOLYSIS,
                    ANVIL_SMITHING,
                    MAGIC,
                    REFINERY,
                    SILEX,
                    FUEL_POOL,
                    RBMK_DISASSEMBLY,
                    WASTE_DECAY,
                    SATELLITE,
                    TOOLING,
                    CONSTRUCTION,
                    ANNIHILATOR,
                    ASSEMBLY,
                    CHEMICAL_PLANT,
                    PRECISION_ASSEMBLER,
                    PUREX,
                    ORE_SLOPPER,
                    BLAST_FURNACE,
                    FUSION,
                    PLASMA_FORGE,
                    REFORMING,
                    HYDROTREATING,
                    OUTGASSER,
                    AMMO_PRESS,
                    ARC_FURNACE_SOLID,
                    ARC_FURNACE_FLUID,
                    ARC_WELDER,
                    COMBINATION,
                    SAWMILL,
                    COMPRESSOR,
                    CRACKING,
                    CRYSTALLIZER,
                    ELECTROLYSER_FLUID,
                    ELECTROLYSER_METAL,
                    ASHPIT,
                    FLUID_BREEDER,
                    FRACTION,
                    BOILING,
                    LIQUEFACTION,
                    MIXER,
                    PYRO_OVEN,
                    VACUUM_REFINERY,
                    ORE_GENERATION);

    private static final List<ClickArea> CLICK_AREAS =
            List.of(
                    area(ScreenAnvil.class, 34, 26, 18, 18, ANVIL_SMITHING),
                    area(ScreenAnvil.class, 70, 26, 18, 18, ANVIL_SMITHING),
                    area(ScreenMachineCentrifuge.class, 68, 18, 80, 38, CENTRIFUGE),
                    area(ScreenMachinePress.class, 104, 35, 22, 14, PRESS),
                    area(ScreenMachineEPress.class, 43, 33, 22, 15, PRESS),
                    area(ScreenMachineShredder.class, 64, 89, 33, 13, SHREDDER),
                    area(ScreenMachineCyclotron.class, 48, 27, 34, 34, CYCLOTRON),
                    area(ScreenMachineExposureChamber.class, 36, 39, 41, 10, EXPOSURE_CHAMBER),
                    area(ScreenMachineSolidifier.class, 42, 17, 38, 17, SOLIDIFICATION),
                    area(ScreenMachineSolidifier.class, 75, 34, 8, 9, SOLIDIFICATION),
                    area(ScreenMachineLiquefactor.class, 42, 17, 41, 17, LIQUEFACTION),
                    area(ScreenMachineLiquefactor.class, 42, 34, 2, 18, LIQUEFACTION),
                    area(ScreenMachineCompressor.class, 42, 26, 54, 17, COMPRESSOR),
                    area(ScreenMachineCoker.class, 60, 22, 32, 18, COKER),
                    area(ScreenMachineRotaryFurnace.class, 63, 30, 32, 10, ROTARY_FURNACE),
                    area(ScreenMachineSolderingStation.class, 72, 28, 32, 14, SOLDERING),
                    area(ScreenMachineArcWelder.class, 72, 37, 32, 14, ARC_WELDER),
                    smelting(ScreenMachineElectricFurnace.class, 43, 36, 27, 12),
                    smelting(ScreenFurnaceIron.class, 53, 36, 69, 5),
                    smelting(ScreenFurnaceSteel.class, 54, 18, 68, 5),
                    smelting(ScreenFurnaceSteel.class, 54, 36, 68, 5),
                    smelting(ScreenFurnaceSteel.class, 54, 54, 68, 5),
                    smelting(ScreenFurnaceBrick.class, 86, 34, 22, 16),
                    smelting(ScreenMachineMicrowave.class, 104, 34, 23, 16),
                    area(ScreenMachineCatalyticReformer.class, 67, 82, 24, 24, REFORMING),
                    area(ScreenMachineHydrotreater.class, 85, 82, 24, 24, HYDROTREATING),
                    area(ScreenMachinePUREX.class, 62, 25, 47, 9, PUREX),
                    area(ScreenMachinePUREX.class, 62, 90, 47, 9, PUREX),
                    area(ScreenMachineBlastFurnace.class, 117, 86, 14, 6, BLAST_FURNACE),
                    area(ScreenMachineSuperComputer.class, 62, 81, 70, 16, SUPERCOMPUTER),
                    area(ScreenRBMKOutgasser.class, 82, 50, 13, 6, OUTGASSER),
                    area(ScreenMachineGasCent.class, 70, 35, 36, 13, GAS_CENTRIFUGE),
                    area(ScreenFurnaceCombination.class, 54, 55, 18, 18, COMBINATION),
                    area(ScreenMachineCrystallizer.class, 80, 47, 27, 12, CRYSTALLIZER),
                    area(ScreenMachineElectrolyserFluid.class, 62, 26, 12, 41, ELECTROLYSER_FLUID),
                    area(ScreenMachineElectrolyserMetal.class, 7, 46, 22, 25, ELECTROLYSER_METAL),
                    area(ScreenMachineMixer.class, 71, 31, 52, 44, MIXER),
                    area(ScreenMachinePyroOven.class, 57, 47, 27, 12, PYRO_OVEN),
                    area(ScreenMachineRadiolysis.class, 71, 35, 26, 16, RADIOLYSIS),
                    area(ScreenMachineRefinery.class, 39, 45, 14, 34, REFINERY),
                    area(ScreenMachineSILEX.class, 44, 71, 60, 50, SILEX),
                    area(ScreenBook.class, 90, 35, 22, 15, MAGIC),
                    area(ScreenMachineArcFurnace.class, 17, 36, 7, 70, ARC_FURNACE_SOLID),
                    area(ScreenMachineArcFurnace.class, 152, 36, 16, 70, ARC_FURNACE_FLUID),
                    area(ScreenMachineRTG.class, 124, 11, 16, 50, RTG),
                    area(ScreenPASource.class, 76, 36, 6, 6, PARTICLE_ACCELERATOR),
                    area(ScreenPADetector.class, 76, 36, 6, 7, PARTICLE_ACCELERATOR),
                    area(ScreenFusionBreeder.class, 67, 48, 42, 10, FLUID_BREEDER),
                    area(ScreenFusionTorus.class, 99, 39, 28, 10, FUSION),
                    area(ScreenMachinePlasmaForge.class, 62, 81, 70, 16, PLASMA_FORGE));

    public static final int CUSTOM_MACHINE_X = 78,
            CUSTOM_MACHINE_Y = 119,
            CUSTOM_MACHINE_WIDTH = 90,
            CUSTOM_MACHINE_HEIGHT = 16;

    private RecipePages() {}

    private static <T extends GenericRecipe> GenericMachinePage.Builder<T> universal(
            String path,
            Class<? extends T> rowType,
            GenericRecipes<T, ?> table,
            String title,
            ItemLike machine) {

        return GenericMachinePage.builder(path, rowType, table, title, machine, Layout.UNIVERSAL)
                .extras(Extras.NONE);
    }

    private static ClickArea area(
            Class<? extends AbstractContainerScreen<?>> screen,
            int x,
            int y,
            int width,
            int height,
            RecipePage<?> page) {
        return new ClickArea(screen, x, y, width, height, page.id());
    }

    private static ClickArea smelting(
            Class<? extends AbstractContainerScreen<?>> screen,
            int x,
            int y,
            int width,
            int height) {
        return new ClickArea(screen, x, y, width, height, VANILLA_SMELTING);
    }

    public static List<RecipePage<?>> fixed() {
        return FIXED;
    }

    public static List<CustomMachinePage> customMachines() {
        List<CustomMachinePage> pages = new ArrayList<>();
        CustomMachineDefinitions.all()
                .forEach((key, definition) -> pages.add(new CustomMachinePage(key, definition)));
        return pages;
    }

    public static List<ClickArea> clickAreas() {
        return CLICK_AREAS;
    }

    public static List<ItemStack> hiddenStacks() {
        List<ItemStack> hidden = new ArrayList<>();
        hidden.add(new ItemStack(ModItems.BOBMAZON_HIDDEN));

        hidden.add(new ItemStack(ModItems.COAL_ETERNAL));
        hidden.add(new ItemStack(ModItems.BOOK_LEMEGETON));
        hidden.add(ModItems.GUIDE_BOOK.stack(com.hbm.items.tool.ItemGuideBook.BookType.TEST));
        hidden.add(ModItems.GUIDE_BOOK.stack(com.hbm.items.tool.ItemGuideBook.BookType.HADRON));

        hidden.add(new ItemStack(ModItems.AMS_CORE_THINGY));

        ItemBattery memory = ModItems.MEMORY.get();
        hidden.add(memory.empty());
        hidden.add(memory.full());
        hidden.add(new ItemStack(ModBlocks.BRICK_FORGOTTEN.get()));
        hidden.add(
                new ItemStack(
                        BuiltInRegistries.BLOCK
                                .getOptional(Library.id("brick_forgotten_lock"))
                                .orElseThrow()
                                .asItem()));
        hidden.add(
                new ItemStack(
                        BuiltInRegistries.ITEM
                                .getOptional(Library.id("coal_eternal"))
                                .orElseThrow()));

        if (InteractionConfig.recipeViewerHidesSecrets) {
            for (var member : ModItems.ITEM_SECRET) hidden.add(new ItemStack(member.get()));
        }
        return hidden;
    }

    public record ClickArea(
            Class<? extends AbstractContainerScreen<?>> screen,
            int x,
            int y,
            int width,
            int height,
            Identifier page) {}
}
