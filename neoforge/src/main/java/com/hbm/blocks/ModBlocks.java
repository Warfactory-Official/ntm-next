// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.blocks.bomb.*;
import com.hbm.blocks.fluid.*;
import com.hbm.blocks.gas.*;
import com.hbm.blocks.generic.*;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.blocks.machine.*;
import com.hbm.blocks.machine.BlockCMPort;
import com.hbm.blocks.machine.BlockCustomMachine;
import com.hbm.blocks.machine.albion.*;
import com.hbm.blocks.machine.fusion.*;
import com.hbm.blocks.machine.pile.*;
import com.hbm.blocks.machine.rbmk.*;
import com.hbm.blocks.machine.storage.*;
import com.hbm.blocks.machine.storage.BlockCrateTungsten;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.BlockMultiblockGeometryCell;
import com.hbm.blocks.multiblock.CellBuckets;
import com.hbm.blocks.network.*;
import com.hbm.blocks.network.pneumatic.*;
import com.hbm.blocks.rail.*;
import com.hbm.blocks.test.ObjTesterBlock;
import com.hbm.blocks.turret.TurretArty;
import com.hbm.blocks.turret.TurretChekhov;
import com.hbm.blocks.turret.TurretFriendly;
import com.hbm.blocks.turret.TurretFritz;
import com.hbm.blocks.turret.TurretHIMARS;
import com.hbm.blocks.turret.TurretHoward;
import com.hbm.blocks.turret.TurretHowardDamaged;
import com.hbm.blocks.turret.TurretJeremy;
import com.hbm.blocks.turret.TurretMaxwell;
import com.hbm.blocks.turret.TurretRichard;
import com.hbm.blocks.turret.TurretSentry;
import com.hbm.blocks.turret.TurretSentryDamaged;
import com.hbm.blocks.turret.TurretTauon;
import com.hbm.client.model.*;
import com.hbm.client.model.ChainModel;
import com.hbm.client.model.SectionedModel;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.itempool.ItemPools;
import com.hbm.items.DescBlockItem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.block.*;
import com.hbm.items.block.ItemCustomMachine;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.Library;
import com.hbm.lib.ModBlockSetTypes;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.ItemSubtype;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.tags.HbmBlockTags;
import com.hbm.tileentity.DoorDecl;
import com.hbm.tileentity.machine.BlockEntityWatz;
import com.hbm.tileentity.machine.storage.CrateType;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("Convert2MethodRef")
public final class ModBlocks {
    @SuppressWarnings("unchecked")
    public static final RegistryHandle<FluidDuctBoxBlock>[][] FLUID_DUCT_BOX =
            new RegistryHandle[3][5];

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<CableBoxBlock>[] RED_CABLE_BOX = new RegistryHandle[5];

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<FluidDuctBoxExhaustBlock>[] FLUID_DUCT_EXHAUST =
            new RegistryHandle[5];

    public static final List<RegistryHandle<TrappedBrick>> JUNGLE_TRAPS = new ArrayList<>();

    public static final List<RegistryHandle<BlockCrashedBomb>> CRASHED_BOMBS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> CRATES = new ArrayList<>();
    public static final List<RegistryHandle<Item>> EXTRA_ORE_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> STRUCTURAL_BLOCKS = new ArrayList<>();
    public static final List<MaterialBlockEntry> MATERIAL_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> DECO_BLOCKS = new ArrayList<>();

    public static final List<RegistryHandle<Item>> RAIL_BLOCKS = new ArrayList<>();

    public static final List<RegistryHandle<Item>> MACHINE_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<BlockAnvil>> ANVILS = new ArrayList<>();
    public static final int[] ANVIL_TIERS = {
        BlockAnvil.TIER_IRON, BlockAnvil.TIER_STEEL, BlockAnvil.TIER_OIL, BlockAnvil.TIER_NUCLEAR,
        BlockAnvil.TIER_RBMK, BlockAnvil.TIER_FUSION, BlockAnvil.TIER_PARTICLE,
                BlockAnvil.TIER_GERALD,
        BlockAnvil.TIER_MURKY
    };
    public static final RegistryHandle<Block> TEST_BLOCK =
            Reg.blockItem(
                    "test_block", Block::new, () -> BlockBehaviour.Properties.of().noLootTable());
    public static final RegistryHandle<ObjTesterBlock> OBJ_TESTER =
            Reg.blockItem(
                    "obj_tester",
                    ObjTesterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(2.5F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CableBlock> CABLE =
            Reg.blockItem(
                            "cable",
                            CableBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new CableModel());
    public static final RegistryHandle<CablePaintableBlock> RED_CABLE_PAINTABLE =
            Reg.blockItem(
                            "red_cable_paintable",
                            CablePaintableBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new PaintableDuctModel());
    public static final RegistryHandle<CableBlock> RED_CABLE_CLASSIC =
            Reg.blockItem(
                            "red_cable_classic",
                            CableBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new CableClassicModel());
    public static final RegistryHandle<CableDiodeBlock> CABLE_DIODE =
            Reg.blockItem(
                            "cable_diode",
                            CableDiodeBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new DescBlockItem(block, props, 1, DescBlockItem.Desc.PLAIN))
                    .bakedBy(() -> new DiodeModel());
    public static final RegistryHandle<CableGaugeBlock> RED_CABLE_GAUGE =
            Reg.blockItem(
                    "red_cable_gauge",
                    CableGaugeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<CableSwitchBlock> CABLE_SWITCH =
            Reg.blockItem(
                    "cable_switch",
                    CableSwitchBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CableDetectorBlock> CABLE_DETECTOR =
            Reg.blockItem(
                    "cable_detector",
                    CableDetectorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockWireCoated> RED_WIRE_COATED =
            Reg.blockItem(
                    "red_wire_coated",
                    BlockWireCoated::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<ConnectorBlock> RED_CONNECTOR =
            Reg.blockItem(
                            "red_connector",
                            props -> new ConnectorBlock(false, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN))
                    .bakedBy(() -> new ConnectorModel(Library.id("models/network/connector.obj")));
    public static final RegistryHandle<ConnectorBlock> RED_CONNECTOR_SUPER =
            Reg.blockItem(
                            "red_connector_super",
                            props -> new ConnectorBlock(true, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN))
                    .bakedBy(
                            () ->
                                    new ConnectorModel(
                                            Library.id("models/network/connector_super.obj")));
    public static final RegistryHandle<PylonBlock> RED_PYLON =
            Reg.blockItem(
                    "red_pylon",
                    PylonBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN));
    public static final RegistryHandle<PylonBlock> RED_PYLON_STEEL =
            Reg.blockItem(
                    "red_pylon_steel",
                    PylonBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN));
    public static final RegistryHandle<PylonMediumBlock> RED_PYLON_MEDIUM_WOOD =
            mediumPylon(
                    "red_pylon_medium_wood",
                    false,
                    "block/models/network/pylon_medium",
                    BlockTags.MINEABLE_WITH_AXE,
                    props -> props.mapColor(MapColor.WOOD).ignitedByLava());
    public static final RegistryHandle<PylonMediumBlock> RED_PYLON_MEDIUM_WOOD_TRANSFORMER =
            mediumPylon(
                    "red_pylon_medium_wood_transformer",
                    true,
                    "block/models/network/pylon_medium",
                    BlockTags.MINEABLE_WITH_AXE,
                    props -> props.mapColor(MapColor.WOOD).ignitedByLava());
    public static final RegistryHandle<PylonMediumBlock> RED_PYLON_MEDIUM_STEEL =
            mediumPylon(
                    "red_pylon_medium_steel",
                    false,
                    "block/models/network/pylon_medium_steel",
                    BlockTags.MINEABLE_WITH_PICKAXE,
                    BlockBehaviour.Properties::requiresCorrectToolForDrops);
    public static final RegistryHandle<PylonMediumBlock> RED_PYLON_MEDIUM_STEEL_TRANSFORMER =
            mediumPylon(
                    "red_pylon_medium_steel_transformer",
                    true,
                    "block/models/network/pylon_medium_steel",
                    BlockTags.MINEABLE_WITH_PICKAXE,
                    BlockBehaviour.Properties::requiresCorrectToolForDrops);
    public static final RegistryHandle<PylonLargeBlock> RED_PYLON_LARGE =
            Reg.blockItem(
                            "red_pylon_large",
                            PylonLargeBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new DescBlockItem(block, props, 3, DescBlockItem.Desc.PLAIN))
                    .bakedBy(() -> new PylonLargeModel());
    public static final RegistryHandle<SubstationBlock> SUBSTATION =
            Reg.blockItem(
                    "substation",
                    SubstationBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN));
    public static final RegistryHandle<PowerDetectorBlock> MACHINE_DETECTOR =
            Reg.blockItem(
                    "machine_detector",
                    PowerDetectorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<FluidPipeBlock> FLUID_PIPE = fluidPipe("fluid_pipe");
    public static final RegistryHandle<FluidPipeBlock> FLUID_PIPE_SILVER =
            fluidPipe("fluid_pipe_silver");
    public static final RegistryHandle<FluidPipeBlock> FLUID_PIPE_COLORED =
            fluidPipe("fluid_pipe_colored");
    public static final RegistryHandle<FluidValveBlock> FLUID_VALVE =
            Reg.blockItem(
                    "fluid_valve",
                    FluidValveBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<FluidSwitchBlock> FLUID_SWITCH =
            Reg.blockItem(
                    "fluid_switch",
                    FluidSwitchBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<FluidCounterValveBlock> FLUID_COUNTER_VALVE =
            Reg.blockItem(
                    "fluid_counter_valve",
                    FluidCounterValveBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<FluidDuctGaugeBlock> FLUID_DUCT_GAUGE =
            Reg.blockItem(
                            "fluid_duct_gauge",
                            FluidDuctGaugeBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new GaugeDuctModel());
    public static final RegistryHandle<PneumoTubeBlock> PNEUMATIC_TUBE =
            Reg.blockItem(
                            "pneumatic_tube",
                            PneumoTubeBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .sound(ModSoundTypes.PIPE)
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 5))
                    .bakedBy(() -> new PneumoTubeModel.Family());
    public static final RegistryHandle<PneumoTubePaintableBlock> PNEUMATIC_TUBE_PAINTABLE =
            Reg.blockItem(
                            "pneumatic_tube_paintable",
                            PneumoTubePaintableBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new PaintableDuctModel());
    public static final RegistryHandle<PneumoStorageAccessBlock> PNEUMATIC_STORAGE_ACCESS =
            Reg.blockItem(
                    "pneumatic_storage_access",
                    PneumoStorageAccessBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PneumoStorageClutterBlock> PNEUMATIC_STORAGE_CLUTTER =
            Reg.blockItem(
                    "pneumatic_storage_clutter",
                    PneumoStorageClutterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops(),
                    ContainerBlockItem::new);
    public static final RegistryHandle<PneumoStorageMonoBlock> PNEUMATIC_STORAGE_MONO =
            Reg.blockItem(
                    "pneumatic_storage_mono",
                    PneumoStorageMonoBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops(),
                    ContainerBlockItem::new);
    public static final RegistryHandle<PneumoStorageImporterBlock> PNEUMATIC_STORAGE_IMPORTER =
            Reg.blockItem(
                    "pneumatic_storage_importer",
                    PneumoStorageImporterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PneumoStorageExporterBlock> PNEUMATIC_STORAGE_EXPORTER =
            Reg.blockItem(
                    "pneumatic_storage_exporter",
                    PneumoStorageExporterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<ConveyorBlock> CONVEYOR =
            Reg.blockItem(
                            "conveyor",
                            ConveyorBlock::new,
                            ModBlocks::conveyorProps,
                            (block, props) -> new DescBlockItem(block, props, 3))
                    .bakedBy(() -> new ConveyorModel());
    public static final RegistryHandle<ConveyorExpressBlock> CONVEYOR_EXPRESS =
            Reg.blockItem(
                            "conveyor_express",
                            ConveyorExpressBlock::new,
                            ModBlocks::conveyorProps,
                            (block, props) -> new DescBlockItem(block, props, 3))
                    .bakedBy(() -> new ConveyorModel());
    public static final RegistryHandle<ConveyorDoubleBlock> CONVEYOR_DOUBLE =
            Reg.blockItem(
                            "conveyor_double",
                            ConveyorDoubleBlock::new,
                            ModBlocks::conveyorProps,
                            (block, props) -> new DescBlockItem(block, props, 3))
                    .bakedBy(() -> new ConveyorModel());
    public static final RegistryHandle<ConveyorTripleBlock> CONVEYOR_TRIPLE =
            Reg.blockItem(
                            "conveyor_triple",
                            ConveyorTripleBlock::new,
                            ModBlocks::conveyorProps,
                            (block, props) -> new DescBlockItem(block, props, 3))
                    .bakedBy(() -> new ConveyorModel());

    public static final RegistryHandle<ConveyorLiftBlock> CONVEYOR_LIFT =
            Reg.blockItem(
                            "conveyor_lift",
                            ConveyorLiftBlock::new,
                            ModBlocks::conveyorProps,
                            (block, props) -> new DescBlockItem(block, props, 2))
                    .bakedBy(() -> new ConveyorModel());
    public static final RegistryHandle<ConveyorChuteBlock> CONVEYOR_CHUTE =
            Reg.blockItem(
                            "conveyor_chute",
                            ConveyorChuteBlock::new,
                            ModBlocks::conveyorProps,
                            (block, props) -> new DescBlockItem(block, props, 2))
                    .bakedBy(() -> new ConveyorChuteModel.Family());

    public static final RegistryHandle<MachineConveyorPress> MACHINE_CONVEYOR_PRESS =
            Reg.blockItem(
                    "machine_conveyor_press",
                    MachineConveyorPress::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 3));

    public static final RegistryHandle<CraneInserter> CRANE_INSERTER =
            Reg.blockItem(
                            "crane_inserter",
                            CraneInserter::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneExtractor> CRANE_EXTRACTOR =
            Reg.blockItem(
                            "crane_extractor",
                            CraneExtractor::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 5))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneGrabber> CRANE_GRABBER =
            Reg.blockItem(
                            "crane_grabber",
                            CraneGrabber::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 6))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneBoxer> CRANE_BOXER =
            Reg.blockItem(
                            "crane_boxer",
                            CraneBoxer::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneUnboxer> CRANE_UNBOXER =
            Reg.blockItem(
                            "crane_unboxer",
                            CraneUnboxer::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneRouter> CRANE_ROUTER =
            Reg.blockItem(
                    "crane_router",
                    CraneRouter::new,
                    ModBlocks::craneProps,
                    (block, props) -> new DescBlockItem(block, props, 3));

    public static final RegistryHandle<CraneSplitter> CRANE_SPLITTER =
            Reg.blockItem(
                    "crane_splitter",
                    CraneSplitter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 3));

    public static final RegistryHandle<CranePartitioner> CRANE_PARTITIONER =
            Reg.blockItem(
                    "crane_partitioner",
                    CranePartitioner::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 3));

    public static final RegistryHandle<DroneCrateBlock> DRONE_CRATE =
            droneStation("drone_crate", DroneCrateBlock::new, 3);
    public static final RegistryHandle<DroneDockBlock> DRONE_DOCK =
            droneStation(
                    "drone_dock", props -> new DroneDockBlock(props, DroneDockBlock.Kind.DOCK), 3);
    public static final RegistryHandle<DroneDockBlock> DRONE_CRATE_PROVIDER =
            droneStation(
                    "drone_crate_provider",
                    props -> new DroneDockBlock(props, DroneDockBlock.Kind.PROVIDER),
                    2);
    public static final RegistryHandle<DroneDockBlock> DRONE_CRATE_REQUESTER =
            droneStation(
                    "drone_crate_requester",
                    props -> new DroneDockBlock(props, DroneDockBlock.Kind.REQUESTER),
                    2);
    public static final RegistryHandle<DroneWaypointBlock> DRONE_WAYPOINT =
            droneWaypoint("drone_waypoint", DroneWaypointBlock.Kind.TRANSPORT, 5);
    public static final RegistryHandle<DroneWaypointBlock> DRONE_WAYPOINT_REQUEST =
            droneWaypoint("drone_waypoint_request", DroneWaypointBlock.Kind.REQUEST, 0);
    public static final RegistryHandle<FluidDuctPaintableBlock> FLUID_DUCT_PAINTABLE =
            Reg.blockItem(
                            "fluid_duct_paintable",
                            FluidDuctPaintableBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new PaintableDuctModel());
    public static final RegistryHandle<FluidDuctPaintableExhaustBlock>
            FLUID_DUCT_PAINTABLE_EXHAUST =
                    Reg.blockItem(
                                    "fluid_duct_paintable_block_exhaust",
                                    FluidDuctPaintableExhaustBlock::new,
                                    () ->
                                            BlockBehaviour.Properties.of()
                                                    .strength(5.0F, 6.0F)
                                                    .requiresCorrectToolForDrops(),
                                    (block, props) -> new DescBlockItem(block, props, 4))
                            .bakedBy(() -> new PaintableDuctModel());
    public static final RegistryHandle<FluidPipeAnchorBlock> PIPE_ANCHOR =
            Reg.blockItem(
                            "pipe_anchor",
                            FluidPipeAnchorBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .sound(ModSoundTypes.PIPE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new FluidPipeAnchorBlock.AnchorBlockItem(block, props))
                    .bakedBy(() -> new AnchorModel());
    public static final RegistryHandle<FluidPumpBlock> FLUID_PUMP =
            Reg.blockItem(
                    "fluid_pump",
                    FluidPumpBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockRadiobox> RADIOBOX =
            Reg.blockItem(
                    "radiobox",
                    BlockRadiobox::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RadioRec> RADIO_REC =
            Reg.blockItem(
                    "radiorec",
                    RadioRec::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RadioTorchBlock> RADIO_TORCH_SENDER =
            radioTorch("radio_torch_sender", RadioTorchBlock.Kind.SENDER);
    public static final RegistryHandle<RadioTorchBlock> RADIO_TORCH_RECEIVER =
            radioTorch("radio_torch_receiver", RadioTorchBlock.Kind.RECEIVER);
    public static final RegistryHandle<RadioTorchBlock> RADIO_TORCH_COUNTER =
            radioTorch("radio_torch_counter", RadioTorchBlock.Kind.COUNTER);
    public static final RegistryHandle<RadioTorchBlock> RADIO_TORCH_LOGIC =
            radioTorch("radio_torch_logic", RadioTorchBlock.Kind.LOGIC);
    public static final RegistryHandle<RadioTorchBlock> RADIO_TORCH_READER =
            radioTorch("radio_torch_reader", RadioTorchBlock.Kind.READER);
    public static final RegistryHandle<RadioTorchBlock> RADIO_TORCH_CONTROLLER =
            radioTorch("radio_torch_controller", RadioTorchBlock.Kind.CONTROLLER);
    public static final RegistryHandle<RadioTelex> RADIO_TELEX =
            Reg.blockItem(
                    "radio_telex",
                    RadioTelex::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .sound(SoundType.STONE)
                                    .strength(3.0F, 6.0F)
                                    .noOcclusion()
                                    .ignitedByLava());
    public static final RegistryHandle<RadioAUTOCAL> RADIO_AUTOCAL =
            Reg.blockItem(
                    "radio_autocal",
                    RadioAUTOCAL::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineCentrifuge> MACHINE_CENTRIFUGE =
            Reg.blockItem(
                    "machine_centrifuge",
                    MachineCentrifuge::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineGasCent> MACHINE_GASCENT =
            Reg.blockItem(
                    "machine_gascent",
                    MachineGasCent::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MachineElectricFurnace> MACHINE_ELECTRIC_FURNACE =
            Reg.blockItem(
                    "machine_electric_furnace",
                    MachineElectricFurnace::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .lightLevel(
                                            s -> s.getValue(MachineElectricFurnace.LIT) ? 15 : 0));
    public static final RegistryHandle<BlockGeigerCounter> GEIGER =
            Reg.blockItem(
                    "geiger",
                    BlockGeigerCounter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(15.0F, 0.15F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BlockAnvil> ANVIL_IRON =
            anvil("anvil_iron", BlockAnvil.TIER_IRON);
    public static final RegistryHandle<BlockAnvil> ANVIL_STEEL =
            anvil("anvil_steel", BlockAnvil.TIER_STEEL);
    public static final RegistryHandle<BlockAnvil> ANVIL_LEAD =
            anvil("anvil_lead", BlockAnvil.TIER_IRON);
    public static final RegistryHandle<BlockAnvil> ANVIL_DESH =
            anvil("anvil_desh", BlockAnvil.TIER_OIL);
    public static final RegistryHandle<BlockAnvil> ANVIL_FERROURANIUM =
            anvil("anvil_ferrouranium", BlockAnvil.TIER_NUCLEAR);
    public static final RegistryHandle<BlockAnvil> ANVIL_SATURNITE =
            anvil("anvil_saturnite", BlockAnvil.TIER_RBMK);
    public static final RegistryHandle<BlockAnvil> ANVIL_BISMUTH_BRONZE =
            anvil("anvil_bismuth_bronze", BlockAnvil.TIER_RBMK);
    public static final RegistryHandle<BlockAnvil> ANVIL_ARSENIC_BRONZE =
            anvil("anvil_arsenic_bronze", BlockAnvil.TIER_RBMK);
    public static final RegistryHandle<BlockAnvil> ANVIL_SCHRABIDATE =
            anvil("anvil_schrabidate", BlockAnvil.TIER_FUSION);
    public static final RegistryHandle<BlockAnvil> ANVIL_DNT =
            anvil("anvil_dnt", BlockAnvil.TIER_PARTICLE);
    public static final RegistryHandle<BlockAnvil> ANVIL_OSMIRIDIUM =
            anvil("anvil_osmiridium", BlockAnvil.TIER_GERALD);
    public static final RegistryHandle<BlockAnvil> ANVIL_MURKY =
            anvil("anvil_murky", BlockAnvil.TIER_MURKY);
    public static final RegistryHandle<MachineBatterySocket> MACHINE_BATTERY_SOCKET =
            Reg.blockItem(
                    "machine_battery_socket",
                    MachineBatterySocket::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<MachineBattery> MACHINE_BATTERY =
            Reg.blockItem(
                    "machine_battery",
                    MachineBattery::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MachineBatteryREDD> MACHINE_BATTERY_REDD =
            Reg.blockItem(
                    "machine_battery_redd",
                    MachineBatteryREDD::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<MachineFENSU> MACHINE_FENSU =
            Reg.blockItem(
                    "machine_fensu",
                    MachineFENSU::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<MachineCapacitor> CAPACITOR_COPPER =
            Reg.blockItem(
                    "capacitor_copper",
                    props -> new MachineCapacitor(1_000_000L, props),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<Block> PRESS_PREHEATER =
            Reg.blockItem(
                    "press_preheater",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachinePress> MACHINE_PRESS =
            Reg.blockItem(
                    "machine_press",
                    MachinePress::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineAutocrafter> MACHINE_AUTOCRAFTER =
            Reg.blockItem(
                    "machine_autocrafter",
                    MachineAutocrafter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineAssemblyMachine> MACHINE_ASSEMBLY_MACHINE =
            Reg.blockItem(
                    "machine_assembly_machine",
                    MachineAssemblyMachine::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachinePrecAss> MACHINE_PRECASS =
            Reg.blockItem(
                    "machine_precass",
                    MachinePrecAss::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineAssemblyFactory> MACHINE_ASSEMBLY_FACTORY =
            Reg.blockItem(
                    "machine_assembly_factory",
                    MachineAssemblyFactory::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 5));
    public static final RegistryHandle<MachineChemicalPlant> MACHINE_CHEMICAL_PLANT =
            Reg.blockItem(
                    "machine_chemical_plant",
                    MachineChemicalPlant::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineRockMill> MACHINE_ROCK_MILL =
            Reg.blockItem(
                    "machine_rockmill",
                    MachineRockMill::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineDeuteriumExtractor> MACHINE_DEUTERIUM_EXTRACTOR =
            Reg.blockItem(
                    "machine_deuterium_extractor",
                    MachineDeuteriumExtractor::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<DeuteriumTower> MACHINE_DEUTERIUM_TOWER =
            Reg.blockItem(
                            "machine_deuterium_tower",
                            DeuteriumTower::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(10.0F, 12.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new DeuteriumTowerModel());
    public static final RegistryHandle<MachineRadiolysis> MACHINE_RADIOLYSIS =
            Reg.blockItem(
                    "machine_radiolysis",
                    MachineRadiolysis::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineChemicalFactory> MACHINE_CHEMICAL_FACTORY =
            Reg.blockItem(
                    "machine_chemical_factory",
                    MachineChemicalFactory::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 5));
    public static final RegistryHandle<MachineElectrolyser> MACHINE_ELECTROLYSER =
            Reg.blockItem(
                    "machine_electrolyser",
                    MachineElectrolyser::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineRefinery> MACHINE_REFINERY =
            Reg.blockItem(
                    "machine_refinery",
                    MachineRefinery::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCatalyticReformer> MACHINE_CATALYTIC_REFORMER =
            Reg.blockItem(
                    "machine_catalytic_reformer",
                    MachineCatalyticReformer::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineHydrotreater> MACHINE_HYDROTREATER =
            Reg.blockItem(
                    "machine_hydrotreater",
                    MachineHydrotreater::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineFractionTower> MACHINE_FRACTION_TOWER =
            Reg.blockItem(
                    "machine_fraction_tower",
                    MachineFractionTower::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FractionSpacer> FRACTION_SPACER =
            Reg.blockItem(
                    "fraction_spacer",
                    FractionSpacer::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCrackingTower> MACHINE_CRACKING_TOWER =
            Reg.blockItem(
                    "machine_cracking_tower",
                    MachineCrackingTower::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachinePUREX> MACHINE_PUREX =
            Reg.blockItem(
                    "machine_purex",
                    MachinePUREX::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<MachineMixer> MACHINE_MIXER =
            Reg.blockItem(
                    "machine_mixer",
                    MachineMixer::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineSuperComputer> MACHINE_SUPERCOMPUTER =
            Reg.blockItem(
                    "machine_supercomputer",
                    MachineSuperComputer::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineSILEX> MACHINE_SILEX =
            Reg.blockItem(
                    "machine_silex",
                    MachineSILEX::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineOreSlopper> MACHINE_ORE_SLOPPER =
            Reg.blockItem(
                    "machine_ore_slopper",
                    MachineOreSlopper::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineAnnihilator> MACHINE_ANNIHILATOR =
            Reg.blockItem(
                    "machine_annihilator",
                    MachineAnnihilator::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineBlastFurnace> MACHINE_BLAST_FURNACE =
            Reg.blockItem(
                    "machine_blast_furnace",
                    MachineBlastFurnace::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineHephaestus> MACHINE_HEPHAESTUS =
            Reg.blockItem(
                    "machine_hephaestus",
                    MachineHephaestus::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCrucible> MACHINE_CRUCIBLE =
            Reg.blockItem(
                    "machine_crucible",
                    MachineCrucible::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));

    public static final RegistryHandle<CoreComponent> DFC_EMITTER =
            Reg.blockItem(
                    "dfc_emitter",
                    props -> new CoreComponent(CoreComponent.Kind.EMITTER, props),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CoreComponent> DFC_RECEIVER =
            Reg.blockItem(
                    "dfc_receiver",
                    props -> new CoreComponent(CoreComponent.Kind.RECEIVER, props),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CoreComponent> DFC_INJECTOR =
            Reg.blockItem(
                    "dfc_injector",
                    props -> new CoreComponent(CoreComponent.Kind.INJECTOR, props),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoreComponent> DFC_STABILIZER =
            Reg.blockItem(
                    "dfc_stabilizer",
                    props -> new CoreComponent(CoreComponent.Kind.STABILIZER, props),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoreCore> DFC_CORE =
            Reg.blockItem(
                    "dfc_core",
                    CoreCore::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineMiningLaser> MACHINE_MINING_LASER =
            Reg.blockItem(
                    "machine_mining_laser",
                    MachineMiningLaser::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineForceField> MACHINE_FORCEFIELD =
            Reg.blockItem(
                    "machine_forcefield",
                    MachineForceField::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineExcavator> MACHINE_EXCAVATOR =
            Reg.blockItem(
                    "machine_excavator",
                    MachineExcavator::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<Block> BARRICADE =
            Reg.blockItem(
                    "barricade",
                    Block::new,
                    () -> BlockBehaviour.Properties.of().strength(1.0F, 1.5F));
    public static final RegistryHandle<MachineStrandCaster> MACHINE_STRAND_CASTER =
            Reg.blockItem(
                    "machine_strand_caster",
                    MachineStrandCaster::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineRotaryFurnace> MACHINE_ROTARY_FURNACE =
            Reg.blockItem(
                    "machine_rotary_furnace",
                    MachineRotaryFurnace::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineArcFurnace> MACHINE_ARC_FURNACE =
            Reg.blockItem(
                    "machine_arc_furnace",
                    MachineArcFurnace::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<HeaterFirebox> HEATER_FIREBOX =
            Reg.blockItem(
                    "heater_firebox",
                    HeaterFirebox::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<HeaterElectric> HEATER_ELECTRIC =
            Reg.blockItem(
                    "heater_electric",
                    HeaterElectric::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<FoundryMold> FOUNDRY_MOLD =
            Reg.blockItem(
                    "foundry_mold",
                    FoundryMold::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FoundryBasin> FOUNDRY_BASIN =
            Reg.blockItem(
                    "foundry_basin",
                    FoundryBasin::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FoundryChannel> FOUNDRY_CHANNEL =
            Reg.blockItem(
                    "foundry_channel",
                    FoundryChannel::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<FoundryOutlet> FOUNDRY_OUTLET =
            Reg.blockItem(
                    "foundry_outlet",
                    FoundryOutlet::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<FoundrySlagtap> FOUNDRY_SLAGTAP =
            Reg.blockItem(
                    "foundry_slagtap",
                    FoundrySlagtap::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<FoundryTank> FOUNDRY_TANK =
            Reg.blockItem(
                            "foundry_tank",
                            FoundryTank::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new FoundryTankModel.Family());
    public static final RegistryHandle<FurnaceIron> FURNACE_IRON =
            Reg.blockItem(
                    "furnace_iron",
                    FurnaceIron::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<FurnaceSteel> FURNACE_STEEL =
            Reg.blockItem(
                    "furnace_steel",
                    FurnaceSteel::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<FurnaceCombination> FURNACE_COMBINATION =
            Reg.blockItem(
                    "furnace_combination",
                    FurnaceCombination::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<MachineFurnaceBrick> MACHINE_FURNACE_BRICK =
            Reg.blockItem(
                    "machine_furnace_brick",
                    MachineFurnaceBrick::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .lightLevel(s -> s.getValue(MachineFurnaceBrick.LIT) ? 15 : 0));
    public static final RegistryHandle<HeaterOven> HEATER_OVEN =
            Reg.blockItem(
                    "heater_oven",
                    HeaterOven::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<MachineAshpit> MACHINE_ASHPIT =
            Reg.blockItem(
                    "machine_ashpit",
                    MachineAshpit::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<HeaterOilburner> HEATER_OILBURNER =
            Reg.blockItem(
                    "heater_oilburner",
                    HeaterOilburner::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<HeaterHeatex> HEATER_HEATEX =
            Reg.blockItem(
                    "heater_heatex",
                    HeaterHeatex::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<MachineCrystallizer> MACHINE_CRYSTALLIZER =
            Reg.blockItem(
                    "machine_crystallizer",
                    MachineCrystallizer::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCoker> MACHINE_COKER =
            Reg.blockItem(
                            "machine_coker",
                            MachineCoker::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion(),
                            (block, props) -> new DescBlockItem(block, props, 3))
                    .bakedBy(() -> new SectionedModel.Family(0));
    public static final RegistryHandle<MachineLiquefactor> MACHINE_LIQUEFACTOR =
            Reg.blockItem(
                    "machine_liquefactor",
                    MachineLiquefactor::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<MachineSolidifier> MACHINE_SOLIDIFIER =
            Reg.blockItem(
                    "machine_solidifier",
                    MachineSolidifier::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<MachinePyroOven> MACHINE_PYROOVEN =
            Reg.blockItem(
                    "machine_pyrooven",
                    MachinePyroOven::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineVacuumDistill> MACHINE_VACUUM_DISTILL =
            Reg.blockItem(
                    "machine_vacuum_distill",
                    MachineVacuumDistill::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineFrackingTower> MACHINE_FRACKING_TOWER =
            Reg.blockItem(
                            "machine_fracking_tower",
                            MachineFrackingTower::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 12.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new SectionedModel.Family(180));
    public static final RegistryHandle<MachinePump> PUMP_ELECTRIC =
            Reg.blockItem(
                    "pump_electric",
                    MachinePump::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<MachineIntake> MACHINE_INTAKE =
            Reg.blockItem(
                    "machine_intake",
                    MachineIntake::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineDrain> MACHINE_DRAIN =
            Reg.blockItem(
                    "machine_drain",
                    MachineDrain::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineFunnel> MACHINE_FUNNEL =
            Reg.blockItem(
                    "machine_funnel",
                    MachineFunnel::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<MachineCondenser> MACHINE_CONDENSER =
            Reg.blockItem(
                    "machine_condenser",
                    MachineCondenser::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCyclotron> MACHINE_CYCLOTRON =
            Reg.blockItem(
                    "machine_cyclotron",
                    MachineCyclotron::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineExposureChamber> MACHINE_EXPOSURE_CHAMBER =
            Reg.blockItem(
                    "machine_exposure_chamber",
                    MachineExposureChamber::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineRadGen> MACHINE_RADGEN =
            Reg.blockItem(
                    "machine_radgen",
                    MachineRadGen::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MachineReactorBreeding> MACHINE_REACTOR_BREEDING =
            Reg.blockItem(
                    "machine_reactor",
                    MachineReactorBreeding::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ReactorResearch> REACTOR_RESEARCH =
            Reg.blockItem(
                    "machine_reactor_small",
                    ReactorResearch::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MachineTowerSmall> MACHINE_TOWER_SMALL =
            Reg.blockItem(
                            "machine_tower_small",
                            MachineTowerSmall::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new SectionedModel.Family(0));
    public static final RegistryHandle<MachineTowerLarge> MACHINE_TOWER_LARGE =
            Reg.blockItem(
                    "machine_tower_large",
                    MachineTowerLarge::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCondenserPowered> MACHINE_CONDENSER_POWERED =
            Reg.blockItem(
                    "machine_condenser_powered",
                    MachineCondenserPowered::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MachineBigAssTank> MACHINE_BIGASSTANK =
            Reg.blockItem(
                    "machine_bigasstank",
                    MachineBigAssTank::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineOrbus> MACHINE_ORBUS =
            Reg.blockItem(
                    "machine_orbus",
                    MachineOrbus::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineUF6Tank> MACHINE_UF6_TANK =
            Reg.blockItem(
                    "machine_uf6_tank",
                    props -> new MachineUF6Tank(props, () -> ModBlockEntities.UF6_TANK.get()),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineUF6Tank> MACHINE_PUF6_TANK =
            Reg.blockItem(
                    "machine_puf6_tank",
                    props -> new MachineUF6Tank(props, () -> ModBlockEntities.PUF6_TANK.get()),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineMicrowave> MACHINE_MICROWAVE =
            Reg.blockItem(
                    "machine_microwave",
                    MachineMicrowave::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<MachineSatLinker> MACHINE_SATLINKER =
            Reg.blockItem(
                    "machine_satlinker",
                    MachineSatLinker::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineSatLink> MACHINE_SATLINK =
            Reg.blockItem(
                    "machine_satlink",
                    MachineSatLink::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineTapeDrive> MACHINE_TAPE_DRIVE =
            Reg.blockItem(
                    "machine_tape_drive",
                    MachineTapeDrive::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineKeyForge> MACHINE_KEYFORGE =
            Reg.blockItem(
                    "machine_keyforge",
                    MachineKeyForge::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineThresher> MACHINE_THRESHER =
            Reg.blockItem(
                    "machine_thresher",
                    MachineThresher::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 7));
    public static final RegistryHandle<MachineAutosaw> MACHINE_AUTOSAW =
            Reg.blockItem(
                    "machine_autosaw",
                    MachineAutosaw::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 7));
    public static final RegistryHandle<MachineStirling> MACHINE_STIRLING =
            stirling(
                    "machine_stirling",
                    MachineStirling.Tier.NORMAL,
                    "block/models/machines/stirling",
                    4);
    public static final RegistryHandle<MachineStirling> MACHINE_STIRLING_STEEL =
            stirling(
                    "machine_stirling_steel",
                    MachineStirling.Tier.STEEL,
                    "block/models/machines/stirling_steel",
                    5);
    public static final RegistryHandle<MachineStirling> MACHINE_STIRLING_CREATIVE =
            stirling(
                    "machine_stirling_creative",
                    MachineStirling.Tier.CREATIVE,
                    "block/models/machines/stirling_creative",
                    4);
    public static final RegistryHandle<MachineSawmill> MACHINE_SAWMILL =
            Reg.blockItem(
                            "machine_sawmill",
                            MachineSawmill::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion(),
                            (block, props) ->
                                    new DescBlockItem(
                                            block,
                                            props.component(
                                                    ModDataComponents.HAS_BLADE.get(), true),
                                            3))
                    .state(ModDataComponents.HAS_BLADE);

    public static final RegistryHandle<MachineAmmoPress> MACHINE_AMMO_PRESS =
            Reg.blockItem(
                    "machine_ammo_press",
                    MachineAmmoPress::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockDynamicSlag> SLAG =
            Reg.block(
                    "slag",
                    BlockDynamicSlag::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .dynamicShape()
                                    .forceSolidOn()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BlockEmitter> DECO_EMITTER =
            Reg.blockItem(
                    "deco_emitter",
                    BlockEmitter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 4, DescBlockItem.Desc.PLAIN));
    public static final RegistryHandle<PartEmitter> PART_EMITTER =
            Reg.blockItem(
                    "part_emitter",
                    PartEmitter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 1, DescBlockItem.Desc.PLAIN));

    public static final RegistryHandle<BlockLoot> LOOT =
            Reg.block(
                    "deco_loot",
                    BlockLoot::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(0.0F, 0.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .noTerrainParticles()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineCompressor> MACHINE_COMPRESSOR =
            Reg.blockItem(
                    "machine_compressor",
                    MachineCompressor::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineCompressorCompact> MACHINE_COMPRESSOR_COMPACT =
            Reg.blockItem(
                    "machine_compressor_compact",
                    MachineCompressorCompact::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineGasFlare> MACHINE_FLARE =
            Reg.blockItem(
                    "machine_flare",
                    MachineGasFlare::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineChimneyBrick> CHIMNEY_BRICK =
            Reg.blockItem(
                    "chimney_brick",
                    MachineChimneyBrick::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<MachineChimneyIndustrial> CHIMNEY_INDUSTRIAL =
            Reg.blockItem(
                            "chimney_industrial",
                            MachineChimneyIndustrial::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 60.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion(),
                            (block, props) -> new DescBlockItem(block, props, 2))
                    .bakedBy(() -> new SectionedModel.Family(180));
    public static final RegistryHandle<MachineOilWell> MACHINE_WELL =
            Reg.blockItem(
                    "machine_well",
                    MachineOilWell::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachinePumpjack> MACHINE_PUMPJACK =
            Reg.blockItem(
                    "machine_pumpjack",
                    MachinePumpjack::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<Block> OIL_PIPE =
            Reg.block(
                    "oil_pipe",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<ReactorZirnox> REACTOR_ZIRNOX =
            Reg.blockItem(
                    "reactor_zirnox",
                    ReactorZirnox::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ZirnoxDestroyed> ZIRNOX_DESTROYED =
            Reg.block(
                    "zirnox_destroyed",
                    ZirnoxDestroyed::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(100.0F, 480.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineFEL> MACHINE_FEL =
            Reg.blockItem(
                    "machine_fel",
                    MachineFEL::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockPASource> PA_SOURCE =
            Reg.blockItem(
                    "pa_source",
                    BlockPASource::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<BlockPABeamline> PA_BEAMLINE =
            Reg.blockItem(
                    "pa_beamline",
                    BlockPABeamline::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<BlockPARFC> PA_RFC =
            Reg.blockItem(
                    "pa_rfc",
                    BlockPARFC::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<BlockPAQuadrupole> PA_QUADRUPOLE =
            Reg.blockItem(
                    "pa_quadrupole",
                    BlockPAQuadrupole::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<BlockPADipole> PA_DIPOLE =
            Reg.blockItem(
                    "pa_dipole",
                    BlockPADipole::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 6));
    public static final RegistryHandle<BlockPADetector> PA_DETECTOR =
            Reg.blockItem(
                    "pa_detector",
                    BlockPADetector::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<MachineSolderingStation> MACHINE_SOLDERING_STATION =
            Reg.blockItem(
                    "machine_soldering_station",
                    MachineSolderingStation::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineArcWelder> MACHINE_ARC_WELDER =
            Reg.blockItem(
                    "machine_arc_welder",
                    MachineArcWelder::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineEPress> MACHINE_EPRESS =
            Reg.blockItem(
                    "machine_epress",
                    MachineEPress::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    private static final Map<CellBuckets.Plain, RegistryHandle<? extends BlockMultiblockCell>>
            PLAIN_CELLS = plainCells();
    public static final RegistryHandle<? extends BlockMultiblockCell> MULTIBLOCK_CELL =
            plainCell(CellBuckets.Plain.of(5F, 18F, MapColor.NONE));
    private static final Map<CellBuckets.Geometry, RegistryHandle<BlockMultiblockGeometryCell>>
            GEOMETRY_CELLS = geometryCells();
    public static final RegistryHandle<MachineFluidTank> MACHINE_FLUID_TANK =
            Reg.blockItem(
                    "machine_fluidtank",
                    MachineFluidTank::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockFluidBarrel> BARREL_PLASTIC =
            Reg.<BlockFluidBarrel>blockItem(
                            "barrel_plastic",
                            props -> new BlockFluidBarrel.Functional(props, 12_000, 3),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new BlockFluidBarrel.BarrelBlockItem(block, props))
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockFluidBarrel> BARREL_CORRODED =
            Reg.blockItem(
                            "barrel_corroded",
                            props -> new BlockFluidBarrel(props, 6_000, 4),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new BlockFluidBarrel.BarrelBlockItem(block, props))
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockFluidBarrel> BARREL_STEEL =
            Reg.<BlockFluidBarrel>blockItem(
                            "barrel_steel",
                            props -> new BlockFluidBarrel.Functional(props, 16_000, 4),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new BlockFluidBarrel.BarrelBlockItem(block, props))
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockFluidBarrel> BARREL_TCALLOY =
            Reg.<BlockFluidBarrel>blockItem(
                            "barrel_tcalloy",
                            props -> new BlockFluidBarrel.Functional(props, 24_000, 3),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new BlockFluidBarrel.BarrelBlockItem(block, props))
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockFluidBarrel> BARREL_ANTIMATTER =
            Reg.<BlockFluidBarrel>blockItem(
                            "barrel_antimatter",
                            props -> new BlockFluidBarrel.Functional(props, 16_000, 3),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new BlockFluidBarrel.BarrelBlockItem(block, props))
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockChargeC4> CHARGE_C4 =
            Reg.blockItem(
                            "charge_c4",
                            BlockChargeC4::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .noLootTable()
                                            .ignitedByLava(),
                            (block, props) -> new ItemBlockCharge(block, props))
                    .bakedBy(() -> new ChargeModel(Library.id("models/blocks/charge_c4.obj")));
    public static final RegistryHandle<BlockChargeDynamite> CHARGE_DYNAMITE =
            Reg.blockItem(
                            "charge_dynamite",
                            BlockChargeDynamite::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .noLootTable()
                                            .ignitedByLava(),
                            (block, props) -> new ItemBlockCharge(block, props))
                    .bakedBy(
                            () -> new ChargeModel(Library.id("models/blocks/charge_dynamite.obj")));
    public static final RegistryHandle<BlockChargeMiner> CHARGE_MINER =
            Reg.blockItem(
                            "charge_miner",
                            BlockChargeMiner::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .noLootTable()
                                            .ignitedByLava(),
                            (block, props) -> new ItemBlockCharge(block, props))
                    .bakedBy(
                            () -> new ChargeModel(Library.id("models/blocks/charge_dynamite.obj")));
    public static final RegistryHandle<BlockChargeSemtex> CHARGE_SEMTEX =
            Reg.blockItem(
                            "charge_semtex",
                            BlockChargeSemtex::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .noLootTable()
                                            .ignitedByLava(),
                            (block, props) -> new ItemBlockCharge(block, props))
                    .bakedBy(() -> new ChargeModel(Library.id("models/blocks/charge_c4.obj")));

    public static final RegistryHandle<BlockRedBarrel> RED_BARREL =
            Reg.blockItem(
                            "red_barrel",
                            BlockRedBarrel::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockRedBarrel> PINK_BARREL =
            Reg.blockItem(
                            "pink_barrel",
                            BlockRedBarrel::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<BlockLoxBarrel> LOX_BARREL =
            Reg.blockItem(
                            "lox_barrel",
                            BlockLoxBarrel::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<BlockTaintBarrel> TAINT_BARREL =
            Reg.blockItem(
                            "taint_barrel",
                            BlockTaintBarrel::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new DescBlockItem(block, props, 1, DescBlockItem.Desc.PLAIN))
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockYellowBarrel> YELLOW_BARREL =
            Reg.blockItem(
                            "yellow_barrel",
                            props -> new BlockYellowBarrel(props, true),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());
    public static final RegistryHandle<BlockYellowBarrel> VITRIFIED_BARREL =
            Reg.blockItem(
                            "vitrified_barrel",
                            props -> new BlockYellowBarrel(props, false),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<BlockLandmineAP> MINE_AP =
            Reg.blockItem(
                            "mine_ap",
                            BlockLandmineAP::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.AP));
    public static final RegistryHandle<BlockLandmineShrap> MINE_SHRAP =
            Reg.blockItem(
                            "mine_shrap",
                            BlockLandmineShrap::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.SHRAP));
    public static final RegistryHandle<BlockLandmineHE> MINE_HE =
            Reg.blockItem(
                            "mine_he",
                            BlockLandmineHE::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.HE));
    public static final RegistryHandle<BlockLandmineFat> MINE_FAT =
            Reg.blockItem(
                            "mine_fat",
                            BlockLandmineFat::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.FAT));
    public static final RegistryHandle<BlockLandmineNaval> MINE_NAVAL =
            Reg.blockItem(
                            "mine_naval",
                            BlockLandmineNaval::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.NAVAL));
    public static final RegistryHandle<BlockPoleSatelliteReceiver> POLE_SATELLITE_RECEIVER =
            Reg.blockItem(
                            "pole_satellite_receiver",
                            BlockPoleSatelliteReceiver::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SatelliteReceiverModel());

    public static final RegistryHandle<DemonLamp> LAMP_DEMON =
            Reg.block(
                            "lamp_demon",
                            DemonLamp::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .sound(SoundType.METAL)
                                            .strength(3.0F)
                                            .lightLevel(s -> 15)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("lamp_demon", h)));
    public static final RegistryHandle<BlockSpotlight> SPOTLIGHT_INCANDESCENT =
            Reg.block(
                            "spotlight_incandescent",
                            props ->
                                    new BlockSpotlight(
                                            2, BlockSpotlight.LightType.INCANDESCENT, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .lightLevel(
                                                    s -> s.getValue(BlockSpotlight.LIT) ? 15 : 0)
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SpotlightModel(BlockSpotlight.LightType.INCANDESCENT))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("spotlight_incandescent", h)));
    public static final RegistryHandle<BlockSpotlight> SPOTLIGHT_FLUORO =
            Reg.block(
                            "spotlight_fluoro",
                            props ->
                                    new BlockSpotlight(
                                            8, BlockSpotlight.LightType.FLUORESCENT, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .lightLevel(
                                                    s -> s.getValue(BlockSpotlight.LIT) ? 15 : 0)
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SpotlightModel(BlockSpotlight.LightType.FLUORESCENT))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("spotlight_fluoro", h)));
    public static final RegistryHandle<BlockSpotlight> SPOTLIGHT_HALOGEN =
            Reg.block(
                            "spotlight_halogen",
                            props ->
                                    new BlockSpotlight(32, BlockSpotlight.LightType.HALOGEN, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.5F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .lightLevel(
                                                    s -> s.getValue(BlockSpotlight.LIT) ? 15 : 0)
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SpotlightModel(BlockSpotlight.LightType.HALOGEN))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("spotlight_halogen", h)));

    public static final RegistryHandle<BlockSpotlightBeam> SPOTLIGHT_BEAM =
            Reg.block(
                    "spotlight_beam",
                    BlockSpotlightBeam::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(-1.0F, 600000.0F)
                                    .replaceable()
                                    .noCollision()
                                    .noLootTable()
                                    .noOcclusion()
                                    .lightLevel(LightBlock.LIGHT_EMISSION));
    public static final RegistryHandle<BlockBarbedWire> BARBED_WIRE =
            Reg.blockItem(
                    "barbed_wire",
                    BlockBarbedWire::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockBarbedWireFire> BARBED_WIRE_FIRE =
            Reg.blockItem(
                    "barbed_wire_fire",
                    BlockBarbedWireFire::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockBarbedWirePoison> BARBED_WIRE_POISON =
            Reg.blockItem(
                    "barbed_wire_poison",
                    BlockBarbedWirePoison::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockBarbedWireAcid> BARBED_WIRE_ACID =
            Reg.blockItem(
                    "barbed_wire_acid",
                    BlockBarbedWireAcid::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockBarbedWireWither> BARBED_WIRE_WITHER =
            Reg.blockItem(
                    "barbed_wire_wither",
                    BlockBarbedWireWither::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockBarbedWireUltradeath> BARBED_WIRE_ULTRADEATH =
            Reg.blockItem(
                    "barbed_wire_ultradeath",
                    BlockBarbedWireUltradeath::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<Block> REINFORCED_LIGHT =
            Reg.blockItem(
                            "reinforced_light",
                            Block::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 48.0F)
                                            .sound(SoundType.STONE)
                                            .lightLevel(s -> 15)
                                            .requiresCorrectToolForDrops(),
                            ItemBlockBlastInfo::new)
                    .afterRegistration(RadiationSystemNT::markRadResistant);

    public static final RegistryHandle<RedstoneLampBlock> REINFORCED_LAMP =
            Reg.block(
                            "reinforced_lamp",
                            RedstoneLampBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 48.0F)
                                            .sound(SoundType.STONE)
                                            .lightLevel(
                                                    s -> s.getValue(RedstoneLampBlock.LIT) ? 15 : 0)
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> DECO_BLOCKS.add(blastItem("reinforced_lamp", h)));
    public static final RegistryHandle<BlockHadronCoil> HADRON_COIL_ALLOY =
            Reg.blockItem(
                    "hadron_coil_alloy",
                    BlockHadronCoil::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    BlockHadronCoil.CoilItem::new);
    public static final RegistryHandle<BlockWoodBarrier> WOOD_BARRIER =
            Reg.blockItem(
                            "wood_barrier",
                            BlockWoodBarrier::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_BARRIER));

    public static final RegistryHandle<BlockWoodStructure> WOOD_STRUCTURE_ROOF =
            Reg.blockItem(
                            "wood_structure_roof",
                            props -> new BlockWoodStructure(BlockWoodStructure.Kind.ROOF, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_ROOF));
    public static final RegistryHandle<BlockWoodStructure> WOOD_STRUCTURE_SCAFFOLD =
            Reg.blockItem(
                            "wood_structure_scaffold",
                            props ->
                                    new BlockWoodStructure(BlockWoodStructure.Kind.SCAFFOLD, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_SCAFFOLD));
    public static final RegistryHandle<BlockWoodStructure> WOOD_STRUCTURE_CEILING =
            Reg.blockItem(
                            "wood_structure_ceiling",
                            props -> new BlockWoodStructure(BlockWoodStructure.Kind.CEILING, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_CEILING));

    public static final RegistryHandle<BlockCharger> CHARGER =
            Reg.blockItem(
                    "charger",
                    BlockCharger::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockRefueler> REFUELER =
            Reg.blockItem(
                    "refueler",
                    BlockRefueler::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockDecoTapeRecorder> TAPE_RECORDER =
            Reg.blockItem(
                    "tape_recorder",
                    BlockDecoTapeRecorder::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 9.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineWoodBurner> MACHINE_WOOD_BURNER =
            Reg.blockItem(
                    "machine_wood_burner",
                    MachineWoodBurner::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));

    public static final RegistryHandle<MachineDiesel> MACHINE_DIESEL =
            Reg.blockItem(
                    "machine_diesel",
                    MachineDiesel::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    ItemBlockDiesel::new);
    public static final RegistryHandle<MachineTurbine> MACHINE_TURBINE =
            Reg.blockItem(
                    "machine_turbine",
                    MachineTurbine::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<MachineLargeTurbine> MACHINE_LARGE_TURBINE =
            Reg.blockItem(
                    "machine_large_turbine",
                    MachineLargeTurbine::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<MachineIndustrialTurbine> MACHINE_INDUSTRIAL_TURBINE =
            Reg.blockItem(
                    "machine_industrial_turbine",
                    MachineIndustrialTurbine::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<MachineChungus> MACHINE_CHUNGUS =
            Reg.blockItem(
                    "machine_chungus",
                    MachineChungus::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<MachineHeatBoiler> MACHINE_BOILER =
            Reg.blockItem(
                    "machine_boiler",
                    MachineHeatBoiler::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<MachineHeatBoilerIndustrial> MACHINE_INDUSTRIAL_BOILER =
            Reg.blockItem(
                    "machine_industrial_boiler",
                    MachineHeatBoilerIndustrial::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<MachineSteamEngine> MACHINE_STEAM_ENGINE =
            Reg.blockItem(
                    "machine_steam_engine",
                    MachineSteamEngine::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<MachineCombustionEngine> MACHINE_COMBUSTION_ENGINE =
            Reg.blockItem(
                    "machine_combustion_engine",
                    MachineCombustionEngine::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineTurbineGas> MACHINE_TURBINEGAS =
            Reg.blockItem(
                    "machine_turbinegas",
                    MachineTurbineGas::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineTurbofan> MACHINE_TURBOFAN =
            Reg.blockItem(
                    "machine_turbofan",
                    MachineTurbofan::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    ItemBlockTurbofan::new);
    public static final RegistryHandle<MachineLPW2> MACHINE_LPW2 =
            Reg.blockItem(
                    "machine_lpw2",
                    MachineLPW2::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MachinePump> PUMP_STEAM =
            Reg.blockItem(
                    "pump_steam",
                    MachinePump::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<MachineSolarBoiler> MACHINE_SOLAR_BOILER =
            Reg.blockItem(
                    "machine_solar_boiler",
                    MachineSolarBoiler::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SolarMirror> SOLAR_MIRROR =
            Reg.blockItem(
                    "solar_mirror",
                    SolarMirror::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<WasteDrum> WASTE_DRUM =
            Reg.blockItem(
                    "machine_waste_drum",
                    WasteDrum::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<StorageDrum> STORAGE_DRUM =
            Reg.blockItem(
                    "machine_storage_drum",
                    StorageDrum::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineSiren> MACHINE_SIREN =
            Reg.blockItem(
                    "machine_siren",
                    MachineSiren::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineShredder> MACHINE_SHREDDER =
            Reg.blockItem(
                    "machine_shredder",
                    MachineShredder::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineTeleporter> MACHINE_TELEPORTER =
            Reg.blockItem(
                    "machine_teleporter",
                    MachineTeleporter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<Block> TELEANCHOR =
            Reg.blockItem(
                    "teleanchor",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockDecon> DECON =
            Reg.blockItem(
                    "decon",
                    BlockDecon::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<Block> RAD_ABSORBER =
            absorber("rad_absorber", "block/absorber");
    public static final RegistryHandle<Block> RAD_ABSORBER_RED =
            absorber("rad_absorber_red", "block/absorber_red");
    public static final RegistryHandle<Block> RAD_ABSORBER_GREEN =
            absorber("rad_absorber_green", "block/absorber_green");
    public static final RegistryHandle<Block> RAD_ABSORBER_PINK =
            absorber("rad_absorber_pink", "block/absorber_pink");
    public static final RegistryHandle<RBMKRod> RBMK_ROD =
            Reg.blockItem("rbmk_fuel_rod", props -> new RBMKRod(props, false), ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKControl> RBMK_CONTROL =
            Reg.blockItem(
                            "rbmk_control",
                            props -> new RBMKControl(props, false),
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControl> RBMK_CONTROL_MOD =
            Reg.blockItem(
                            "rbmk_control_mod",
                            props -> new RBMKControl(props, true),
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControlAuto> RBMK_CONTROL_AUTO =
            Reg.blockItem("rbmk_control_auto", RBMKControlAuto::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControl> RBMK_CONTROL_REASIM =
            Reg.blockItem(
                            "rbmk_control_reasim",
                            props -> new RBMKControl(props, false),
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControlAuto> RBMK_CONTROL_REASIM_AUTO =
            Reg.blockItem(
                            "rbmk_control_reasim_auto",
                            RBMKControlAuto::new,
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKBoiler> RBMK_BOILER =
            Reg.blockItem("rbmk_boiler", RBMKBoiler::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.BOILER));
    public static final RegistryHandle<RBMKModerator> RBMK_MODERATOR =
            Reg.blockItem("rbmk_moderator", RBMKModerator::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKReflector> RBMK_REFLECTOR =
            Reg.blockItem("rbmk_reflector", RBMKReflector::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKAbsorber> RBMK_ABSORBER =
            Reg.blockItem("rbmk_absorber", RBMKAbsorber::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKBlank> RBMK_BLANK =
            Reg.blockItem("rbmk_blank", RBMKBlank::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKRod> RBMK_ROD_MODERATED =
            Reg.blockItem(
                            "rbmk_fuel_rod_moderated",
                            props -> new RBMKRod(props, true),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKRodReaSim> RBMK_ROD_REASIM =
            Reg.blockItem(
                            "rbmk_fuel_rod_reasim",
                            props -> new RBMKRodReaSim(props, false),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKRodReaSim> RBMK_ROD_REASIM_MODERATED =
            Reg.blockItem(
                            "rbmk_fuel_rod_reasim_moderated",
                            props -> new RBMKRodReaSim(props, true),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKCooler> RBMK_COOLER =
            Reg.blockItem("rbmk_cooler", RBMKCooler::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKStorage> RBMK_STORAGE =
            Reg.blockItem("rbmk_storage", RBMKStorage::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKHeater> RBMK_HEATER =
            Reg.blockItem("rbmk_heater", RBMKHeater::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.BOILER));
    public static final RegistryHandle<RBMKOutgasser> RBMK_OUTGASSER =
            Reg.blockItem("rbmk_outgasser", RBMKOutgasser::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKInlet> RBMK_INLET =
            Reg.blockItem(
                    "rbmk_steam_inlet",
                    RBMKInlet::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<RBMKOutlet> RBMK_OUTLET =
            Reg.blockItem(
                    "rbmk_steam_outlet",
                    RBMKOutlet::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<RBMKConsole> RBMK_CONSOLE =
            Reg.blockItem("rbmk_console", RBMKConsole::new, ModBlocks::rbmkProps);
    public static final RegistryHandle<RBMKCraneConsole> RBMK_CRANE_CONSOLE =
            Reg.blockItem("rbmk_crane_console", RBMKCraneConsole::new, ModBlocks::rbmkProps);
    public static final RegistryHandle<RBMKGauge> RBMK_GAUGE =
            miniPanel("rbmk_gauge", RBMKGauge::new, 3);
    public static final RegistryHandle<RBMKDisplay> RBMK_DISPLAY =
            miniPanel("rbmk_display", RBMKDisplay::new, 3);
    public static final RegistryHandle<RBMKMiniPanelBase> RBMK_DISPLAY_BLANK =
            miniPanel("rbmk_display_blank", RBMKMiniPanelBase::new, 1);
    public static final RegistryHandle<RBMKNumitron> RBMK_NUMITRON =
            miniPanel("rbmk_numitron", RBMKNumitron::new, 3);
    public static final RegistryHandle<RBMKIndicator> RBMK_INDICATOR =
            miniPanel("rbmk_indicator", RBMKIndicator::new, 4);
    public static final RegistryHandle<RBMKGraph> RBMK_GRAPH =
            miniPanel("rbmk_graph", RBMKGraph::new, 3);
    public static final RegistryHandle<RBMKLever> RBMK_LEVER =
            miniPanel("rbmk_lever", RBMKLever::new, 3);
    public static final RegistryHandle<RBMKKeyPad> RBMK_KEY_PAD =
            miniPanel("rbmk_key_pad", RBMKKeyPad::new, 3);
    public static final RegistryHandle<RBMKTerminal> RBMK_TERMINAL =
            miniPanel("rbmk_terminal", RBMKTerminal::new, 1);
    public static final RegistryHandle<RBMKAutoloader> RBMK_AUTOLOADER =
            Reg.blockItem(
                    "rbmk_autoloader",
                    RBMKAutoloader::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RBMKLoader> RBMK_LOADER =
            Reg.blockItem(
                    "rbmk_loader",
                    RBMKLoader::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<Block> RBMK_DEBRIS =
            debris("rbmk_debris", "models/rbmk/pribris.obj");
    public static final RegistryHandle<RBMKDebrisBurning> RBMK_DEBRIS_BURNING =
            debris(
                    "rbmk_debris_burning",
                    "models/rbmk/pribris_burning.obj",
                    RBMKDebrisBurning::new);
    public static final RegistryHandle<RBMKDebrisRadiating> RBMK_DEBRIS_RADIATING =
            debris(
                    "rbmk_debris_radiating",
                    "models/rbmk/pribris_radiating.obj",
                    RBMKDebrisRadiating::new);
    public static final RegistryHandle<RBMKDebrisDigamma> RBMK_DEBRIS_DIGAMMA =
            Reg.blockItem(
                    "rbmk_debris_digamma",
                    RBMKDebrisDigamma::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(50.0F, 360.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockAshDigamma> ASH_DIGAMMA =
            Reg.blockItem(
                    "ash_digamma",
                    BlockAshDigamma::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(0.5F, 90.0F)
                                    .sound(SoundType.SAND));

    public static final RegistryHandle<DigammaMatter> DIGAMMA_MATTER =
            Reg.block(
                    "digamma_matter",
                    DigammaMatter::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(-1.0F, 10800000.0F)
                                    .noCollision()
                                    .noOcclusion()
                                    .noLootTable());
    public static final RegistryHandle<BlockDigammaFlame> FIRE_DIGAMMA =
            Reg.block(
                    "fire_digamma",
                    BlockDigammaFlame::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .replaceable()
                                    .noCollision()
                                    .strength(0.0F, 90.0F)
                                    .lightLevel(state -> 15)
                                    .pushReaction(PushReaction.DESTROY)
                                    .noLootTable());
    public static final RegistryHandle<BlockCoriumFinite> CORIUM =
            Reg.block(
                    "corium_block",
                    BlockCoriumFinite::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(0.0F, 300.0F)
                                    .noCollision()
                                    .randomTicks()
                                    .lightLevel(BlockCoriumFinite::light)
                                    .noLootTable()
                                    .pushReaction(PushReaction.DESTROY));
    public static RegistryHandle<BlockGasRadon> GAS_RADON;
    public static RegistryHandle<BlockGasRadonDense> GAS_RADON_DENSE;
    public static RegistryHandle<BlockGasRadonTomb> GAS_RADON_TOMB;
    public static final RegistryHandle<BlockOutgas> ANCIENT_SCRAP =
            Reg.blockItem(
                    "ancient_scrap",
                    props -> new BlockOutgas(props, () -> GAS_RADON_TOMB.get(), true, true),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(100.0F, 3600.0F)
                                    .randomTicks()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<Block> BLOCK_CORIUM =
            Reg.blockItem(
                    "block_corium",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(100.0F, 3600.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BlockOutgas> BLOCK_CORIUM_COBBLE =
            Reg.blockItem(
                    "block_corium_cobble",
                    props -> new BlockOutgas(props, () -> GAS_RADON.get(), true, true),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(100.0F, 3600.0F)
                                    .randomTicks()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachinePWRController> PWR_CONTROLLER =
            Reg.blockItem(
                    "pwr_controller",
                    MachinePWRController::new,
                    ModBlocks::pwrProps,
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<BlockPWR> PWR_BLOCK =
            Reg.block(
                    "pwr_block",
                    BlockPWR::new,
                    () ->
                            pwrProps()
                                    .strength(15.0F, 6.0F)
                                    .noLootTable()
                                    .pushReaction(PushReaction.BLOCK));
    public static final RegistryHandle<Block> PWR_CASING = pwrPart("pwr_casing", 2);
    public static final RegistryHandle<Block> PWR_PORT = pwrPart("pwr_port", 2);
    public static final RegistryHandle<Block> PWR_REFLECTOR = pwrPart("pwr_reflector", 3);
    public static final RegistryHandle<Block> PWR_FUEL = pwrPart("pwr_fuel", 2);
    public static final RegistryHandle<Block> PWR_CONTROL = pwrPart("pwr_control", 2);
    public static final RegistryHandle<Block> PWR_CHANNEL = pwrPart("pwr_channel", 2);
    public static final RegistryHandle<Block> PWR_HEATEX = pwrPart("pwr_heatex", 2);
    public static final RegistryHandle<Block> PWR_HEATSINK = pwrPart("pwr_heatsink", 3);
    public static final RegistryHandle<Block> PWR_NEUTRON_SOURCE = pwrPart("pwr_neutron_source", 3);
    public static final RegistryHandle<Block> ICF_CASING = icfPart("icf_casing");
    public static final RegistryHandle<Block> ICF_PORT = icfPart("icf_port");
    public static final RegistryHandle<Block> ICF_CELL = icfPart("icf_cell");
    public static final RegistryHandle<Block> ICF_EMITTER = icfPart("icf_emitter");
    public static final RegistryHandle<Block> ICF_CAPACITOR =
            Reg.blockItem("icf_capacitor", Block::new, ModBlocks::icfProps);
    public static final RegistryHandle<Block> ICF_TURBOCHARGER =
            Reg.blockItem("icf_turbocharger", Block::new, ModBlocks::icfProps);
    public static final RegistryHandle<Block> ICF_COMPONENT = icfPart("icf_component");
    public static final RegistryHandle<Block> ICF_COMPONENT_VESSEL_WELDED =
            icfPart("icf_component_vessel_welded");
    public static final RegistryHandle<Block> ICF_COMPONENT_STRUCTURE_BOLTED =
            icfPart("icf_component_structure_bolted");

    public static final RegistryHandle<BlockToolConversion> ICF_COMPONENT_VESSEL =
            Reg.blockItem(
                    "icf_component_vessel",
                    props ->
                            new BlockToolConversion(
                                    props,
                                    IToolable.ToolType.TORCH,
                                    () -> ICF_COMPONENT_VESSEL_WELDED.get(),
                                    CountIngredient.of(
                                            OreDictManager.ANY_BISMOIDBRONZE.plateCast(), 1)),
                    ModBlocks::icfProps);
    public static final RegistryHandle<BlockToolConversion> ICF_COMPONENT_STRUCTURE =
            Reg.blockItem(
                    "icf_component_structure",
                    props ->
                            new BlockToolConversion(
                                    props,
                                    IToolable.ToolType.BOLT,
                                    () -> ICF_COMPONENT_STRUCTURE_BOLTED.get(),
                                    CountIngredient.of(OreDictManager.STEEL.plateCast(), 1),
                                    CountIngredient.of(OreDictManager.DURA.bolt(), 4)),
                    ModBlocks::icfProps);
    public static final RegistryHandle<MachineICFController> ICF_CONTROLLER =
            Reg.blockItem("icf_controller", MachineICFController::new, ModBlocks::icfProps);

    public static final RegistryHandle<BlockICF> ICF_BLOCK =
            Reg.block(
                    "icf_block",
                    BlockICF::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noLootTable()
                                    .pushReaction(PushReaction.BLOCK));
    public static final RegistryHandle<MachineICF> MACHINE_ICF =
            Reg.blockItem("icf", MachineICF::new, () -> icfProps().noOcclusion());

    public static final RegistryHandle<BlockICFStruct> STRUCT_ICF_CORE =
            Reg.blockItem(
                    "struct_icf_core",
                    BlockICFStruct::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(s -> 15)
                                    .noOcclusion());
    public static final RegistryHandle<MachineICFPress> MACHINE_ICF_PRESS =
            Reg.blockItem(
                    "machine_icf_press",
                    MachineICFPress::new,
                    ModBlocks::icfProps,
                    (block, props) ->
                            new DescBlockItem(block, props, 3, DescBlockItem.Desc.YELLOW));
    public static final RegistryHandle<NukeMan> NUKE_MAN =
            Reg.blockItem(
                    "nuke_man",
                    NukeMan::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeGadget> NUKE_GADGET =
            Reg.blockItem(
                    "nuke_gadget",
                    NukeGadget::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeBoy> NUKE_BOY =
            Reg.blockItem(
                    "nuke_boy",
                    NukeBoy::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeMike> NUKE_MIKE =
            Reg.blockItem(
                    "nuke_mike",
                    NukeMike::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeTsar> NUKE_TSAR =
            Reg.blockItem(
                    "nuke_tsar",
                    NukeTsar::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeFleija> NUKE_FLEIJA =
            Reg.blockItem(
                    "nuke_fleija",
                    NukeFleija::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukePrototype> NUKE_PROTOTYPE =
            Reg.blockItem(
                    "nuke_prototype",
                    NukePrototype::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeSolinium> NUKE_SOLINIUM =
            Reg.blockItem(
                    "nuke_solinium",
                    NukeSolinium::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<NukeN2> NUKE_N2 =
            Reg.blockItem(
                    "nuke_n2",
                    NukeN2::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineMissileAssembly> MACHINE_MISSILE_ASSEMBLY =
            Reg.blockItem(
                    "machine_missile_assembly",
                    MachineMissileAssembly::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<LaunchPad> LAUNCH_PAD =
            Reg.blockItem(
                    "launch_pad",
                    LaunchPad::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<LaunchPadRusted> LAUNCH_PAD_RUSTED =
            Reg.blockItem(
                    "launch_pad_rusted",
                    LaunchPadRusted::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<LaunchPadLarge> LAUNCH_PAD_LARGE =
            Reg.blockItem(
                    "launch_pad_large",
                    LaunchPadLarge::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<CompactLauncher> COMPACT_LAUNCHER =
            Reg.blockItem(
                    "compact_launcher",
                    CompactLauncher::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<MachineRadar> MACHINE_RADAR =
            Reg.blockItem(
                    "machine_radar",
                    MachineRadar::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineRadarLarge> MACHINE_RADAR_LARGE =
            Reg.blockItem(
                    "machine_radar_large",
                    MachineRadarLarge::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineRadarScreen> RADAR_SCREEN =
            Reg.blockItem(
                    "radar_screen",
                    MachineRadarScreen::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<RadarMastCell> RADAR_MAST_CELL =
            Reg.block(
                            "multiblock_cell_radar_mast",
                            RadarMastCell::new,
                            () -> cellProperties(CellBuckets.Plain.of(5F, 6F, MapColor.METAL)))
                    .afterRegistration(
                            cell ->
                                    RadiationSystemNT.markRadResistant(
                                            cell,
                                            state -> state.getValue(BlockMultiblockCell.SEALED)));
    public static final RegistryHandle<LaunchTable> LAUNCH_TABLE =
            Reg.blockItem(
                    "launch_table",
                    LaunchTable::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockStruct> STRUCT_LAUNCHER_CORE =
            Reg.blockItem(
                    "struct_launcher_core",
                    props -> new BlockStruct(props, false),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockStruct> STRUCT_LAUNCHER_CORE_LARGE =
            Reg.blockItem(
                    "struct_launcher_core_large",
                    props -> new BlockStruct(props, true),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockSoyuzStruct> STRUCT_SOYUZ_CORE =
            Reg.blockItem(
                    "struct_soyuz_core",
                    BlockSoyuzStruct::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SoyuzLauncher> SOYUZ_LAUNCHER =
            Reg.blockItem(
                    "soyuz_launcher",
                    SoyuzLauncher::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<LaunchpadSoyuz> LAUNCHPAD_SOYUZ =
            Reg.blockItem(
                    "launchpad_soyuz",
                    LaunchpadSoyuz::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<MachineSatDock> SAT_DOCK =
            Reg.blockItem(
                    "sat_dock",
                    MachineSatDock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SoyuzCapsule> SOYUZ_CAPSULE =
            Reg.blockItem(
                    "soyuz_capsule",
                    SoyuzCapsule::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockDoorGeneric> SECURE_ACCESS_DOOR =
            Reg.blockItem(
                            "secure_access_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.SECURE_ACCESS_DOOR, true),
                            () -> doorProperties().strength(20.0F, 1200.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> QE_SLIDING_DOOR =
            Reg.blockItem(
                            "qe_sliding_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.QE_SLIDING),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> CARGO_DOOR =
            Reg.blockItem(
                            "cargo_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.CARGO_DOOR),
                            () -> doorProperties().strength(5.0F, 30.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> WATER_DOOR =
            Reg.blockItem(
                            "water_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.WATER_DOOR),
                            () -> doorProperties().strength(5.0F, 30.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> TRANSITION_SEAL =
            Reg.blockItem(
                            "transition_seal",
                            props -> new BlockDoorGeneric(props, DoorDecl.TRANSITION_SEAL, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> VAULT_DOOR =
            Reg.blockItem(
                            "vault_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.VAULT_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> SLIDING_SEAL_DOOR =
            Reg.blockItem(
                            "sliding_seal_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.SLIDING_SEAL_DOOR),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> QE_CONTAINMENT =
            Reg.blockItem(
                            "qe_containment",
                            props -> new BlockDoorGeneric(props, DoorDecl.QE_CONTAINMENT, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> ROUND_AIRLOCK_DOOR =
            Reg.blockItem(
                            "round_airlock_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.ROUND_AIRLOCK_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> SLIDING_BLAST_DOOR =
            Reg.blockItem(
                            "sliding_blast_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.SLIDE_DOOR),
                            () -> doorProperties().strength(10.0F, 450.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> LARGE_VEHICLE_DOOR =
            Reg.blockItem(
                            "large_vehicle_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.LARGE_VEHICLE_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());

    public static final RegistryHandle<BlastDoor> BLAST_DOOR =
            Reg.blockItem(
                            "blast_door",
                            BlastDoor::new,
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed);
    public static final RegistryHandle<BlockDoorGeneric> FIRE_DOOR =
            Reg.blockItem(
                            "fire_door",
                            props -> new BlockDoorGeneric(props, DoorDecl.FIRE_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> SILO_HATCH =
            Reg.blockItem(
                            "silo_hatch",
                            props -> new BlockDoorGeneric(props, DoorDecl.SILO_HATCH),
                            ModBlocks::doorProperties)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlockDoorGeneric> SILO_HATCH_LARGE =
            Reg.blockItem(
                            "silo_hatch_large",
                            props -> new BlockDoorGeneric(props, DoorDecl.SILO_HATCH_LARGE),
                            ModBlocks::doorProperties)
                    .bakedBy(() -> new DoorModel());

    public static final RegistryHandle<DungeonSpawner> DUNGEON_SPAWNER =
            Reg.block(
                            "dungeon_spawner",
                            DungeonSpawner::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(-1.0F, 300000.0F)
                                            .sound(SoundType.STONE)
                                            .noLootTable())
                    .andThen(h -> decoItem("dungeon_spawner", h));

    public static final RegistryHandle<BlockSkeletonHolder> SKELETON_HOLDER =
            Reg.blockItem(
                    "skeleton_holder",
                    BlockSkeletonHolder::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(2.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlockPedestal> PEDESTAL =
            Reg.blockItem(
                            "pedestal",
                            BlockPedestal::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(2.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new StandardBlockModel());

    public static final RegistryHandle<BlockBobble> BOBBLEHEAD =
            Reg.<BlockBobble>blockItem(
                    "bobblehead",
                    BlockBobble::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.0F, 0.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new ItemBlockTrinket<>(block, props, BobbleType.NONE));

    public static final ItemFamily<BobbleType, ItemBlockTrinket<BobbleType>> BOBBLEHEADS =
            Reg.family(
                    Arrays.copyOfRange(BobbleType.values(), 1, BobbleType.values().length),
                    type -> "bobblehead_" + type.name().toLowerCase(Locale.ROOT),
                    (props, type) -> new ItemBlockTrinket<>(BOBBLEHEAD.get(), props, type),
                    Item.Properties::new);

    public static final RegistryHandle<BlockSnowglobe> SNOWGLOBE =
            Reg.<BlockSnowglobe>blockItem(
                    "snowglobe",
                    BlockSnowglobe::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(0.0F, 0.0F)
                                    .sound(SoundType.STONE)
                                    .instrument(NoteBlockInstrument.HAT)
                                    .noOcclusion()
                                    .noLootTable(),
                    (block, props) -> new ItemBlockTrinket<>(block, props, SnowglobeType.NONE));

    public static final ItemFamily<SnowglobeType, ItemBlockTrinket<SnowglobeType>> SNOWGLOBES =
            Reg.family(
                    Arrays.copyOfRange(SnowglobeType.values(), 1, SnowglobeType.values().length),
                    type -> "snowglobe_" + type.name().toLowerCase(Locale.ROOT),
                    (props, type) -> new ItemBlockTrinket<>(SNOWGLOBE.get(), props, type),
                    Item.Properties::new);

    public static final RegistryHandle<BlockPlushie> PLUSHIE =
            Reg.<BlockPlushie>blockItem(
                    "plushie",
                    BlockPlushie::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOL)
                                    .strength(0.0F, 0.0F)
                                    .sound(SoundType.WOOL)
                                    .noOcclusion()
                                    .noLootTable()
                                    .ignitedByLava(),
                    (block, props) -> new ItemBlockPlushie(block, props, PlushieType.NONE));

    public static final ItemFamily<PlushieType, ItemBlockPlushie> PLUSHIES =
            Reg.family(
                    Arrays.copyOfRange(PlushieType.values(), 1, PlushieType.values().length),
                    type -> "plushie_" + type.name().toLowerCase(Locale.ROOT),
                    (props, type) -> new ItemBlockPlushie(PLUSHIE.get(), props, type),
                    Item.Properties::new);

    public static final RegistryHandle<Floodlight> FLOODLIGHT =
            Reg.blockItem(
                            "floodlight",
                            Floodlight::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new FloodlightModel());

    public static final RegistryHandle<FloodlightBeam> FLOODLIGHT_BEAM =
            Reg.block(
                    "floodlight_beam",
                    FloodlightBeam::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(-1.0F, 600000.0F)
                                    .noCollision()
                                    .noOcclusion()
                                    .replaceable()
                                    .lightLevel(state -> 15)
                                    .noLootTable());

    public static final RegistryHandle<MachineRTG> MACHINE_RTG =
            Reg.blockItem(
                            "machine_rtg_grey",
                            MachineRTG::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new RTGModel());

    public static final RegistryHandle<MachineBoilerOff> MACHINE_BOILER_OFF =
            Reg.blockItem(
                    "machine_boiler_off",
                    MachineBoilerOff::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockWeaponTable> MACHINE_WEAPON_TABLE =
            Reg.blockItem(
                    "machine_weapon_table",
                    BlockWeaponTable::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockArmorTable> MACHINE_ARMOR_TABLE =
            Reg.blockItem(
                    "machine_armor_table",
                    BlockArmorTable::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<HEVBattery> HEV_BATTERY =
            Reg.blockItem(
                    "hev_battery",
                    HEVBattery::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.5F, 0.15F)
                                    .lightLevel(s -> 10)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MachineReactorControl> MACHINE_CONTROLLER =
            Reg.blockItem(
                    "machine_controller",
                    MachineReactorControl::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretSentryDamaged> TURRET_SENTRY_DAMAGED =
            Reg.blockItem(
                    "turret_sentry_damaged",
                    TurretSentryDamaged::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 3.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretChekhov> TURRET_CHEKHOV =
            Reg.blockItem(
                    "turret_chekhov",
                    TurretChekhov::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretFriendly> TURRET_FRIENDLY =
            Reg.blockItem(
                    "turret_friendly",
                    TurretFriendly::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretJeremy> TURRET_JEREMY =
            Reg.blockItem(
                    "turret_jeremy",
                    TurretJeremy::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretRichard> TURRET_RICHARD =
            Reg.blockItem(
                    "turret_richard",
                    TurretRichard::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretHoward> TURRET_HOWARD =
            Reg.blockItem(
                    "turret_howard",
                    TurretHoward::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 36.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretTauon> TURRET_TAUON =
            Reg.blockItem(
                    "turret_tauon",
                    TurretTauon::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 36.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretFritz> TURRET_FRITZ =
            Reg.blockItem(
                    "turret_fritz",
                    TurretFritz::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretMaxwell> TURRET_MAXWELL =
            Reg.blockItem(
                    "turret_maxwell",
                    TurretMaxwell::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 36.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretSentry> TURRET_SENTRY =
            Reg.blockItem(
                    "turret_sentry",
                    TurretSentry::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 3.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretHowardDamaged> TURRET_HOWARD_DAMAGED =
            Reg.blockItem(
                            "turret_howard_damaged",
                            TurretHowardDamaged::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 360.0F)
                                            .noOcclusion()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new TurretHowardDamagedModel());
    public static final RegistryHandle<TurretArty> TURRET_ARTY =
            Reg.blockItem(
                    "turret_arty",
                    TurretArty::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretHIMARS> TURRET_HIMARS =
            Reg.blockItem(
                    "turret_himars",
                    TurretHIMARS::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockSmolder> ORE_NETHER_SMOLDERING =
            Reg.block(
                            "ore_nether_smoldering",
                            BlockSmolder::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NETHER)
                                            .strength(0.4F, 6.0F)
                                            .lightLevel(s -> 15)
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("ore_nether_smoldering", h)));
    public static final RegistryHandle<BlockBedrockOre> ORE_BEDROCK =
            Reg.block(
                            "ore_bedrock",
                            BlockBedrockOre::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(-1.0F, 600000.0F)
                                            .sound(SoundType.STONE)
                                            .noLootTable()
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .bakedBy(() -> new BedrockOreModel.Family())
                    .andThen(h -> decoItem("ore_bedrock", h));

    public static final RegistryHandle<BlockGeysir> GEYSIR_NETHER =
            Reg.block(
                    "geysir_nether",
                    BlockGeysir::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NETHER)
                                    .strength(2.0F, 2.0F)
                                    .lightLevel(s -> 15)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BlockGeysir> GEYSIR_CHLORINE =
            Reg.block(
                    "geysir_chlorine",
                    BlockGeysir::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 5.0F)
                                    .sound(SoundType.STONE)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BlockMeteorMolten> BLOCK_METEOR_MOLTEN =
            Reg.block(
                    "block_meteor_molten",
                    BlockMeteorMolten::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NETHER)
                                    .strength(15.0F, 216.0F)
                                    .sound(SoundType.STONE)
                                    .lightLevel(s -> 11)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<Block> ORE_METEOR_IRON =
            oreCube("ore_meteor_iron", 5.0F, 6.0F);
    public static final RegistryHandle<Block> ORE_METEOR_COPPER =
            oreCube("ore_meteor_copper", 5.0F, 6.0F);
    public static final RegistryHandle<Block> ORE_METEOR_ALUMINIUM =
            oreCube("ore_meteor_aluminium", 5.0F, 6.0F);
    public static final RegistryHandle<Block> ORE_METEOR_RAREEARTH =
            oreCube("ore_meteor_rareearth", 5.0F, 6.0F);
    public static final RegistryHandle<Block> ORE_METEOR_COBALT =
            oreCube("ore_meteor_cobalt", 5.0F, 6.0F);
    public static final RegistryHandle<BlockBiomeStone> STONE_BIOME_DESERT =
            Reg.block(
                            "stone_biome_desert",
                            BlockBiomeStone::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.SAND)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("stone_biome_desert", h)));
    public static final RegistryHandle<BlockBiomeStone> STONE_BIOME_WOODLAND =
            Reg.block(
                            "stone_biome_woodland",
                            BlockBiomeStone::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.PODZOL)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("stone_biome_woodland", h)));
    public static final RegistryHandle<BlockHazardFalling> BLOCK_YELLOWCAKE =
            Reg.block(
                            "block_yellowcake",
                            BlockHazardFalling::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.SAND)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.SAND))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("block_yellowcake", h)));
    public static final RegistryHandle<BlockMushHuge> MUSH_BLOCK =
            Reg.blockItem(
                    "mush_block",
                    BlockMushHuge::new,
                    () -> BlockMushHuge.properties(MapColor.DIRT));
    public static final RegistryHandle<BlockMushHuge> MUSH_BLOCK_STEM =
            Reg.blockItem(
                    "mush_block_stem",
                    BlockMushHuge::new,
                    () -> BlockMushHuge.properties(MapColor.WOOL));
    public static final RegistryHandle<Block> GLYPHID_BASE =
            Reg.blockItem(
                    "glyphid_base",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .noLootTable()
                                    .pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<Block> GLYPHID_BASE_INFESTED =
            Reg.blockItem(
                    "glyphid_base_infested",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .noLootTable()
                                    .pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<Block> GLYPHID_BASE_RAD =
            Reg.blockItem(
                    "glyphid_base_rad",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .noLootTable()
                                    .pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<BlockGlyphidSpawner> GLYPHID_SPAWNER =
            Reg.blockItem(
                    "glyphid_spawner",
                    BlockGlyphidSpawner::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<BlockGlyphidSpawner> GLYPHID_SPAWNER_INFESTED =
            Reg.blockItem(
                    "glyphid_spawner_infested",
                    BlockGlyphidSpawner::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<BlockGlyphidSpawner> GLYPHID_SPAWNER_RAD =
            Reg.blockItem(
                    "glyphid_spawner_rad",
                    BlockGlyphidSpawner::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<BlockTritiumLamp> LAMP_TRITIUM_GREEN =
            tritiumLamp("lamp_tritium_green");
    public static final RegistryHandle<BlockTritiumLamp> LAMP_TRITIUM_BLUE =
            tritiumLamp("lamp_tritium_blue");
    public static final RegistryHandle<BlockLantern> LANTERN =
            Reg.blockItem(
                    "lantern",
                    BlockLantern::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(3.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                                    .lightLevel(s -> 15)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockLanternBehemoth> LANTERN_BEHEMOTH =
            Reg.blockItem(
                    "lantern_behemoth",
                    BlockLanternBehemoth::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockHangingVine> VINE_PHOSPHOR =
            Reg.block(
                            "vine_phosphor",
                            BlockHangingVine::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.PLANT)
                                            .strength(0.5F)
                                            .sound(SoundType.GRASS)
                                            .noCollision()
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .ignitedByLava()
                                            .pushReaction(PushReaction.DESTROY))
                    .bakedBy(() -> new HangingVineModel.Family())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("vine_phosphor", h)));
    public static final RegistryHandle<BlockStatueElbF> STATUE_ELB_F =
            Reg.blockItem(
                    "statue_elb_f",
                    BlockStatueElbF::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    .noOcclusion()
                                    .lightLevel(s -> 15)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockSellafieldOre> ORE_SELLAFIELD_DIAMOND =
            sellafieldOre("ore_sellafield_diamond", "diamond", () -> Items.DIAMOND, 3, 7);
    public static final RegistryHandle<BlockSellafieldOre> ORE_SELLAFIELD_EMERALD =
            sellafieldOre("ore_sellafield_emerald", "emerald", () -> Items.EMERALD, 3, 7);
    public static final RegistryHandle<BlockSellafieldOre> ORE_SELLAFIELD_RADGEM =
            sellafieldOre("ore_sellafield_radgem", "radgem", () -> ModItems.GEM_RAD.get(), 3, 7);
    public static final RegistryHandle<BlockSellafieldOre> ORE_SELLAFIELD_SCHRABIDIUM =
            sellafieldOre("ore_sellafield_schrabidium", "schrabidium", null, 0, 0);
    public static final RegistryHandle<BlockSellafieldOre> ORE_SELLAFIELD_URANIUM_SCORCHED =
            sellafieldOre("ore_sellafield_uranium_scorched", "uranium_scorched", null, 0, 0);

    public static final ClassicFluid.Pair SULFURIC_ACID_FLUID =
            Reg.classicFluid(
                    "sulfuric_acid_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.WATER,
                            8,
                            5,
                            1840,
                            true,
                            Library.id("block/fluid/sulfuric_acid"),
                            () -> ModBlocks.SULFURIC_ACID_BLOCK.get(),
                            () -> ItemFluidBucket.make(NTMFluids.SULFURIC_ACID)));
    public static final RegistryHandle<BlockSulfuricAcid> SULFURIC_ACID_BLOCK =
            Reg.block(
                    "sulfuric_acid_block",
                    props -> new BlockSulfuricAcid(SULFURIC_ACID_FLUID.source().get(), props),
                    () -> classicFluidProps(MapColor.WATER));

    public static final ClassicFluid.Pair ACID_FLUID =
            Reg.classicFluid(
                    "acid_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.NONE,
                            4,
                            7,
                            2500,
                            false,
                            Library.id("block/fluid/acid"),
                            () -> ModBlocks.ACID_BLOCK.get(),
                            () -> new ItemStack(ModItems.BUCKET_ACID.get())));
    public static final RegistryHandle<AcidBlock> ACID_BLOCK =
            Reg.block(
                    "acid_block",
                    props -> new AcidBlock(ACID_FLUID.source().get(), props),
                    () ->
                            classicFluidProps(MapColor.COLOR_PURPLE)
                                    .randomTicks()
                                    .lightLevel(state -> BlockFluidClassicBase.light(state, 4, 5)));

    public static final ClassicFluid.Pair TOXIC_FLUID =
            Reg.classicFluid(
                    "toxic_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.NONE,
                            4,
                            10,
                            2500,
                            false,
                            Library.id("block/fluid/toxic"),
                            () -> ModBlocks.TOXIC_BLOCK.get(),
                            () -> new ItemStack(ModItems.BUCKET_TOXIC.get())));
    public static final RegistryHandle<BlockToxic> TOXIC_BLOCK =
            Reg.block(
                    "toxic_block",
                    props -> new BlockToxic(TOXIC_FLUID.source().get(), props),
                    () ->
                            classicFluidProps(MapColor.COLOR_GREEN)
                                    .lightLevel(
                                            state -> BlockFluidClassicBase.light(state, 4, 15)));

    public static final ClassicFluid.Pair SCHRABIDIC_FLUID =
            Reg.classicFluid(
                    "schrabidic_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.NONE,
                            4,
                            2,
                            31200,
                            false,
                            Library.id("block/fluid/schrabidic_acid"),
                            () -> ModBlocks.SCHRABIDIC_BLOCK.get(),
                            () -> ItemFluidBucket.make(NTMFluids.SCHRABIDIC)));
    public static final RegistryHandle<BlockSchrabidic> SCHRABIDIC_BLOCK =
            Reg.block(
                    "schrabidic_block",
                    props -> new BlockSchrabidic(SCHRABIDIC_FLUID.source().get(), props),
                    () -> classicFluidProps(MapColor.COLOR_CYAN));
    public static final RegistryHandle<BlockStalactite> STALACTITE_SULFUR =
            Reg.block("stalactite_sulfur", BlockStalactite::new, ModBlocks::spikeProps)
                    .andThen(h -> DECO_BLOCKS.add(decoItem("stalactite_sulfur", h)));
    public static final RegistryHandle<BlockStalactite> STALACTITE_ASBESTOS =
            Reg.block("stalactite_asbestos", BlockStalactite::new, ModBlocks::spikeProps)
                    .andThen(h -> DECO_BLOCKS.add(decoItem("stalactite_asbestos", h)));
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_SULFUR =
            Reg.block("stalagmite_sulfur", BlockStalagmite::new, ModBlocks::spikeProps)
                    .andThen(h -> DECO_BLOCKS.add(decoItem("stalagmite_sulfur", h)));
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_ASBESTOS =
            Reg.block("stalagmite_asbestos", BlockStalagmite::new, ModBlocks::spikeProps)
                    .andThen(h -> DECO_BLOCKS.add(decoItem("stalagmite_asbestos", h)));
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_ICE =
            stalagmiteVariant("ice", 2, "hbm:powder_ice");
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_SNOW =
            stalagmiteVariant("snow", 3, "minecraft:snowball");
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_GLYPHID1 =
            stalagmiteVariant("glyphid1", 4, null);
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_GLYPHID2 =
            stalagmiteVariant("glyphid2", 5, null);
    public static final RegistryHandle<BlockStalagmite> STALAGMITE_GLYPHID3 =
            stalagmiteVariant("glyphid3", 6, null);
    public static final RegistryHandle<BlockStalactite> STALACTITE_ICE =
            stalactiteVariant("ice", 2, "hbm:powder_ice");
    public static final RegistryHandle<BlockStalactite> STALACTITE_SNOW =
            stalactiteVariant("snow", 3, "minecraft:snowball");
    public static final RegistryHandle<BlockStalactite> STALACTITE_GLYPHID1 =
            stalactiteVariant("glyphid1", 4, null);
    public static final RegistryHandle<BlockStalactite> STALACTITE_GLYPHID2 =
            stalactiteVariant("glyphid2", 5, null);
    public static final RegistryHandle<BlockStalactite> STALACTITE_GLYPHID3 =
            stalactiteVariant("glyphid3", 6, null);
    public static final RegistryHandle<Block> VINYL_TILE_LARGE =
            decoBlast("vinyl_tile_large", 10.0F, 36.0F, SoundType.GLASS);
    public static final RegistryHandle<Block> VINYL_TILE_SMALL =
            decoBlast("vinyl_tile_small", 10.0F, 36.0F, SoundType.GLASS);
    public static final RegistryHandle<Block> WATZ_END_BOLTED =
            Reg.blockItem(
                    "watz_end_bolted",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockToolConversion> WATZ_END =
            Reg.blockItem(
                    "watz_end",
                    props ->
                            new BlockToolConversion(
                                    props,
                                    IToolable.ToolType.BOLT,
                                    () -> WATZ_END_BOLTED.get(),
                                    CountIngredient.of(OreDictManager.DURA.bolt(), 4)),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<Watz> WATZ =
            Reg.blockItem(
                    "watz",
                    Watz::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<WatzPump> WATZ_PUMP =
            Reg.blockItem(
                    "watz_pump",
                    WatzPump::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockWatzStruct> STRUCT_WATZ_CORE =
            Reg.blockItem(
                    "struct_watz_core",
                    BlockWatzStruct::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .lightLevel(state -> 15)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final ClassicFluid.Pair MUD_FLUID =
            Reg.classicFluid(
                    "mud_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.NONE,
                            4,
                            15,
                            2500,
                            false,
                            Library.id("block/fluid/mud"),
                            () -> ModBlocks.MUD_BLOCK.get(),
                            () -> ItemFluidBucket.make(NTMFluids.WATZ_MUD)));
    public static final RegistryHandle<BlockMud> MUD_BLOCK =
            Reg.block(
                    "mud_block",
                    props -> new BlockMud(MUD_FLUID.source().get(), props),
                    () ->
                            classicFluidProps(MapColor.COLOR_ORANGE)
                                    .lightLevel(state -> BlockFluidClassicBase.light(state, 4, 5)));

    public static final RegistryHandle<Block> BLOCK_EUPHEMIUM =
            deco("block_euphemium", 5.0F, 36000.0F, SoundType.METAL);

    public static final RegistryHandle<TransparentBlock> GLASS_QUARTZ =
            Reg.block(
                            "glass_quartz",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(1.0F, 24.0F)
                                            .sound(SoundType.GLASS)
                                            .noOcclusion()
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("glass_quartz", h)));

    public static final RegistryHandle<TransparentBlock> GLASS_BORON =
            Reg.block(
                            "glass_boron",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.3F)
                                            .sound(SoundType.GLASS)
                                            .noOcclusion()
                                            .instrument(NoteBlockInstrument.HAT)
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("glass_boron", h)));

    public static final RegistryHandle<TransparentBlock> GLASS_LEAD =
            Reg.block(
                            "glass_lead",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.3F)
                                            .sound(SoundType.GLASS)
                                            .noOcclusion()
                                            .instrument(NoteBlockInstrument.HAT)
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("glass_lead", h)));
    public static final RegistryHandle<TransparentBlock> GLASS_URANIUM =
            litGlass("glass_uranium", DyeColor.YELLOW);
    public static final RegistryHandle<TransparentBlock> GLASS_TRINITITE =
            Reg.block(
                            "glass_trinitite",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.3F)
                                            .sound(SoundType.GLASS)
                                            .noOcclusion()
                                            .instrument(NoteBlockInstrument.HAT)
                                            .lightLevel(s -> 5)
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("glass_trinitite", h)));
    public static final RegistryHandle<TransparentBlock> GLASS_POLONIUM =
            litGlass("glass_polonium", DyeColor.RED);
    public static final RegistryHandle<TransparentBlock> GLASS_ASH =
            Reg.block(
                            "glass_ash",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(3.0F)
                                            .sound(SoundType.GLASS)
                                            .noOcclusion()
                                            .instrument(NoteBlockInstrument.HAT)
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("glass_ash", h)));

    public static final RegistryHandle<TransparentBlock> GLASS_POLARIZED =
            Reg.<TransparentBlock>block(
                            "glass_polarized",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.3F)
                                            .sound(SoundType.GLASS)
                                            .noOcclusion()
                                            .instrument(NoteBlockInstrument.HAT)
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false))
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("glass_polarized", h)));

    public static final RegistryHandle<TransparentBlock> REINFORCED_GLASS =
            Reg.block(
                            "reinforced_glass",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 15.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false)
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> DECO_BLOCKS.add(blastItem("reinforced_glass", h)));

    public static final RegistryHandle<BlockReinforcedGlassPane> REINFORCED_GLASS_PANE =
            Reg.block(
                            "reinforced_glass_pane",
                            BlockReinforcedGlassPane::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 15.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> DECO_BLOCKS.add(blastItem("reinforced_glass_pane", h)));

    public static final RegistryHandle<TransparentBlock> REINFORCED_LAMINATE =
            Reg.block(
                            "reinforced_laminate",
                            TransparentBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 180.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .isRedstoneConductor((state, level, pos) -> false)
                                            .isSuffocating((state, level, pos) -> false)
                                            .isViewBlocking((state, level, pos) -> false)
                                            .isValidSpawn((state, level, pos, type) -> false)
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> DECO_BLOCKS.add(blastItem("reinforced_laminate", h)));
    public static final RegistryHandle<BlockReinforcedGlassPane> REINFORCED_LAMINATE_PANE =
            Reg.block(
                            "reinforced_laminate_pane",
                            BlockReinforcedGlassPane::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 180.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(h -> DECO_BLOCKS.add(blastItem("reinforced_laminate_pane", h)));

    public static final RegistryHandle<LadderBlock> LADDER_STURDY =
            Reg.block(
                            "ladder_sturdy",
                            LadderBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.25F, 1.2F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("ladder_sturdy", h)));
    public static final RegistryHandle<LadderBlock> LADDER_GOLD =
            Reg.block(
                            "ladder_gold",
                            LadderBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.25F, 1.2F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("ladder_gold", h)));
    public static final RegistryHandle<LadderBlock> LADDER_TITANIUM =
            Reg.block(
                            "ladder_titanium",
                            LadderBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.25F, 1.2F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("ladder_titanium", h)));
    public static final RegistryHandle<LadderBlock> LADDER_COPPER =
            Reg.block(
                            "ladder_copper",
                            LadderBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.25F, 1.2F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("ladder_copper", h)));
    public static final RegistryHandle<LadderBlock> LADDER_STEEL =
            Reg.block(
                            "ladder_steel",
                            LadderBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.25F, 1.2F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("ladder_steel", h)));

    public static final RegistryHandle<TrapDoorBlock> TRAPDOOR_STEEL =
            Reg.block(
                            "trapdoor_steel",
                            props -> new TrapDoorBlock(ModBlockSetTypes.HBM_METAL, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(3.0F, 4.8F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("trapdoor_steel", h)));
    public static final RegistryHandle<DoorBlock> DOOR_METAL =
            Reg.block(
                            "door_metal",
                            props -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 3.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("door_metal", h)));
    public static final RegistryHandle<BlockKeyhole> STONE_KEYHOLE =
            Reg.blockItem(
                    "stone_keyhole",
                    BlockKeyhole::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(0.0F, 0.0F));
    public static final RegistryHandle<BlockRedBrick> BRICK_RED =
            Reg.blockItem(
                    "brick_red",
                    BlockRedBrick::new,
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                                    .strength(0.0F, 6000.0F));
    public static final RegistryHandle<BlockRedBrickKeyhole> STONE_KEYHOLE_META =
            Reg.blockItem(
                    "stone_keyhole_meta",
                    BlockRedBrickKeyhole::new,
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                                    .strength(0.0F, 6000.0F));
    public static final RegistryHandle<DoorBlock> DOOR_RED =
            Reg.block(
                            "door_red",
                            props -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, props),
                            () ->
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR)
                                            .strength(10.0F, 60.0F)
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> decoItem("door_red", h));
    public static final RegistryHandle<DoorBlock> DOOR_OFFICE =
            Reg.block(
                            "door_office",
                            props -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(10.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("door_office", h)));
    public static final RegistryHandle<DoorBlock> DOOR_BUNKER =
            Reg.block(
                            "door_bunker",
                            props -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(10.0F, 60.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(PushReaction.DESTROY))
                    .andThen(h -> DECO_BLOCKS.add(decoItem("door_bunker", h)));
    public static final RegistryHandle<BlockSteelScaffold> STEEL_SCAFFOLD =
            Reg.block(
                            "steel_scaffold",
                            BlockSteelScaffold::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new ScaffoldModel())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_scaffold", h)));
    public static final RegistryHandle<BlockSteelScaffold> STEEL_SCAFFOLD_RED =
            Reg.block(
                            "steel_scaffold_red",
                            BlockSteelScaffold::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new ScaffoldModel())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_scaffold_red", h)));
    public static final RegistryHandle<BlockSteelScaffold> STEEL_SCAFFOLD_WHITE =
            Reg.block(
                            "steel_scaffold_white",
                            BlockSteelScaffold::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new ScaffoldModel())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_scaffold_white", h)));
    public static final RegistryHandle<BlockSteelScaffold> STEEL_SCAFFOLD_YELLOW =
            Reg.block(
                            "steel_scaffold_yellow",
                            BlockSteelScaffold::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new ScaffoldModel())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_scaffold_yellow", h)));
    public static final RegistryHandle<BlockSteelBeam> STEEL_BEAM =
            Reg.block(
                            "steel_beam",
                            BlockSteelBeam::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_beam", h)));

    public static final RegistryHandle<BlockSteelWall> STEEL_WALL =
            Reg.block(
                            "steel_wall",
                            BlockSteelWall::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_wall", h)));
    public static final RegistryHandle<BlockSteelCorner> STEEL_CORNER =
            Reg.block(
                            "steel_corner",
                            BlockSteelCorner::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_corner", h)));
    public static final RegistryHandle<BlockSteelRoof> STEEL_ROOF =
            Reg.block(
                            "steel_roof",
                            BlockSteelRoof::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_roof", h)));
    public static final RegistryHandle<BlockSteelPoles> STEEL_POLES =
            Reg.block(
                            "steel_poles",
                            BlockSteelPoles::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_poles", h)));
    public static final RegistryHandle<BlockPoleTop> POLE_TOP =
            Reg.block(
                            "pole_top",
                            BlockPoleTop::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("pole_top", h)));

    public static final RegistryHandle<BlockMetalFence> FENCE_METAL =
            Reg.block(
                            "fence_metal",
                            BlockMetalFence::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 0.15F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("fence_metal", h)));
    public static final RegistryHandle<BlockMetalFence> FENCE_METAL_POST =
            Reg.block(
                            "fence_metal_post",
                            BlockMetalFence::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 0.15F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem("fence_metal_post", h)));
    public static final RegistryHandle<BlockSteelGrate> STEEL_GRATE =
            Reg.block(
                            "steel_grate",
                            props -> new BlockSteelGrate(props, false),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(grateItem("steel_grate", h, false)));
    public static final RegistryHandle<BlockSteelGrate> STEEL_GRATE_WIDE =
            Reg.block(
                            "steel_grate_wide",
                            props -> new BlockSteelGrate(props, true),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(grateItem("steel_grate_wide", h, true)));

    public static final RegistryHandle<BlockRailGeneric> RAIL_WOOD =
            Reg.block(
                            "rail_wood",
                            props -> new BlockRailGeneric(props, 0.2F),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(h -> RAIL_BLOCKS.add(railItem("rail_wood", h, 1)));
    public static final RegistryHandle<RailBlock> RAIL_NARROW =
            Reg.block(
                            "rail_narrow",
                            RailBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(h -> RAIL_BLOCKS.add(decoItem("rail_narrow", h)));
    public static final RegistryHandle<BlockRailStraight> RAIL_HIGHSPEED =
            Reg.block(
                            "rail_highspeed",
                            props -> new BlockRailStraight(props, 1.0F),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(h -> RAIL_BLOCKS.add(railItem("rail_highspeed", h, 2)));
    public static final RegistryHandle<BlockRailBooster> RAIL_BOOSTER =
            Reg.block(
                            "rail_booster",
                            props -> new BlockRailBooster(props, 1.0F),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(h -> RAIL_BLOCKS.add(railItem("rail_booster", h, 2)));
    public static final RegistryHandle<Spikes> SPIKES =
            Reg.block(
                            "spikes",
                            Spikes::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.5F, 3.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("spikes", h)));
    public static final RegistryHandle<BlockChain> DUNGEON_CHAIN =
            Reg.block(
                            "dungeon_chain",
                            BlockChain::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(0.25F, 1.2F)
                                            .noOcclusion()
                                            .noCollision()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new ChainModel())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("dungeon_chain", h)));
    public static final RegistryHandle<BlockDecoComputer> DECO_COMPUTER =
            Reg.block(
                            "deco_computer",
                            BlockDecoComputer::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_computer", h)));

    public static final RegistryHandle<BlockDecoCRT> DECO_CRT_CLEAN =
            Reg.block(
                            "deco_crt_clean",
                            BlockDecoCRT::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_crt_clean", h)));
    public static final RegistryHandle<BlockDecoCRT> DECO_CRT_BROKEN =
            Reg.block(
                            "deco_crt_broken",
                            BlockDecoCRT::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_crt_broken", h)));
    public static final RegistryHandle<BlockDecoCRT> DECO_CRT_BLINKING =
            Reg.block(
                            "deco_crt_blinking",
                            BlockDecoCRT::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_crt_blinking", h)));
    public static final RegistryHandle<BlockDecoCRT> DECO_CRT_BSOD =
            Reg.block(
                            "deco_crt_bsod",
                            BlockDecoCRT::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_crt_bsod", h)));
    public static final RegistryHandle<BlockDecoToaster> DECO_TOASTER_IRON =
            Reg.block(
                            "deco_toaster_iron",
                            BlockDecoToaster::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_toaster_iron", h)));
    public static final RegistryHandle<BlockDecoToaster> DECO_TOASTER_STEEL =
            Reg.block(
                            "deco_toaster_steel",
                            BlockDecoToaster::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_toaster_steel", h)));
    public static final RegistryHandle<BlockDecoToaster> DECO_TOASTER_WOOD =
            Reg.block(
                            "deco_toaster_wood",
                            BlockDecoToaster::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("deco_toaster_wood", h)));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE =
            Reg.block(
                            "deco_pipe",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.PLAIN));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RUSTED =
            Reg.block(
                            "deco_pipe_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.PLAIN));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_GREEN =
            Reg.block(
                            "deco_pipe_green",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_green", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.PLAIN));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_GREEN_RUSTED =
            Reg.block(
                            "deco_pipe_green_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_green_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.PLAIN));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RED =
            Reg.block(
                            "deco_pipe_red",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_red", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.PLAIN));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_MARKED =
            Reg.block(
                            "deco_pipe_marked",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_marked", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.PLAIN));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RIM_RUSTED =
            Reg.block(
                            "deco_pipe_rim_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rim_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.RIM));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RIM_GREEN =
            Reg.block(
                            "deco_pipe_rim_green",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rim_green", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.RIM));

    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RIM =
            Reg.block(
                            "deco_pipe_rim",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rim", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.RIM));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RIM_GREEN_RUSTED =
            Reg.block(
                            "deco_pipe_rim_green_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rim_green_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.RIM));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RIM_RED =
            Reg.block(
                            "deco_pipe_rim_red",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rim_red", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.RIM));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_RIM_MARKED =
            Reg.block(
                            "deco_pipe_rim_marked",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_rim_marked", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.RIM));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_QUAD =
            Reg.block(
                            "deco_pipe_quad",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_quad", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.QUAD));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_QUAD_RUSTED =
            Reg.block(
                            "deco_pipe_quad_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_quad_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.QUAD));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_QUAD_GREEN =
            Reg.block(
                            "deco_pipe_quad_green",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_quad_green", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.QUAD));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_QUAD_GREEN_RUSTED =
            Reg.block(
                            "deco_pipe_quad_green_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_quad_green_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.QUAD));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_QUAD_RED =
            Reg.block(
                            "deco_pipe_quad_red",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_quad_red", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.QUAD));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_QUAD_MARKED =
            Reg.block(
                            "deco_pipe_quad_marked",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_quad_marked", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.QUAD));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_FRAMED =
            Reg.block(
                            "deco_pipe_framed",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_framed", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.FRAMED));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_FRAMED_RUSTED =
            Reg.block(
                            "deco_pipe_framed_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_framed_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.FRAMED));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_FRAMED_GREEN =
            Reg.block(
                            "deco_pipe_framed_green",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_framed_green", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.FRAMED));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_FRAMED_GREEN_RUSTED =
            Reg.block(
                            "deco_pipe_framed_green_rusted",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_framed_green_rusted", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.FRAMED));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_FRAMED_RED =
            Reg.block(
                            "deco_pipe_framed_red",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_framed_red", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.FRAMED));
    public static final RegistryHandle<RotatedPillarBlock> DECO_PIPE_FRAMED_MARKED =
            Reg.block(
                            "deco_pipe_framed_marked",
                            RotatedPillarBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 3.0F)
                                            .sound(BlockSteelGrate.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe_framed_marked", h)))
                    .bakedBy(() -> new PipeModel(PipeModel.Kind.FRAMED));
    public static final RegistryHandle<GuideBlock> BOOK_GUIDE =
            Reg.blockItem(
                    "book_guide",
                    GuideBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockCrate> CRATE_IRON =
            crate("crate_iron", CrateType.IRON, 5.0F, 6.0F);
    public static final RegistryHandle<BlockCrate> CRATE_STEEL =
            crate("crate_steel", CrateType.STEEL, 5.0F, 6.0F);
    public static final RegistryHandle<BlockCrate> CRATE_DESH =
            crate("crate_desh", CrateType.DESH, 5.0F, 6.0F);
    public static final RegistryHandle<BlockCrate> CRATE_TUNGSTEN =
            crate("crate_tungsten", BlockCrateTungsten::new, 7.5F, 180.0F)
                    .afterRegistration(RadiationSystemNT::markRadResistant);
    public static final RegistryHandle<BlockCrate> SAFE =
            crate("safe", BlockSafe::new, 7.5F, 6000.0F);

    public static final RegistryHandle<BlockMassStorage> MASS_STORAGE_WOOD =
            massStorage("mass_storage_wood", 100, "_wood");
    public static final RegistryHandle<BlockMassStorage> MASS_STORAGE_IRON =
            massStorage("mass_storage_iron", 10_000, "_iron");
    public static final RegistryHandle<BlockMassStorage> MASS_STORAGE_DESH =
            massStorage("mass_storage_desh", 100_000, "_desh");
    public static final RegistryHandle<BlockMassStorage> MASS_STORAGE_TCALLOY =
            massStorage("mass_storage_tcalloy", 1_000_000, "");
    public static final RegistryHandle<Block> ORE_OIL_SAND =
            fallingCube("ore_oil_sand", MapColor.SAND, SoundType.SAND, 0.5F, 0.6F);
    public static final RegistryHandle<Block> DIRT_DEAD =
            fallingCube("dirt_dead", MapColor.DIRT, SoundType.GRAVEL, 0.5F, 0.5F);
    public static final RegistryHandle<Block> DIRT_OILY =
            fallingCube("dirt_oily", MapColor.DIRT, SoundType.GRAVEL, 0.5F, 0.5F);
    public static final RegistryHandle<Block> SAND_DIRTY =
            fallingCube("sand_dirty", MapColor.SAND, SoundType.SAND, 0.5F, 0.5F);
    public static final RegistryHandle<Block> SAND_DIRTY_RED =
            fallingCube("sand_dirty_red", MapColor.COLOR_ORANGE, SoundType.SAND, 0.5F, 0.5F);
    public static final RegistryHandle<Block> STONE_CRACKED =
            fallingCube(
                    "stone_cracked",
                    MapColor.STONE,
                    SoundType.STONE,
                    5.0F,
                    5.0F,
                    BlockBehaviour.Properties::requiresCorrectToolForDrops);

    public static final RegistryHandle<BlockOilSpill> OIL_SPILL =
            Reg.block("oil_spill", BlockOilSpill::new, BlockOilSpill::defaultProperties)
                    .andThen(h -> DECO_BLOCKS.add(decoItem("oil_spill", h)));
    public static final RegistryHandle<BlockOilSpill> LEAVES_LAYER =
            Reg.block(
                            "leaves_layer",
                            BlockOilSpill::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.PLANT)
                                            .strength(0.1F)
                                            .sound(SoundType.GRASS)
                                            .noOcclusion()
                                            .noLootTable()
                                            .ignitedByLava()
                                            .pushReaction(PushReaction.DESTROY)
                                            .replaceable())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("leaves_layer", h)));
    public static final RegistryHandle<Block> PLANT_DEAD = deadPlant("plant_dead");
    public static final RegistryHandle<Block> PLANT_DEAD_GRASS = deadPlant("plant_dead_grass");
    public static final RegistryHandle<Block> PLANT_DEAD_FLOWER = deadPlant("plant_dead_flower");
    public static final RegistryHandle<Block> PLANT_DEAD_BIGFLOWER =
            deadPlant("plant_dead_bigflower");
    public static final RegistryHandle<Block> PLANT_DEAD_FERN = deadPlant("plant_dead_fern");

    public static final RegistryHandle<BlockNTMFlower> PLANT_FLOWER_FOXGLOVE =
            flower("plant_flower_foxglove", BlockNTMFlower.Variant.FOXGLOVE);

    public static final RegistryHandle<BlockNTMFlower> PLANT_FLOWER_TOBACCO =
            tintedFlower("plant_flower_tobacco", BlockNTMFlower.Variant.TOBACCO);
    public static final RegistryHandle<BlockNTMFlower> PLANT_FLOWER_NIGHTSHADE =
            flower("plant_flower_nightshade", BlockNTMFlower.Variant.NIGHTSHADE);
    public static final RegistryHandle<BlockNTMFlower> PLANT_FLOWER_WEED =
            tintedFlower("plant_flower_weed", BlockNTMFlower.Variant.WEED);
    public static final RegistryHandle<BlockNTMFlower> PLANT_FLOWER_CD0 =
            flower("plant_flower_cd0", BlockNTMFlower.Variant.CD0);

    public static final RegistryHandle<BlockNTMFlower> PLANT_FLOWER_CD1 =
            flower("plant_flower_cd1", BlockNTMFlower.Variant.CD1);

    public static final RegistryHandle<BlockCustomMachine> CUSTOM_MACHINE = customMachine();
    public static final RegistryHandle<Block> CM_BLOCK_STEEL = cmCube("cm_block_steel");
    public static final RegistryHandle<Block> CM_BLOCK_ALLOY = cmCube("cm_block_alloy");
    public static final RegistryHandle<Block> CM_BLOCK_DESH = cmCube("cm_block_desh");
    public static final RegistryHandle<Block> CM_BLOCK_TCALLOY = cmCube("cm_block_tcalloy");
    public static final RegistryHandle<Block> CM_SHEET_STEEL = cmCube("cm_sheet_steel");
    public static final RegistryHandle<Block> CM_SHEET_ALLOY = cmCube("cm_sheet_alloy");
    public static final RegistryHandle<Block> CM_SHEET_DESH = cmCube("cm_sheet_desh");
    public static final RegistryHandle<Block> CM_SHEET_TCALLOY = cmCube("cm_sheet_tcalloy");
    public static final RegistryHandle<Block> CM_ENGINE_STANDARD = cmCube("cm_engine_standard");
    public static final RegistryHandle<Block> CM_ENGINE_DESH = cmCube("cm_engine_desh");
    public static final RegistryHandle<Block> CM_ENGINE_BISMUTH = cmCube("cm_engine_bismuth");
    public static final RegistryHandle<BlockCMTank> CM_TANK_STEEL = cmTank("cm_tank_steel");
    public static final RegistryHandle<BlockCMTank> CM_TANK_ALLOY = cmTank("cm_tank_alloy");
    public static final RegistryHandle<BlockCMTank> CM_TANK_DESH = cmTank("cm_tank_desh");
    public static final RegistryHandle<BlockCMTank> CM_TANK_TCALLOY = cmTank("cm_tank_tcalloy");
    public static final RegistryHandle<Block> CM_CIRCUIT_ALUMINIUM = cmCube("cm_circuit_aluminium");
    public static final RegistryHandle<Block> CM_CIRCUIT_COPPER = cmCube("cm_circuit_copper");
    public static final RegistryHandle<Block> CM_CIRCUIT_RED_COPPER =
            cmCube("cm_circuit_red_copper");
    public static final RegistryHandle<Block> CM_CIRCUIT_GOLD = cmCube("cm_circuit_gold");
    public static final RegistryHandle<Block> CM_CIRCUIT_SCHRABIDIUM =
            cmCube("cm_circuit_schrabidium");
    public static final RegistryHandle<BlockCMPort> CM_PORT_STEEL = cmPort("cm_port_steel");
    public static final RegistryHandle<BlockCMPort> CM_PORT_ALLOY = cmPort("cm_port_alloy");
    public static final RegistryHandle<BlockCMPort> CM_PORT_DESH = cmPort("cm_port_desh");
    public static final RegistryHandle<BlockCMPort> CM_PORT_TCALLOY = cmPort("cm_port_tcalloy");

    public static final RegistryHandle<Block> CM_FLUX = cmPillar("cm_flux");
    public static final RegistryHandle<Block> CM_HEAT = cmPillar("cm_heat");
    public static final RegistryHandle<Block> CUSTOM_MACHINE_ANCHOR = cmAnchor();
    public static final RegistryHandle<BlockReeds> PLANT_REEDS = reeds("plant_reeds");

    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_WEED =
            tintedTallPlant(
                    "plant_tall_weed", BlockTallPlant.Variant.WEED, "hbm:plant_flower_weed");
    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_CD2 =
            tallPlant("plant_tall_cd2", BlockTallPlant.Variant.CD2, "hbm:plant_flower_cd0");
    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_CD3 =
            tallPlant("plant_tall_cd3", BlockTallPlant.Variant.CD3, "hbm:plant_flower_cd0");

    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_CD4 =
            tallPlant("plant_tall_cd4", BlockTallPlant.Variant.CD4, "hbm:plant_flower_cd0");
    public static final RegistryHandle<BlockGraphiteDrilled> BLOCK_GRAPHITE_DRILLED =
            pile("block_graphite_drilled", BlockGraphiteDrilled::new);
    public static final RegistryHandle<BlockGraphiteFuel> BLOCK_GRAPHITE_FUEL =
            pile("block_graphite_fuel", BlockGraphiteFuel::new);
    public static final RegistryHandle<BlockGraphiteSource> BLOCK_GRAPHITE_PLUTONIUM =
            pile(
                    "block_graphite_plutonium",
                    props ->
                            new BlockGraphiteSource(
                                    props, () -> ModItems.PILE_ROD_PLUTONIUM.get()));
    public static final RegistryHandle<BlockGraphiteSource> BLOCK_GRAPHITE_SOURCE =
            pile(
                    "block_graphite_source",
                    props -> new BlockGraphiteSource(props, () -> ModItems.PILE_ROD_SOURCE.get()));
    public static final RegistryHandle<BlockGraphiteRod> BLOCK_GRAPHITE_ROD =
            pile("block_graphite_rod", BlockGraphiteRod::new);
    public static final RegistryHandle<BlockGraphiteBreedingFuel> BLOCK_GRAPHITE_LITHIUM =
            pile("block_graphite_lithium", BlockGraphiteBreedingFuel::new);
    public static final RegistryHandle<BlockGraphiteBreedingProduct> BLOCK_GRAPHITE_TRITIUM =
            pile("block_graphite_tritium", BlockGraphiteBreedingProduct::new);
    public static final RegistryHandle<BlockGraphiteNeutronDetector> BLOCK_GRAPHITE_DETECTOR =
            pile("block_graphite_detector", BlockGraphiteNeutronDetector::new);
    public static final RegistryHandle<BlockPileBrick> PILE_BRICK =
            Reg.blockItem(
                    "pile_brick",
                    BlockPileBrick::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockPile> PILE_BLOCK =
            Reg.blockItem(
                    "pile_block",
                    BlockPile::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(15.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops()
                                    .pushReaction(PushReaction.BLOCK));
    public static final RegistryHandle<BlockCargoElevator> CARGO_ELEVATOR =
            Reg.blockItem(
                    "cargo_elevator",
                    BlockCargoElevator::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .dynamicShape()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockVendingMachine> VENDING_MACHINE =
            Reg.blockItem(
                    "vending_machine",
                    props -> new BlockVendingMachine(props, ItemPools.POOL_SODA),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockVendingMachine> VENDING_MACHINE_SNACKS =
            Reg.blockItem(
                    "vending_machine_snacks",
                    props -> new BlockVendingMachine(props, ItemPools.POOL_SNACKS),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<BlockPileDevice> PILE_LOADER =
            pileDevice(
                    "pile_loader",
                    props -> new BlockPileDevice(props, BlockPileDevice.Kind.LOADER));
    public static final RegistryHandle<BlockPileVent> PILE_VENT =
            pileDevice("pile_vent", BlockPileVent::new);
    public static final RegistryHandle<BlockPileDevice> PILE_CONTROL =
            pileDevice(
                    "pile_control",
                    props -> new BlockPileDevice(props, BlockPileDevice.Kind.CONTROL));
    public static final RegistryHandle<MachineFan> FAN =
            Reg.blockItem(
                            "fan",
                            MachineFan::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new FanModel());
    public static final RegistryHandle<PistonInserter> PISTON_INSERTER =
            Reg.blockItem(
                            "piston_inserter",
                            PistonInserter::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new PistonInserterModel());
    public static final RegistryHandle<MachineFusionTorus> FUSION_TORUS =
            fusionMachine("fusion_torus", MachineFusionTorus::new, 4);
    public static final RegistryHandle<MachineFusionKlystron> FUSION_KLYSTRON =
            fusionMachine("fusion_klystron", MachineFusionKlystron::new, 2);
    public static final RegistryHandle<MachineFusionKlystronCreative> FUSION_KLYSTRON_CREATIVE =
            fusionMachine("fusion_klystron_creative", MachineFusionKlystronCreative::new, 1);
    public static final RegistryHandle<MachineFusionBreeder> FUSION_BREEDER =
            fusionMachine("fusion_breeder", MachineFusionBreeder::new, 3);
    public static final RegistryHandle<MachineFusionCollector> FUSION_COLLECTOR =
            fusionMachine("fusion_collector", MachineFusionCollector::new, 2);
    public static final RegistryHandle<MachineFusionBoiler> FUSION_BOILER =
            fusionMachine("fusion_boiler", MachineFusionBoiler::new, 2);
    public static final RegistryHandle<MachineFusionMHDT> FUSION_MHDT =
            fusionMachine("fusion_mhdt", MachineFusionMHDT::new, 4);
    public static final RegistryHandle<MachineFusionCoupler> FUSION_COUPLER =
            fusionMachine("fusion_coupler", MachineFusionCoupler::new, 3);
    public static final RegistryHandle<MachineFusionPlasmaForge> FUSION_PLASMA_FORGE =
            fusionMachine("fusion_plasma_forge", MachineFusionPlasmaForge::new, 0);
    public static final RegistryHandle<Block> FUSION_COMPONENT_BSCCO_WELDED =
            fusionComponent("fusion_component_bscco_welded", Block::new);
    public static final RegistryHandle<Block> FUSION_COMPONENT_BLANKET =
            fusionComponent("fusion_component_blanket", Block::new);
    public static final RegistryHandle<Block> FUSION_COMPONENT_MOTOR =
            fusionComponent("fusion_component_motor", Block::new);
    public static final RegistryHandle<BlockToolConversion> FUSION_COMPONENT =
            fusionComponent(
                    "fusion_component",
                    props ->
                            new BlockToolConversion(
                                    props,
                                    IToolable.ToolType.TORCH,
                                    () -> FUSION_COMPONENT_BSCCO_WELDED.get(),
                                    CountIngredient.of(OreDictManager.STEEL.plateCast(), 1)));
    public static final RegistryHandle<BlockFusionTorusStruct> STRUCT_TORUS_CORE =
            Reg.blockItem(
                    "struct_torus_core",
                    BlockFusionTorusStruct::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(state -> 15)
                                    .noOcclusion());
    private static final String[] SELLAFIELD_STAGES = {
        "block/sellafield_slaked", "block/sellafield_slaked_1",
        "block/sellafield_slaked_2", "block/sellafield_slaked_3"
    };
    public static RegistryHandle<Block> CMB_BRICK_REINFORCED;
    public static RegistryHandle<Block> BLOCK_GRAPHITE;
    public static RegistryHandle<Block> SEAL_FRAME;
    public static RegistryHandle<Block> MACHINE_TRANSFORMER;
    public static RegistryHandle<Block> STRUCT_LAUNCHER;
    public static RegistryHandle<Block> STRUCT_SCAFFOLD;
    public static RegistryHandle<Block> FUSION_HEATER;
    public static RegistryHandle<Block> FUSION_HATCH;
    public static RegistryHandle<Block> WATZ_ELEMENT;
    public static RegistryHandle<Block> WATZ_COOLER;
    public static RegistryHandle<Block> STONE_POROUS;
    public static RegistryHandle<Block> STONE_GNEISS;
    public static RegistryHandle<Block> ORE_URANIUM;
    public static RegistryHandle<Block> CONCRETE;
    public static RegistryHandle<Block> CONCRETE_SMOOTH;
    public static RegistryHandle<Block> CONCRETE_ASBESTOS;
    public static RegistryHandle<Block> CONCRETE_REBAR;
    public static final List<RegistryHandle<Block>> CONCRETE_COLORED = new ArrayList<>(16);
    public static RegistryHandle<BlockRebar> REBAR;
    public static RegistryHandle<Block> DECO_RUSTY_STEEL;
    public static RegistryHandle<Block> METEOR_POLISHED;
    public static RegistryHandle<Block> METEOR_BRICK;
    public static RegistryHandle<Block> METEOR_BRICK_MOSSY;
    public static RegistryHandle<Block> METEOR_BRICK_CRACKED;
    public static RegistryHandle<Block> CONCRETE_LIME;
    public static RegistryHandle<Block> ORE_SCHRABIDIUM;
    public static RegistryHandle<Block> ORE_URANIUM_SCORCHED;
    public static RegistryHandle<Block> ORE_NETHER_URANIUM;
    public static RegistryHandle<Block> ORE_NETHER_SCHRABIDIUM;
    public static RegistryHandle<Block> ORE_NETHER_URANIUM_SCORCHED;
    public static RegistryHandle<Block> ORE_GNEISS_URANIUM;
    public static RegistryHandle<Block> ORE_GNEISS_SCHRABIDIUM;
    public static RegistryHandle<Block> ORE_GNEISS_URANIUM_SCORCHED;
    public static RegistryHandle<Block> ORE_BERYLLIUM;
    public static RegistryHandle<Block> ORE_COLTAN;
    public static RegistryHandle<Block> ORE_LIGNITE;
    public static RegistryHandle<Block> DECO_STEEL;
    public static RegistryHandle<BlockPlatemetal> PLATEMETAL;
    public static RegistryHandle<Block> WASTE_PLANKS;
    public static RegistryHandle<BlockFrozenEarth> FROZEN_DIRT;
    public static RegistryHandle<BlockFrozenEarth> FROZEN_GRASS;
    public static RegistryHandle<Block> FROZEN_LOG;
    public static RegistryHandle<Block> FROZEN_PLANKS;
    public static RegistryHandle<Block> BLOCK_LEAD;
    public static RegistryHandle<Block> BLOCK_BERYLLIUM;
    public static RegistryHandle<Block> BLOCK_DESH;
    public static RegistryHandle<Block> ORE_GNEISS_GAS;

    public static RegistryHandle<BlockOreBasalt> ORE_BASALT_ASBESTOS;
    public static RegistryHandle<Block> ORE_BASALT_GEM;
    public static RegistryHandle<BlockHazardFalling> BLOCK_FALLOUT;
    public static RegistryHandle<Block> BLOCK_COKE_COAL;
    public static RegistryHandle<Block> BLOCK_COKE_LIGNITE;
    public static RegistryHandle<Block> BLOCK_COKE_PETROLEUM;
    public static RegistryHandle<Block> BLOCK_SEMTEX;
    public static RegistryHandle<Block> BLOCK_C4;
    public static RegistryHandle<Block> STONE_RESOURCE_HEMATITE;
    public static RegistryHandle<Block> STONE_RESOURCE_BAUXITE;
    public static RegistryHandle<Block> STONE_RESOURCE_MALACHITE;
    public static RegistryHandle<Block> STONE_RESOURCE_LIMESTONE;
    public static RegistryHandle<BlockDepth> STONE_DEPTH;
    public static RegistryHandle<BlockDepth> STONE_DEPTH_NETHER;
    public static RegistryHandle<BlockDepth> CLUSTER_DEPTH_IRON;
    public static RegistryHandle<BlockDepth> CLUSTER_DEPTH_TITANIUM;
    public static RegistryHandle<BlockDepth> CLUSTER_DEPTH_TUNGSTEN;
    public static RegistryHandle<BlockDepth> ORE_DEPTH_CINNABAR;
    public static RegistryHandle<BlockDepth> ORE_DEPTH_ZIRCONIUM;
    public static RegistryHandle<BlockDepth> ORE_DEPTH_BORAX;
    public static RegistryHandle<BlockDepth> ORE_DEPTH_NETHER_NEODYMIUM;
    public static RegistryHandle<Block> CLUSTER_IRON;
    public static RegistryHandle<Block> CLUSTER_TITANIUM;
    public static RegistryHandle<Block> CLUSTER_ALUMINIUM;
    public static RegistryHandle<Block> CLUSTER_COPPER;
    public static RegistryHandle<BlockDepth> ORE_ALEXANDRITE;
    public static RegistryHandle<Block> BLOCK_METEOR;
    public static RegistryHandle<Block> BLOCK_METEOR_COBBLE;
    public static RegistryHandle<Block> BLOCK_METEOR_BROKEN;
    public static RegistryHandle<Block> BLOCK_METEOR_TREASURE;
    public static RegistryHandle<Block> ORE_OIL;
    public static RegistryHandle<Block> ORE_OIL_EMPTY;
    public static RegistryHandle<Block> ORE_BEDROCK_OIL;
    public static RegistryHandle<BlockOreNetherFire> ORE_NETHER_FIRE;
    public static RegistryHandle<Block> ORE_NETHER_COAL;
    public static RegistryHandle<Block> ORE_TIKITE;
    public static RegistryHandle<Block> ORE_AUSTRALIUM;
    public static RegistryHandle<RotatedPillarBlock> METEOR_PILLAR;
    public static RegistryHandle<Block> NTM_DIRT;
    public static RegistryHandle<NukeCustom> NUKE_CUSTOM;
    public static RegistryHandle<NukeBalefire> NUKE_FSTBMB;
    public static RegistryHandle<BombMulti> BOMB_MULTI;
    public static RegistryHandle<BlockFireworks> FIREWORKS;
    public static RegistryHandle<BlockDynamite> DYNAMITE;
    public static RegistryHandle<BlockTNT> TNT_NTM;
    public static RegistryHandle<BlockFissureBomb> FISSURE_BOMB;
    public static RegistryHandle<BlockSemtex> SEMTEX;
    public static RegistryHandle<BlockC4> C4;
    public static RegistryHandle<BlockDetCharge> DET_CHARGE;
    public static RegistryHandle<BlockDetCord> DET_CORD;
    public static RegistryHandle<BlockDetNuke> DET_NUKE;
    public static RegistryHandle<BlockDetMiner> DET_MINER;
    public static RegistryHandle<BlockBalefire> BALEFIRE;

    public static RegistryHandle<BlockTaint> TAINT;
    public static RegistryHandle<BombFlameWar> FLAME_WAR;
    public static RegistryHandle<BlockEMPBomb> EMP_BOMB;
    public static RegistryHandle<BlockFloatBomb> FLOAT_BOMB;
    public static RegistryHandle<BlockThermoBomb.Endothermic> THERM_ENDO;
    public static RegistryHandle<BlockThermoBomb.Exothermic> THERM_EXO;
    public static RegistryHandle<Block> CRYSTAL_HARDENED;
    public static RegistryHandle<CrystalVirus> CRYSTAL_VIRUS;
    public static RegistryHandle<CrystalPulsar> CRYSTAL_PULSAR;

    public static RegistryHandle<Block> BLOCK_SCHRABIDIUM_CLUSTER;
    public static RegistryHandle<Block> BLOCK_EUPHEMIUM_CLUSTER;
    public static RegistryHandle<Block> ORE_TEKTITE_OSMIRIDIUM;
    public static RegistryHandle<Block> TEKTITE;
    public static RegistryHandle<BlockCybercrab> METEOR_SPAWNER;
    public static RegistryHandle<MachineTesla> TESLA;
    public static RegistryHandle<BlockLootCrate> CRATE;
    public static RegistryHandle<BlockLootCrate> CRATE_RED;
    public static RegistryHandle<Block> METEOR_BATTERY;
    public static RegistryHandle<Block> STONE_RESOURCE_SULFUR;
    public static RegistryHandle<Block> STONE_RESOURCE_ASBESTOS;
    public static RegistryHandle<Block> REINFORCED_STONE;
    public static RegistryHandle<SlabBlock> REINFORCED_STONE_SLAB;
    public static RegistryHandle<StairBlock> REINFORCED_STONE_STAIRS;
    public static RegistryHandle<Block> BRICK_JUNGLE;
    public static RegistryHandle<Block> BRICK_JUNGLE_CRACKED;
    public static RegistryHandle<Block> BRICK_JUNGLE_LAVA;
    public static RegistryHandle<BlockJungleOoze> BRICK_JUNGLE_OOZE;
    public static RegistryHandle<BlockJungleMystic> BRICK_JUNGLE_MYSTIC;
    public static RegistryHandle<FragileBrick> BRICK_JUNGLE_FRAGILE;
    public static RegistryHandle<Block> CRATE_JUNGLE;

    public static RegistryHandle<LogicBlock> LOGIC_BLOCK;
    public static RegistryHandle<LogicBlockInvis> LOGIC_BLOCK_INVIS;
    public static RegistryHandle<WasteEarth> WASTE_EARTH;
    public static RegistryHandle<BlockBurningEarth> BURNING_EARTH;
    public static RegistryHandle<BlockImpactDirt> IMPACT_DIRT;
    public static RegistryHandle<BlockMush> MUSH;
    public static RegistryHandle<BlockWasteMycelium> WASTE_MYCELIUM;
    public static RegistryHandle<Block> WASTE_LEAVES;
    public static RegistryHandle<BlockFallout> FALLOUT;
    public static RegistryHandle<RotatedPillarBlock> BLOCK_FIBERGLASS;
    public static RegistryHandle<Block> BLOCK_INSULATOR;
    public static RegistryHandle<Block> BLOCK_ASBESTOS;
    public static RegistryHandle<BlockNuclearWaste> BLOCK_WASTE;
    public static RegistryHandle<BlockNuclearWaste> BLOCK_WASTE_PAINTED;
    public static RegistryHandle<BlockNuclearWaste> BLOCK_WASTE_VITRIFIED;
    public static RegistryHandle<BlockGasMeltdown> GAS_MELTDOWN;
    public static RegistryHandle<PinkCloudBroadcaster> BROADCASTER_PC;
    public static RegistryHandle<BlockSeal> SEAL_CONTROLLER;
    public static RegistryHandle<BlockSealHatch> SEAL_HATCH;
    public static RegistryHandle<BlockVent> VENT_CHLORINE;
    public static RegistryHandle<BlockVent> VENT_CLOUD;
    public static RegistryHandle<BlockVent> VENT_PINK_CLOUD;
    public static RegistryHandle<BlockChlorineSeal> VENT_CHLORINE_SEAL;
    public static RegistryHandle<MachineFieldDisturber> FIELD_DISTURBER;
    public static RegistryHandle<BlockGasChlorine> CHLORINE_GAS;
    public static RegistryHandle<BlockGasMonoxide> GAS_MONOXIDE;
    public static RegistryHandle<BlockGasCoal> GAS_COAL;
    public static RegistryHandle<BlockGasAsbestos> GAS_ASBESTOS;
    public static RegistryHandle<BlockGasFlammable> GAS_FLAMMABLE;
    public static RegistryHandle<BlockGasExplosive> GAS_EXPLOSIVE;
    public static RegistryHandle<BlockSellafieldSlaked> SELLAFIELD_SLAKED;
    public static RegistryHandle<BlockSellafieldSlaked> SELLAFIELD_BEDROCK;
    public static RegistryHandle<BlockSellafield> SELLAFIELD;
    public static RegistryHandle<BlockWasteLog> WASTE_LOG;
    public static RegistryHandle<Block> PINK_LOG;
    public static RegistryHandle<BlockWasteTrinitite> WASTE_TRINITITE;
    public static RegistryHandle<BlockWasteTrinitite> WASTE_TRINITITE_RED;
    public static RegistryHandle<BlockVolcano> VOLCANO_CORE;
    public static RegistryHandle<BlockVolcano> VOLCANO_RAD_CORE;
    public static RegistryHandle<BlockFissure> ORE_VOLCANO;
    public static ClassicFluid.Pair VOLCANIC_LAVA_FLUID;
    public static RegistryHandle<BlockVolcanicLava> VOLCANIC_LAVA_BLOCK;
    public static ClassicFluid.Pair RAD_LAVA_FLUID;
    public static RegistryHandle<BlockRadLava> RAD_LAVA_BLOCK;
    public static RegistryHandle<BlockLootCrate> CRATE_WEAPON;
    public static RegistryHandle<BlockLootCrate> CRATE_LEAD;
    public static RegistryHandle<BlockLootCrate> CRATE_METAL;
    public static RegistryHandle<BlockCrateAmmo> CRATE_AMMO;
    public static RegistryHandle<BlockCrateCan> CRATE_CAN;
    public static RegistryHandle<BlockCrateSupply> CRATE_SUPPLY;
    public static RegistryHandle<Block> BLOCK_SCRAP;
    public static RegistryHandle<Block> BLOCK_ELECTRICAL_SCRAP;
    public static RegistryHandle<BlockSlag> BLOCK_SLAG;
    public static RegistryHandle<Block> ASPHALT;
    public static RegistryHandle<Block> ASPHALT_LIGHT;
    public static RegistryHandle<Block> BLOCK_FOAM;
    public static RegistryHandle<BlockBoxcar> BOXCAR;
    public static RegistryHandle<Block> BOAT;
    public static RegistryHandle<BlockLayering> FOAM_LAYER;
    public static RegistryHandle<ColoredFallingBlock> SAND_BORON;
    public static RegistryHandle<ColoredFallingBlock> SAND_LEAD;
    public static RegistryHandle<ColoredFallingBlock> SAND_URANIUM;
    public static RegistryHandle<ColoredFallingBlock> SAND_POLONIUM;
    public static RegistryHandle<ColoredFallingBlock> SAND_QUARTZ;
    public static RegistryHandle<BlockLayering> SAND_BORON_LAYER;
    public static RegistryHandle<BlockFileCabinet> FILING_CABINET_GREEN;
    public static RegistryHandle<BlockFileCabinet> FILING_CABINET_STEEL;
    public static RegistryHandle<Block> GRAVEL_OBSIDIAN;
    public static RegistryHandle<BlockFallingBase> GRAVEL_DIAMOND;
    public static RegistryHandle<Block> REINFORCED_DUCRETE;
    public static RegistryHandle<RotatedPillarBlock> CONCRETE_PILLAR;
    public static RegistryHandle<BlockUberConcrete> CONCRETE_SUPER;
    public static RegistryHandle<Block> CONCRETE_SUPER_BROKEN;
    public static RegistryHandle<Block> CONCRETE_EXT_MACHINE;
    public static RegistryHandle<Block> CONCRETE_EXT_MACHINE_STRIPE;
    public static RegistryHandle<Block> CONCRETE_EXT_INDIGO;
    public static RegistryHandle<Block> CONCRETE_EXT_PURPLE;
    public static RegistryHandle<Block> CONCRETE_EXT_PINK;
    public static RegistryHandle<Block> CONCRETE_EXT_HAZARD;
    public static RegistryHandle<Block> CONCRETE_EXT_SAND;
    public static RegistryHandle<Block> CONCRETE_EXT_BRONZE;
    public static RegistryHandle<Block> LIGHTSTONE_UNREFINED;
    public static RegistryHandle<Block> LIGHTSTONE_TILE;
    public static RegistryHandle<Block> LIGHTSTONE_BRICKS;
    public static RegistryHandle<Block> LIGHTSTONE_BRICKS_CHISELED;
    public static RegistryHandle<Block> LIGHTSTONE_CHISELED;
    public static RegistryHandle<StairBlock> LIGHTSTONE_TILE_STAIRS;
    public static RegistryHandle<StairBlock> LIGHTSTONE_BRICKS_STAIRS;
    public static RegistryHandle<Block> DECO_ASBESTOS;
    public static RegistryHandle<BlockSandbags> SANDBAGS;
    public static RegistryHandle<Block> BRICK_CONCRETE_MARKED;
    public static RegistryHandle<BlockForgottenBrick> BRICK_FORGOTTEN;
    public static RegistryHandle<BlockForgottenLock> BRICK_FORGOTTEN_LOCK;
    public static RegistryHandle<Block> TILE_LAB;
    public static RegistryHandle<Block> TILE_LAB_CRACKED;
    public static RegistryHandle<Block> TILE_LAB_BROKEN;

    static {
        String[] boxMaterials = {"silver", "copper", "white"};
        for (int mat = 0; mat < 3; mat++) {
            for (int size = 0; size < 5; size++) {
                String name = "fluid_duct_box_" + boxMaterials[mat] + "_" + size;
                String sprite = "block/boxduct_" + boxMaterials[mat];
                int s = size;
                boolean tinted = mat == 2;
                FLUID_DUCT_BOX[mat][size] =
                        Reg.block(
                                        name,
                                        props -> new FluidDuctBoxBlock(props, s, tinted),
                                        () ->
                                                BlockBehaviour.Properties.of()
                                                        .strength(5.0F, 6.0F)
                                                        .sound(ModSoundTypes.PIPE)
                                                        .noOcclusion()
                                                        .forceSolidOn()
                                                        .requiresCorrectToolForDrops())
                                .bakedBy(() -> new BoxDuctModel());
                RegistryHandle<FluidDuctBoxBlock> handle = FLUID_DUCT_BOX[mat][size];
                Reg.item(
                        name,
                        props -> new BlockItem(handle.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
            }
        }

        for (int size = 0; size < 5; size++) {
            String name = "red_cable_box_" + size;
            int s = size;
            RED_CABLE_BOX[size] =
                    Reg.block(
                                    name,
                                    props -> new CableBoxBlock(props, s),
                                    () ->
                                            BlockBehaviour.Properties.of()
                                                    .strength(5.0F, 6.0F)
                                                    .noOcclusion()
                                                    .forceSolidOn()
                                                    .requiresCorrectToolForDrops())
                            .bakedBy(() -> new BoxDuctModel());
            RegistryHandle<CableBoxBlock> cableBoxHandle = RED_CABLE_BOX[size];
            Reg.item(
                    name,
                    props -> new BlockItem(cableBoxHandle.get(), props),
                    () -> new Item.Properties().useBlockDescriptionPrefix());
        }
        for (int size = 0; size < 5; size++) {
            String name = "fluid_duct_exhaust_" + size;
            int s = size;
            FLUID_DUCT_EXHAUST[size] =
                    Reg.block(
                                    name,
                                    props -> new FluidDuctBoxExhaustBlock(props, s),
                                    () ->
                                            BlockBehaviour.Properties.of()
                                                    .strength(5.0F, 6.0F)
                                                    .sound(ModSoundTypes.PIPE)
                                                    .noOcclusion()
                                                    .forceSolidOn()
                                                    .requiresCorrectToolForDrops())
                            .bakedBy(() -> new BoxDuctModel());
            RegistryHandle<FluidDuctBoxExhaustBlock> exhaustHandle = FLUID_DUCT_EXHAUST[size];
            Reg.item(
                    name,
                    props -> new BlockItem(exhaustHandle.get(), props),
                    () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    static {
        ORE_URANIUM = oreOutgas("ore_uranium", Mats.MAT_URANIUM, 5.0F, 6.0F, () -> GAS_RADON.get());
        ore("ore_thorium", Mats.MAT_THORIUM, 5.0F, 6.0F);
        ore("ore_titanium", Mats.MAT_TITANIUM, 5.0F, 6.0F);
        ore("ore_sulfur", Mats.MAT_SULFUR, 5.0F, 6.0F);
        ore("ore_aluminium", Mats.MAT_ALUMINIUM, 5.0F, 6.0F);

        ore("ore_fluorite", Mats.MAT_FLUORITE, 5.0F, 6.0F);
        ore("ore_niter", Mats.MAT_KNO, 5.0F, 6.0F);
        ore("ore_tungsten", Mats.MAT_TUNGSTEN, 5.0F, 6.0F);
        ore("ore_lead", Mats.MAT_LEAD, 5.0F, 6.0F);
        ORE_BERYLLIUM = ore("ore_beryllium", Mats.MAT_BERYLLIUM, 5.0F, 9.0F);
        ore("ore_rare", Mats.MAT_RAREEARTH, 5.0F, 6.0F);
        ORE_LIGNITE = ore("ore_lignite", Mats.MAT_LIGNITE, 5.0F, 9.0F);
        oreOutgas("ore_asbestos", Mats.MAT_ASBESTOS, 5.0F, 9.0F, () -> GAS_ASBESTOS.get());
        ore("ore_cinnabar", Mats.MAT_CINNABAR, 5.0F, 6.0F);
        ore("ore_cobalt", Mats.MAT_COBALT, 5.0F, 6.0F);
        ORE_COLTAN = ore("ore_coltan", OreDictManager.COLTAN, 15.0F, 6.0F);
        ORE_URANIUM_SCORCHED =
                oreOutgas(
                        "ore_uranium_scorched",
                        Mats.MAT_URANIUM,
                        5.0F,
                        6.0F,
                        () -> GAS_RADON.get());
        ORE_SCHRABIDIUM = ore("ore_schrabidium", Mats.MAT_SCHRABIDIUM, 15.0F, 360.0F);
    }

    static {
        ORE_NETHER_URANIUM =
                oreOutgas(
                        "ore_nether_uranium", Mats.MAT_URANIUM, 0.4F, 6.0F, () -> GAS_RADON.get());
        ORE_NETHER_URANIUM_SCORCHED =
                oreOutgas(
                        "ore_nether_uranium_scorched",
                        Mats.MAT_URANIUM,
                        0.4F,
                        6.0F,
                        () -> GAS_RADON.get());
        ore("ore_nether_tungsten", Mats.MAT_TUNGSTEN, 0.4F, 6.0F);
        ore("ore_nether_sulfur", Mats.MAT_SULFUR, 0.4F, 6.0F);
        ore("ore_nether_cobalt", Mats.MAT_COBALT, 0.4F, 6.0F);
        ore("ore_nether_plutonium", Mats.MAT_PLUTONIUM, 0.4F, 6.0F);
        ORE_NETHER_SCHRABIDIUM = ore("ore_nether_schrabidium", Mats.MAT_SCHRABIDIUM, 15.0F, 360.0F);
    }

    static {
        ore("ore_gneiss_iron", Mats.MAT_IRON, 1.5F, 6.0F);
        ore("ore_gneiss_gold", Mats.MAT_GOLD, 1.5F, 6.0F);
        ORE_GNEISS_URANIUM =
                oreOutgas(
                        "ore_gneiss_uranium", Mats.MAT_URANIUM, 1.5F, 6.0F, () -> GAS_RADON.get());
        ORE_GNEISS_URANIUM_SCORCHED =
                oreOutgas(
                        "ore_gneiss_uranium_scorched",
                        Mats.MAT_URANIUM,
                        1.5F,
                        6.0F,
                        () -> GAS_RADON.get());
        ore("ore_gneiss_copper", Mats.MAT_COPPER, 1.5F, 6.0F);
        oreOutgas("ore_gneiss_asbestos", Mats.MAT_ASBESTOS, 1.5F, 6.0F, () -> GAS_ASBESTOS.get());
        ore("ore_gneiss_lithium", Mats.MAT_LITHIUM, 1.5F, 6.0F);
        ORE_GNEISS_SCHRABIDIUM = ore("ore_gneiss_schrabidium", Mats.MAT_SCHRABIDIUM, 1.5F, 6.0F);
        ore("ore_gneiss_rare", Mats.MAT_RAREEARTH, 1.5F, 6.0F);
    }

    static {
        basaltOre("ore_basalt_sulfur", Mats.MAT_SULFUR);
        basaltOre("ore_basalt_fluorite", Mats.MAT_FLUORITE);
        ORE_BASALT_ASBESTOS =
                Reg.block(
                        "ore_basalt_asbestos",
                        props -> new BlockOreBasalt(props, () -> GAS_ASBESTOS.get()),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> basaltAsbestosItem =
                Reg.item(
                        "ore_basalt_asbestos",
                        props -> new BlockItem(ORE_BASALT_ASBESTOS.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        ORE_BASALT_ASBESTOS,
                        basaltAsbestosItem,
                        Mats.MAT_ASBESTOS.dict,
                        MaterialShapes.ORE));
        basaltOre("ore_basalt_molysite", Mats.MAT_MOLYSITE);
        ORE_BASALT_GEM =
                Reg.block(
                        "ore_basalt_gem",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("ore_basalt_gem", ORE_BASALT_GEM));
    }

    static {
        oreOutgasDeepslate(
                "ore_uranium_deepslate", Mats.MAT_URANIUM, 5.0F, 6.0F, () -> GAS_RADON.get());
        oreDeepslate("ore_thorium_deepslate", Mats.MAT_THORIUM, 5.0F, 6.0F);
        oreDeepslate("ore_titanium_deepslate", Mats.MAT_TITANIUM, 5.0F, 6.0F);
        oreDeepslate("ore_sulfur_deepslate", Mats.MAT_SULFUR, 5.0F, 6.0F);
        oreDeepslate("ore_aluminium_deepslate", Mats.MAT_ALUMINIUM, 5.0F, 6.0F);
        oreDeepslate("ore_fluorite_deepslate", Mats.MAT_FLUORITE, 5.0F, 6.0F);
        oreDeepslate("ore_niter_deepslate", Mats.MAT_KNO, 5.0F, 6.0F);
        oreDeepslate("ore_tungsten_deepslate", Mats.MAT_TUNGSTEN, 5.0F, 6.0F);
        oreDeepslate("ore_lead_deepslate", Mats.MAT_LEAD, 5.0F, 6.0F);
        oreDeepslate("ore_beryllium_deepslate", Mats.MAT_BERYLLIUM, 5.0F, 9.0F);
        oreDeepslate("ore_rare_deepslate", Mats.MAT_RAREEARTH, 5.0F, 6.0F);
        oreDeepslate("ore_cinnabar_deepslate", Mats.MAT_CINNABAR, 5.0F, 6.0F);
        oreDeepslate("ore_cobalt_deepslate", Mats.MAT_COBALT, 5.0F, 6.0F);
        oreDeepslate("ore_lignite_deepslate", Mats.MAT_LIGNITE, 5.0F, 9.0F);
        oreOutgasDeepslate(
                "ore_asbestos_deepslate", Mats.MAT_ASBESTOS, 5.0F, 9.0F, () -> GAS_ASBESTOS.get());
        oreDeepslate("ore_coltan_deepslate", OreDictManager.COLTAN, 15.0F, 6.0F);
        ORE_GNEISS_GAS = extraOre("ore_gneiss_gas", 1.5F, 6.0F);
        ORE_AUSTRALIUM = extraOre("ore_australium", 5.0F, 6.0F);
        ORE_TIKITE = extraOre("ore_tikite", 5.0F, 6.0F);
        ORE_TEKTITE_OSMIRIDIUM =
                Reg.block(
                        "ore_tektite_osmiridium",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .strength(0.5F, 0.5F)
                                        .sound(SoundType.SAND));
        EXTRA_ORE_BLOCKS.add(decoItem("ore_tektite_osmiridium", ORE_TEKTITE_OSMIRIDIUM));
        ORE_NETHER_FIRE =
                extraOre(
                        "ore_nether_fire",
                        BlockOreNetherFire::new,
                        0.4F,
                        6.0F,
                        UnaryOperator.identity());
        ORE_OIL = extraOre("ore_oil", 5.0F, 6.0F);
        ORE_OIL_EMPTY = extraOre("ore_oil_empty", 5.0F, 6.0F);

        ORE_BEDROCK_OIL =
                extraOre(
                        "ore_bedrock_oil",
                        -1.0F,
                        600000.0F,
                        p -> p.isValidSpawn((s, l, pos, t) -> false));
        STONE_RESOURCE_HEMATITE = resourceStone("stone_resource_hematite");
        STONE_RESOURCE_BAUXITE = resourceStone("stone_resource_bauxite");
        STONE_RESOURCE_MALACHITE =
                resourceStone("stone_resource_malachite", BlockResourceStoneMalachite::new, true);
        STONE_RESOURCE_LIMESTONE = resourceStone("stone_resource_limestone");
        STONE_DEPTH = depthBlock("stone_depth");
        depthBlock("depth_brick");
        depthBlock("depth_tiles");
        STONE_DEPTH_NETHER = depthBlock("stone_depth_nether");
        depthBlock("depth_nether_brick");
        depthBlock("depth_nether_tiles");
        depthBlock("depth_dnt", 36000.0F);
        CLUSTER_DEPTH_IRON = depthBlock("cluster_depth_iron");
        CLUSTER_DEPTH_TITANIUM = depthBlock("cluster_depth_titanium");
        CLUSTER_DEPTH_TUNGSTEN = depthBlock("cluster_depth_tungsten");
        ORE_DEPTH_CINNABAR = depthBlock("ore_depth_cinnabar");
        ORE_DEPTH_ZIRCONIUM = depthBlock("ore_depth_zirconium");
        ORE_DEPTH_BORAX = depthBlock("ore_depth_borax");
        ORE_DEPTH_NETHER_NEODYMIUM = depthBlock("ore_depth_nether_neodymium");
        CLUSTER_IRON = oreCube("cluster_iron", 5.0F, 9.0F);
        CLUSTER_TITANIUM = oreCube("cluster_titanium", 5.0F, 9.0F);
        CLUSTER_ALUMINIUM = oreCube("cluster_aluminium", 5.0F, 9.0F);
        CLUSTER_COPPER = oreCube("cluster_copper", 5.0F, 9.0F);
        ORE_ALEXANDRITE = depthBlock("ore_alexandrite");
        ORE_NETHER_COAL =
                Reg.<Block>block(
                        "ore_nether_coal",
                        BlockNetherCoal::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(0.4F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(state -> 10)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("ore_nether_coal", ORE_NETHER_COAL));
    }

    static {
        EXTRA_ORE_BLOCKS.add(decoItem("ore_sellafield_diamond", ORE_SELLAFIELD_DIAMOND));
        EXTRA_ORE_BLOCKS.add(decoItem("ore_sellafield_emerald", ORE_SELLAFIELD_EMERALD));
        EXTRA_ORE_BLOCKS.add(decoItem("ore_sellafield_radgem", ORE_SELLAFIELD_RADGEM));
        RegistryHandle<Item> schrabItem =
                decoItem("ore_sellafield_schrabidium", ORE_SELLAFIELD_SCHRABIDIUM);
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        ORE_SELLAFIELD_SCHRABIDIUM,
                        schrabItem,
                        Mats.MAT_SCHRABIDIUM.dict,
                        MaterialShapes.ORE));
        RegistryHandle<Item> scorchedItem =
                decoItem("ore_sellafield_uranium_scorched", ORE_SELLAFIELD_URANIUM_SCORCHED);
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        ORE_SELLAFIELD_URANIUM_SCORCHED,
                        scorchedItem,
                        Mats.MAT_URANIUM.dict,
                        MaterialShapes.ORE));
        STONE_RESOURCE_SULFUR = resourceStone("stone_resource_sulfur");
        STONE_RESOURCE_ASBESTOS = resourceStone("stone_resource_asbestos");
    }

    static {
        hazardStorage(
                "block_uranium", Mats.MAT_URANIUM, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_u233",
                Mats.MAT_U233,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_u235",
                Mats.MAT_U235,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage("block_u238", Mats.MAT_U238, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_thorium", Mats.MAT_THORIUM, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_plutonium",
                Mats.MAT_PLUTONIUM,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_pu_mix",
                Mats.MAT_RGP,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_pu238",
                Mats.MAT_PU238,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG,
                5);
        hazardStorage(
                "block_pu239",
                Mats.MAT_PU239,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_pu240",
                Mats.MAT_PU240,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_neptunium",
                Mats.MAT_NEPTUNIUM,
                5.0F,
                36.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_polonium",
                Mats.MAT_POLONIUM,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.RADFOG);
        hazardStorage("block_ra226", Mats.MAT_RADIUM, 5.0F, 6.0F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_actinium", Mats.MAT_ACTINIUM, 5.0F, 6.0F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_schrabidium",
                Mats.MAT_SCHRABIDIUM,
                5.0F,
                360.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.SCHRAB);
        hazardStorage(
                "block_solinium",
                Mats.MAT_SOLINIUM,
                5.0F,
                360.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.SCHRAB);
        hazardStorage(
                "block_schrabidate",
                Mats.MAT_SCHRABIDATE,
                5.0F,
                360.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.SCHRAB);
        hazardStorage(
                "block_schraranium",
                Mats.MAT_SCHRARANIUM,
                5.0F,
                150.0F,
                SoundType.METAL,
                MapColor.METAL,
                BlockHazard.ExtDisplayEffect.SCHRAB);
    }

    static {
        storage("block_titanium", Mats.MAT_TITANIUM, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        BLOCK_LEAD =
                storage("block_lead", Mats.MAT_LEAD, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL)
                        .afterRegistration(RadiationSystemNT::markRadResistant);

        BLOCK_GRAPHITE =
                Reg.block(
                        "block_graphite",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        BLOCK_GRAPHITE,
                        Reg.item(
                                "block_graphite",
                                props -> new BlockItem(BLOCK_GRAPHITE.get(), props),
                                () -> new Item.Properties().useBlockDescriptionPrefix()),
                        Mats.MAT_GRAPHITE.dict,
                        MaterialShapes.BLOCK));
        storage(
                "block_red_copper",
                Mats.MAT_MINGRADE,
                5.0F,
                15.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_tungsten", Mats.MAT_TUNGSTEN, 5.0F, 12.0F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_aluminium",
                Mats.MAT_ALUMINIUM,
                5.0F,
                12.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_steel", Mats.MAT_STEEL, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        storage("block_tcalloy", Mats.MAT_TCALLOY, 5.0F, 42.0F, SoundType.METAL, MapColor.METAL);
        storage("block_cdalloy", Mats.MAT_CDALLOY, 5.0F, 42.0F, SoundType.METAL, MapColor.METAL);
        storage("block_cadmium", Mats.MAT_CADMIUM, 5.0F, 54.0F, SoundType.METAL, MapColor.METAL);
        storage("block_bismuth", Mats.MAT_BISMUTH, 5.0F, 54.0F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_coltan",
                OreDictManager.COLTAN,
                5.0F,
                30.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_niobium", Mats.MAT_NIOBIUM, 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        BLOCK_BERYLLIUM =
                storage(
                        "block_beryllium",
                        Mats.MAT_BERYLLIUM,
                        5.0F,
                        12.0F,
                        SoundType.METAL,
                        MapColor.METAL);
        storage("block_boron", Mats.MAT_BORON, 5.0F, 6.0F, SoundType.METAL, MapColor.METAL)
                .afterRegistration(RadiationSystemNT::markRadResistant);
        storage("block_cobalt", Mats.MAT_COBALT, 5.0F, 30.0F, SoundType.STONE, MapColor.METAL);
        storage(
                "block_zirconium",
                Mats.MAT_ZIRCONIUM,
                5.0F,
                18.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage(
                "block_lithium",
                Mats.MAT_LITHIUM.dict,
                BlockLithium::new,
                5.0F,
                6.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage(
                "block_lanthanium",
                Mats.MAT_LANTHANIUM,
                5.0F,
                6.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_dura_steel", Mats.MAT_DURA, 5.0F, 120.0F, SoundType.METAL, MapColor.METAL);
        storage("block_combine_steel", Mats.MAT_CMB, 5.0F, 360.0F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_magnetized_tungsten",
                Mats.MAT_MAGTUNG,
                5.0F,
                45.0F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_starmetal", Mats.MAT_STAR, 5.0F, 240.0F, SoundType.METAL, MapColor.METAL);
        BLOCK_DESH =
                storage("block_desh", Mats.MAT_DESH, 5.0F, 180.0F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_dineutronium",
                Mats.MAT_DNT,
                5.0F,
                36000.0F,
                SoundType.METAL,
                MapColor.METAL);
    }

    static {
        fuelBlock("block_uranium_fuel", 5.0F, 30.0F, null);
        fuelBlock("block_thorium_fuel", 5.0F, 30.0F, null);
        fuelBlock("block_mox_fuel", 5.0F, 30.0F, BlockHazard.ExtDisplayEffect.RADFOG);
        fuelBlock("block_plutonium_fuel", 5.0F, 30.0F, BlockHazard.ExtDisplayEffect.RADFOG);
        fuelBlock(
                "block_schrabidium_fuel",
                5.0F,
                360.0F,
                BlockHazard.ExtDisplayEffect.SCHRAB,
                Rarity.RARE);
        beaconStorage("block_tantalium", 5.0F, 30.0F, SoundType.METAL, MapColor.METAL);
        fuelBlock("block_trinitite", 5.0F, 6.0F, null, null, SoundType.STONE);
        BLOCK_SCHRABIDIUM_CLUSTER =
                pillarBlock(
                        "block_schrabidium_cluster",
                        5.0F,
                        36000.0F,
                        SoundType.STONE,
                        MapColor.STONE,
                        Rarity.RARE);
        BLOCK_EUPHEMIUM_CLUSTER =
                pillarBlock(
                        "block_euphemium_cluster",
                        5.0F,
                        36000.0F,
                        SoundType.STONE,
                        MapColor.STONE,
                        null);
        BLOCK_INSULATOR =
                Reg.<Block>block(
                        "block_insulator",
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.WOOL)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("block_insulator", BLOCK_INSULATOR));
        fuelBlock(
                "block_white_phosphorus", 5.0F, 6.0F, null, null, SoundType.STONE, MapColor.STONE);
        BLOCK_FALLOUT =
                Reg.block(
                        "block_fallout",
                        BlockHazardFalling::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .strength(0.2F)
                                        .sound(SoundType.GRAVEL));
        DECO_BLOCKS.add(decoItem("block_fallout", BLOCK_FALLOUT));
        BLOCK_COKE_COAL = cokeBlock("block_coke_coal", OreDictManager.COALCOKE);
        BLOCK_COKE_LIGNITE = cokeBlock("block_coke_lignite", OreDictManager.LIGCOKE);
        BLOCK_COKE_PETROLEUM = cokeBlock("block_coke_petroleum", OreDictManager.PETCOKE);
        pillarBlockNoTool(
                "block_tritium",
                3.0F,
                1.2F,
                SoundType.GLASS,
                MapColor.NONE,
                NoteBlockInstrument.HAT);
        BLOCK_SEMTEX = plasticExplosive("block_semtex");
        BLOCK_C4 = plasticExplosive("block_c4");
        beaconStorage("block_australium", 5.0F, 6.0F, SoundType.STONE, MapColor.METAL)
                .afterRegistration(RadiationSystemNT::markRadResistant);
        capBlock("block_cap_nuka");
        capBlock("block_cap_quantum");
        capBlock("block_cap_sparkle");
        capBlock("block_cap_rad");
        capBlock("block_cap_korl");
        capBlock("block_cap_fritz");
    }

    static {
        storage("block_polymer", Mats.MAT_POLYMER, 3.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        storage("block_bakelite", Mats.MAT_BAKELITE, 3.0F, 3.0F, SoundType.STONE, MapColor.METAL);
        storage("block_rubber", Mats.MAT_RUBBER, 3.0F, 9.0F, SoundType.STONE, MapColor.METAL);
        BLOCK_FIBERGLASS =
                Reg.block(
                        "block_fiberglass",
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOL)
                                        .strength(5.0F, 9.0F)
                                        .sound(SoundType.WOOL)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("block_fiberglass", BLOCK_FIBERGLASS));
        BLOCK_ASBESTOS =
                storageOutgas(
                        "block_asbestos",
                        Mats.MAT_ASBESTOS,
                        5.0F,
                        9.0F,
                        SoundType.WOOL,
                        MapColor.WOOL,
                        () -> GAS_ASBESTOS.get());
        storage("block_sulfur", Mats.MAT_SULFUR, 5.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        storage("block_niter", Mats.MAT_KNO, 5.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        storage("block_fluorite", Mats.MAT_FLUORITE, 5.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        RegistryHandle<BlockHazardFalling> redPhosphorus =
                Reg.block(
                        "block_red_phosphorus",
                        BlockHazardFalling::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.SAND));
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        redPhosphorus,
                        Reg.item(
                                "block_red_phosphorus",
                                props -> new BlockItem(redPhosphorus.get(), props),
                                () -> new Item.Properties().useBlockDescriptionPrefix()),
                        Mats.MAT_PHOSPHORUS.dict,
                        MaterialShapes.BLOCK));
        REBAR =
                Reg.block(
                                "rebar",
                                BlockRebar::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.METAL)
                                                .strength(15.0F, 12.0F)
                                                .noOcclusion()
                                                .isSuffocating((state, level, pos) -> false)
                                                .isViewBlocking((state, level, pos) -> false)
                                                .isValidSpawn((state, level, pos, type) -> false)
                                                .requiresCorrectToolForDrops())
                        .bakedBy(() -> new RebarModel.Family());
        DECO_BLOCKS.add(decoItem("rebar", REBAR));
        REINFORCED_STONE = decoBlast("reinforced_stone", 15.0F, 60.0F);
        REINFORCED_STONE_SLAB = decoSlab("reinforced_stone_slab", 15.0F, 60.0F);
        REINFORCED_STONE_STAIRS =
                decoStairs("reinforced_stone_stairs", REINFORCED_STONE, 15.0F, 60.0F);
    }

    static {
        CRYSTAL_HARDENED =
                decoUnlisted("crystal_hardened", 15.0F, Float.POSITIVE_INFINITY, SoundType.STONE);
        CRYSTAL_VIRUS =
                Reg.block(
                        "crystal_virus",
                        CrystalVirus::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, Float.POSITIVE_INFINITY)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        decoItem("crystal_virus", CRYSTAL_VIRUS);
        CRYSTAL_PULSAR =
                Reg.block(
                        "crystal_pulsar",
                        CrystalPulsar::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, Float.POSITIVE_INFINITY)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        decoItem("crystal_pulsar", CRYSTAL_PULSAR);
        RegistryHandle<Block> brickConcrete =
                decoNoSpawnBlast("brick_concrete", 15.0F, 96.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        RegistryHandle<Block> brickConcreteMossy =
                decoBlast("brick_concrete_mossy", 15.0F, 96.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        RegistryHandle<Block> brickConcreteCracked =
                decoBlast("brick_concrete_cracked", 15.0F, 36.0F);
        RegistryHandle<Block> brickConcreteBroken =
                decoBlast("brick_concrete_broken", 15.0F, 27.0F);

        BRICK_CONCRETE_MARKED =
                Reg.<Block>block(
                        "brick_concrete_marked",
                        BlockWriting::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 96.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(
                Reg.item(
                        "brick_concrete_marked",
                        props -> new ItemBlockBlastInfo(BRICK_CONCRETE_MARKED.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix()));
        RegistryHandle<Block> brickCompound =
                decoBlast("brick_compound", 15.0F, 240.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        RegistryHandle<Block> reinforcedBrick =
                decoBlast("reinforced_brick", 15.0F, 180.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        RegistryHandle<Block> brickLight = decoBlast("brick_light", 5.0F, 12.0F);
        decoBlast("reinforced_sand", 15.0F, 24.0F);
        RegistryHandle<Block> brickObsidian = decoBlast("brick_obsidian", 15.0F, 72.0F);
        RegistryHandle<Block> brickAsbestos =
                Reg.<Block>block(
                        "brick_asbestos",
                        props -> new BlockOutgas(props, () -> ModBlocks.GAS_ASBESTOS.get(), true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 600.0F)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_asbestos", brickAsbestos));
        RegistryHandle<Block> brickFire = decoBlast("brick_fire", 5.0F, 21.0F);
        decoBlast("cmb_brick", 25.0F, 3000.0F);
        Reg.Handle<Block> concrete = decoNoSpawnBlast("concrete", 15.0F, 84.0F);
        Reg.Handle<Block> concreteSmooth = decoNoSpawnBlast("concrete_smooth", 15.0F, 84.0F);
        CONCRETE = concrete;
        CONCRETE_SMOOTH = concreteSmooth;
        RegistryHandle<Block> concreteAsbestos =
                CONCRETE_ASBESTOS = decoNoSpawnBlast("concrete_asbestos", 15.0F, 90.0F);
        CONCRETE_REBAR = decoNoSpawnBlast("concrete_rebar", 50.0F, 144.0F);

        Reg.Handle<Block> ducreteSmooth =
                decoNoSpawnBlast("ducrete_smooth", 20.0F, 300.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        Reg.Handle<Block> ducrete =
                decoNoSpawnBlast("ducrete", 20.0F, 300.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        RegistryHandle<Block> brickDucrete =
                decoNoSpawnBlast("brick_ducrete", 15.0F, 450.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        REINFORCED_DUCRETE =
                decoNoSpawnBlast("reinforced_ducrete", 20.0F, 600.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);

        concreteColored("concrete_white");
        concreteColored("concrete_orange");
        concreteColored("concrete_magenta");
        concreteColored("concrete_light_blue");
        concreteColored("concrete_yellow");
        CONCRETE_LIME = concreteColored("concrete_lime");
        concreteColored("concrete_pink");
        concreteColored("concrete_gray");
        concreteColored("concrete_silver");
        concreteColored("concrete_cyan");
        concreteColored("concrete_purple");
        concreteColored("concrete_blue");
        concreteColored("concrete_brown");
        concreteColored("concrete_green");
        CONCRETE_RED = concreteColored("concrete_red");
        concreteColored("concrete_black");
        CONCRETE_PILLAR =
                Reg.block(
                                "concrete_pillar",
                                RotatedPillarBlock::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .strength(15.0F, 108.0F)
                                                .sound(SoundType.STONE)
                                                .requiresCorrectToolForDrops())
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        DECO_BLOCKS.add(blastItem("concrete_pillar", CONCRETE_PILLAR));

        CONCRETE_SUPER =
                Reg.block(
                        "concrete_super",
                        BlockUberConcrete::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(150.0F, 600.0F)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem("concrete_super", CONCRETE_SUPER));
        CONCRETE_SUPER_BROKEN =
                fallingCubeBlast(
                        "concrete_super_broken", MapColor.STONE, SoundType.STONE, 10.0F, 12.0F);
        CONCRETE_EXT_MACHINE = concreteExt("concrete_ext_machine");
        CONCRETE_EXT_INDIGO = concreteExt("concrete_ext_indigo");
        CONCRETE_EXT_PURPLE = concreteExt("concrete_ext_purple");
        CONCRETE_EXT_PINK = concreteExt("concrete_ext_pink");
        CONCRETE_EXT_HAZARD = concreteExt("concrete_ext_hazard");
        CONCRETE_EXT_SAND = concreteExt("concrete_ext_sand");
        CONCRETE_EXT_BRONZE = concreteExt("concrete_ext_bronze");

        CONCRETE_EXT_MACHINE_STRIPE = decoNoSpawnBlast("concrete_ext_machine_stripe", 15.0F, 84.0F);
        LIGHTSTONE_UNREFINED = lightstone("lightstone_unrefined");
        LIGHTSTONE_TILE = lightstone("lightstone_tile");
        LIGHTSTONE_BRICKS = lightstone("lightstone_bricks");
        LIGHTSTONE_BRICKS_CHISELED = deco("lightstone_bricks_chiseled", 2.0F, 9.0F);
        LIGHTSTONE_CHISELED = deco("lightstone_chiseled", 2.0F, 9.0F);
        LIGHTSTONE_TILE_STAIRS = decoStairs("lightstone_tile_stairs", LIGHTSTONE_TILE, 2.0F, 9.0F);
        LIGHTSTONE_BRICKS_STAIRS =
                decoStairs("lightstone_bricks_stairs", LIGHTSTONE_BRICKS, 2.0F, 9.0F);

        decoSlab("lightstone_tile_slab", 2.0F, 9.0F);
        decoSlab("lightstone_bricks_slab", 2.0F, 9.0F);

        BRICK_FORGOTTEN =
                Reg.block(
                        "brick_forgotten",
                        BlockForgottenBrick::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(-1.0F, 399999.6F)
                                        .sound(SoundType.STONE)
                                        .noLootTable());
        decoItem("brick_forgotten", BRICK_FORGOTTEN);
        BRICK_FORGOTTEN_LOCK =
                Reg.block(
                        "brick_forgotten_lock",
                        BlockForgottenLock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(-1.0F, 399999.6F)
                                        .sound(SoundType.STONE)
                                        .noLootTable());
        decoItem("brick_forgotten_lock", BRICK_FORGOTTEN_LOCK);

        TILE_LAB = tileLab("tile_lab", false);
        TILE_LAB_CRACKED = tileLab("tile_lab_cracked", false);
        TILE_LAB_BROKEN = tileLab("tile_lab_broken", true);
        GRAVEL_OBSIDIAN =
                fallingCubeBlast("gravel_obsidian", MapColor.METAL, SoundType.GRAVEL, 5.0F, 144.0F);
        GRAVEL_DIAMOND =
                Reg.block(
                        "gravel_diamond",
                        BlockFallingBase::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.GRAVEL)
                                        .strength(0.6F));
        DECO_BLOCKS.add(
                Reg.item(
                        "gravel_diamond",
                        props -> new ItemBlockDiamondGravel(GRAVEL_DIAMOND.get(), props),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .rarity(Rarity.RARE)));

        STONE_POROUS =
                Reg.block(
                        "stone_porous",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(1.5F, 18.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("stone_porous", STONE_POROUS));
        STONE_GNEISS = deco("stone_gneiss", 1.5F, 6.0F);
        deco("gneiss_brick", 1.5F, 6.0F);
        deco("gneiss_tile", 1.5F, 6.0F);
        deco("gneiss_chiseled", 1.5F, 6.0F);
        METEOR_POLISHED = meteorDeco("meteor_polished");
        METEOR_BRICK = meteorDeco("meteor_brick");
        METEOR_BRICK_MOSSY = meteorDeco("meteor_brick_mossy");
        METEOR_BRICK_CRACKED = meteorDeco("meteor_brick_cracked");
        meteorDeco("meteor_brick_chiseled");

        METEOR_SPAWNER =
                Reg.block(
                        "meteor_spawner",
                        BlockCybercrab::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(15.0F, 216.0F)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "meteor_spawner",
                props -> new BlockItem(METEOR_SPAWNER.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        TESLA =
                Reg.block(
                        "tesla",
                        MachineTesla::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "tesla",
                props -> new BlockItem(TESLA.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE =
                Reg.block(
                        "crate",
                        BlockLootCrate::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        Reg.item(
                "crate",
                props -> new BlockItem(CRATE.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATE_RED =
                Reg.block(
                        "crate_red",
                        BlockLootCrate::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_red",
                props -> new BlockItem(CRATE_RED.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_WEAPON =
                Reg.block(
                        "crate_weapon",
                        BlockLootCrate::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        Reg.item(
                "crate_weapon",
                props -> new BlockItem(CRATE_WEAPON.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATE_LEAD =
                Reg.block(
                        "crate_lead",
                        BlockLootCrate::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_lead",
                props -> new BlockItem(CRATE_LEAD.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATE_METAL =
                Reg.block(
                        "crate_metal",
                        BlockLootCrate::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_metal",
                props -> new BlockItem(CRATE_METAL.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATE_AMMO =
                Reg.block(
                        "crate_ammo",
                        BlockCrateAmmo::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(1.0F, 1.5F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_ammo",
                props -> new BlockItem(CRATE_AMMO.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATE_CAN =
                Reg.block(
                        "crate_can",
                        BlockCrateCan::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(1.0F, 1.5F)
                                        .sound(SoundType.WOOD)
                                        .noOcclusion()
                                        .ignitedByLava());
        Reg.item(
                "crate_can",
                props -> new BlockItem(CRATE_CAN.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATE_SUPPLY =
                Reg.block(
                        "crate_supply",
                        BlockCrateSupply::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(1.0F, 1.5F)
                                        .sound(SoundType.WOOD)
                                        .noOcclusion()
                                        .ignitedByLava());
        Reg.item(
                "crate_supply",
                props -> new ContainerBlockItem(CRATE_SUPPLY.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        BLOCK_SCRAP = fallingCube("block_scrap", MapColor.SAND, SoundType.GRAVEL, 2.5F, 3.0F);
        BLOCK_ELECTRICAL_SCRAP =
                fallingCube(
                        "block_electrical_scrap",
                        MapColor.METAL,
                        SoundType.METAL,
                        2.5F,
                        3.0F,
                        BlockBehaviour.Properties::requiresCorrectToolForDrops);

        BLOCK_SLAG =
                Reg.block(
                        "block_slag",
                        BlockSlag::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .sound(SoundType.STONE)
                                        .strength(2.0F)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("block_slag", BLOCK_SLAG));
        SANDBAGS =
                Reg.block(
                        "sandbags",
                        BlockSandbags::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(5.0F, 18.0F)
                                        .sound(SoundType.STONE)
                                        .noOcclusion());
        DECO_BLOCKS.add(decoItem("sandbags", SANDBAGS));
        ASPHALT =
                Reg.<Block>block(
                        "asphalt",
                        props -> new BlockSpeedy(1.5D, props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .sound(SoundType.STONE)
                                        .strength(15.0F, 72.0F)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem("asphalt", ASPHALT));
        ASPHALT_LIGHT =
                Reg.<Block>block(
                        "asphalt_light",
                        props -> new BlockSpeedy(1.5D, props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .sound(SoundType.STONE)
                                        .strength(15.0F, 72.0F)
                                        .lightLevel(s -> 15)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem("asphalt_light", ASPHALT_LIGHT));
        BOXCAR =
                Reg.block(
                        "boxcar",
                        BlockBoxcar::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .sound(SoundType.METAL)
                                        .strength(10.0F, 6.0F)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());
        DECO_BLOCKS.add(decoItem("boxcar", BOXCAR));
        BOAT =
                Reg.block(
                        "boat",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .sound(SoundType.METAL)
                                        .strength(10.0F, 6.0F)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("boat", BOAT));
        BLOCK_FOAM =
                Reg.block(
                        "block_foam",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SNOW)
                                        .sound(SoundType.SNOW)
                                        .strength(0.5F, 0.0F));
        DECO_BLOCKS.add(decoItem("block_foam", BLOCK_FOAM));
        FOAM_LAYER =
                Reg.block(
                        "foam_layer",
                        props -> new BlockLayering(props, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SNOW)
                                        .sound(SoundType.SNOW)
                                        .strength(0.1F)
                                        .forceSolidOff()
                                        .noLootTable()
                                        .pushReaction(PushReaction.DESTROY)
                                        .replaceable());
        DECO_BLOCKS.add(decoItem("foam_layer", FOAM_LAYER));

        SAND_BORON = sandMix("sand_boron", 0xC9C6A2);
        SAND_LEAD = sandMix("sand_lead", 0x858591);
        SAND_URANIUM = sandMix("sand_uranium", 0xABA792);
        SAND_POLONIUM = sandMix("sand_polonium", 0x9C8471);
        SAND_QUARTZ = sandMix("sand_quartz", 0xCACACA);
        SAND_BORON_LAYER =
                Reg.block(
                        "sand_boron_layer",
                        props -> new BlockLayering(props, false),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.SAND)
                                        .strength(0.1F)
                                        .forceSolidOff()
                                        .noLootTable());
        DECO_BLOCKS.add(decoItem("sand_boron_layer", SAND_BORON_LAYER));
        FILING_CABINET_GREEN = filingCabinet("filing_cabinet_green", "block/filing_cabinet");
        FILING_CABINET_STEEL = filingCabinet("filing_cabinet_steel", "block/filing_cabinet_steel");

        NUKE_CUSTOM =
                Reg.blockItem(
                        "nuke_custom",
                        NukeCustom::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());

        NUKE_FSTBMB =
                Reg.blockItem(
                        "nuke_fstbmb",
                        NukeBalefire::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());

        BOMB_MULTI =
                Reg.blockItem(
                                "bomb_multi",
                                BombMulti::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .strength(0.0F, 120.0F)
                                                .noOcclusion()
                                                .requiresCorrectToolForDrops())
                        .bakedBy(
                                () ->
                                        new BombMultiModel(
                                                Library.id("models/blocks/bomb_generic.obj")));
        FIREWORKS =
                Reg.block(
                        "fireworks",
                        BlockFireworks::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F, 3.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "fireworks",
                props -> new BlockItem(FIREWORKS.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        DYNAMITE =
                Reg.block(
                        "dynamite",
                        BlockDynamite::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava());
        Reg.item(
                "dynamite",
                props -> new BlockItem(DYNAMITE.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        TNT_NTM =
                Reg.block(
                        "tnt",
                        BlockTNT::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava());
        Reg.item(
                "tnt",
                props -> new BlockItem(TNT_NTM.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        SEMTEX =
                Reg.block(
                        "semtex",
                        BlockSemtex::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava());
        Reg.item(
                "semtex",
                props -> new BlockItem(SEMTEX.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        C4 =
                Reg.block(
                        "c4",
                        BlockC4::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava());
        Reg.item(
                "c4",
                props -> new BlockItem(C4.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        FISSURE_BOMB =
                Reg.block(
                        "fissure_bomb",
                        BlockFissureBomb::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava());
        Reg.item(
                "fissure_bomb",
                props -> new BlockItem(FISSURE_BOMB.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        DET_CHARGE =
                Reg.block(
                        "det_charge",
                        BlockDetCharge::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.1F, 0.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "det_charge",
                props -> new BlockItem(DET_CHARGE.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        DET_CORD =
                Reg.<BlockDetCord>block(
                                "det_cord",
                                BlockDetCord::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .strength(0.1F, 0.0F)
                                                .sound(SoundType.STONE)
                                                .noOcclusion()
                                                .requiresCorrectToolForDrops())
                        .bakedBy(() -> new DetCordModel.Family());
        Reg.item(
                "det_cord",
                props -> new BlockItem(DET_CORD.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        DET_NUKE =
                Reg.<BlockDetNuke>block(
                        "det_nuke",
                        BlockDetNuke::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.1F, 0.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "det_nuke",
                props -> new BlockItem(DET_NUKE.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        DET_MINER =
                Reg.<BlockDetMiner>block(
                        "det_miner",
                        BlockDetMiner::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.1F, 0.0F)
                                        .sound(SoundType.STONE)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "det_miner",
                props -> new BlockItem(DET_MINER.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        FLAME_WAR =
                Reg.block(
                        "flame_war",
                        BombFlameWar::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "flame_war",
                props -> new BlockItem(FLAME_WAR.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        EMP_BOMB =
                Reg.block(
                        "emp_bomb",
                        BlockEMPBomb::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "emp_bomb",
                props -> new BlockItem(EMP_BOMB.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        FLOAT_BOMB =
                Reg.block(
                        "float_bomb",
                        BlockFloatBomb::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "float_bomb",
                props -> new BlockItem(FLOAT_BOMB.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        THERM_ENDO =
                Reg.block(
                        "therm_endo",
                        BlockThermoBomb.Endothermic::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "therm_endo",
                props -> new BlockItem(THERM_ENDO.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        THERM_EXO =
                Reg.block(
                        "therm_exo",
                        BlockThermoBomb.Exothermic::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "therm_exo",
                props -> new BlockItem(THERM_EXO.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        TAINT =
                Reg.block(
                        "taint",
                        BlockTaint::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 6.0F)
                                        .randomTicks()
                                        .sound(SoundType.STONE)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops());

        BALEFIRE =
                Reg.block(
                        "balefire",
                        BlockBalefire::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .replaceable()
                                        .noCollision()
                                        .instabreak()
                                        .lightLevel(s -> 15)
                                        .pushReaction(PushReaction.DESTROY)
                                        .noLootTable());
        Reg.item(
                "balefire",
                props -> new BlockItem(BALEFIRE.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        METEOR_PILLAR =
                Reg.block(
                        "meteor_pillar",
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("meteor_pillar", METEOR_PILLAR));

        NTM_DIRT =
                Reg.block(
                        "dirt",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F)
                                        .sound(SoundType.GRAVEL));
        decoItem("dirt", NTM_DIRT);
        METEOR_BATTERY = meteorDeco("meteor_battery");
        Reg.Handle<ColoredFallingBlock> moonTurf =
                Reg.block(
                        "moon_turf",
                        props -> new ColoredFallingBlock(new ColorRGBA(0xA1A1A1), props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.SAND)
                                        .strength(0.5F));
        DECO_BLOCKS.add(decoItem("moon_turf", moonTurf));
        BRICK_JUNGLE = deco("brick_jungle", 15.0F, 216.0F);
        BRICK_JUNGLE_CRACKED = deco("brick_jungle_cracked", 15.0F, 216.0F);
        RegistryHandle<BlockBallsSpawner> jungleCircle =
                Reg.block(
                        "brick_jungle_circle",
                        BlockBallsSpawner::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_circle", jungleCircle));
        BRICK_JUNGLE_LAVA =
                Reg.block(
                        "brick_jungle_lava",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(s -> 5)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_lava", BRICK_JUNGLE_LAVA));
        BRICK_JUNGLE_OOZE =
                Reg.block(
                        "brick_jungle_ooze",
                        BlockJungleOoze::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(s -> 5)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_ooze", BRICK_JUNGLE_OOZE));
        BRICK_JUNGLE_MYSTIC =
                Reg.block(
                        "brick_jungle_mystic",
                        BlockJungleMystic::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(s -> 5)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_mystic", BRICK_JUNGLE_MYSTIC));

        BRICK_JUNGLE_FRAGILE =
                Reg.block(
                        "brick_jungle_fragile",
                        FragileBrick::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "brick_jungle_fragile",
                props -> new BlockItem(BRICK_JUNGLE_FRAGILE.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        for (int i = 0; i < 16; i++) {
            deco("brick_jungle_glyph_" + i, 15.0F, 216.0F);
        }

        CRATE_JUNGLE = decoUnlisted("crate_jungle", 1.0F, 1.5F, SoundType.STONE);
        for (TrappedBrick.Trap trap : TrappedBrick.Trap.values()) {
            String name = "brick_jungle_trap_" + trap.name().toLowerCase();
            RegistryHandle<TrappedBrick> block =
                    Reg.block(
                            name,
                            props ->
                                    trap.type == TrappedBrick.TrapType.DETECTOR
                                            ? new TrappedBrick.Detector(props, trap)
                                            : new TrappedBrick(props, trap),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(15.0F, 216.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops());
            DECO_BLOCKS.add(decoItem(name, block));
            JUNGLE_TRAPS.add(block);
        }

        for (BlockCrashedBomb.EnumDudType dudType : BlockCrashedBomb.EnumDudType.values()) {
            String skin = dudType.name().toLowerCase();
            String name = "crashed_" + skin;
            RegistryHandle<BlockCrashedBomb> block =
                    Reg.block(
                            name,
                            props -> new BlockCrashedBomb(props, dudType),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .strength(-1.0F, 3600.0F)
                                            .noOcclusion()
                                            .noLootTable());
            decoItem(name, block);
            CRASHED_BOMBS.add(block);
        }

        LOGIC_BLOCK =
                Reg.block(
                        "logic_block",
                        LogicBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        decoItem("logic_block", LOGIC_BLOCK);
        LOGIC_BLOCK_INVIS =
                Reg.block(
                        "logic_block_invis",
                        LogicBlockInvis::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F)
                                        .sound(SoundType.STONE)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());
        decoItem("logic_block_invis", LOGIC_BLOCK_INVIS);
        TEKTITE =
                Reg.block(
                        "tektite",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .strength(0.5F, 0.5F)
                                        .sound(SoundType.SAND));
        DECO_BLOCKS.add(decoItem("tektite", TEKTITE));
        deco("deco_titanium", 5.0F, 6.0F, MapColor.METAL);
        decoCt("deco_red_copper", 5.0F, 6.0F);
        decoCt("deco_tungsten", 5.0F, 6.0F);
        decoCt("deco_aluminium", 5.0F, 6.0F);
        DECO_STEEL = decoCt("deco_steel", 5.0F, 6.0F);
        DECO_RUSTY_STEEL = decoCt("deco_rusty_steel", 5.0F, 6.0F);
        decoCt("deco_lead", 5.0F, 6.0F);
        decoCt("deco_beryllium", 5.0F, 6.0F);
        DECO_ASBESTOS =
                Reg.<Block>block(
                        "deco_asbestos",
                        props -> new BlockOutgas(props, () -> ModBlocks.GAS_ASBESTOS.get(), true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOL)
                                        .strength(5.0F, 6.0F)
                                        .randomTicks()
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("deco_asbestos", DECO_ASBESTOS));
        PLATEMETAL =
                Reg.block(
                        "platemetal",
                        BlockPlatemetal::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(ModSoundTypes.PLATEMETAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                        "platemetal",
                        props -> new BlockPlatemetal.PlatemetalItem(PLATEMETAL.get(), props),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .component(
                                                DataComponents.BLOCK_STATE,
                                                BlockPlatemetal.stateFor(
                                                        BlockPlatemetal.Variant.BASE)))
                .state(() -> DataComponents.BLOCK_STATE);
        deco("block_smore", 15.0F, 360.0F);
        deco("basalt", 5.0F, 6.0F);
        deco("basalt_smooth", 5.0F, 6.0F);

        deco("deco_rbmk", 5.0F, 60.0F);
        deco("deco_rbmk_smooth", 5.0F, 60.0F);
        deco("basalt_brick", 5.0F, 6.0F);
        deco("basalt_polished", 5.0F, 6.0F);
        deco("basalt_tiles", 5.0F, 6.0F);

        PINK_LOG =
                Reg.<Block>block(
                        "pink_log",
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
                                        .strength(0.5F, 2.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        decoItem("pink_log", PINK_LOG);

        RegistryHandle<Block> pinkPlanks =
                Reg.block(
                        "pink_planks",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F, 0.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        decoItem("pink_planks", pinkPlanks);
        BLOCK_METEOR = meteorDeco("block_meteor");
        BLOCK_METEOR_COBBLE = meteorDeco("block_meteor_cobble");
        BLOCK_METEOR_BROKEN = meteorDeco("block_meteor_broken");

        DECO_BLOCKS.add(decoItem("block_meteor_molten", BLOCK_METEOR_MOLTEN));
        BLOCK_METEOR_TREASURE = meteorDeco("block_meteor_treasure");
        WASTE_PLANKS =
                Reg.block(
                        "waste_planks",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("waste_planks", WASTE_PLANKS));

        FROZEN_DIRT =
                Reg.block(
                        "frozen_dirt",
                        BlockFrozenEarth::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS));
        DECO_BLOCKS.add(decoItem("frozen_dirt", FROZEN_DIRT));
        FROZEN_GRASS =
                Reg.block(
                        "frozen_grass",
                        BlockFrozenEarth::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS));
        DECO_BLOCKS.add(decoItem("frozen_grass", FROZEN_GRASS));

        FROZEN_LOG =
                Reg.block(
                        "frozen_log",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("frozen_log", FROZEN_LOG));
        FROZEN_PLANKS =
                Reg.block(
                        "frozen_planks",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("frozen_planks", FROZEN_PLANKS));
        WASTE_EARTH =
                Reg.block(
                        "waste_earth",
                        WasteEarth::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.6F, 0.6F)
                                        .sound(SoundType.GRASS)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("waste_earth", WASTE_EARTH));
        WASTE_MYCELIUM =
                Reg.block(
                        "waste_mycelium",
                        BlockWasteMycelium::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.6F)
                                        .sound(SoundType.GRASS)
                                        .lightLevel(s -> 15)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("waste_mycelium", WASTE_MYCELIUM));

        BURNING_EARTH =
                Reg.block(
                        "burning_earth",
                        BlockBurningEarth::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.6F)
                                        .sound(SoundType.GRASS)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("burning_earth", BURNING_EARTH));

        IMPACT_DIRT =
                Reg.block(
                        "impact_dirt",
                        BlockImpactDirt::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.5F)
                                        .sound(SoundType.GRAVEL)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("impact_dirt", IMPACT_DIRT));

        MUSH =
                Reg.block(
                        "mush",
                        BlockMush::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .instabreak()
                                        .noCollision()
                                        .sound(SoundType.GRASS)
                                        .lightLevel(s -> 7)
                                        .randomTicks()
                                        .pushReaction(PushReaction.DESTROY));
        DECO_BLOCKS.add(
                Reg.<Item>item(
                        "mush",
                        props -> new BlockItem(MUSH.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix()));
        WASTE_LEAVES =
                Reg.<Block>block(
                        "waste_leaves",
                        WasteLeaves::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .strength(0.1F, 0.1F)
                                        .sound(SoundType.GRASS)
                                        .noOcclusion()
                                        .isRedstoneConductor((state, level, pos) -> false)
                                        .isSuffocating((state, level, pos) -> false)
                                        .isViewBlocking((state, level, pos) -> false)
                                        .isValidSpawn(
                                                (state, level, pos, type) ->
                                                        type == EntityTypes.OCELOT
                                                                || type == EntityTypes.PARROT)
                                        .ignitedByLava()
                                        .pushReaction(PushReaction.DESTROY)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("waste_leaves", WASTE_LEAVES));
        FALLOUT = Reg.block("fallout", BlockFallout::new, BlockFallout::defaultProperties);
        DECO_BLOCKS.add(decoItem("fallout", FALLOUT));
        BLOCK_WASTE =
                Reg.block("block_waste", BlockNuclearWaste::new, ModBlocks::wasteBlockProperties);
        DECO_BLOCKS.add(decoItem("block_waste", BLOCK_WASTE));
        BLOCK_WASTE_PAINTED =
                Reg.block(
                        "block_waste_painted",
                        BlockNuclearWaste::new,
                        ModBlocks::wasteBlockProperties);
        DECO_BLOCKS.add(decoItem("block_waste_painted", BLOCK_WASTE_PAINTED));
        BLOCK_WASTE_VITRIFIED =
                Reg.block(
                        "block_waste_vitrified",
                        BlockNuclearWaste::new,
                        ModBlocks::wasteBlockProperties);
        DECO_BLOCKS.add(decoItem("block_waste_vitrified", BLOCK_WASTE_VITRIFIED));

        GAS_MELTDOWN =
                Reg.block("gas_meltdown", BlockGasMeltdown::new, BlockGasBase::defaultProperties);
        decoItem("gas_meltdown", GAS_MELTDOWN);
        BROADCASTER_PC =
                Reg.blockItem(
                        "broadcaster_pc",
                        PinkCloudBroadcaster::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 9.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());
        SEAL_CONTROLLER =
                Reg.blockItem(
                        "seal_controller",
                        BlockSeal::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(10.0F, 60.0F)
                                        .requiresCorrectToolForDrops());

        SEAL_HATCH =
                Reg.blockItem(
                        "seal_hatch",
                        BlockSealHatch::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        .requiresCorrectToolForDrops()
                                        .pushReaction(PushReaction.BLOCK));
        VENT_CHLORINE = vent("vent_chlorine", BlockVent.CHLORINE, 1.5D);
        VENT_CLOUD = vent("vent_cloud", BlockVent.CLOUD, 1.75D);
        VENT_PINK_CLOUD = vent("vent_pink_cloud", BlockVent.PINK_CLOUD, 2.0D);
        VENT_CHLORINE_SEAL =
                Reg.blockItem(
                        "vent_chlorine_seal",
                        BlockChlorineSeal::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .requiresCorrectToolForDrops());
        CHLORINE_GAS =
                Reg.block("chlorine_gas", BlockGasChlorine::new, BlockGasBase::defaultProperties);
        decoItem("chlorine_gas", CHLORINE_GAS);

        FIELD_DISTURBER =
                Reg.block(
                        "field_disturber",
                        MachineFieldDisturber::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        decoItem("field_disturber", FIELD_DISTURBER);
        GAS_RADON = Reg.block("gas_radon", BlockGasRadon::new, BlockGasBase::defaultProperties);
        decoItem("gas_radon", GAS_RADON);
        GAS_RADON_DENSE =
                Reg.block(
                        "gas_radon_dense",
                        BlockGasRadonDense::new,
                        BlockGasBase::defaultProperties);
        decoItem("gas_radon_dense", GAS_RADON_DENSE);
        GAS_RADON_TOMB =
                Reg.block(
                        "gas_radon_tomb", BlockGasRadonTomb::new, BlockGasBase::defaultProperties);
        decoItem("gas_radon_tomb", GAS_RADON_TOMB);

        GAS_ASBESTOS =
                Reg.block("gas_asbestos", BlockGasAsbestos::new, BlockGasBase::defaultProperties);
        decoItem("gas_asbestos", GAS_ASBESTOS);

        GAS_FLAMMABLE =
                Reg.block("gas_flammable", BlockGasFlammable::new, BlockGasBase::defaultProperties);
        decoItem("gas_flammable", GAS_FLAMMABLE);
        GAS_EXPLOSIVE =
                Reg.block("gas_explosive", BlockGasExplosive::new, BlockGasBase::defaultProperties);
        decoItem("gas_explosive", GAS_EXPLOSIVE);
        GAS_MONOXIDE =
                Reg.block("gas_monoxide", BlockGasMonoxide::new, BlockGasBase::defaultProperties);
        decoItem("gas_monoxide", GAS_MONOXIDE);

        GAS_COAL = Reg.block("gas_coal", BlockGasCoal::new, BlockGasBase::defaultProperties);
        decoItem("gas_coal", GAS_COAL);
        SELLAFIELD_SLAKED =
                Reg.block(
                        "sellafield_slaked",
                        BlockSellafieldSlaked::new,
                        BlockSellafieldSlaked::defaultProperties);
        DECO_BLOCKS.add(decoItem("sellafield_slaked", SELLAFIELD_SLAKED));
        SELLAFIELD_BEDROCK =
                Reg.block(
                        "sellafield_bedrock",
                        BlockSellafieldSlaked::new,
                        BlockSellafieldSlaked::sellafieldBedrockProperties);
        DECO_BLOCKS.add(decoItem("sellafield_bedrock", SELLAFIELD_BEDROCK));
        SELLAFIELD =
                Reg.block("sellafield", BlockSellafield::new, BlockSellafield::defaultProperties);

        Reg.item(
                        "sellafield",
                        props -> new BlockSellafield.SellafieldBlockItem(SELLAFIELD.get(), props),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .component(DataComponents.BLOCK_STATE, levelState(0)))
                .state(() -> DataComponents.BLOCK_STATE);
        WASTE_LOG =
                Reg.block(
                        "waste_log",
                        props -> new BlockWasteLog(props, () -> ModItems.BURNT_BARK.get()),
                        BlockWasteLog::defaultProperties);
        DECO_BLOCKS.add(decoItem("waste_log", WASTE_LOG));
        WASTE_TRINITITE =
                Reg.block(
                        "waste_trinitite",
                        BlockWasteTrinitite::new,
                        BlockWasteTrinitite::defaultProperties);
        DECO_BLOCKS.add(decoItem("waste_trinitite", WASTE_TRINITITE));
        WASTE_TRINITITE_RED =
                Reg.block(
                        "waste_trinitite_red",
                        BlockWasteTrinitite::new,
                        BlockWasteTrinitite::defaultProperties);
        DECO_BLOCKS.add(decoItem("waste_trinitite_red", WASTE_TRINITITE_RED));

        VOLCANO_CORE = volcano("volcano_core");
        VOLCANO_RAD_CORE = volcano("volcano_rad_core");

        ORE_VOLCANO =
                Reg.block(
                        "ore_volcano",
                        BlockFissure::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(-1.0F, 600000.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(state -> 15)
                                        .randomTicks()
                                        .noLootTable());
        DECO_BLOCKS.add(decoItem("ore_volcano", ORE_VOLCANO));

        VOLCANIC_LAVA_FLUID =
                Reg.classicFluid(
                        "volcanic_lava_fluid",
                        new ClassicFluid.Spec(
                                ClassicFluid.Physics.LAVA,
                                4,
                                15,
                                3000,
                                true,
                                Library.id("block/fluid/volcanic_lava"),
                                () -> ModBlocks.VOLCANIC_LAVA_BLOCK.get(),
                                ModBlocks::lavaBucket));
        VOLCANIC_LAVA_BLOCK =
                Reg.block(
                        "volcanic_lava_block",
                        props -> new BlockVolcanicLava(VOLCANIC_LAVA_FLUID.source().get(), props),
                        () ->
                                classicFluidProps(MapColor.FIRE)
                                        .lightLevel(
                                                state ->
                                                        BlockFluidClassicBase.light(state, 4, 15)));

        RAD_LAVA_FLUID =
                Reg.classicFluid(
                        "rad_lava_fluid",
                        new ClassicFluid.Spec(
                                ClassicFluid.Physics.LAVA,
                                4,
                                15,
                                3000,
                                true,
                                Library.id("block/fluid/rad_lava"),
                                () -> ModBlocks.RAD_LAVA_BLOCK.get(),
                                ModBlocks::lavaBucket));
        RAD_LAVA_BLOCK =
                Reg.block(
                        "rad_lava_block",
                        props -> new BlockRadLava(RAD_LAVA_FLUID.source().get(), props),
                        () ->
                                classicFluidProps(MapColor.FIRE)
                                        .lightLevel(
                                                state ->
                                                        BlockFluidClassicBase.light(state, 4, 15)));

        decoSlab("brick_concrete_slab", 15.0F, 96.0F);
        decoSlab("brick_concrete_mossy_slab", 15.0F, 96.0F);
        decoSlab("brick_concrete_cracked_slab", 15.0F, 36.0F);
        decoSlab("brick_concrete_broken_slab", 15.0F, 27.0F);
        decoSlab("reinforced_brick_slab", 15.0F, 180.0F);
        decoSlab("brick_light_slab", 5.0F, 12.0F, SoundType.STONE);
        decoSlab("brick_compound_slab", 15.0F, 240.0F);
        decoSlab("brick_asbestos_slab", 5.0F, 600.0F);
        decoSlab("brick_fire_slab", 5.0F, 21.0F, SoundType.STONE);
        decoSlab("brick_obsidian_slab", 15.0F, 72.0F);
        CMB_BRICK_REINFORCED =
                decoBlast("cmb_brick_reinforced", 25.0F, 30000.0F)
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        decoSlab("concrete_slab", 15.0F, 84.0F);
        decoSlab("concrete_smooth_slab", 15.0F, 84.0F);
        decoSlab("concrete_asbestos_slab", 15.0F, 90.0F);
        decoSlab("ducrete_smooth_slab", 20.0F, 300.0F);
        decoSlab("ducrete_slab", 20.0F, 300.0F);
        decoSlab("brick_ducrete_slab", 15.0F, 450.0F);
        Reg.Handle<SlabBlock> asphaltSlab =
                Reg.<SlabBlock>block(
                        "asphalt_slab",
                        BlockSpeedySlab::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 72.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("asphalt_slab", asphaltSlab));

        RegistryHandle<SlabBlock> pinkSlab =
                Reg.block(
                        "pink_slab",
                        SlabBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F, 0.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        decoItem("pink_slab", pinkSlab);
        decoStairs("brick_concrete_stairs", brickConcrete, 15.0F, 96.0F);
        decoStairs("brick_concrete_mossy_stairs", brickConcreteMossy, 15.0F, 96.0F);
        decoStairs("brick_compound_stairs", brickCompound, 15.0F, 240.0F);
        decoStairs("reinforced_brick_stairs", reinforcedBrick, 15.0F, 180.0F);
        decoStairs("brick_concrete_cracked_stairs", brickConcreteCracked, 15.0F, 36.0F);
        decoStairs("brick_concrete_broken_stairs", brickConcreteBroken, 15.0F, 27.0F);
        decoStairs("brick_light_stairs", brickLight, 5.0F, 12.0F);
        decoStairs("brick_obsidian_stairs", brickObsidian, 15.0F, 72.0F);
        decoStairs("brick_asbestos_stairs", brickAsbestos, 5.0F, 600.0F);
        decoStairs("brick_fire_stairs", brickFire, 5.0F, 21.0F);
        decoStairs("concrete_stairs", concrete, 15.0F, 84.0F);
        decoStairs("concrete_smooth_stairs", concreteSmooth, 15.0F, 84.0F);
        decoStairs("concrete_asbestos_stairs", concreteAsbestos, 15.0F, 90.0F);
        decoStairs("ducrete_smooth_stairs", ducreteSmooth, 20.0F, 300.0F);
        decoStairs("ducrete_stairs", ducrete, 20.0F, 300.0F);
        decoStairs("brick_ducrete_stairs", brickDucrete, 15.0F, 450.0F);
        Reg.Handle<StairBlock> asphaltStairs =
                Reg.<StairBlock>block(
                        "asphalt_stairs",
                        props ->
                                new BlockSpeedyStairs(
                                        ASPHALT.get().defaultBlockState(), 1.5D, props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(15.0F, 72.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("asphalt_stairs", asphaltStairs));

        RegistryHandle<StairBlock> pinkStairs =
                Reg.<StairBlock>block(
                        "pink_stairs",
                        props -> new StairBlock(pinkPlanks.get().defaultBlockState(), props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.0F, 0.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        decoItem("pink_stairs", pinkStairs);
        SEAL_FRAME = structural("seal_frame", BlockSealFrame::new, 10.0F, 60.0F);
        MACHINE_TRANSFORMER = structural("machine_transformer", 5.0F, 6.0F);
        STRUCT_LAUNCHER = structural("struct_launcher", 5.0F, 6.0F);
        STRUCT_SCAFFOLD = structural("struct_scaffold", 5.0F, 6.0F);

        FUSION_HEATER = structuralUnlisted("fusion_heater", 5.0F, 6.0F);
        FUSION_HATCH = structuralUnlisted("fusion_hatch", FusionHatch::new, 5.0F, 6.0F);
        WATZ_ELEMENT = structural("watz_element", 5.0F, 6.0F);
        WATZ_COOLER = structural("watz_cooler", 5.0F, 6.0F);
    }

    public static RegistryHandle<Block> CONCRETE_RED;
    private static final String[] TURNED_FACINGS = {"north", "south", "west", "east"};

    public static final RegistryHandle<RailStandardStraight> RAIL_LARGE_STRAIGHT =
            Reg.blockItem(
                    "rail_large_straight",
                    RailStandardStraight::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardStraightShort> RAIL_LARGE_STRAIGHT_SHORT =
            Reg.blockItem(
                    "rail_large_straight_short",
                    RailStandardStraightShort::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardCurveBase> RAIL_LARGE_CURVE =
            Reg.blockItem(
                    "rail_large_curve",
                    RailStandardCurveBase::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardCurveWide7> RAIL_LARGE_CURVE_7 =
            Reg.blockItem(
                    "rail_large_curve_7",
                    RailStandardCurveWide7::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardCurveWide9> RAIL_LARGE_CURVE_9 =
            Reg.blockItem(
                    "rail_large_curve_9",
                    RailStandardCurveWide9::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardRamp> RAIL_LARGE_RAMP =
            Reg.blockItem(
                    "rail_large_ramp",
                    RailStandardRamp::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardBuffer> RAIL_LARGE_BUFFER =
            Reg.blockItem(
                    "rail_large_buffer",
                    RailStandardBuffer::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardSwitch> RAIL_LARGE_SWITCH =
            Reg.blockItem(
                    "rail_large_switch",
                    RailStandardSwitch::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailStandardSwitchFlipped> RAIL_LARGE_SWITCH_FLIPPED =
            Reg.blockItem(
                    "rail_large_switch_flipped",
                    RailStandardSwitchFlipped::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailNarrowStraight> RAIL_NARROW_STRAIGHT =
            Reg.blockItem(
                    "rail_narrow_straight",
                    RailNarrowStraight::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RailNarrowCurve> RAIL_NARROW_CURVE =
            Reg.blockItem(
                    "rail_narrow_curve",
                    RailNarrowCurve::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    private ModBlocks() {}

    private static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(10.0F, 60.0F)
                .noOcclusion()
                .dynamicShape()
                .forceSolidOn()
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties craneProps() {
        return BlockBehaviour.Properties.of().strength(5.0F, 6.0F).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties conveyorProps() {
        return BlockBehaviour.Properties.of()
                .strength(2.0F, 1.2F)
                .noOcclusion()
                .requiresCorrectToolForDrops();
    }

    private static <T extends Block> Reg.Handle<T> fusionMachine(
            String name, Function<BlockBehaviour.Properties, T> factory, int descLines) {
        Reg.Handle<T> handle =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 36.0F)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());
        Reg.item(
                name,
                props -> new DescBlockItem(handle.get(), props, descLines),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return handle;
    }

    private static RegistryHandle<MachineStirling> stirling(
            String name, MachineStirling.Tier tier, String sheet, int descLines) {
        return Reg.blockItem(
                        name,
                        props -> new MachineStirling(tier, props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion(),
                        (block, props) ->
                                new DescBlockItem(
                                        block,
                                        props.component(ModDataComponents.HAS_COG.get(), true),
                                        descLines))
                .state(ModDataComponents.HAS_COG);
    }

    private static <T extends Block> Reg.Handle<T> fusionComponent(
            String name, Function<BlockBehaviour.Properties, T> factory) {
        Reg.Handle<T> handle =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 18.0F)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                name,
                props -> new BlockItem(handle.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return handle;
    }

    private static <B extends BlockPileDevice> Reg.Handle<B> pileDevice(
            String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
        Reg.Handle<B> block =
                Reg.<B>block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());
        block.bakedBy(() -> new PileDeviceModel(block.get().kind()));
        Reg.item(
                name,
                props -> new BlockItem(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static <B extends BlockGraphiteDrilledBase> Reg.Handle<B> pile(
            String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
        Reg.Handle<B> block = Reg.block(name, factory, BlockGraphiteDrilledBase::pileProperties);

        Reg.item(
                name,
                props -> new BlockItem(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static <B extends Block> Reg.Handle<B> droneStation(
            String name, Function<BlockBehaviour.Properties, B> factory, int descLines) {
        return Reg.blockItem(
                name,
                factory,
                () ->
                        BlockBehaviour.Properties.of()
                                .strength(0.1F, 6.0F)
                                .requiresCorrectToolForDrops(),
                (block, props) -> new DescBlockItem(block, props, descLines));
    }

    private static Reg.Handle<DroneWaypointBlock> droneWaypoint(
            String name, DroneWaypointBlock.Kind kind, int descLines) {
        Reg.Handle<DroneWaypointBlock> block =
                Reg.block(
                                name,
                                props -> new DroneWaypointBlock(props, kind),
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .strength(0.1F, 6.0F)
                                                .noCollision()
                                                .noOcclusion()
                                                .pushReaction(PushReaction.DESTROY))
                        .bakedBy(
                                () -> {
                                    Identifier base = Library.id("block/" + name + "_base");
                                    return new DroneWaypointModel(base);
                                });
        Reg.item(
                name,
                props ->
                        descLines > 0
                                ? new DescBlockItem(block.get(), props, descLines)
                                : new BlockItem(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static Reg.Handle<BlockCrate> crate(
            String name, CrateType type, float hardness, float resistance) {
        return crate(name, props -> new BlockCrate(props, type), hardness, resistance);
    }

    private static Reg.Handle<BlockCrate> crate(
            String name,
            Function<BlockBehaviour.Properties, BlockCrate> factory,
            float hardness,
            float resistance) {
        Reg.Handle<BlockCrate> block =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new ItemBlockStorageCrate(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATES.add(item);
        return block;
    }

    private static Reg.Handle<BlockMassStorage> massStorage(String name, int capacity, String art) {
        Reg.Handle<BlockMassStorage> block =
                Reg.block(
                        name,
                        props -> new BlockMassStorage(props, capacity),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());

        Reg.item(
                name,
                props -> new ContainerBlockItem.WithInfo(block.get(), props),
                () ->
                        new Item.Properties()
                                .useBlockDescriptionPrefix()
                                .component(
                                        DataComponents.TOOLTIP_DISPLAY,
                                        new TooltipDisplay(
                                                false,
                                                new ReferenceLinkedOpenHashSet<>(
                                                        List.of(DataComponents.CONTAINER)))));
        return block;
    }

    private static void sealsWhileClosed(Block door) {
        RadiationSystemNT.markRadResistant(door, state -> !state.getValue(BlockDoorGeneric.OPEN));
    }

    private static Reg.Handle<Block> meteorDeco(String name) {

        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> deco(String name, float hardness, float resistance) {
        return deco(name, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<Block> deco(
            String name, float hardness, float resistance, MapColor color) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> deco(
            String name, float hardness, float resistance, SoundType sound) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> decoBlast(String name, float hardness, float resistance) {
        return decoBlast(name, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<Block> decoBlast(
            String name, float hardness, float resistance, SoundType sound) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> decoUnlisted(
            String name, float hardness, float resistance, SoundType sound) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        decoItem(name, block);
        return block;
    }

    private static Reg.Handle<Block> decoCt(String name, float hardness, float resistance) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(
                Reg.<Item>item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix()));
        return block;
    }

    private static Reg.Handle<Block> decoNoSpawnBlast(
            String name, float hardness, float resistance) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .isValidSpawn((s, l, p, t) -> false)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(
                Reg.item(
                        name,
                        props -> new ItemBlockNoSpawnBlastInfo(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix()));
        return block;
    }

    private static RegistryHandle<Block> concreteColored(String name) {
        RegistryHandle<Block> block = decoNoSpawnBlast(name, 15.0F, 84.0F);
        CONCRETE_COLORED.add(block);
        return block;
    }

    private static Reg.Handle<SlabBlock> decoSlab(String name, float hardness, float resistance) {
        return decoSlab(name, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<SlabBlock> decoSlab(
            String name, float hardness, float resistance, SoundType sound) {
        Reg.Handle<SlabBlock> block = registerSlab(name, hardness, resistance, sound);
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<StairBlock> decoStairs(
            String name, RegistryHandle<Block> base, float hardness, float resistance) {
        return decoStairs(name, base, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<StairBlock> decoStairs(
            String name,
            RegistryHandle<Block> base,
            float hardness,
            float resistance,
            SoundType sound) {
        Reg.Handle<StairBlock> block = registerStair(name, base, hardness, resistance, sound);
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<SlabBlock> registerSlab(
            String name, float hardness, float resistance, SoundType sound) {
        return Reg.block(
                name,
                SlabBlock::new,
                () ->
                        BlockBehaviour.Properties.of()
                                .strength(hardness, resistance)
                                .sound(sound)
                                .requiresCorrectToolForDrops());
    }

    private static Reg.Handle<StairBlock> registerStair(
            String name,
            RegistryHandle<Block> base,
            float hardness,
            float resistance,
            SoundType sound) {
        return Reg.block(
                name,
                props -> new StairBlock(base.get().defaultBlockState(), props),
                () ->
                        BlockBehaviour.Properties.of()
                                .strength(hardness, resistance)
                                .sound(sound)
                                .requiresCorrectToolForDrops());
    }

    private static Reg.Handle<ColoredFallingBlock> sandMix(String name, int dust) {
        Reg.Handle<ColoredFallingBlock> block =
                Reg.block(
                        name,
                        props -> new ColoredFallingBlock(new ColorRGBA(dust), props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.SAND)
                                        .strength(0.5F));
        decoItem(name, block);
        return block;
    }

    private static String sandbagsSuffix(int mask) {
        StringBuilder name = new StringBuilder();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (BlockSandbags.set(mask, dir)) name.append('_').append(dir.getName());
        }
        return name.toString();
    }

    private static String sandbagsState(int mask) {
        StringBuilder state = new StringBuilder();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!state.isEmpty()) state.append(',');
            state.append(dir.getName()).append('=').append(BlockSandbags.set(mask, dir));
        }
        return state.toString();
    }

    private static BlockBehaviour.Properties classicFluidProps(MapColor color) {

        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .replaceable()
                .noCollision()
                .strength(0.0F, 300.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    private static ItemStack lavaBucket() {
        return new ItemStack(Items.LAVA_BUCKET);
    }

    private static RegistryHandle<BlockVent> vent(
            String name, BlockVent.Plume plume, double spread) {
        return Reg.blockItem(
                name,
                props -> new BlockVent(props, plume, spread),
                () ->
                        BlockBehaviour.Properties.of()
                                .strength(5.0F, 6.0F)
                                .noLootTable()
                                .requiresCorrectToolForDrops());
    }

    private static RegistryHandle<Item> decoItem(
            String name, RegistryHandle<? extends Block> block) {
        return Reg.item(
                name,
                props -> new BlockItem(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<Item> railItem(
            String name, RegistryHandle<? extends Block> block, int lines) {
        return Reg.item(
                name,
                props -> new DescBlockItem(block.get(), props, lines, DescBlockItem.Desc.PLAIN),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<Item> blastItem(
            String name, RegistryHandle<? extends Block> block) {
        return Reg.item(
                name,
                props -> new ItemBlockBlastInfo(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static Reg.Handle<Block> concreteExt(String name) {
        Reg.Handle<Block> block = decoNoSpawnBlast(name, 15.0F, 84.0F);
        return block;
    }

    private static RegistryHandle<BlockTritiumLamp> tritiumLamp(String name) {
        return Reg.<BlockTritiumLamp>block(
                        name,
                        BlockTritiumLamp::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.NONE)
                                        .strength(3.0F, 3.0F)
                                        .sound(SoundType.GLASS)
                                        .lightLevel(s -> s.getValue(BlockTritiumLamp.LIT) ? 15 : 0))
                .andThen(h -> DECO_BLOCKS.add(decoItem(name, h)));
    }

    private static RegistryHandle<TransparentBlock> litGlass(String name, DyeColor color) {
        return Reg.<TransparentBlock>block(
                        name,
                        TransparentBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(0.3F)
                                        .sound(SoundType.GLASS)
                                        .noOcclusion()
                                        .instrument(NoteBlockInstrument.HAT)
                                        .lightLevel(s -> 5)
                                        .isRedstoneConductor((state, level, pos) -> false)
                                        .isSuffocating((state, level, pos) -> false)
                                        .isViewBlocking((state, level, pos) -> false)
                                        .isValidSpawn((state, level, pos, type) -> false))
                .andThen(h -> STRUCTURAL_BLOCKS.add(decoItem(name, h)));
    }

    private static Reg.Handle<Block> lightstone(String name) {
        Reg.Handle<Block> block = deco(name, 2.0F, 9.0F);
        return block;
    }

    private static Reg.Handle<Block> tileLab(String name, boolean randomTick) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        props -> new BlockOutgas(props, () -> ModBlocks.GAS_ASBESTOS.get(), true),
                        () -> {
                            BlockBehaviour.Properties props =
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(1.0F, 12.0F)
                                            .sound(SoundType.GLASS)
                                            .requiresCorrectToolForDrops();
                            return randomTick ? props.randomTicks() : props;
                        });
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> oreCube(String name, float hardness, float resistance) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> absorber(String name, String texture) {
        return Reg.blockItem(
                name,
                Block::new,
                () ->
                        BlockBehaviour.Properties.of()
                                .strength(5.0F, 6.0F)
                                .requiresCorrectToolForDrops());
    }

    private static Reg.Handle<BlockDepth> depthBlock(String name) {
        return depthBlock(name, 6.0F);
    }

    private static Reg.Handle<BlockDepth> depthBlock(String name, float resistance) {
        Reg.Handle<BlockDepth> block =
                Reg.block(
                        name,
                        BlockDepth::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(-1.0F, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(
                Reg.item(
                        name,
                        props -> new ItemBlockDepth(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix()));
        return block;
    }

    private static RegistryHandle<BlockStalagmite> stalagmiteVariant(
            String variant, int meta, @Nullable String drop) {
        String name = "stalagmite_" + variant;
        String texture = "block/stalagmite." + variant;
        return Reg.block(name, BlockStalagmite::new, ModBlocks::spikeProps)
                .andThen(h -> DECO_BLOCKS.add(decoItem(name, h)));
    }

    private static RegistryHandle<BlockStalactite> stalactiteVariant(
            String variant, int meta, @Nullable String drop) {
        String name = "stalactite_" + variant;
        String texture = "block/stalactite." + variant;
        return Reg.block(name, BlockStalactite::new, ModBlocks::spikeProps)
                .andThen(h -> DECO_BLOCKS.add(decoItem(name, h)));
    }

    private static BlockBehaviour.Properties spikeProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.5F, 1.2F)
                .sound(SoundType.STONE)
                .noOcclusion()
                .noCollision()
                .forceSolidOn()
                .requiresCorrectToolForDrops();
    }

    private static Reg.Handle<Block> resourceStone(String name) {
        return resourceStone(name, Block::new, false);
    }

    private static Reg.Handle<Block> resourceStone(
            String name,
            Function<BlockBehaviour.Properties, ? extends Block> factory,
            boolean ownDrops) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        factory,
                        () -> {
                            BlockBehaviour.Properties props =
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops();
                            return ownDrops ? props.noLootTable() : props;
                        });
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<BlockSellafieldOre> sellafieldOre(
            String name, String overlay, @Nullable Supplier<Item> drop, int xpMin, int xpMax) {
        return Reg.block(
                name,
                props -> new BlockSellafieldOre(props, drop, xpMin, xpMax),
                BlockSellafieldOre::defaultProperties);
    }

    private static BlockBehaviour.Properties volcanoProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 6000.0F)
                .sound(SoundType.STONE)
                .isValidSpawn((s, l, p, t) -> false)
                .noLootTable();
    }

    private static BlockBehaviour.Properties wasteBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static Reg.Handle<Block> extraOre(String name, float hardness, float resistance) {
        return extraOre(name, hardness, resistance, UnaryOperator.identity());
    }

    private static Reg.Handle<Block> extraOre(
            String name,
            float hardness,
            float resistance,
            UnaryOperator<BlockBehaviour.Properties> extra) {
        return extraOre(name, Block::new, hardness, resistance, extra);
    }

    private static <B extends Block> Reg.Handle<B> extraOre(
            String name,
            Function<BlockBehaviour.Properties, B> factory,
            float hardness,
            float resistance,
            UnaryOperator<BlockBehaviour.Properties> extra) {
        Reg.Handle<B> block =
                Reg.block(
                        name,
                        factory,
                        () ->
                                extra.apply(
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.STONE)
                                                .strength(hardness, resistance)
                                                .sound(SoundType.STONE)
                                                .requiresCorrectToolForDrops()));
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        EXTRA_ORE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<Block> deadPlant(String name) {
        RegistryHandle<Block> block =
                Reg.<Block>block(
                        name,
                        BlockDeadPlant::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .noCollision()
                                        .instabreak()
                                        .sound(SoundType.GRASS)
                                        .pushReaction(PushReaction.DESTROY));
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<BlockNTMFlower> flower(String name, BlockNTMFlower.Variant variant) {
        Reg.Handle<BlockNTMFlower> block =
                Reg.<BlockNTMFlower>block(
                        name,
                        props -> new BlockNTMFlower(props, variant),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .noCollision()
                                        .noOcclusion()
                                        .instabreak()
                                        .sound(SoundType.GRASS)
                                        .pushReaction(PushReaction.DESTROY));
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static BlockBehaviour.Properties cmProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static RegistryHandle<Block> cmCube(String name) {
        RegistryHandle<Block> block = Reg.block(name, Block::new, ModBlocks::cmProps);
        MACHINE_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<BlockCMTank> cmTank(String name) {
        RegistryHandle<BlockCMTank> block =
                Reg.<BlockCMTank>block(name, BlockCMTank::new, () -> cmProps().noOcclusion());
        MACHINE_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> cmPillar(String name) {
        RegistryHandle<Block> block = Reg.block(name, Block::new, ModBlocks::cmProps);
        MACHINE_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> cmAnchor() {
        RegistryHandle<Block> block =
                Reg.<Block>block("custom_machine_anchor", BlockCMAnchor::new, ModBlocks::cmProps);
        MACHINE_BLOCKS.add(decoItem("custom_machine_anchor", block));
        return block;
    }

    private static RegistryHandle<BlockCMPort> cmPort(String name) {
        RegistryHandle<BlockCMPort> block =
                Reg.<BlockCMPort>block(name, BlockCMPort::new, ModBlocks::cmProps);
        MACHINE_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<BlockCustomMachine> customMachine() {
        RegistryHandle<BlockCustomMachine> block =
                Reg.<BlockCustomMachine>block(
                        "custom_machine",
                        BlockCustomMachine::new,
                        () -> cmProps().lightLevel(state -> 15));

        MACHINE_BLOCKS.add(
                Reg.<Item>item(
                                "custom_machine",
                                props -> new ItemCustomMachine(block.get(), props),
                                () -> new Item.Properties().useBlockDescriptionPrefix())
                        .subtypes(ItemSubtype.CUSTOM_MACHINE));
        return block;
    }

    private static RegistryHandle<BlockReeds> reeds(String name) {
        Reg.Handle<BlockReeds> block =
                Reg.<BlockReeds>block(
                                name,
                                BlockReeds::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.PLANT)
                                                .noCollision()
                                                .noOcclusion()
                                                .instabreak()
                                                .sound(SoundType.GRASS)
                                                .pushReaction(PushReaction.DESTROY))
                        .bakedBy(() -> new ReedsModel.Family());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<BlockTallPlant> tallPlant(
            String name, BlockTallPlant.Variant variant, String flower) {
        Reg.Handle<BlockTallPlant> block =
                Reg.<BlockTallPlant>block(
                        name,
                        props -> new BlockTallPlant(props, variant),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .noCollision()
                                        .noOcclusion()
                                        .instabreak()
                                        .sound(SoundType.GRASS)
                                        .pushReaction(PushReaction.DESTROY)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<BlockTallPlant> tintedTallPlant(
            String name, BlockTallPlant.Variant variant, String flower) {
        return tallPlant(name, variant, flower);
    }

    private static Reg.Handle<BlockNTMFlower> tintedFlower(
            String name, BlockNTMFlower.Variant variant) {
        return flower(name, variant);
    }

    private static Reg.Handle<Block> fallingCube(
            String name, MapColor color, SoundType sound, float hardness, float resistance) {

        return fallingCube(name, color, sound, hardness, resistance, UnaryOperator.identity());
    }

    private static Reg.Handle<Block> fallingCube(
            String name,
            MapColor color,
            SoundType sound,
            float hardness,
            float resistance,
            UnaryOperator<BlockBehaviour.Properties> material) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        BlockFallingBase::new,
                        () ->
                                material.apply(
                                        BlockBehaviour.Properties.of()
                                                .mapColor(color)
                                                .sound(sound)
                                                .strength(hardness, resistance)));
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> fallingCubeBlast(
            String name, MapColor color, SoundType sound, float hardness, float resistance) {
        RegistryHandle<Block> block =
                Reg.block(
                        name,
                        BlockFallingBase::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .sound(sound)
                                        .strength(hardness, resistance)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem(name, block));
        return block;
    }

    private static RegistryHandle<Item> pipeItem(
            String name, RegistryHandle<? extends Block> block) {
        return Reg.<Item>item(
                name,
                props -> new DescBlockItem(block.get(), props, 1, DescBlockItem.Desc.PLAIN),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<Item> grateItem(
            String name, RegistryHandle<? extends Block> block, boolean wide) {
        return Reg.item(
                name,
                props -> new ItemBlockGrate(block.get(), props, wide),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static Reg.Handle<Block> structural(String name, float hardness, float resistance) {
        return structural(name, Block::new, hardness, resistance);
    }

    private static Reg.Handle<Block> structural(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            float hardness,
            float resistance) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .requiresCorrectToolForDrops());
        STRUCTURAL_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> structuralUnlisted(
            String name, float hardness, float resistance) {
        return structuralUnlisted(name, Block::new, hardness, resistance);
    }

    private static Reg.Handle<Block> structuralUnlisted(
            String name,
            Function<BlockBehaviour.Properties, ? extends Block> factory,
            float hardness,
            float resistance) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(hardness, resistance)
                                        .requiresCorrectToolForDrops());
        decoItem(name, block);
        return block;
    }

    private static Reg.Handle<Block> ore(
            String name, NTMMaterial mat, float hardness, float resistance) {
        return ore(name, mat.dict, hardness, resistance);
    }

    private static Reg.Handle<Block> ore(
            String name, OreDictManager.DictFrame dict, float hardness, float resistance) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, dict, MaterialShapes.ORE));
        return block;
    }

    private static Reg.Handle<Block> oreOutgas(
            String name, NTMMaterial mat, float hardness, float resistance, Supplier<Block> gas) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        props -> new BlockOutgas(props, gas, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, mat.dict, MaterialShapes.ORE));
        return block;
    }

    private static Reg.Handle<Block> basaltOre(String name, NTMMaterial mat) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, mat.dict, MaterialShapes.ORE));
        return block;
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            BlockHazard.@Nullable ExtDisplayEffect display) {
        return fuelBlock(name, hardness, resistance, display, null, SoundType.METAL);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            BlockHazard.@Nullable ExtDisplayEffect display,
            @Nullable Rarity rarity) {
        return fuelBlock(
                name, hardness, resistance, display, rarity, SoundType.METAL, MapColor.METAL);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            BlockHazard.@Nullable ExtDisplayEffect display,
            @Nullable Rarity rarity,
            SoundType sound) {
        return fuelBlock(name, hardness, resistance, display, rarity, sound, MapColor.METAL);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            BlockHazard.@Nullable ExtDisplayEffect display,
            @Nullable Rarity rarity,
            SoundType sound,
            MapColor color) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        props -> new BlockHazard(props).setDisplayEffect(display),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> {
                            Item.Properties p = new Item.Properties().useBlockDescriptionPrefix();
                            if (rarity != null) p.rarity(rarity);
                            return p;
                        });
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<Block> beaconStorage(
            String name, float hardness, float resistance, SoundType sound, MapColor color) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> pillarBlock(
            String name,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            @Nullable Rarity rarity) {
        RegistryHandle<Block> block =
                Reg.<Block>block(
                        name,
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> {
                            Item.Properties p = new Item.Properties().useBlockDescriptionPrefix();
                            if (rarity != null) p.rarity(rarity);
                            return p;
                        });
        DECO_BLOCKS.add(item);
        return block;
    }

    private static void pillarBlockNoTool(
            String name,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            NoteBlockInstrument instrument) {
        RegistryHandle<Block> block =
                Reg.<Block>block(
                        name,
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .instrument(instrument)
                                        .strength(hardness, resistance)
                                        .sound(sound));
        DECO_BLOCKS.add(decoItem(name, block));
    }

    private static Reg.Handle<Block> cokeBlock(String name, OreDictManager.DictFrame coke) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> plasticExplosive(String name) {
        RegistryHandle<Block> block =
                Reg.<Block>block(
                        name,
                        BlockPlasticExplosive::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(2.0F, 1.2F)
                                        .sound(SoundType.METAL)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> capBlock(String name) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> oreDeepslate(
            String name, NTMMaterial mat, float hardness, float resistance) {
        return oreDeepslate(name, mat.dict, hardness, resistance);
    }

    private static Reg.Handle<Block> oreDeepslate(
            String name, OreDictManager.DictFrame dict, float hardness, float resistance) {
        Reg.Handle<Block> block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DEEPSLATE)
                                        .strength(hardness * 1.5F, resistance)
                                        .sound(SoundType.DEEPSLATE)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, dict, MaterialShapes.ORE));
        return block;
    }

    private static Reg.Handle<Block> oreOutgasDeepslate(
            String name, NTMMaterial mat, float hardness, float resistance, Supplier<Block> gas) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        props -> new BlockOutgas(props, gas, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DEEPSLATE)
                                        .strength(hardness * 1.5F, resistance)
                                        .sound(SoundType.DEEPSLATE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, mat.dict, MaterialShapes.ORE));
        return block;
    }

    private static Reg.Handle<Block> storage(
            String name,
            NTMMaterial mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        return storage(name, mat.dict, hardness, resistance, sound, color);
    }

    private static Reg.Handle<Block> storage(
            String name,
            OreDictManager.DictFrame dict,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        return storage(name, dict, Block::new, hardness, resistance, sound, color);
    }

    private static Reg.Handle<Block> storage(
            String name,
            OreDictManager.DictFrame dict,
            Function<BlockBehaviour.Properties, ? extends Block> factory,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, dict, MaterialShapes.BLOCK));
        return block;
    }

    private static Reg.Handle<Block> storageOutgas(
            String name,
            NTMMaterial mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            Supplier<Block> gas) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        props -> new BlockOutgas(props, gas, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .randomTicks()
                                        .ignitedByLava());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, mat.dict, MaterialShapes.BLOCK));
        return block;
    }

    private static Reg.Handle<Block> hazardStorage(
            String name,
            NTMMaterial mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        return hazardStorage(name, mat, hardness, resistance, sound, color, null);
    }

    private static Reg.Handle<Block> hazardStorage(
            String name,
            NTMMaterial mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            BlockHazard.ExtDisplayEffect display) {
        return hazardStorage(name, mat, hardness, resistance, sound, color, display, 0);
    }

    private static Reg.Handle<Block> hazardStorage(
            String name,
            NTMMaterial mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            BlockHazard.ExtDisplayEffect display,
            int light) {
        Reg.Handle<Block> block =
                Reg.<Block>block(
                        name,
                        props -> new BlockHazard(props).setDisplayEffect(display),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .lightLevel(s -> light)
                                        .requiresCorrectToolForDrops());
        RegistryHandle<Item> item =
                Reg.item(
                        name,
                        props -> new BlockItem(block.get(), props),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, mat.dict, MaterialShapes.BLOCK));
        return block;
    }

    private static <T extends RBMKMiniPanelBase> Reg.Handle<T> miniPanel(
            String name, Function<BlockBehaviour.Properties, T> factory, int descLines) {
        return Reg.blockItem(
                name,
                factory,
                ModBlocks::rbmkProps,
                (block, props) -> new DescBlockItem(block, props, descLines));
    }

    private static BlockBehaviour.Properties rbmkProps() {
        return BlockBehaviour.Properties.of()
                .strength(3.0F, 18.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    private static BlockBehaviour.Properties rbmkColumnProps() {
        return BlockBehaviour.Properties.of().strength(3.0F, 18.0F).requiresCorrectToolForDrops();
    }

    private static Map<CellBuckets.Plain, RegistryHandle<? extends BlockMultiblockCell>>
            plainCells() {
        Map<CellBuckets.Plain, RegistryHandle<? extends BlockMultiblockCell>> out =
                new LinkedHashMap<>();
        for (CellBuckets.Plain key : CellBuckets.plain()) {
            out.put(
                    key,
                    Reg.block(
                                    key.name(),
                                    props -> BlockMultiblockCell.create(key, props),
                                    () -> cellProperties(key))
                            .afterRegistration(
                                    cell ->
                                            RadiationSystemNT.markRadResistant(
                                                    cell,
                                                    state ->
                                                            state.getValue(
                                                                    BlockMultiblockCell.SEALED))));
        }
        return out;
    }

    private static Map<CellBuckets.Geometry, RegistryHandle<BlockMultiblockGeometryCell>>
            geometryCells() {
        Map<CellBuckets.Geometry, RegistryHandle<BlockMultiblockGeometryCell>> out =
                new LinkedHashMap<>();
        for (CellBuckets.Geometry bucket : CellBuckets.geometry()) {
            RegistryHandle<BlockMultiblockGeometryCell> handle =
                    Reg.block(
                                    bucket.name(),
                                    props -> BlockMultiblockGeometryCell.create(bucket, props),
                                    () ->
                                            cellProperties(bucket.key())
                                                    .dynamicShape()
                                                    .forceSolidOn())
                            .afterRegistration(
                                    cell -> {
                                        if (!bucket.part().seals()) return;
                                        RadiationSystemNT.markRadResistant(
                                                cell,
                                                state ->
                                                        !state.getValue(
                                                                BlockMultiblockGeometryCell.OPEN));
                                    });
            out.put(bucket, handle);
        }
        return out;
    }

    public static RegistryHandle<? extends BlockMultiblockCell> plainCell(CellBuckets.Plain key) {
        return PLAIN_CELLS.get(key);
    }

    public static RegistryHandle<BlockMultiblockGeometryCell> geometryCell(
            CellBuckets.Geometry bucket) {
        return GEOMETRY_CELLS.get(bucket);
    }

    public static List<RegistryHandle<? extends Block>> cellHandles() {
        List<RegistryHandle<? extends Block>> out = new ArrayList<>(PLAIN_CELLS.values());
        out.addAll(GEOMETRY_CELLS.values());
        out.add(RADAR_MAST_CELL);
        return out;
    }

    private static BlockBehaviour.Properties cellProperties(CellBuckets.Plain key) {
        return BlockBehaviour.Properties.of()
                .mapColor(key.mapColor())
                .strength(key.hardness(), key.resistance())
                .lightLevel(state -> key.light())
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    private static BlockBehaviour.Properties paProperties() {
        return BlockBehaviour.Properties.of()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    private static BlockBehaviour.Properties icfProps() {
        return BlockBehaviour.Properties.of()
                .strength(5.0F, 36.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static Reg.Handle<Block> icfPart(String name) {
        Reg.Handle<Block> block = Reg.block(name, Block::new, ModBlocks::icfProps);
        rbmkItem(name, block);
        return block;
    }

    private static BlockBehaviour.Properties pwrProps() {
        return BlockBehaviour.Properties.of().strength(5.0F, 6.0F).requiresCorrectToolForDrops();
    }

    private static Reg.Handle<Block> pwrPart(String name, int descLines) {
        Reg.Handle<Block> block = Reg.block(name, Block::new, ModBlocks::pwrProps);
        Reg.item(
                name,
                props -> new DescBlockItem(block.get(), props, descLines),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static void rbmkItem(String name, RegistryHandle<? extends Block> block) {
        Reg.item(
                name,
                props -> new BlockItem(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<RadioTorchBlock> radioTorch(
            String id, RadioTorchBlock.Kind kind) {
        String skin =
                switch (kind) {
                    case SENDER -> "sender_off";
                    case RECEIVER -> "rec_off";
                    case COUNTER -> "counter";
                    case LOGIC -> "logic_off";
                    case READER -> "reader";
                    case CONTROLLER -> "controller";
                };
        String litSkin =
                switch (kind) {
                    case SENDER -> "sender_on";
                    case RECEIVER -> "rec_on";
                    case LOGIC -> "logic_on";
                    case COUNTER, READER, CONTROLLER -> null;
                };
        RegistryHandle<RadioTorchBlock> block =
                Reg.block(
                                id,
                                props -> new RadioTorchBlock(props, kind),
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .strength(0.1F, 6.0F)
                                                .noCollision()
                                                .noOcclusion()
                                                .pushReaction(PushReaction.DESTROY))
                        .bakedBy(() -> new RadioTorchModel());
        int descLines =
                switch (kind) {
                    case RECEIVER -> 1;
                    case SENDER, COUNTER, READER, CONTROLLER -> 2;
                    case LOGIC -> 3;
                };
        Reg.item(
                id,
                props -> new DescBlockItem(block.get(), props, descLines),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static Reg.Handle<BlockAnvil> anvil(String id, int tier) {
        Reg.Handle<BlockAnvil> block =
                Reg.block(
                        id,
                        props -> new BlockAnvil(props, tier),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 60.0F)
                                        .sound(SoundType.ANVIL)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                id,
                props -> new ItemBlockAnvil(block.get(), props, tier),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        ANVILS.add(block);
        return block;
    }

    private static RegistryHandle<PylonMediumBlock> mediumPylon(
            String name,
            boolean transformer,
            String sheet,
            TagKey<Block> mineable,
            UnaryOperator<BlockBehaviour.Properties> material) {
        String[] parts =
                transformer ? new String[] {"Pylon", "Transformer"} : new String[] {"Pylon"};
        String[] hidden = transformer ? new String[0] : new String[] {"Transformer"};
        return Reg.blockItem(
                name,
                props -> new PylonMediumBlock(transformer, props),
                () ->
                        material.apply(
                                BlockBehaviour.Properties.of().strength(5.0F, 6.0F).noOcclusion()),
                (block, props) -> new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN));
    }

    private static RegistryHandle<Block> debris(String name, String mesh) {
        return debris(name, mesh, Block::new);
    }

    private static <B extends Block> RegistryHandle<B> debris(
            String name, String mesh, Function<BlockBehaviour.Properties, B> factory) {

        RegistryHandle<B> block =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(50.0F, 360.0F)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());
        rbmkItem(name, block);
        return block;
    }

    private static Reg.Handle<FluidPipeBlock> fluidPipe(String name) {
        return Reg.blockItem(
                        name,
                        FluidPipeBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .strength(5.0F, 6.0F)
                                        .sound(ModSoundTypes.PIPE)
                                        .forceSolidOn()
                                        .requiresCorrectToolForDrops(),
                        (block, props) -> new FluidPipeBlockItem(block, props))
                .subtypes(ItemSubtype.FLUID_CONTENT)
                .bakedBy(() -> new FluidPipeModel());
    }

    private static Reg.Handle<BlockFileCabinet> filingCabinet(String name, String texture) {
        return Reg.blockItem(
                name,
                BlockFileCabinet::new,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(10.0F, 9.0F)
                                .requiresCorrectToolForDrops());
    }

    private static BlockItemStateProperties levelState(int level) {
        return BlockItemStateProperties.EMPTY.with(BlockSellafield.LEVEL, level);
    }

    private static RegistryHandle<BlockVolcano> volcano(String name) {
        Reg.Handle<BlockVolcano> block =
                Reg.block(name, BlockVolcano::new, ModBlocks::volcanoProperties);
        Reg.item(
                        name,
                        props -> new BlockVolcano.VolcanoBlockItem(block.get(), props),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .component(
                                                DataComponents.BLOCK_STATE,
                                                BlockItemStateProperties.EMPTY.with(
                                                        BlockVolcano.MODE,
                                                        BlockVolcano.Mode.STATIC_ACTIVE)))
                .state(() -> DataComponents.BLOCK_STATE);
        return block;
    }

    public record MaterialBlockEntry(
            RegistryHandle<? extends Block> block,
            RegistryHandle<Item> item,
            OreDictManager.DictFrame dict,
            MaterialShapes shape) {}
}
