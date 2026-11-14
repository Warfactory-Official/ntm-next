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

package com.hbm.blocks;

import com.hbm.blockentity.door.DoorTypes;
import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.blocks.bomb.*;
import com.hbm.blocks.bomb.*;
import com.hbm.blocks.fluid.*;
import com.hbm.blocks.fluid.MudBlock;
import com.hbm.blocks.gas.*;
import com.hbm.blocks.generic.*;
import com.hbm.blocks.generic.*;
import com.hbm.blocks.generic.BobbleBlock.BobbleType;
import com.hbm.blocks.generic.ChainBlock;
import com.hbm.blocks.generic.LanternBlock;
import com.hbm.blocks.generic.PlushieBlock.PlushieType;
import com.hbm.blocks.generic.SnowglobeBlock.SnowglobeType;
import com.hbm.blocks.machine.*;
import com.hbm.blocks.machine.*;
import com.hbm.blocks.machine.AnvilBlock;
import com.hbm.blocks.machine.BlastFurnaceBlock;
import com.hbm.blocks.machine.CMPortBlock;
import com.hbm.blocks.machine.CustomMachineBlock;
import com.hbm.blocks.machine.albion.*;
import com.hbm.blocks.machine.fusion.*;
import com.hbm.blocks.machine.pile.*;
import com.hbm.blocks.machine.pile.*;
import com.hbm.blocks.machine.rbmk.*;
import com.hbm.blocks.machine.storage.*;
import com.hbm.blocks.machine.storage.*;
import com.hbm.blocks.machine.storage.TungstenCrateBlock;
import com.hbm.blocks.multiblock.CellBuckets;
import com.hbm.blocks.multiblock.MultiblockCellBlock;
import com.hbm.blocks.multiblock.MultiblockGeometryCellBlock;
import com.hbm.blocks.network.*;
import com.hbm.blocks.network.pneumatic.*;
import com.hbm.blocks.rail.*;
import com.hbm.blocks.rail.*;
import com.hbm.blocks.test.ObjTesterBlock;
import com.hbm.blocks.turret.TurretArtyBlock;
import com.hbm.blocks.turret.TurretChekhovBlock;
import com.hbm.blocks.turret.TurretFriendlyBlock;
import com.hbm.blocks.turret.TurretFritzBlock;
import com.hbm.blocks.turret.TurretHIMARSBlock;
import com.hbm.blocks.turret.TurretHowardBlock;
import com.hbm.blocks.turret.TurretHowardDamagedBlock;
import com.hbm.blocks.turret.TurretJeremyBlock;
import com.hbm.blocks.turret.TurretMaxwellBlock;
import com.hbm.blocks.turret.TurretRichardBlock;
import com.hbm.blocks.turret.TurretSentryBlock;
import com.hbm.blocks.turret.TurretSentryDamagedBlock;
import com.hbm.blocks.turret.TurretTauonBlock;
import com.hbm.client.model.*;
import com.hbm.client.model.ChainModel;
import com.hbm.client.model.SectionedModel;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.material.MaterialTags;
import com.hbm.inventory.material.Materials;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.itempool.ItemPoolIds;
import com.hbm.items.DescBlockItem;
import com.hbm.items.DroneWaypointItem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.block.*;
import com.hbm.items.block.CustomMachineItem;
import com.hbm.items.machine.FluidBucketItem;
import com.hbm.lib.ModBlockSetTypes;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.ItemSubtype;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.RegistryUtil;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
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

public final class ModBlocks {
    @SuppressWarnings("unchecked")
    public static final RegistryHandle<FluidDuctBoxBlock>[][] FLUID_DUCT_BOX =
            new RegistryHandle[3][5];

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<CableBoxBlock>[] RED_CABLE_BOX = new RegistryHandle[5];

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<FluidDuctBoxExhaustBlock>[] FLUID_DUCT_EXHAUST =
            new RegistryHandle[5];

    public static final List<RegistryHandle<TrappedBrickBlock>> JUNGLE_TRAPS = new ArrayList<>();
    public static final List<RegistryHandle<CrashedBombBlock>> CRASHED_BOMBS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> CRATES = new ArrayList<>();
    public static final List<RegistryHandle<Item>> EXTRA_ORE_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> STRUCTURAL_BLOCKS = new ArrayList<>();
    public static final List<MaterialBlockEntry> MATERIAL_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> DECO_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> RAIL_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<Item>> MACHINE_BLOCKS = new ArrayList<>();
    public static final List<RegistryHandle<AnvilBlock>> ANVILS = new ArrayList<>();
    public static final int[] ANVIL_TIERS = {
        AnvilBlock.TIER_IRON, AnvilBlock.TIER_STEEL, AnvilBlock.TIER_OIL, AnvilBlock.TIER_NUCLEAR,
        AnvilBlock.TIER_RBMK, AnvilBlock.TIER_FUSION, AnvilBlock.TIER_PARTICLE,
                AnvilBlock.TIER_GERALD,
        AnvilBlock.TIER_MURKY
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
                                            .mapColor(MapColor.METAL)
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
                                            .mapColor(MapColor.METAL)
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
                                            .mapColor(MapColor.METAL)
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
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, properties) ->
                                    new DescBlockItem(
                                            block, properties, 1, DescBlockItem.Desc.PLAIN))
                    .bakedBy(() -> new DiodeModel());

    public static final RegistryHandle<CableGaugeBlock> RED_CABLE_GAUGE =
            Reg.blockItem(
                    "red_cable_gauge",
                    CableGaugeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, properties) -> new DescBlockItem(block, properties, 4));

    public static final RegistryHandle<CableSwitchBlock> CABLE_SWITCH =
            Reg.blockItem(
                    "cable_switch",
                    CableSwitchBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CableDetectorBlock> CABLE_DETECTOR =
            Reg.blockItem(
                    "cable_detector",
                    CableDetectorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoatedWireBlock> RED_WIRE_COATED =
            Reg.blockItem(
                    "red_wire_coated",
                    CoatedWireBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<ConnectorBlock> RED_CONNECTOR =
            Reg.blockItem(
                            "red_connector",
                            props -> new ConnectorBlock(false, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops(),
                            (block, props) ->
                                    new DescBlockItem(block, props, 2, DescBlockItem.Desc.PLAIN))
                    .bakedBy(
                            () ->
                                    new ConnectorModel(
                                            RegistryUtil.id("models/network/connector.obj")));
    public static final RegistryHandle<ConnectorBlock> RED_CONNECTOR_SUPER =
            Reg.blockItem(
                            "red_connector_super",
                            properties -> new ConnectorBlock(true, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops(),
                            (block, properties) ->
                                    new DescBlockItem(
                                            block, properties, 2, DescBlockItem.Desc.PLAIN))
                    .bakedBy(
                            () ->
                                    new ConnectorModel(
                                            RegistryUtil.id("models/network/connector_super.obj")));

    public static final RegistryHandle<PylonBlock> RED_PYLON =
            Reg.blockItem(
                    "red_pylon",
                    PylonBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, properties) ->
                            new DescBlockItem(block, properties, 2, DescBlockItem.Desc.PLAIN));

    public static final RegistryHandle<PylonBlock> RED_PYLON_STEEL =
            Reg.blockItem(
                    "red_pylon_steel",
                    PylonBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, properties) ->
                            new DescBlockItem(block, properties, 2, DescBlockItem.Desc.PLAIN));

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
                                            .mapColor(MapColor.METAL)
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
                                    .mapColor(MapColor.METAL)
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
                                    .mapColor(MapColor.METAL)
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
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<FluidSwitchBlock> FLUID_SWITCH =
            Reg.blockItem(
                    "fluid_switch",
                    FluidSwitchBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<FluidCounterValveBlock> FLUID_COUNTER_VALVE =
            Reg.blockItem(
                    "fluid_counter_valve",
                    FluidCounterValveBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<FluidDuctGaugeBlock> FLUID_DUCT_GAUGE =
            Reg.blockItem(
                            "fluid_duct_gauge",
                            FluidDuctGaugeBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
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
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 6.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .sound(ModSoundTypes.PIPE)
                                            .requiresCorrectToolForDrops(),
                            (block, properties) -> new DescBlockItem(block, properties, 5))
                    .bakedBy(() -> new PneumoTubeModel.Family());

    public static final RegistryHandle<PneumoTubePaintableBlock> PNEUMATIC_TUBE_PAINTABLE =
            Reg.blockItem(
                            "pneumatic_tube_paintable",
                            PneumoTubePaintableBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
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
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PneumoStorageClutterBlock> PNEUMATIC_STORAGE_CLUTTER =
            Reg.blockItem(
                    "pneumatic_storage_clutter",
                    PneumoStorageClutterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
                                    .mapColor(MapColor.METAL)
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
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(ModSoundTypes.PIPE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PneumoStorageExporterBlock> PNEUMATIC_STORAGE_EXPORTER =
            Reg.blockItem(
                    "pneumatic_storage_exporter",
                    PneumoStorageExporterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
    public static final RegistryHandle<ConveyorPressBlock> MACHINE_CONVEYOR_PRESS =
            Reg.blockItem(
                    "machine_conveyor_press",
                    ConveyorPressBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<CraneInserterBlock> CRANE_INSERTER =
            Reg.blockItem(
                            "crane_inserter",
                            CraneInserterBlock::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneExtractorBlock> CRANE_EXTRACTOR =
            Reg.blockItem(
                            "crane_extractor",
                            CraneExtractorBlock::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 5))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneGrabberBlock> CRANE_GRABBER =
            Reg.blockItem(
                            "crane_grabber",
                            CraneGrabberBlock::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 6))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneBoxerBlock> CRANE_BOXER =
            Reg.blockItem(
                            "crane_boxer",
                            CraneBoxerBlock::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneUnboxerBlock> CRANE_UNBOXER =
            Reg.blockItem(
                            "crane_unboxer",
                            CraneUnboxerBlock::new,
                            ModBlocks::craneProps,
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new CraneModel());
    public static final RegistryHandle<CraneRouterBlock> CRANE_ROUTER =
            Reg.blockItem(
                    "crane_router",
                    CraneRouterBlock::new,
                    ModBlocks::craneProps,
                    (block, properties) -> new DescBlockItem(block, properties, 3));

    public static final RegistryHandle<CraneSplitterBlock> CRANE_SPLITTER =
            Reg.blockItem(
                    "crane_splitter",
                    CraneSplitterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, properties) -> new DescBlockItem(block, properties, 3));

    public static final RegistryHandle<CranePartitionerBlock> CRANE_PARTITIONER =
            Reg.blockItem(
                    "crane_partitioner",
                    CranePartitionerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, properties) -> new DescBlockItem(block, properties, 3));

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
                                            .mapColor(MapColor.METAL)
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
                                                    .mapColor(MapColor.METAL)
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
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .sound(ModSoundTypes.PIPE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops(),
                            FluidPipeAnchorBlock.AnchorBlockItem::new)
                    .bakedBy(() -> new AnchorModel());

    public static final RegistryHandle<FluidPumpBlock> FLUID_PUMP =
            Reg.blockItem(
                    "fluid_pump",
                    FluidPumpBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<RadioboxBlock> RADIOBOX =
            Reg.blockItem(
                    "radiobox",
                    RadioboxBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RadioRecBlock> RADIO_REC =
            Reg.blockItem(
                    "radiorec",
                    RadioRecBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
    public static final RegistryHandle<RadioTelexBlock> RADIO_TELEX =
            Reg.blockItem(
                    "radio_telex",
                    RadioTelexBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .sound(SoundType.STONE)
                                    .strength(3.0F, 6.0F)
                                    .noOcclusion()
                                    .ignitedByLava());
    public static final RegistryHandle<RadioAUTOCALBlock> RADIO_AUTOCAL =
            Reg.blockItem(
                    "radio_autocal",
                    RadioAUTOCALBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CentrifugeBlock> MACHINE_CENTRIFUGE =
            Reg.blockItem(
                    "machine_centrifuge",
                    CentrifugeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<GasCentBlock> MACHINE_GASCENT =
            Reg.blockItem(
                    "machine_gascent",
                    GasCentBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<ElectricFurnaceBlock> MACHINE_ELECTRIC_FURNACE =
            Reg.blockItem(
                    "machine_electric_furnace",
                    ElectricFurnaceBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .lightLevel(
                                            state -> {
                                                if (state.getValue(ElectricFurnaceBlock.LIT)) {
                                                    return 15;
                                                }
                                                return 0;
                                            }));

    public static final RegistryHandle<GeigerCounterBlock> GEIGER =
            Reg.blockItem(
                    "geiger",
                    GeigerCounterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(15.0F, 0.15F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<AnvilBlock> ANVIL_IRON =
            anvil("anvil_iron", AnvilBlock.TIER_IRON);
    public static final RegistryHandle<AnvilBlock> ANVIL_STEEL =
            anvil("anvil_steel", AnvilBlock.TIER_STEEL);
    public static final RegistryHandle<AnvilBlock> ANVIL_LEAD =
            anvil("anvil_lead", AnvilBlock.TIER_IRON);
    public static final RegistryHandle<AnvilBlock> ANVIL_DESH =
            anvil("anvil_desh", AnvilBlock.TIER_OIL);
    public static final RegistryHandle<AnvilBlock> ANVIL_FERROURANIUM =
            anvil("anvil_ferrouranium", AnvilBlock.TIER_NUCLEAR);
    public static final RegistryHandle<AnvilBlock> ANVIL_SATURNITE =
            anvil("anvil_saturnite", AnvilBlock.TIER_RBMK);
    public static final RegistryHandle<AnvilBlock> ANVIL_BISMUTH_BRONZE =
            anvil("anvil_bismuth_bronze", AnvilBlock.TIER_RBMK);
    public static final RegistryHandle<AnvilBlock> ANVIL_ARSENIC_BRONZE =
            anvil("anvil_arsenic_bronze", AnvilBlock.TIER_RBMK);
    public static final RegistryHandle<AnvilBlock> ANVIL_SCHRABIDATE =
            anvil("anvil_schrabidate", AnvilBlock.TIER_FUSION);
    public static final RegistryHandle<AnvilBlock> ANVIL_DNT =
            anvil("anvil_dnt", AnvilBlock.TIER_PARTICLE);
    public static final RegistryHandle<AnvilBlock> ANVIL_OSMIRIDIUM =
            anvil("anvil_osmiridium", AnvilBlock.TIER_GERALD);
    public static final RegistryHandle<AnvilBlock> ANVIL_MURKY =
            anvil("anvil_murky", AnvilBlock.TIER_MURKY);

    public static final RegistryHandle<BatterySocketBlock> MACHINE_BATTERY_SOCKET =
            Reg.blockItem(
                    "machine_battery_socket",
                    BatterySocketBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 4));

    public static final RegistryHandle<BatteryBlock> MACHINE_BATTERY =
            Reg.blockItem(
                    "machine_battery",
                    BatteryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<REDDBatteryBlock> MACHINE_BATTERY_REDD =
            Reg.blockItem(
                    "machine_battery_redd",
                    REDDBatteryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<FENSUBlock> MACHINE_FENSU =
            Reg.blockItem(
                    "machine_fensu",
                    FENSUBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CapacitorBlock> CAPACITOR_COPPER =
            Reg.blockItem(
                    "capacitor_copper",
                    properties -> new CapacitorBlock(1_000_000L, properties),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    CapacitorItem::new);

    public static final RegistryHandle<Block> PRESS_PREHEATER =
            Reg.blockItem(
                    "press_preheater",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PressBlock> MACHINE_PRESS =
            Reg.blockItem(
                    "machine_press",
                    PressBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<AutocrafterBlock> MACHINE_AUTOCRAFTER =
            Reg.blockItem(
                    "machine_autocrafter",
                    AutocrafterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<AssemblyMachineBlock> MACHINE_ASSEMBLY_MACHINE =
            Reg.blockItem(
                    "machine_assembly_machine",
                    AssemblyMachineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<PrecAssBlock> MACHINE_PRECASS =
            Reg.blockItem(
                    "machine_precass",
                    PrecAssBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<AssemblyFactoryBlock> MACHINE_ASSEMBLY_FACTORY =
            Reg.blockItem(
                    "machine_assembly_factory",
                    AssemblyFactoryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 5));
    public static final RegistryHandle<ChemicalPlantBlock> MACHINE_CHEMICAL_PLANT =
            Reg.blockItem(
                    "machine_chemical_plant",
                    ChemicalPlantBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RockMillBlock> MACHINE_ROCK_MILL =
            Reg.blockItem(
                    "machine_rockmill",
                    RockMillBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<DeuteriumExtractorBlock> MACHINE_DEUTERIUM_EXTRACTOR =
            Reg.blockItem(
                    "machine_deuterium_extractor",
                    DeuteriumExtractorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<DeuteriumTowerBlock> MACHINE_DEUTERIUM_TOWER =
            Reg.blockItem(
                            "machine_deuterium_tower",
                            DeuteriumTowerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(10.0F, 12.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new DeuteriumTowerModel());
    public static final RegistryHandle<RadiolysisBlock> MACHINE_RADIOLYSIS =
            Reg.blockItem(
                    "machine_radiolysis",
                    RadiolysisBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<ChemicalFactoryBlock> MACHINE_CHEMICAL_FACTORY =
            Reg.blockItem(
                    "machine_chemical_factory",
                    ChemicalFactoryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 5));
    public static final RegistryHandle<ElectrolyserBlock> MACHINE_ELECTROLYSER =
            Reg.blockItem(
                    "machine_electrolyser",
                    ElectrolyserBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RefineryBlock> MACHINE_REFINERY =
            Reg.blockItem(
                    "machine_refinery",
                    RefineryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<CatalyticReformerBlock> MACHINE_CATALYTIC_REFORMER =
            Reg.blockItem(
                    "machine_catalytic_reformer",
                    CatalyticReformerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<HydrotreaterBlock> MACHINE_HYDROTREATER =
            Reg.blockItem(
                    "machine_hydrotreater",
                    HydrotreaterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FractionTowerBlock> MACHINE_FRACTION_TOWER =
            Reg.blockItem(
                    "machine_fraction_tower",
                    FractionTowerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FractionSpacerBlock> FRACTION_SPACER =
            Reg.blockItem(
                    "fraction_spacer",
                    FractionSpacerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<CrackingTowerBlock> MACHINE_CRACKING_TOWER =
            Reg.blockItem(
                    "machine_cracking_tower",
                    CrackingTowerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<PUREXBlock> MACHINE_PUREX =
            Reg.blockItem(
                    "machine_purex",
                    PUREXBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<MixerBlock> MACHINE_MIXER =
            Reg.blockItem(
                    "machine_mixer",
                    MixerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SuperComputerBlock> MACHINE_SUPERCOMPUTER =
            Reg.blockItem(
                    "machine_supercomputer",
                    SuperComputerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SILEXBlock> MACHINE_SILEX =
            Reg.blockItem(
                    "machine_silex",
                    SILEXBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<OreSlopperBlock> MACHINE_ORE_SLOPPER =
            Reg.blockItem(
                    "machine_ore_slopper",
                    OreSlopperBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<AnnihilatorBlock> MACHINE_ANNIHILATOR =
            Reg.blockItem(
                    "machine_annihilator",
                    AnnihilatorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BlastFurnaceBlock> MACHINE_BLAST_FURNACE =
            Reg.blockItem(
                    "machine_blast_furnace",
                    BlastFurnaceBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<HephaestusBlock> MACHINE_HEPHAESTUS =
            Reg.blockItem(
                    "machine_hephaestus",
                    HephaestusBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<CrucibleBlock> MACHINE_CRUCIBLE =
            Reg.blockItem(
                    "machine_crucible",
                    CrucibleBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<CoreComponentBlock> DFC_EMITTER =
            Reg.blockItem(
                    "dfc_emitter",
                    properties ->
                            new CoreComponentBlock(CoreComponentBlock.Kind.EMITTER, properties),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoreComponentBlock> DFC_RECEIVER =
            Reg.blockItem(
                    "dfc_receiver",
                    properties ->
                            new CoreComponentBlock(CoreComponentBlock.Kind.RECEIVER, properties),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoreComponentBlock> DFC_INJECTOR =
            Reg.blockItem(
                    "dfc_injector",
                    properties ->
                            new CoreComponentBlock(CoreComponentBlock.Kind.INJECTOR, properties),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoreComponentBlock> DFC_STABILIZER =
            Reg.blockItem(
                    "dfc_stabilizer",
                    properties ->
                            new CoreComponentBlock(CoreComponentBlock.Kind.STABILIZER, properties),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CoreCoreBlock> DFC_CORE =
            Reg.blockItem(
                    "dfc_core",
                    CoreCoreBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MiningLaserBlock> MACHINE_MINING_LASER =
            Reg.blockItem(
                    "machine_mining_laser",
                    MiningLaserBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ForceFieldBlock> MACHINE_FORCEFIELD =
            Reg.blockItem(
                    "machine_forcefield",
                    ForceFieldBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ExcavatorBlock> MACHINE_EXCAVATOR =
            Reg.blockItem(
                    "machine_excavator",
                    ExcavatorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<Block> BARRICADE =
            Reg.blockItem(
                    "barricade",
                    Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.SAND)
                                    .strength(1.0F, 1.5F));
    public static final RegistryHandle<StrandCasterBlock> MACHINE_STRAND_CASTER =
            Reg.blockItem(
                    "machine_strand_caster",
                    StrandCasterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<RotaryFurnaceBlock> MACHINE_ROTARY_FURNACE =
            Reg.blockItem(
                    "machine_rotary_furnace",
                    RotaryFurnaceBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<ArcFurnaceBlock> MACHINE_ARC_FURNACE =
            Reg.blockItem(
                    "machine_arc_furnace",
                    ArcFurnaceBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<HeaterFireboxBlock> HEATER_FIREBOX =
            Reg.blockItem(
                    "heater_firebox",
                    HeaterFireboxBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<HeaterElectricBlock> HEATER_ELECTRIC =
            Reg.blockItem(
                    "heater_electric",
                    HeaterElectricBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<FoundryMoldBlock> FOUNDRY_MOLD =
            Reg.blockItem(
                    "foundry_mold",
                    FoundryMoldBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FoundryBasinBlock> FOUNDRY_BASIN =
            Reg.blockItem(
                    "foundry_basin",
                    FoundryBasinBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FoundryChannelBlock> FOUNDRY_CHANNEL =
            Reg.blockItem(
                    "foundry_channel",
                    FoundryChannelBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());

    public static final RegistryHandle<FoundryOutletBlock> FOUNDRY_OUTLET =
            Reg.blockItem(
                            "foundry_outlet",
                            FoundryOutletBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion()
                                            .forceSolidOn())
                    .bakedBy(() -> new FoundryOutletModel.Family());
    public static final RegistryHandle<FoundrySlagtapBlock> FOUNDRY_SLAGTAP =
            Reg.blockItem(
                            "foundry_slagtap",
                            FoundrySlagtapBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion()
                                            .forceSolidOn())
                    .bakedBy(() -> new FoundryOutletModel.Family());
    public static final RegistryHandle<FoundryTankBlock> FOUNDRY_TANK =
            Reg.blockItem(
                            "foundry_tank",
                            FoundryTankBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new FoundryTankModel.Family());
    public static final RegistryHandle<FurnaceIronBlock> FURNACE_IRON =
            Reg.blockItem(
                    "furnace_iron",
                    FurnaceIronBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 2));

    public static final RegistryHandle<FurnaceSteelBlock> FURNACE_STEEL =
            Reg.blockItem(
                    "furnace_steel",
                    FurnaceSteelBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<FurnaceCombinationBlock> FURNACE_COMBINATION =
            Reg.blockItem(
                    "furnace_combination",
                    FurnaceCombinationBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<FurnaceBrickBlock> MACHINE_FURNACE_BRICK =
            Reg.blockItem(
                    "machine_furnace_brick",
                    FurnaceBrickBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .lightLevel(
                                            state ->
                                                    state.getValue(FurnaceBrickBlock.LIT)
                                                            ? 15
                                                            : 0));

    public static final RegistryHandle<HeaterOvenBlock> HEATER_OVEN =
            Reg.blockItem(
                    "heater_oven",
                    HeaterOvenBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<AshpitBlock> MACHINE_ASHPIT =
            Reg.blockItem(
                    "machine_ashpit",
                    AshpitBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<HeaterOilburnerBlock> HEATER_OILBURNER =
            Reg.blockItem(
                    "heater_oilburner",
                    HeaterOilburnerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<HeaterHeatexBlock> HEATER_HEATEX =
            Reg.blockItem(
                    "heater_heatex",
                    HeaterHeatexBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<CrystallizerBlock> MACHINE_CRYSTALLIZER =
            Reg.blockItem(
                    "machine_crystallizer",
                    CrystallizerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<CokerBlock> MACHINE_COKER =
            Reg.blockItem(
                            "machine_coker",
                            CokerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion(),
                            (block, props) -> new DescBlockItem(block, props, 3))
                    .bakedBy(() -> new SectionedModel.Family(0));
    public static final RegistryHandle<LiquefactorBlock> MACHINE_LIQUEFACTOR =
            Reg.blockItem(
                    "machine_liquefactor",
                    LiquefactorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<SolidifierBlock> MACHINE_SOLIDIFIER =
            Reg.blockItem(
                    "machine_solidifier",
                    SolidifierBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<PyroOvenBlock> MACHINE_PYROOVEN =
            Reg.blockItem(
                    "machine_pyrooven",
                    PyroOvenBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<VacuumDistillationBlock> MACHINE_VACUUM_DISTILL =
            Reg.blockItem(
                    "machine_vacuum_distill",
                    VacuumDistillationBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FrackingTowerBlock> MACHINE_FRACKING_TOWER =
            Reg.blockItem(
                            "machine_fracking_tower",
                            FrackingTowerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 12.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new SectionedModel.Family(180));

    public static final RegistryHandle<PumpBlock> PUMP_ELECTRIC =
            Reg.blockItem(
                    "pump_electric",
                    PumpBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<IntakeBlock> MACHINE_INTAKE =
            Reg.blockItem(
                    "machine_intake",
                    IntakeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<DrainBlock> MACHINE_DRAIN =
            Reg.blockItem(
                    "machine_drain",
                    DrainBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<FunnelBlock> MACHINE_FUNNEL =
            Reg.blockItem(
                    "machine_funnel",
                    FunnelBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 4));

    public static final RegistryHandle<CondenserBlock> MACHINE_CONDENSER =
            Reg.blockItem(
                    "machine_condenser",
                    CondenserBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<CyclotronBlock> MACHINE_CYCLOTRON =
            Reg.blockItem(
                    "machine_cyclotron",
                    CyclotronBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ExposureChamberBlock> MACHINE_EXPOSURE_CHAMBER =
            Reg.blockItem(
                    "machine_exposure_chamber",
                    ExposureChamberBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RadGenBlock> MACHINE_RADGEN =
            Reg.blockItem(
                    "machine_radgen",
                    RadGenBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ReactorBreedingBlock> MACHINE_REACTOR_BREEDING =
            Reg.blockItem(
                    "machine_reactor",
                    ReactorBreedingBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ReactorResearchBlock> REACTOR_RESEARCH =
            Reg.blockItem(
                    "machine_reactor_small",
                    ReactorResearchBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<SmallTowerBlock> MACHINE_TOWER_SMALL =
            Reg.blockItem(
                            "machine_tower_small",
                            SmallTowerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new SectionedModel.Family(0));
    public static final RegistryHandle<LargeTowerBlock> MACHINE_TOWER_LARGE =
            Reg.blockItem(
                    "machine_tower_large",
                    LargeTowerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<PoweredCondenserBlock> MACHINE_CONDENSER_POWERED =
            Reg.blockItem(
                    "machine_condenser_powered",
                    PoweredCondenserBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<BigAssTankBlock> MACHINE_BIGASSTANK =
            Reg.blockItem(
                    "machine_bigasstank",
                    BigAssTankBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<OrbusBlock> MACHINE_ORBUS =
            Reg.blockItem(
                    "machine_orbus",
                    OrbusBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<UF6TankBlock> MACHINE_UF6_TANK =
            Reg.blockItem(
                    "machine_uf6_tank",
                    properties ->
                            new UF6TankBlock(properties, () -> ModBlockEntities.UF6_TANK.get()),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<UF6TankBlock> MACHINE_PUF6_TANK =
            Reg.blockItem(
                    "machine_puf6_tank",
                    properties ->
                            new UF6TankBlock(properties, () -> ModBlockEntities.PUF6_TANK.get()),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<MicrowaveBlock> MACHINE_MICROWAVE =
            Reg.blockItem(
                    "machine_microwave",
                    MicrowaveBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());
    public static final RegistryHandle<SatLinkerBlock> MACHINE_SATLINKER =
            Reg.blockItem(
                    "machine_satlinker",
                    SatLinkerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<SatLinkBlock> MACHINE_SATLINK =
            Reg.blockItem(
                    "machine_satlink",
                    SatLinkBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<TapeDriveBlock> MACHINE_TAPE_DRIVE =
            Reg.blockItem(
                    "machine_tape_drive",
                    TapeDriveBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<KeyForgeBlock> MACHINE_KEYFORGE =
            Reg.blockItem(
                    "machine_keyforge",
                    KeyForgeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<ThresherBlock> MACHINE_THRESHER =
            Reg.blockItem(
                    "machine_thresher",
                    ThresherBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 7));
    public static final RegistryHandle<AutosawBlock> MACHINE_AUTOSAW =
            Reg.blockItem(
                    "machine_autosaw",
                    AutosawBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 7));
    public static final RegistryHandle<StirlingBlock> MACHINE_STIRLING =
            stirling(
                    "machine_stirling",
                    StirlingBlock.Tier.NORMAL,
                    "block/models/machines/stirling",
                    4);
    public static final RegistryHandle<StirlingBlock> MACHINE_STIRLING_STEEL =
            stirling(
                    "machine_stirling_steel",
                    StirlingBlock.Tier.STEEL,
                    "block/models/machines/stirling_steel",
                    5);
    public static final RegistryHandle<StirlingBlock> MACHINE_STIRLING_CREATIVE =
            stirling(
                    "machine_stirling_creative",
                    StirlingBlock.Tier.CREATIVE,
                    "block/models/machines/stirling_creative",
                    4);
    public static final RegistryHandle<SawmillBlock> MACHINE_SAWMILL =
            Reg.blockItem(
                            "machine_sawmill",
                            SawmillBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
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
    public static final RegistryHandle<AmmoPressBlock> MACHINE_AMMO_PRESS =
            Reg.blockItem(
                    "machine_ammo_press",
                    AmmoPressBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<DynamicSlagBlock> SLAG =
            Reg.block(
                    "slag",
                    DynamicSlagBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .dynamicShape()
                                    .forceSolidOn()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<EmitterBlock> DECO_EMITTER =
            Reg.blockItem(
                    "deco_emitter",
                    EmitterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 4, DescBlockItem.Desc.PLAIN));
    public static final RegistryHandle<PartEmitterBlock> PART_EMITTER =
            Reg.blockItem(
                    "part_emitter",
                    PartEmitterBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 1, DescBlockItem.Desc.PLAIN));
    public static final RegistryHandle<LootBlock> LOOT =
            Reg.block(
                    "deco_loot",
                    LootBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.0F, 0.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .noTerrainParticles()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<CompressorBlock> MACHINE_COMPRESSOR =
            Reg.blockItem(
                    "machine_compressor",
                    CompressorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<CompactCompressorBlock> MACHINE_COMPRESSOR_COMPACT =
            Reg.blockItem(
                    "machine_compressor_compact",
                    CompactCompressorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<GasFlareBlock> MACHINE_FLARE =
            Reg.blockItem(
                    "machine_flare",
                    GasFlareBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<ChimneyBrickBlock> CHIMNEY_BRICK =
            Reg.blockItem(
                    "chimney_brick",
                    ChimneyBrickBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<ChimneyIndustrialBlock> CHIMNEY_INDUSTRIAL =
            Reg.blockItem(
                            "chimney_industrial",
                            ChimneyIndustrialBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 60.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion(),
                            (block, properties) -> new DescBlockItem(block, properties, 2))
                    .bakedBy(() -> new SectionedModel.Family(180));

    public static final RegistryHandle<OilWellBlock> MACHINE_WELL =
            Reg.blockItem(
                    "machine_well",
                    OilWellBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<PumpjackBlock> MACHINE_PUMPJACK =
            Reg.blockItem(
                    "machine_pumpjack",
                    PumpjackBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
    public static final RegistryHandle<ReactorZirnoxBlock> REACTOR_ZIRNOX =
            Reg.blockItem(
                    "reactor_zirnox",
                    ReactorZirnoxBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<DestroyedZirnoxBlock> ZIRNOX_DESTROYED =
            Reg.block(
                    "zirnox_destroyed",
                    DestroyedZirnoxBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(100.0F, 480.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<FELBlock> MACHINE_FEL =
            Reg.blockItem(
                    "machine_fel",
                    FELBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<PASourceBlock> PA_SOURCE =
            Reg.blockItem(
                    "pa_source",
                    PASourceBlock::new,
                    ModBlocks::paProperties,
                    (block, properties) -> new DescBlockItem(block, properties, 2));

    public static final RegistryHandle<PABeamlineBlock> PA_BEAMLINE =
            Reg.blockItem(
                    "pa_beamline",
                    PABeamlineBlock::new,
                    ModBlocks::paProperties,
                    (block, properties) -> new DescBlockItem(block, properties, 3));

    public static final RegistryHandle<PARFCBlock> PA_RFC =
            Reg.blockItem(
                    "pa_rfc",
                    PARFCBlock::new,
                    ModBlocks::paProperties,
                    (block, properties) -> new DescBlockItem(block, properties, 4));

    public static final RegistryHandle<PAQuadrupoleBlock> PA_QUADRUPOLE =
            Reg.blockItem(
                    "pa_quadrupole",
                    PAQuadrupoleBlock::new,
                    ModBlocks::paProperties,
                    (block, properties) -> new DescBlockItem(block, properties, 3));

    public static final RegistryHandle<PADipoleBlock> PA_DIPOLE =
            Reg.blockItem(
                    "pa_dipole",
                    PADipoleBlock::new,
                    ModBlocks::paProperties,
                    (block, props) -> new DescBlockItem(block, props, 6));
    public static final RegistryHandle<PADetectorBlock> PA_DETECTOR =
            Reg.blockItem(
                    "pa_detector",
                    PADetectorBlock::new,
                    ModBlocks::paProperties,
                    (block, properties) -> new DescBlockItem(block, properties, 4));

    public static final RegistryHandle<SolderingStationBlock> MACHINE_SOLDERING_STATION =
            Reg.blockItem(
                    "machine_soldering_station",
                    SolderingStationBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<ArcWelderBlock> MACHINE_ARC_WELDER =
            Reg.blockItem(
                    "machine_arc_welder",
                    ArcWelderBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 18.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<EPressBlock> MACHINE_EPRESS =
            Reg.blockItem(
                    "machine_epress",
                    EPressBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    private static final Map<CellBuckets.Plain, RegistryHandle<? extends MultiblockCellBlock>>
            PLAIN_CELLS = plainCells();
    private static final Map<CellBuckets.Geometry, RegistryHandle<MultiblockGeometryCellBlock>>
            GEOMETRY_CELLS = geometryCells();
    public static final RegistryHandle<FluidTankBlock> MACHINE_FLUID_TANK =
            Reg.blockItem(
                    "machine_fluidtank",
                    FluidTankBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 12.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<FluidBarrelBlock> BARREL_PLASTIC =
            Reg.<FluidBarrelBlock>blockItem(
                            "barrel_plastic",
                            properties -> new FluidBarrelBlock.Functional(properties, 12_000, 3),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            FluidBarrelBlock.BarrelBlockItem::new)
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<FluidBarrelBlock> BARREL_CORRODED =
            Reg.blockItem(
                            "barrel_corroded",
                            properties -> new FluidBarrelBlock(properties, 6_000, 4),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            FluidBarrelBlock.BarrelBlockItem::new)
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<FluidBarrelBlock> BARREL_STEEL =
            Reg.<FluidBarrelBlock>blockItem(
                            "barrel_steel",
                            properties -> new FluidBarrelBlock.Functional(properties, 16_000, 4),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            FluidBarrelBlock.BarrelBlockItem::new)
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<FluidBarrelBlock> BARREL_TCALLOY =
            Reg.<FluidBarrelBlock>blockItem(
                            "barrel_tcalloy",
                            properties -> new FluidBarrelBlock.Functional(properties, 24_000, 3),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            FluidBarrelBlock.BarrelBlockItem::new)
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<FluidBarrelBlock> BARREL_ANTIMATTER =
            Reg.<FluidBarrelBlock>blockItem(
                            "barrel_antimatter",
                            properties -> new FluidBarrelBlock.Functional(properties, 16_000, 3),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            FluidBarrelBlock.BarrelBlockItem::new)
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<ChargeC4Block> CHARGE_C4 =
            Reg.blockItem(
                            "charge_c4",
                            ChargeC4Block::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .ignitedByLava(),
                            ChargeBlockItem::new)
                    .bakedBy(() -> new ChargeModel(RegistryUtil.id("models/blocks/charge_c4.obj")));

    public static final RegistryHandle<ChargeDynamiteBlock> CHARGE_DYNAMITE =
            Reg.blockItem(
                            "charge_dynamite",
                            ChargeDynamiteBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .ignitedByLava(),
                            ChargeBlockItem::new)
                    .bakedBy(
                            () ->
                                    new ChargeModel(
                                            RegistryUtil.id("models/blocks/charge_dynamite.obj")));

    public static final RegistryHandle<ChargeMinerBlock> CHARGE_MINER =
            Reg.blockItem(
                            "charge_miner",
                            ChargeMinerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .ignitedByLava(),
                            ChargeBlockItem::new)
                    .bakedBy(
                            () ->
                                    new ChargeModel(
                                            RegistryUtil.id("models/blocks/charge_dynamite.obj")));

    public static final RegistryHandle<ChargeSemtexBlock> CHARGE_SEMTEX =
            Reg.blockItem(
                            "charge_semtex",
                            ChargeSemtexBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F, 0.6F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .ignitedByLava(),
                            ChargeBlockItem::new)
                    .bakedBy(() -> new ChargeModel(RegistryUtil.id("models/blocks/charge_c4.obj")));

    public static final RegistryHandle<RedBarrelBlock> RED_BARREL =
            Reg.blockItem(
                            "red_barrel",
                            RedBarrelBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<RedBarrelBlock> PINK_BARREL =
            Reg.blockItem(
                            "pink_barrel",
                            RedBarrelBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<LoxBarrelBlock> LOX_BARREL =
            Reg.blockItem(
                            "lox_barrel",
                            LoxBarrelBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<TaintBarrelBlock> TAINT_BARREL =
            Reg.blockItem(
                            "taint_barrel",
                            TaintBarrelBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, properties) ->
                                    new DescBlockItem(
                                            block, properties, 1, DescBlockItem.Desc.PLAIN))
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<YellowBarrelBlock> YELLOW_BARREL =
            Reg.blockItem(
                            "yellow_barrel",
                            properties -> new YellowBarrelBlock(properties, true),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<YellowBarrelBlock> VITRIFIED_BARREL =
            Reg.blockItem(
                            "vitrified_barrel",
                            properties -> new YellowBarrelBlock(properties, false),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.5F, 1.5F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new BarrelModel());

    public static final RegistryHandle<APLandmineBlock> MINE_AP =
            Reg.blockItem(
                            "mine_ap",
                            APLandmineBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.AP));

    public static final RegistryHandle<ShrapnelLandmineBlock> MINE_SHRAP =
            Reg.blockItem(
                            "mine_shrap",
                            ShrapnelLandmineBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.SHRAP));

    public static final RegistryHandle<HELandmineBlock> MINE_HE =
            Reg.blockItem(
                            "mine_he",
                            HELandmineBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.HE));

    public static final RegistryHandle<FatLandmineBlock> MINE_FAT =
            Reg.blockItem(
                            "mine_fat",
                            FatLandmineBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.FAT));

    public static final RegistryHandle<NavalLandmineBlock> MINE_NAVAL =
            Reg.blockItem(
                            "mine_naval",
                            NavalLandmineBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(1.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new MineModel(MineModel.Kind.NAVAL));

    public static final RegistryHandle<PoleSatelliteReceiverBlock> POLE_SATELLITE_RECEIVER =
            Reg.blockItem(
                            "pole_satellite_receiver",
                            PoleSatelliteReceiverBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SatelliteReceiverModel());
    public static final RegistryHandle<DemonLampBlock> LAMP_DEMON =
            Reg.block(
                            "lamp_demon",
                            DemonLampBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .sound(SoundType.METAL)
                                            .strength(3.0F)
                                            .lightLevel(_ -> 15)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("lamp_demon", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SpotlightBlock> SPOTLIGHT_INCANDESCENT =
            Reg.block(
                            "spotlight_incandescent",
                            properties ->
                                    new SpotlightBlock(
                                            2, SpotlightBlock.LightType.INCANDESCENT, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(0.5F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .lightLevel(
                                                    state ->
                                                            state.getValue(SpotlightBlock.LIT)
                                                                    ? 15
                                                                    : 0)
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SpotlightModel(SpotlightBlock.LightType.INCANDESCENT))
                    .andThen(
                            block -> {
                                var item = decoItem("spotlight_incandescent", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SpotlightBlock> SPOTLIGHT_FLUORO =
            Reg.block(
                            "spotlight_fluoro",
                            properties ->
                                    new SpotlightBlock(
                                            8, SpotlightBlock.LightType.FLUORESCENT, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(0.5F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .lightLevel(
                                                    state ->
                                                            state.getValue(SpotlightBlock.LIT)
                                                                    ? 15
                                                                    : 0)
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SpotlightModel(SpotlightBlock.LightType.FLUORESCENT))
                    .andThen(
                            block -> {
                                var item = decoItem("spotlight_fluoro", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SpotlightBlock> SPOTLIGHT_HALOGEN =
            Reg.block(
                            "spotlight_halogen",
                            properties ->
                                    new SpotlightBlock(
                                            32, SpotlightBlock.LightType.HALOGEN, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(0.5F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .lightLevel(
                                                    state ->
                                                            state.getValue(SpotlightBlock.LIT)
                                                                    ? 15
                                                                    : 0)
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new SpotlightModel(SpotlightBlock.LightType.HALOGEN))
                    .andThen(
                            block -> {
                                var item = decoItem("spotlight_halogen", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SpotlightBeamBlock> SPOTLIGHT_BEAM =
            Reg.block(
                    "spotlight_beam",
                    SpotlightBeamBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .strength(-1.0F, 600000.0F)
                                    .replaceable()
                                    .noCollision()
                                    .noLootTable()
                                    .noOcclusion()
                                    .lightLevel(LightBlock.LIGHT_EMISSION));
    public static final RegistryHandle<BarbedWireBlock> BARBED_WIRE =
            Reg.blockItem(
                    "barbed_wire",
                    BarbedWireBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BarbedWireFireBlock> BARBED_WIRE_FIRE =
            Reg.blockItem(
                    "barbed_wire_fire",
                    BarbedWireFireBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BarbedWirePoisonBlock> BARBED_WIRE_POISON =
            Reg.blockItem(
                    "barbed_wire_poison",
                    BarbedWirePoisonBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BarbedWireAcidBlock> BARBED_WIRE_ACID =
            Reg.blockItem(
                    "barbed_wire_acid",
                    BarbedWireAcidBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BarbedWireWitherBlock> BARBED_WIRE_WITHER =
            Reg.blockItem(
                    "barbed_wire_wither",
                    BarbedWireWitherBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<BarbedWireUltradeathBlock> BARBED_WIRE_ULTRADEATH =
            Reg.blockItem(
                    "barbed_wire_ultradeath",
                    BarbedWireUltradeathBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
                                            .mapColor(MapColor.STONE)
                                            .strength(15.0F, 48.0F)
                                            .sound(SoundType.STONE)
                                            .lightLevel(s -> 15)
                                            .requiresCorrectToolForDrops(),
                            BlastInfoBlockItem::new)
                    .afterRegistration(RadiationSystemNT::markRadResistant);
    public static final RegistryHandle<RedstoneLampBlock> REINFORCED_LAMP =
            Reg.block(
                            "reinforced_lamp",
                            RedstoneLampBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(15.0F, 48.0F)
                                            .sound(SoundType.STONE)
                                            .lightLevel(
                                                    state ->
                                                            state.getValue(RedstoneLampBlock.LIT)
                                                                    ? 15
                                                                    : 0)
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(
                            block -> {
                                var item = blastItem("reinforced_lamp", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<HadronCoilBlock> HADRON_COIL_ALLOY =
            Reg.blockItem(
                    "hadron_coil_alloy",
                    HadronCoilBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops(),
                    HadronCoilBlock.CoilItem::new);

    public static final RegistryHandle<WoodBarrierBlock> WOOD_BARRIER =
            Reg.blockItem(
                            "wood_barrier",
                            WoodBarrierBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.WOOD)
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_BARRIER));
    public static final RegistryHandle<WoodStructureBlock> WOOD_STRUCTURE_ROOF =
            Reg.blockItem(
                            "wood_structure_roof",
                            props -> new WoodStructureBlock(WoodStructureBlock.Kind.ROOF, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.WOOD)
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_ROOF));
    public static final RegistryHandle<WoodStructureBlock> WOOD_STRUCTURE_SCAFFOLD =
            Reg.blockItem(
                            "wood_structure_scaffold",
                            props ->
                                    new WoodStructureBlock(WoodStructureBlock.Kind.SCAFFOLD, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.WOOD)
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_SCAFFOLD));
    public static final RegistryHandle<WoodStructureBlock> WOOD_STRUCTURE_CEILING =
            Reg.blockItem(
                            "wood_structure_ceiling",
                            props -> new WoodStructureBlock(WoodStructureBlock.Kind.CEILING, props),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.WOOD)
                                            .strength(5.0F, 9.0F)
                                            .sound(SoundType.WOOD)
                                            .noOcclusion()
                                            .ignitedByLava())
                    .bakedBy(() -> new NtBoxModel.Family(NtBoxModel.Shapes.WOOD_CEILING));
    public static final RegistryHandle<ChargerBlock> CHARGER =
            Reg.blockItem(
                    "charger",
                    ChargerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<RefuelerBlock> REFUELER =
            Reg.blockItem(
                    "refueler",
                    RefuelerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<DecoTapeRecorderBlock> TAPE_RECORDER =
            Reg.blockItem(
                    "tape_recorder",
                    DecoTapeRecorderBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 9.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<WoodBurnerBlock> MACHINE_WOOD_BURNER =
            Reg.blockItem(
                    "machine_wood_burner",
                    WoodBurnerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 3));

    public static final RegistryHandle<DieselBlock> MACHINE_DIESEL =
            Reg.blockItem(
                    "machine_diesel",
                    DieselBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    DieselBlockItem::new);
    public static final RegistryHandle<TurbineBlock> MACHINE_TURBINE =
            Reg.blockItem(
                    "machine_turbine",
                    TurbineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 1));
    public static final RegistryHandle<LargeTurbineBlock> MACHINE_LARGE_TURBINE =
            Reg.blockItem(
                    "machine_large_turbine",
                    LargeTurbineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 1));

    public static final RegistryHandle<IndustrialTurbineBlock> MACHINE_INDUSTRIAL_TURBINE =
            Reg.blockItem(
                    "machine_industrial_turbine",
                    IndustrialTurbineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 1));

    public static final RegistryHandle<ChungusBlock> MACHINE_CHUNGUS =
            Reg.blockItem(
                    "machine_chungus",
                    ChungusBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 1));

    public static final RegistryHandle<HeatBoilerBlock> MACHINE_BOILER =
            Reg.blockItem(
                    "machine_boiler",
                    HeatBoilerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<IndustrialHeatBoilerBlock> MACHINE_INDUSTRIAL_BOILER =
            Reg.blockItem(
                    "machine_industrial_boiler",
                    IndustrialHeatBoilerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 4));
    public static final RegistryHandle<SteamEngineBlock> MACHINE_STEAM_ENGINE =
            Reg.blockItem(
                    "machine_steam_engine",
                    SteamEngineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, properties) -> new DescBlockItem(block, properties, 1));

    public static final RegistryHandle<CombustionEngineBlock> MACHINE_COMBUSTION_ENGINE =
            Reg.blockItem(
                    "machine_combustion_engine",
                    CombustionEngineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<GasTurbineBlock> MACHINE_TURBINEGAS =
            Reg.blockItem(
                    "machine_turbinegas",
                    GasTurbineBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<TurbofanBlock> MACHINE_TURBOFAN =
            Reg.blockItem(
                    "machine_turbofan",
                    TurbofanBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    TurbofanBlockItem::new);
    public static final RegistryHandle<LPW2Block> MACHINE_LPW2 =
            Reg.blockItem(
                    "machine_lpw2",
                    LPW2Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 60.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<PumpBlock> PUMP_STEAM =
            Reg.blockItem(
                    "pump_steam",
                    PumpBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<SolarBoilerBlock> MACHINE_SOLAR_BOILER =
            Reg.blockItem(
                    "machine_solar_boiler",
                    SolarBoilerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SolarMirrorBlock> SOLAR_MIRROR =
            Reg.blockItem(
                    "solar_mirror",
                    SolarMirrorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<WasteDrumBlock> WASTE_DRUM =
            Reg.blockItem(
                    "machine_waste_drum",
                    WasteDrumBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<StorageDrumBlock> STORAGE_DRUM =
            Reg.blockItem(
                    "machine_storage_drum",
                    StorageDrumBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SirenBlock> MACHINE_SIREN =
            Reg.blockItem(
                    "machine_siren",
                    SirenBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<ShredderBlock> MACHINE_SHREDDER =
            Reg.blockItem(
                    "machine_shredder",
                    ShredderBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TeleporterBlock> MACHINE_TELEPORTER =
            Reg.blockItem(
                    "machine_teleporter",
                    TeleporterBlock::new,
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
    public static final RegistryHandle<DeconBlock> DECON =
            Reg.blockItem(
                    "decon",
                    DeconBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
    public static final RegistryHandle<RBMKRodBlock> RBMK_ROD =
            Reg.blockItem(
                            "rbmk_fuel_rod",
                            props -> new RBMKRodBlock(props, false),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKControlBlock> RBMK_CONTROL =
            Reg.blockItem(
                            "rbmk_control",
                            props -> new RBMKControlBlock(props, false),
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControlBlock> RBMK_CONTROL_MOD =
            Reg.blockItem(
                            "rbmk_control_mod",
                            props -> new RBMKControlBlock(props, true),
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControlAutoBlock> RBMK_CONTROL_AUTO =
            Reg.blockItem(
                            "rbmk_control_auto",
                            RBMKControlAutoBlock::new,
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControlBlock> RBMK_CONTROL_REASIM =
            Reg.blockItem(
                            "rbmk_control_reasim",
                            props -> new RBMKControlBlock(props, false),
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKControlAutoBlock> RBMK_CONTROL_REASIM_AUTO =
            Reg.blockItem(
                            "rbmk_control_reasim_auto",
                            RBMKControlAutoBlock::new,
                            ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.CONTROL));
    public static final RegistryHandle<RBMKBoilerBlock> RBMK_BOILER =
            Reg.blockItem("rbmk_boiler", RBMKBoilerBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.BOILER));
    public static final RegistryHandle<RBMKModeratorBlock> RBMK_MODERATOR =
            Reg.blockItem("rbmk_moderator", RBMKModeratorBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKReflectorBlock> RBMK_REFLECTOR =
            Reg.blockItem("rbmk_reflector", RBMKReflectorBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKAbsorberBlock> RBMK_ABSORBER =
            Reg.blockItem("rbmk_absorber", RBMKAbsorberBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKBlankBlock> RBMK_BLANK =
            Reg.blockItem("rbmk_blank", RBMKBlankBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKRodBlock> RBMK_ROD_MODERATED =
            Reg.blockItem(
                            "rbmk_fuel_rod_moderated",
                            props -> new RBMKRodBlock(props, true),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKRodReaSimBlock> RBMK_ROD_REASIM =
            Reg.blockItem(
                            "rbmk_fuel_rod_reasim",
                            props -> new RBMKRodReaSimBlock(props, false),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKRodReaSimBlock> RBMK_ROD_REASIM_MODERATED =
            Reg.blockItem(
                            "rbmk_fuel_rod_reasim_moderated",
                            props -> new RBMKRodReaSimBlock(props, true),
                            ModBlocks::rbmkProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.FUEL));
    public static final RegistryHandle<RBMKCoolerBlock> RBMK_COOLER =
            Reg.blockItem("rbmk_cooler", RBMKCoolerBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKStorageBlock> RBMK_STORAGE =
            Reg.blockItem("rbmk_storage", RBMKStorageBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKHeaterBlock> RBMK_HEATER =
            Reg.blockItem("rbmk_heater", RBMKHeaterBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.BOILER));
    public static final RegistryHandle<RBMKOutgasserBlock> RBMK_OUTGASSER =
            Reg.blockItem("rbmk_outgasser", RBMKOutgasserBlock::new, ModBlocks::rbmkColumnProps)
                    .bakedBy(() -> new RBMKColumnModel(RBMKColumnModel.Kind.PLAIN));
    public static final RegistryHandle<RBMKInletBlock> RBMK_INLET =
            Reg.blockItem(
                    "rbmk_steam_inlet",
                    RBMKInletBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<RBMKOutletBlock> RBMK_OUTLET =
            Reg.blockItem(
                    "rbmk_steam_outlet",
                    RBMKOutletBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 2));
    public static final RegistryHandle<RBMKConsoleBlock> RBMK_CONSOLE =
            Reg.blockItem("rbmk_console", RBMKConsoleBlock::new, ModBlocks::rbmkProps);

    public static final RegistryHandle<RBMKCraneConsoleBlock> RBMK_CRANE_CONSOLE =
            Reg.blockItem("rbmk_crane_console", RBMKCraneConsoleBlock::new, ModBlocks::rbmkProps);

    public static final RegistryHandle<RBMKGaugeBlock> RBMK_GAUGE =
            miniPanel("rbmk_gauge", RBMKGaugeBlock::new, 3);
    public static final RegistryHandle<RBMKDisplayBlock> RBMK_DISPLAY =
            miniPanel("rbmk_display", RBMKDisplayBlock::new, 3);
    public static final RegistryHandle<RBMKMiniPanelBlock> RBMK_DISPLAY_BLANK =
            miniPanel("rbmk_display_blank", RBMKMiniPanelBlock::new, 1);
    public static final RegistryHandle<RBMKNumitronBlock> RBMK_NUMITRON =
            miniPanel("rbmk_numitron", RBMKNumitronBlock::new, 3);
    public static final RegistryHandle<RBMKIndicatorBlock> RBMK_INDICATOR =
            miniPanel("rbmk_indicator", RBMKIndicatorBlock::new, 4);
    public static final RegistryHandle<RBMKGraphBlock> RBMK_GRAPH =
            miniPanel("rbmk_graph", RBMKGraphBlock::new, 3);
    public static final RegistryHandle<RBMKLeverBlock> RBMK_LEVER =
            miniPanel("rbmk_lever", RBMKLeverBlock::new, 3);
    public static final RegistryHandle<RBMKKeyPadBlock> RBMK_KEY_PAD =
            miniPanel("rbmk_key_pad", RBMKKeyPadBlock::new, 3);
    public static final RegistryHandle<RBMKTerminalBlock> RBMK_TERMINAL =
            miniPanel("rbmk_terminal", RBMKTerminalBlock::new, 1);

    public static final RegistryHandle<RBMKAutoloaderBlock> RBMK_AUTOLOADER =
            Reg.blockItem(
                    "rbmk_autoloader",
                    RBMKAutoloaderBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RBMKLoaderBlock> RBMK_LOADER =
            Reg.blockItem(
                    "rbmk_loader",
                    RBMKLoaderBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(50.0F, 36.0F)
                                    .requiresCorrectToolForDrops(),
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<Block> RBMK_DEBRIS =
            debris("rbmk_debris", "models/rbmk/pribris.obj");
    public static final RegistryHandle<RBMKDebrisBurningBlock> RBMK_DEBRIS_BURNING =
            debris(
                    "rbmk_debris_burning",
                    "models/rbmk/pribris_burning.obj",
                    RBMKDebrisBurningBlock::new);
    public static final RegistryHandle<RBMKDebrisRadiatingBlock> RBMK_DEBRIS_RADIATING =
            debris(
                    "rbmk_debris_radiating",
                    "models/rbmk/pribris_radiating.obj",
                    RBMKDebrisRadiatingBlock::new);
    public static final RegistryHandle<RBMKDebrisDigammaBlock> RBMK_DEBRIS_DIGAMMA =
            Reg.blockItem(
                    "rbmk_debris_digamma",
                    RBMKDebrisDigammaBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(50.0F, 360.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<AshDigammaBlock> ASH_DIGAMMA =
            Reg.blockItem(
                    "ash_digamma",
                    AshDigammaBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(0.5F, 90.0F)
                                    .sound(SoundType.SAND));
    public static final RegistryHandle<DigammaMatterBlock> DIGAMMA_MATTER =
            Reg.block(
                            "digamma_matter",
                            DigammaMatterBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(-1.0F, 10800000.0F)
                                            .noCollision()
                                            .noOcclusion()
                                            .noLootTable())
                    .bakedBy(() -> new DigammaMatterModel.Family());
    public static final RegistryHandle<DigammaFlameBlock> FIRE_DIGAMMA =
            Reg.block(
                    "fire_digamma",
                    DigammaFlameBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .replaceable()
                                    .noCollision()
                                    .strength(0.0F, 90.0F)
                                    .lightLevel(state -> 15)
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY)))
                                    .noLootTable());
    public static final RegistryHandle<FiniteCoriumBlock> CORIUM =
            Reg.block(
                    "corium_block",
                    FiniteCoriumBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BROWN)
                                    .strength(0.0F, 300.0F)
                                    .noCollision()
                                    .randomTicks()
                                    .lightLevel(FiniteCoriumBlock::light)
                                    .noLootTable()
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));

    public static RegistryHandle<RadonGasBlock> GAS_RADON;
    public static RegistryHandle<DenseRadonGasBlock> GAS_RADON_DENSE;
    public static RegistryHandle<TombRadonGasBlock> GAS_RADON_TOMB;
    public static final RegistryHandle<OutgasBlock> ANCIENT_SCRAP =
            Reg.blockItem(
                    "ancient_scrap",
                    props -> new OutgasBlock(props, () -> GAS_RADON_TOMB.get(), true, true),
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
                                    .mapColor(MapColor.METAL)
                                    .strength(100.0F, 3600.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<OutgasBlock> BLOCK_CORIUM_COBBLE =
            Reg.blockItem(
                    "block_corium_cobble",
                    props -> new OutgasBlock(props, () -> GAS_RADON.get(), true, true),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(100.0F, 3600.0F)
                                    .randomTicks()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PWRControllerBlock> PWR_CONTROLLER =
            Reg.blockItem(
                    "pwr_controller",
                    PWRControllerBlock::new,
                    ModBlocks::pwrProps,
                    (block, props) -> new DescBlockItem(block, props, 3));
    public static final RegistryHandle<PWRBlock> PWR_BLOCK =
            Reg.block(
                    "pwr_block",
                    PWRBlock::new,
                    () ->
                            pwrProps()
                                    .strength(15.0F, 6.0F)
                                    .noLootTable()
                                    .pushReaction(((PushReaction) (PushReaction.BLOCK))));

    public static final RegistryHandle<Block> PWR_CASING = pwrPart("pwr_casing", 2);
    public static final RegistryHandle<Block> PWR_PORT =
            pwrPart("pwr_port", 2, PhysicalPortBlock::fluid);
    public static final RegistryHandle<Block> PWR_REFLECTOR = pwrPart("pwr_reflector", 3);
    public static final RegistryHandle<Block> PWR_FUEL = pwrPart("pwr_fuel", 2);
    public static final RegistryHandle<Block> PWR_CONTROL = pwrPart("pwr_control", 2);
    public static final RegistryHandle<Block> PWR_CHANNEL = pwrPart("pwr_channel", 2);
    public static final RegistryHandle<Block> PWR_HEATEX = pwrPart("pwr_heatex", 2);
    public static final RegistryHandle<Block> PWR_HEATSINK = pwrPart("pwr_heatsink", 3);
    public static final RegistryHandle<Block> PWR_NEUTRON_SOURCE = pwrPart("pwr_neutron_source", 3);
    public static final RegistryHandle<Block> ICF_CASING = icfPart("icf_casing");
    public static final RegistryHandle<Block> ICF_PORT =
            icfPart("icf_port", PhysicalPortBlock::power);
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
    public static final RegistryHandle<ToolConversionBlock> ICF_COMPONENT_VESSEL =
            Reg.blockItem(
                    "icf_component_vessel",
                    props ->
                            new ToolConversionBlock(
                                    props,
                                    ToolType.TORCH,
                                    () -> ICF_COMPONENT_VESSEL_WELDED.get(),
                                    CountIngredient.of(
                                            MaterialTagRegistry.ANY_BISMOIDBRONZE.plateCast(), 1)),
                    ModBlocks::icfProps);
    public static final RegistryHandle<ToolConversionBlock> ICF_COMPONENT_STRUCTURE =
            Reg.blockItem(
                    "icf_component_structure",
                    props ->
                            new ToolConversionBlock(
                                    props,
                                    ToolType.BOLT,
                                    () -> ICF_COMPONENT_STRUCTURE_BOLTED.get(),
                                    CountIngredient.of(MaterialTagRegistry.STEEL.plateCast(), 1),
                                    CountIngredient.of(MaterialTagRegistry.DURA.bolt(), 4)),
                    ModBlocks::icfProps);
    public static final RegistryHandle<ICFControllerBlock> ICF_CONTROLLER =
            Reg.blockItem("icf_controller", ICFControllerBlock::new, ModBlocks::icfProps);
    public static final RegistryHandle<ICFBlock> ICF_BLOCK =
            Reg.block(
                    "icf_block",
                    ICFBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noLootTable()
                                    .pushReaction(((PushReaction) (PushReaction.BLOCK))));

    public static final RegistryHandle<ICFMachineBlock> MACHINE_ICF =
            Reg.blockItem("icf", ICFMachineBlock::new, () -> icfProps().noOcclusion());
    public static final RegistryHandle<ICFStructBlock> STRUCT_ICF_CORE =
            Reg.blockItem(
                    "struct_icf_core",
                    ICFStructBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(s -> 15)
                                    .noOcclusion());
    public static final RegistryHandle<ICFPressBlock> MACHINE_ICF_PRESS =
            Reg.blockItem(
                    "machine_icf_press",
                    ICFPressBlock::new,
                    ModBlocks::icfProps,
                    (block, props) ->
                            new DescBlockItem(block, props, 3, DescBlockItem.Desc.YELLOW));
    public static final RegistryHandle<NukeManBlock> NUKE_MAN =
            Reg.blockItem(
                    "nuke_man",
                    NukeManBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeGadgetBlock> NUKE_GADGET =
            Reg.blockItem(
                    "nuke_gadget",
                    NukeGadgetBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeBoyBlock> NUKE_BOY =
            Reg.blockItem(
                    "nuke_boy",
                    NukeBoyBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeMikeBlock> NUKE_MIKE =
            Reg.blockItem(
                    "nuke_mike",
                    NukeMikeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeTsarBlock> NUKE_TSAR =
            Reg.blockItem(
                    "nuke_tsar",
                    NukeTsarBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeFleijaBlock> NUKE_FLEIJA =
            Reg.blockItem(
                    "nuke_fleija",
                    NukeFleijaBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukePrototypeBlock> NUKE_PROTOTYPE =
            Reg.blockItem(
                    "nuke_prototype",
                    NukePrototypeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeSoliniumBlock> NUKE_SOLINIUM =
            Reg.blockItem(
                    "nuke_solinium",
                    NukeSoliniumBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<NukeN2Block> NUKE_N2 =
            Reg.blockItem(
                    "nuke_n2",
                    NukeN2Block::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 120.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<MissileAssemblyBlock> MACHINE_MISSILE_ASSEMBLY =
            Reg.blockItem(
                    "machine_missile_assembly",
                    MissileAssemblyBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<LaunchPadBlock> LAUNCH_PAD =
            Reg.blockItem(
                    "launch_pad",
                    LaunchPadBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RustedLaunchPadBlock> LAUNCH_PAD_RUSTED =
            Reg.blockItem(
                    "launch_pad_rusted",
                    RustedLaunchPadBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<LargeLaunchPadBlock> LAUNCH_PAD_LARGE =
            Reg.blockItem(
                    "launch_pad_large",
                    LargeLaunchPadBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());

    public static final RegistryHandle<CompactLauncherBlock> COMPACT_LAUNCHER =
            Reg.blockItem(
                    "compact_launcher",
                    CompactLauncherBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .forceSolidOn());

    public static final RegistryHandle<RadarBlock> MACHINE_RADAR =
            Reg.blockItem(
                    "machine_radar",
                    RadarBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<RadarLargeBlock> MACHINE_RADAR_LARGE =
            Reg.blockItem(
                    "machine_radar_large",
                    RadarLargeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<RadarScreenBlock> RADAR_SCREEN =
            Reg.blockItem(
                    "radar_screen",
                    RadarScreenBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<RadarMastCellBlock> RADAR_MAST_CELL =
            Reg.block(
                    "multiblock_cell_radar_mast",
                    RadarMastCellBlock::new,
                    () -> cellProperties(CellBuckets.Plain.of(5.0F, 6.0F, MapColor.METAL)));

    public static final RegistryHandle<LaunchTableBlock> LAUNCH_TABLE =
            Reg.blockItem(
                    "launch_table",
                    LaunchTableBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<StructBlock> STRUCT_LAUNCHER_CORE =
            Reg.blockItem(
                    "struct_launcher_core",
                    props -> new StructBlock(props, false),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<StructBlock> STRUCT_LAUNCHER_CORE_LARGE =
            Reg.blockItem(
                    "struct_launcher_core_large",
                    props -> new StructBlock(props, true),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SoyuzStructBlock> STRUCT_SOYUZ_CORE =
            Reg.blockItem(
                    "struct_soyuz_core",
                    SoyuzStructBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SoyuzLauncherBlock> SOYUZ_LAUNCHER =
            Reg.blockItem(
                    "soyuz_launcher",
                    SoyuzLauncherBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<LaunchpadSoyuzAnchorCellBlock> LAUNCHPAD_SOYUZ_ANCHOR =
            Reg.block(
                    "multiblock_cell_launchpad_soyuz_anchor",
                    LaunchpadSoyuzAnchorCellBlock::new,
                    () -> cellProperties(CellBuckets.Plain.of(5F, 6F, MapColor.METAL)));
    public static final RegistryHandle<LaunchpadSoyuzBlock> LAUNCHPAD_SOYUZ =
            Reg.blockItem(
                    "launchpad_soyuz",
                    LaunchpadSoyuzBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<SatDockBlock> SAT_DOCK =
            Reg.blockItem(
                    "sat_dock",
                    SatDockBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());
    public static final RegistryHandle<SoyuzCapsuleBlock> SOYUZ_CAPSULE =
            Reg.blockItem(
                    "soyuz_capsule",
                    SoyuzCapsuleBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<GenericDoorBlock> SECURE_ACCESS_DOOR =
            Reg.blockItem(
                            "secure_access_door",
                            props ->
                                    new GenericDoorBlock(props, DoorTypes.SECURE_ACCESS_DOOR, true),
                            () -> doorProperties().strength(20.0F, 1200.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> QE_SLIDING_DOOR =
            Reg.blockItem(
                            "qe_sliding_door",
                            props -> new GenericDoorBlock(props, DoorTypes.QE_SLIDING_DOOR),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> CARGO_DOOR =
            Reg.blockItem(
                            "cargo_door",
                            props -> new GenericDoorBlock(props, DoorTypes.CARGO_DOOR),
                            () -> doorProperties().strength(5.0F, 30.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> WATER_DOOR =
            Reg.blockItem(
                            "water_door",
                            props -> new GenericDoorBlock(props, DoorTypes.WATER_DOOR),
                            () -> doorProperties().strength(5.0F, 30.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> TRANSITION_SEAL =
            Reg.blockItem(
                            "transition_seal",
                            properties ->
                                    new GenericDoorBlock(
                                            properties, DoorTypes.TRANSITION_SEAL, true),
                            () -> doorProperties().strength(10F, 600F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());

    public static final RegistryHandle<GenericDoorBlock> VAULT_DOOR =
            Reg.blockItem(
                            "vault_door",
                            props -> new GenericDoorBlock(props, DoorTypes.VAULT_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> SLIDING_SEAL_DOOR =
            Reg.blockItem(
                            "sliding_seal_door",
                            props -> new GenericDoorBlock(props, DoorTypes.SLIDING_SEAL_DOOR),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> QE_CONTAINMENT =
            Reg.blockItem(
                            "qe_containment",
                            props -> new GenericDoorBlock(props, DoorTypes.QE_CONTAINMENT, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> ROUND_AIRLOCK_DOOR =
            Reg.blockItem(
                            "round_airlock_door",
                            props ->
                                    new GenericDoorBlock(props, DoorTypes.ROUND_AIRLOCK_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> SLIDING_BLAST_DOOR =
            Reg.blockItem(
                            "sliding_blast_door",
                            props -> new GenericDoorBlock(props, DoorTypes.SLIDING_BLAST_DOOR),
                            () -> doorProperties().strength(10.0F, 450.0F))
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> LARGE_VEHICLE_DOOR =
            Reg.blockItem(
                            "large_vehicle_door",
                            props ->
                                    new GenericDoorBlock(props, DoorTypes.LARGE_VEHICLE_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<BlastDoorBlock> BLAST_DOOR =
            Reg.blockItem(
                            "blast_door",
                            BlastDoorBlock::new,
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed);
    public static final RegistryHandle<GenericDoorBlock> FIRE_DOOR =
            Reg.blockItem(
                            "fire_door",
                            props -> new GenericDoorBlock(props, DoorTypes.FIRE_DOOR, true),
                            () -> doorProperties().strength(10.0F, 600.0F))
                    .afterRegistration(ModBlocks::sealsWhileClosed)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> SILO_HATCH =
            Reg.blockItem(
                            "silo_hatch",
                            props -> new GenericDoorBlock(props, DoorTypes.SILO_HATCH),
                            ModBlocks::doorProperties)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<GenericDoorBlock> SILO_HATCH_LARGE =
            Reg.blockItem(
                            "silo_hatch_large",
                            props -> new GenericDoorBlock(props, DoorTypes.SILO_HATCH_LARGE),
                            ModBlocks::doorProperties)
                    .bakedBy(() -> new DoorModel());
    public static final RegistryHandle<DungeonSpawnerBlock> DUNGEON_SPAWNER =
            Reg.block(
                            "dungeon_spawner",
                            DungeonSpawnerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(-1.0F, 300000.0F)
                                            .sound(SoundType.STONE)
                                            .noLootTable())
                    .andThen(h -> decoItem("dungeon_spawner", h));
    public static final RegistryHandle<SkeletonHolderBlock> SKELETON_HOLDER =
            Reg.blockItem(
                    "skeleton_holder",
                    SkeletonHolderBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(2F, 6F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion());

    public static final RegistryHandle<PedestalBlock> PEDESTAL =
            Reg.blockItem(
                            "pedestal",
                            PedestalBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(2F, 6F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new StandardBlockModel());

    public static final RegistryHandle<BobbleBlock> BOBBLEHEAD =
            Reg.<BobbleBlock>blockItem(
                    "bobblehead",
                    BobbleBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.0F, 0.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops(),
                    (block, properties) ->
                            new TrinketBlockItem<BobbleType>(block, properties, BobbleType.NONE));

    public static final ItemFamily<BobbleType, TrinketBlockItem<BobbleType>> BOBBLEHEADS =
            Reg.<BobbleType, TrinketBlockItem<BobbleType>>family(
                    Arrays.copyOfRange(BobbleType.values(), 1, BobbleType.values().length),
                    type -> "bobblehead_" + type.name().toLowerCase(Locale.ROOT),
                    (properties, type) ->
                            new TrinketBlockItem<>(BOBBLEHEAD.get(), properties, type),
                    Item.Properties::new);

    public static final RegistryHandle<SnowglobeBlock> SNOWGLOBE =
            Reg.<SnowglobeBlock>blockItem(
                    "snowglobe",
                    SnowglobeBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .strength(0.0F, 0.0F)
                                    .sound(SoundType.STONE)
                                    .instrument(NoteBlockInstrument.HAT)
                                    .noOcclusion()
                                    .noLootTable(),
                    (block, properties) ->
                            new TrinketBlockItem<SnowglobeType>(
                                    block, properties, SnowglobeType.NONE));

    public static final ItemFamily<SnowglobeType, TrinketBlockItem<SnowglobeType>> SNOWGLOBES =
            Reg.<SnowglobeType, TrinketBlockItem<SnowglobeType>>family(
                    Arrays.copyOfRange(SnowglobeType.values(), 1, SnowglobeType.values().length),
                    type -> "snowglobe_" + type.name().toLowerCase(Locale.ROOT),
                    (properties, type) -> new TrinketBlockItem<>(SNOWGLOBE.get(), properties, type),
                    Item.Properties::new);

    public static final RegistryHandle<PlushieBlock> PLUSHIE =
            Reg.<PlushieBlock>blockItem(
                    "plushie",
                    PlushieBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOL)
                                    .strength(0.0F, 0.0F)
                                    .sound(SoundType.WOOL)
                                    .noOcclusion()
                                    .noLootTable()
                                    .ignitedByLava(),
                    (block, properties) ->
                            new PlushieBlockItem(block, properties, PlushieType.NONE));

    public static final ItemFamily<PlushieType, PlushieBlockItem> PLUSHIES =
            Reg.<PlushieType, PlushieBlockItem>family(
                    Arrays.copyOfRange(PlushieType.values(), 1, PlushieType.values().length),
                    type -> "plushie_" + type.name().toLowerCase(Locale.ROOT),
                    (properties, type) -> new PlushieBlockItem(PLUSHIE.get(), properties, type),
                    Item.Properties::new);

    public static final RegistryHandle<FloodlightBlock> FLOODLIGHT =
            Reg.blockItem(
                            "floodlight",
                            FloodlightBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new FloodlightModel());

    public static final RegistryHandle<FloodlightBeamBlock> FLOODLIGHT_BEAM =
            Reg.block(
                    "floodlight_beam",
                    FloodlightBeamBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .strength(-1.0F, 600000.0F)
                                    .noCollision()
                                    .noOcclusion()
                                    .replaceable()
                                    .lightLevel(state -> 15)
                                    .noLootTable());
    public static final RegistryHandle<RTGBlock> MACHINE_RTG =
            Reg.blockItem(
                            "machine_rtg_grey",
                            RTGBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new RTGModel());

    public static final RegistryHandle<BoilerOffBlock> MACHINE_BOILER_OFF =
            Reg.blockItem(
                    "machine_boiler_off",
                    BoilerOffBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<WeaponTableBlock> MACHINE_WEAPON_TABLE =
            Reg.blockItem(
                    "machine_weapon_table",
                    WeaponTableBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<ArmorTableBlock> MACHINE_ARMOR_TABLE =
            Reg.blockItem(
                    "machine_armor_table",
                    ArmorTableBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<HEVBatteryBlock> HEV_BATTERY =
            Reg.blockItem(
                    "hev_battery",
                    HEVBatteryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.5F, 0.15F)
                                    .lightLevel(_ -> 10)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<ReactorControlBlock> MACHINE_CONTROLLER =
            Reg.blockItem(
                    "machine_controller",
                    ReactorControlBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.STONE)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<TurretSentryDamagedBlock> TURRET_SENTRY_DAMAGED =
            Reg.blockItem(
                    "turret_sentry_damaged",
                    TurretSentryDamagedBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 3.0F)
                                    .noOcclusion()
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretChekhovBlock> TURRET_CHEKHOV =
            Reg.blockItem(
                    "turret_chekhov",
                    TurretChekhovBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretFriendlyBlock> TURRET_FRIENDLY =
            Reg.blockItem(
                    "turret_friendly",
                    TurretFriendlyBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretJeremyBlock> TURRET_JEREMY =
            Reg.blockItem(
                    "turret_jeremy",
                    TurretJeremyBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretRichardBlock> TURRET_RICHARD =
            Reg.blockItem(
                    "turret_richard",
                    TurretRichardBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretHowardBlock> TURRET_HOWARD =
            Reg.blockItem(
                    "turret_howard",
                    TurretHowardBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 36.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretTauonBlock> TURRET_TAUON =
            Reg.blockItem(
                    "turret_tauon",
                    TurretTauonBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 36.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretFritzBlock> TURRET_FRITZ =
            Reg.blockItem(
                    "turret_fritz",
                    TurretFritzBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretMaxwellBlock> TURRET_MAXWELL =
            Reg.blockItem(
                    "turret_maxwell",
                    TurretMaxwellBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 36.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretSentryBlock> TURRET_SENTRY =
            Reg.blockItem(
                    "turret_sentry",
                    TurretSentryBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 3.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretHowardDamagedBlock> TURRET_HOWARD_DAMAGED =
            Reg.blockItem(
                            "turret_howard_damaged",
                            TurretHowardDamagedBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 360.0F)
                                            .noOcclusion()
                                            .noLootTable()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new TurretHowardDamagedModel());

    public static final RegistryHandle<TurretArtyBlock> TURRET_ARTY =
            Reg.blockItem(
                    "turret_arty",
                    TurretArtyBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<TurretHIMARSBlock> TURRET_HIMARS =
            Reg.blockItem(
                    "turret_himars",
                    TurretHIMARSBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 360.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<SmolderBlock> ORE_NETHER_SMOLDERING =
            Reg.block(
                            "ore_nether_smoldering",
                            SmolderBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NETHER)
                                            .strength(0.4F, 6.0F)
                                            .lightLevel(_ -> 15)
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("ore_nether_smoldering", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<BedrockOreBlock> ORE_BEDROCK =
            Reg.block(
                            "ore_bedrock",
                            BedrockOreBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(-1.0F, 600000.0F)
                                            .sound(SoundType.STONE)
                                            .noLootTable()
                                            .isValidSpawn(
                                                    (_,
                                                            _,
                                                            _,
                                                            _) -> false))
                    .bakedBy(() -> new BedrockOreModel.Family())
                    .andThen(block -> decoItem("ore_bedrock", block));

    public static final RegistryHandle<GeysirBlock> GEYSIR_NETHER =
            Reg.block(
                    "geysir_nether",
                    GeysirBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NETHER)
                                    .strength(2.0F, 2.0F)
                                    .lightLevel(s -> 15)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<GeysirBlock> GEYSIR_CHLORINE =
            Reg.block(
                    "geysir_chlorine",
                    GeysirBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 5.0F)
                                    .sound(SoundType.STONE)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<MeteorMoltenBlock> BLOCK_METEOR_MOLTEN =
            Reg.block(
                    "block_meteor_molten",
                    MeteorMoltenBlock::new,
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
    public static final RegistryHandle<BiomeStoneBlock> STONE_BIOME_DESERT =
            Reg.block(
                            "stone_biome_desert",
                            BiomeStoneBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.SAND)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("stone_biome_desert", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<BiomeStoneBlock> STONE_BIOME_WOODLAND =
            Reg.block(
                            "stone_biome_woodland",
                            BiomeStoneBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.PODZOL)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("stone_biome_woodland", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<HazardFallingBlock> BLOCK_YELLOWCAKE =
            Reg.block(
                            "block_yellowcake",
                            HazardFallingBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.SAND)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.SAND))
                    .andThen(
                            block -> {
                                var item = decoItem("block_yellowcake", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<MushHugeBlock> MUSH_BLOCK =
            Reg.blockItem(
                    "mush_block",
                    MushHugeBlock::new,
                    () -> MushHugeBlock.properties(MapColor.DIRT));
    public static final RegistryHandle<MushHugeBlock> MUSH_BLOCK_STEM =
            Reg.blockItem(
                    "mush_block_stem",
                    MushHugeBlock::new,
                    () -> MushHugeBlock.properties(MapColor.WOOL));
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
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));
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
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));
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
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));
    public static final RegistryHandle<GlyphidSpawnerBlock> GLYPHID_SPAWNER =
            Reg.blockItem(
                    "glyphid_spawner",
                    GlyphidSpawnerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));
    public static final RegistryHandle<GlyphidSpawnerBlock> GLYPHID_SPAWNER_INFESTED =
            Reg.blockItem(
                    "glyphid_spawner_infested",
                    GlyphidSpawnerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));
    public static final RegistryHandle<GlyphidSpawnerBlock> GLYPHID_SPAWNER_RAD =
            Reg.blockItem(
                    "glyphid_spawner_rad",
                    GlyphidSpawnerBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.5F, 0.5F)
                                    .sound(ModSoundTypes.FLESH)
                                    .pushReaction(((PushReaction) (PushReaction.DESTROY))));
    public static final RegistryHandle<TritiumLampBlock> LAMP_TRITIUM_GREEN =
            tritiumLamp("lamp_tritium_green");
    public static final RegistryHandle<TritiumLampBlock> LAMP_TRITIUM_BLUE =
            tritiumLamp("lamp_tritium_blue");
    public static final RegistryHandle<LanternBlock> LANTERN =
            Reg.blockItem(
                    "lantern",
                    LanternBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                                    .lightLevel(_ -> 15)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<LanternBehemothBlock> LANTERN_BEHEMOTH =
            Reg.blockItem(
                    "lantern_behemoth",
                    LanternBehemothBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<HangingVineBlock> VINE_PHOSPHOR =
            Reg.block(
                            "vine_phosphor",
                            HangingVineBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.PLANT)
                                            .strength(0.5F)
                                            .sound(SoundType.GRASS)
                                            .noCollision()
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .ignitedByLava()
                                            .pushReaction(((PushReaction) (PushReaction.DESTROY))))
                    .bakedBy(() -> new HangingVineModel.Family())
                    .andThen(
                            block -> {
                                var item = decoItem("vine_phosphor", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<StatueElbFBlock> STATUE_ELB_F =
            Reg.blockItem(
                    "statue_elb_f",
                    StatueElbFBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    .noOcclusion()
                                    .lightLevel(s -> 15)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<SellafieldOreBlock> ORE_SELLAFIELD_DIAMOND =
            sellafieldOre("ore_sellafield_diamond", "diamond", () -> Items.DIAMOND, 3, 7);
    public static final RegistryHandle<SellafieldOreBlock> ORE_SELLAFIELD_EMERALD =
            sellafieldOre("ore_sellafield_emerald", "emerald", () -> Items.EMERALD, 3, 7);
    public static final RegistryHandle<SellafieldOreBlock> ORE_SELLAFIELD_RADGEM =
            sellafieldOre("ore_sellafield_radgem", "radgem", () -> ModItems.GEM_RAD.get(), 3, 7);
    public static final RegistryHandle<SellafieldOreBlock> ORE_SELLAFIELD_SCHRABIDIUM =
            sellafieldOre(
                    "ore_sellafield_schrabidium",
                    "schrabidium",
                    () -> ModItems.RAW_SCHRABIDIUM.get(),
                    0,
                    0);
    public static final RegistryHandle<SellafieldOreBlock> ORE_SELLAFIELD_URANIUM_SCORCHED =
            sellafieldOre(
                    "ore_sellafield_uranium_scorched",
                    "uranium_scorched",
                    () -> ModItems.RAW_URANIUM.get(),
                    0,
                    0);
    public static final ClassicFluid.Pair SULFURIC_ACID_FLUID =
            Reg.classicFluid(
                    "sulfuric_acid_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.WATER,
                            8,
                            5,
                            1840,
                            true,
                            RegistryUtil.id("block/fluid/sulfuric_acid"),
                            () -> ModBlocks.SULFURIC_ACID_BLOCK.get(),
                            () -> FluidBucketItem.make(NTMFluids.SULFURIC_ACID)));
    public static final RegistryHandle<SulfuricAcidBlock> SULFURIC_ACID_BLOCK =
            Reg.block(
                    "sulfuric_acid_block",
                    props -> new SulfuricAcidBlock(SULFURIC_ACID_FLUID.source().get(), props),
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
                            RegistryUtil.id("block/fluid/acid"),
                            () -> ModBlocks.ACID_BLOCK.get(),
                            () -> new ItemStack(ModItems.BUCKET_ACID.get())));
    public static final RegistryHandle<AcidBlock> ACID_BLOCK =
            Reg.block(
                    "acid_block",
                    props -> new AcidBlock(ACID_FLUID.source().get(), props),
                    () ->
                            classicFluidProps(MapColor.COLOR_PURPLE)
                                    .randomTicks()
                                    .lightLevel(state -> ClassicFluidBlock.light(state, 4, 5)));
    public static final ClassicFluid.Pair TOXIC_FLUID =
            Reg.classicFluid(
                    "toxic_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.NONE,
                            4,
                            10,
                            2500,
                            false,
                            RegistryUtil.id("block/fluid/toxic"),
                            () -> ModBlocks.TOXIC_BLOCK.get(),
                            () -> new ItemStack(ModItems.BUCKET_TOXIC.get())));
    public static final RegistryHandle<ToxicBlock> TOXIC_BLOCK =
            Reg.block(
                    "toxic_block",
                    props -> new ToxicBlock(TOXIC_FLUID.source().get(), props),
                    () ->
                            classicFluidProps(MapColor.COLOR_GREEN)
                                    .lightLevel(state -> ClassicFluidBlock.light(state, 4, 15)));
    public static final ClassicFluid.Pair SCHRABIDIC_FLUID =
            Reg.classicFluid(
                    "schrabidic_fluid",
                    new ClassicFluid.Spec(
                            ClassicFluid.Physics.NONE,
                            4,
                            2,
                            31200,
                            false,
                            RegistryUtil.id("block/fluid/schrabidic_acid"),
                            () -> ModBlocks.SCHRABIDIC_BLOCK.get(),
                            () -> FluidBucketItem.make(NTMFluids.SCHRABIDIC)));
    public static final RegistryHandle<SchrabidicBlock> SCHRABIDIC_BLOCK =
            Reg.block(
                    "schrabidic_block",
                    props -> new SchrabidicBlock(SCHRABIDIC_FLUID.source().get(), props),
                    () -> classicFluidProps(MapColor.COLOR_CYAN));
    public static final RegistryHandle<StalactiteBlock> STALACTITE_SULFUR =
            Reg.block("stalactite_sulfur", StalactiteBlock::new, ModBlocks::spikeProps)
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("stalactite_sulfur", handle)));

    public static final RegistryHandle<StalactiteBlock> STALACTITE_ASBESTOS =
            Reg.block("stalactite_asbestos", StalactiteBlock::new, ModBlocks::spikeProps)
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("stalactite_asbestos", handle)));

    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_SULFUR =
            Reg.block("stalagmite_sulfur", StalagmiteBlock::new, ModBlocks::spikeProps)
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("stalagmite_sulfur", handle)));

    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_ASBESTOS =
            Reg.block("stalagmite_asbestos", StalagmiteBlock::new, ModBlocks::spikeProps)
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("stalagmite_asbestos", handle)));

    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_ICE =
            stalagmiteVariant("ice", 2, "hbm:powder_ice");
    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_SNOW =
            stalagmiteVariant("snow", 3, "minecraft:snowball");
    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_GLYPHID1 =
            stalagmiteVariant("glyphid1", 4, null);
    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_GLYPHID2 =
            stalagmiteVariant("glyphid2", 5, null);
    public static final RegistryHandle<StalagmiteBlock> STALAGMITE_GLYPHID3 =
            stalagmiteVariant("glyphid3", 6, null);
    public static final RegistryHandle<StalactiteBlock> STALACTITE_ICE =
            stalactiteVariant("ice", 2, "hbm:powder_ice");
    public static final RegistryHandle<StalactiteBlock> STALACTITE_SNOW =
            stalactiteVariant("snow", 3, "minecraft:snowball");
    public static final RegistryHandle<StalactiteBlock> STALACTITE_GLYPHID1 =
            stalactiteVariant("glyphid1", 4, null);
    public static final RegistryHandle<StalactiteBlock> STALACTITE_GLYPHID2 =
            stalactiteVariant("glyphid2", 5, null);
    public static final RegistryHandle<StalactiteBlock> STALACTITE_GLYPHID3 =
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
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<ToolConversionBlock> WATZ_END =
            Reg.blockItem(
                    "watz_end",
                    props ->
                            new ToolConversionBlock(
                                    props,
                                    ToolType.BOLT,
                                    () -> WATZ_END_BOLTED.get(),
                                    CountIngredient.of(MaterialTagRegistry.DURA.bolt(), 4)),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<WatzBlock> WATZ =
            Reg.blockItem(
                    "watz",
                    WatzBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<WatzPumpBlock> WATZ_PUMP =
            Reg.blockItem(
                    "watz_pump",
                    WatzPumpBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<WatzStructBlock> STRUCT_WATZ_CORE =
            Reg.blockItem(
                    "struct_watz_core",
                    WatzStructBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
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
                            RegistryUtil.id("block/fluid/mud"),
                            () -> ModBlocks.MUD_BLOCK.get(),
                            () -> FluidBucketItem.make(NTMFluids.WATZ_MUD)));
    public static final RegistryHandle<MudBlock> MUD_BLOCK =
            Reg.block(
                    "mud_block",
                    props -> new MudBlock(MUD_FLUID.source().get(), props),
                    () ->
                            classicFluidProps(MapColor.COLOR_ORANGE)
                                    .lightLevel(state -> ClassicFluidBlock.light(state, 4, 5)));
    public static final RegistryHandle<Block> BLOCK_EUPHEMIUM =
            deco("block_euphemium", 5.0F, 36000.0F, SoundType.METAL, MapColor.METAL);
    public static final RegistryHandle<TransparentBlock> GLASS_QUARTZ =
            Reg.block(
                            "glass_quartz",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.ICE)
                                                    .strength(1.0F, 24.0F)
                                                    .sound(SoundType.GLASS)
                                                    .noOcclusion()))
                    .andThen(block -> DECO_BLOCKS.add(decoItem("glass_quartz", block)));

    public static final RegistryHandle<TransparentBlock> GLASS_BORON =
            Reg.block(
                            "glass_boron",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.NONE)
                                                    .strength(0.3F)
                                                    .sound(SoundType.GLASS)
                                                    .noOcclusion()
                                                    .instrument(NoteBlockInstrument.HAT)))
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(block -> STRUCTURAL_BLOCKS.add(decoItem("glass_boron", block)));

    public static final RegistryHandle<TransparentBlock> GLASS_LEAD =
            Reg.block(
                            "glass_lead",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.NONE)
                                                    .strength(0.3F)
                                                    .sound(SoundType.GLASS)
                                                    .noOcclusion()
                                                    .instrument(NoteBlockInstrument.HAT)))
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(block -> STRUCTURAL_BLOCKS.add(decoItem("glass_lead", block)));

    public static final RegistryHandle<TransparentBlock> GLASS_URANIUM =
            litGlass("glass_uranium", DyeColor.YELLOW);
    public static final RegistryHandle<TransparentBlock> GLASS_TRINITITE =
            Reg.block(
                            "glass_trinitite",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.NONE)
                                                    .strength(0.3F)
                                                    .sound(SoundType.GLASS)
                                                    .noOcclusion()
                                                    .instrument(NoteBlockInstrument.HAT)
                                                    .lightLevel(_ -> 5)))
                    .andThen(block -> STRUCTURAL_BLOCKS.add(decoItem("glass_trinitite", block)));

    public static final RegistryHandle<TransparentBlock> GLASS_POLONIUM =
            litGlass("glass_polonium", DyeColor.RED);
    public static final RegistryHandle<TransparentBlock> GLASS_ASH =
            Reg.block(
                            "glass_ash",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.NONE)
                                                    .strength(3.0F)
                                                    .sound(SoundType.GLASS)
                                                    .noOcclusion()
                                                    .instrument(NoteBlockInstrument.HAT)))
                    .andThen(block -> STRUCTURAL_BLOCKS.add(decoItem("glass_ash", block)));

    public static final RegistryHandle<TransparentBlock> GLASS_POLARIZED =
            Reg.<TransparentBlock>block(
                            "glass_polarized",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.NONE)
                                                    .strength(0.3F)
                                                    .sound(SoundType.GLASS)
                                                    .noOcclusion()
                                                    .instrument(NoteBlockInstrument.HAT)))
                    .andThen(block -> STRUCTURAL_BLOCKS.add(decoItem("glass_polarized", block)));

    public static final RegistryHandle<TransparentBlock> REINFORCED_GLASS =
            Reg.block(
                            "reinforced_glass",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                                    BlockBehaviour.Properties.of()
                                                            .mapColor(MapColor.STONE)
                                                            .strength(2.0F, 15.0F)
                                                            .sound(SoundType.STONE)
                                                            .noOcclusion())
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(block -> DECO_BLOCKS.add(blastItem("reinforced_glass", block)));

    public static final RegistryHandle<ReinforcedGlassPaneBlock> REINFORCED_GLASS_PANE =
            Reg.block(
                            "reinforced_glass_pane",
                            ReinforcedGlassPaneBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(2.0F, 15.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(block -> DECO_BLOCKS.add(blastItem("reinforced_glass_pane", block)));

    public static final RegistryHandle<TransparentBlock> REINFORCED_LAMINATE =
            Reg.block(
                            "reinforced_laminate",
                            TransparentBlock::new,
                            () ->
                                    nonBlockingGlass(
                                                    BlockBehaviour.Properties.of()
                                                            .mapColor(MapColor.STONE)
                                                            .strength(15.0F, 180.0F)
                                                            .sound(SoundType.STONE)
                                                            .noOcclusion())
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(block -> DECO_BLOCKS.add(blastItem("reinforced_laminate", block)));

    public static final RegistryHandle<ReinforcedGlassPaneBlock> REINFORCED_LAMINATE_PANE =
            Reg.block(
                            "reinforced_laminate_pane",
                            ReinforcedGlassPaneBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(15.0F, 180.0F)
                                            .sound(SoundType.STONE)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .afterRegistration(RadiationSystemNT::markRadResistant)
                    .andThen(
                            block -> DECO_BLOCKS.add(blastItem("reinforced_laminate_pane", block)));

    public static final RegistryHandle<LadderBlock> LADDER_STURDY = ladder("ladder_sturdy");

    public static final RegistryHandle<LadderBlock> LADDER_GOLD = ladder("ladder_gold");

    public static final RegistryHandle<LadderBlock> LADDER_TITANIUM = ladder("ladder_titanium");

    public static final RegistryHandle<LadderBlock> LADDER_COPPER = ladder("ladder_copper");

    public static final RegistryHandle<LadderBlock> LADDER_STEEL = ladder("ladder_steel");

    public static final RegistryHandle<TrapDoorBlock> TRAPDOOR_STEEL =
            Reg.block(
                            "trapdoor_steel",
                            properties -> new TrapDoorBlock(ModBlockSetTypes.HBM_METAL, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(3.0F, 4.8F)
                                            .sound(SoundType.METAL)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("trapdoor_steel", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<DoorBlock> DOOR_METAL =
            Reg.block(
                            "door_metal",
                            properties -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 3.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(((PushReaction) (PushReaction.DESTROY))))
                    .andThen(
                            block -> {
                                var item = decoItem("door_metal", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<KeyholeBlock> STONE_KEYHOLE =
            Reg.blockItem(
                    "stone_keyhole",
                    KeyholeBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(0.0F, 0.0F));
    public static final RegistryHandle<RedBrickBlock> BRICK_RED =
            Reg.blockItem(
                    "brick_red",
                    RedBrickBlock::new,
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                                    .strength(0.0F, 6000.0F));
    public static final RegistryHandle<RedBrickKeyholeBlock> STONE_KEYHOLE_META =
            Reg.blockItem(
                    "stone_keyhole_meta",
                    RedBrickKeyholeBlock::new,
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
                                            .pushReaction(((PushReaction) (PushReaction.DESTROY))))
                    .andThen(h -> decoItem("door_red", h));
    public static final RegistryHandle<DoorBlock> DOOR_OFFICE =
            Reg.block(
                            "door_office",
                            properties -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(10.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(((PushReaction) (PushReaction.DESTROY))))
                    .andThen(
                            block -> {
                                var item = decoItem("door_office", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<DoorBlock> DOOR_BUNKER =
            Reg.block(
                            "door_bunker",
                            properties -> new DoorBlock(ModBlockSetTypes.HBM_DOOR, properties),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(10.0F, 60.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops()
                                            .pushReaction(((PushReaction) (PushReaction.DESTROY))))
                    .andThen(
                            block -> {
                                var item = decoItem("door_bunker", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SteelScaffoldBlock> STEEL_SCAFFOLD =
            scaffold("steel_scaffold", "block/scaffold_steel", 0);

    public static final RegistryHandle<SteelScaffoldBlock> STEEL_SCAFFOLD_RED =
            scaffold("steel_scaffold_red", "block/scaffold_red", 1);

    public static final RegistryHandle<SteelScaffoldBlock> STEEL_SCAFFOLD_WHITE =
            scaffold("steel_scaffold_white", "block/scaffold_white", 2);

    public static final RegistryHandle<SteelScaffoldBlock> STEEL_SCAFFOLD_YELLOW =
            scaffold("steel_scaffold_yellow", "block/scaffold_yellow", 3);

    public static final RegistryHandle<SteelBeamBlock> STEEL_BEAM =
            Reg.block(
                            "steel_beam",
                            SteelBeamBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("steel_beam", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SteelWallBlock> STEEL_WALL =
            Reg.block(
                            "steel_wall",
                            SteelWallBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("steel_wall", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SteelCornerBlock> STEEL_CORNER =
            Reg.block(
                            "steel_corner",
                            SteelCornerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(15.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("steel_corner", h)));
    public static final RegistryHandle<SteelRoofBlock> STEEL_ROOF =
            Reg.block(
                            "steel_roof",
                            SteelRoofBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("steel_roof", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SteelPolesBlock> STEEL_POLES =
            Reg.block(
                            "steel_poles",
                            SteelPolesBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("steel_poles", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<PoleTopBlock> POLE_TOP =
            Reg.block(
                            "pole_top",
                            PoleTopBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 9.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("pole_top", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<MetalFenceBlock> FENCE_METAL =
            Reg.block(
                            "fence_metal",
                            MetalFenceBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(15.0F, 0.15F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("fence_metal", block);
                                STRUCTURAL_BLOCKS.add(item);
                            });

    public static final RegistryHandle<MetalFenceBlock> FENCE_METAL_POST =
            Reg.block(
                            "fence_metal_post",
                            MetalFenceBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(15.0F, 0.15F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("fence_metal_post", block);
                                STRUCTURAL_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SteelGrateBlock> STEEL_GRATE =
            Reg.block(
                            "steel_grate",
                            properties -> new SteelGrateBlock(properties, false),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SteelGrateBlock.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = grateItem("steel_grate", block, false);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SteelGrateBlock> STEEL_GRATE_WIDE =
            Reg.block(
                            "steel_grate_wide",
                            properties -> new SteelGrateBlock(properties, true),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SteelGrateBlock.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = grateItem("steel_grate_wide", block, true);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<RailGenericBlock> RAIL_WOOD =
            Reg.block(
                            "rail_wood",
                            properties -> new RailGenericBlock(properties, 0.2F),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(
                            block -> {
                                var item = railItem("rail_wood", block, 1);
                                RAIL_BLOCKS.add(item);
                            });

    public static final RegistryHandle<RailBlock> RAIL_NARROW =
            Reg.block(
                            "rail_narrow",
                            RailBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(
                            block -> {
                                var item = decoItem("rail_narrow", block);
                                RAIL_BLOCKS.add(item);
                            });

    public static final RegistryHandle<RailStraightBlock> RAIL_HIGHSPEED =
            Reg.block(
                            "rail_highspeed",
                            properties -> new RailStraightBlock(properties, 1.0F),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(
                            block -> {
                                var item = railItem("rail_highspeed", block, 2);
                                RAIL_BLOCKS.add(item);
                            });

    public static final RegistryHandle<RailBoosterBlock> RAIL_BOOSTER =
            Reg.block(
                            "rail_booster",
                            properties -> new RailBoosterBlock(properties, 1.0F),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.NONE)
                                            .strength(5.0F, 6.0F)
                                            .noCollision()
                                            .sound(SoundType.STONE))
                    .andThen(
                            block -> {
                                var item = railItem("rail_booster", block, 2);
                                RAIL_BLOCKS.add(item);
                            });

    public static final RegistryHandle<SpikesBlock> SPIKES =
            Reg.block(
                            "spikes",
                            SpikesBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.5F, 3.0F)
                                            .noOcclusion()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops())
                    .andThen(
                            block -> {
                                var item = decoItem("spikes", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<ChainBlock> DUNGEON_CHAIN =
            Reg.block(
                            "dungeon_chain",
                            ChainBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(0.25F, 1.2F)
                                            .noOcclusion()
                                            .noCollision()
                                            .forceSolidOn()
                                            .requiresCorrectToolForDrops())
                    .bakedBy(() -> new ChainModel())
                    .andThen(
                            block -> {
                                var item = decoItem("dungeon_chain", block);
                                DECO_BLOCKS.add(item);
                            });

    public static final RegistryHandle<DecoComputerBlock> DECO_COMPUTER =
            Reg.block(
                            "deco_computer",
                            DecoComputerBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5F, 6F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("deco_computer", handle)));

    public static final RegistryHandle<DecoCRTBlock> DECO_CRT_CLEAN =
            Reg.block(
                            "deco_crt_clean",
                            DecoCRTBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5F, 6F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("deco_crt_clean", handle)));

    public static final RegistryHandle<DecoCRTBlock> DECO_CRT_BROKEN =
            Reg.block(
                            "deco_crt_broken",
                            DecoCRTBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5F, 6F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("deco_crt_broken", handle)));

    public static final RegistryHandle<DecoCRTBlock> DECO_CRT_BLINKING =
            Reg.block(
                            "deco_crt_blinking",
                            DecoCRTBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5F, 6F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("deco_crt_blinking", handle)));

    public static final RegistryHandle<DecoCRTBlock> DECO_CRT_BSOD =
            Reg.block(
                            "deco_crt_bsod",
                            DecoCRTBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5F, 6F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(handle -> DECO_BLOCKS.add(decoItem("deco_crt_bsod", handle)));

    public static final RegistryHandle<DecoToasterBlock> DECO_TOASTER_IRON =
            toaster("deco_toaster_iron", "block/toaster_iron", 0);

    public static final RegistryHandle<DecoToasterBlock> DECO_TOASTER_STEEL =
            toaster("deco_toaster_steel", "block/toaster_steel", 1);

    public static final RegistryHandle<DecoToasterBlock> DECO_TOASTER_WOOD =
            toaster("deco_toaster_wood", "block/toaster_wood", 2);

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE =
            Reg.block(
                            "deco_pipe",
                            DecoPipeBlock.Kind.PLAIN::create,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(2.0F, 3.0F)
                                            .sound(SteelGrateBlock.SOUND)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops())
                    .andThen(h -> DECO_BLOCKS.add(pipeItem("deco_pipe", h)))
                    .bakedBy(() -> new PipeModel(DecoPipeBlock.Kind.PLAIN));
    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RUSTED =
            decoPipe("deco_pipe_rusted", DecoPipeBlock.Kind.PLAIN, "_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_GREEN =
            decoPipe("deco_pipe_green", DecoPipeBlock.Kind.PLAIN, "_green");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_GREEN_RUSTED =
            decoPipe("deco_pipe_green_rusted", DecoPipeBlock.Kind.PLAIN, "_green_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RED =
            decoPipe("deco_pipe_red", DecoPipeBlock.Kind.PLAIN, "_red");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_MARKED =
            decoPipe("deco_pipe_marked", DecoPipeBlock.Kind.PLAIN, "_marked");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RIM_RUSTED =
            decoPipe("deco_pipe_rim_rusted", DecoPipeBlock.Kind.RIM, "_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RIM_GREEN =
            decoPipe("deco_pipe_rim_green", DecoPipeBlock.Kind.RIM, "_green");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RIM =
            decoPipe("deco_pipe_rim", DecoPipeBlock.Kind.RIM, "");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RIM_GREEN_RUSTED =
            decoPipe("deco_pipe_rim_green_rusted", DecoPipeBlock.Kind.RIM, "_green_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RIM_RED =
            decoPipe("deco_pipe_rim_red", DecoPipeBlock.Kind.RIM, "_red");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_RIM_MARKED =
            decoPipe("deco_pipe_rim_marked", DecoPipeBlock.Kind.RIM, "_marked");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_QUAD =
            decoPipe("deco_pipe_quad", DecoPipeBlock.Kind.QUAD, "");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_QUAD_RUSTED =
            decoPipe("deco_pipe_quad_rusted", DecoPipeBlock.Kind.QUAD, "_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_QUAD_GREEN =
            decoPipe("deco_pipe_quad_green", DecoPipeBlock.Kind.QUAD, "_green");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_QUAD_GREEN_RUSTED =
            decoPipe("deco_pipe_quad_green_rusted", DecoPipeBlock.Kind.QUAD, "_green_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_QUAD_RED =
            decoPipe("deco_pipe_quad_red", DecoPipeBlock.Kind.QUAD, "_red");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_QUAD_MARKED =
            decoPipe("deco_pipe_quad_marked", DecoPipeBlock.Kind.QUAD, "_marked");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_FRAMED =
            decoPipe("deco_pipe_framed", DecoPipeBlock.Kind.FRAMED, "");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_FRAMED_RUSTED =
            decoPipe("deco_pipe_framed_rusted", DecoPipeBlock.Kind.FRAMED, "_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_FRAMED_GREEN =
            decoPipe("deco_pipe_framed_green", DecoPipeBlock.Kind.FRAMED, "_green");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_FRAMED_GREEN_RUSTED =
            decoPipe("deco_pipe_framed_green_rusted", DecoPipeBlock.Kind.FRAMED, "_green_rusty");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_FRAMED_RED =
            decoPipe("deco_pipe_framed_red", DecoPipeBlock.Kind.FRAMED, "_red");

    public static final RegistryHandle<DecoPipeBlock> DECO_PIPE_FRAMED_MARKED =
            decoPipe("deco_pipe_framed_marked", DecoPipeBlock.Kind.FRAMED, "_marked");

    public static final RegistryHandle<GuideBlock> BOOK_GUIDE =
            Reg.blockItem(
                    "book_guide",
                    GuideBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<CrateBlock> CRATE_IRON =
            crate("crate_iron", CrateType.IRON, 5.0F, 6.0F);
    public static final RegistryHandle<CrateBlock> CRATE_STEEL =
            crate("crate_steel", CrateType.STEEL, 5.0F, 6.0F);
    public static final RegistryHandle<CrateBlock> CRATE_DESH =
            crate("crate_desh", CrateType.DESH, 5.0F, 6.0F);
    public static final RegistryHandle<CrateBlock> CRATE_TUNGSTEN =
            crate("crate_tungsten", TungstenCrateBlock::new, 7.5F, 180.0F)
                    .afterRegistration(RadiationSystemNT::markRadResistant);
    public static final RegistryHandle<CrateBlock> SAFE =
            crate("safe", SafeBlock::new, 7.5F, 6000.0F);
    public static final RegistryHandle<MassStorageBlock> MASS_STORAGE_WOOD =
            massStorage("mass_storage_wood", 100, "_wood");
    public static final RegistryHandle<MassStorageBlock> MASS_STORAGE_IRON =
            massStorage("mass_storage_iron", 10_000, "_iron");
    public static final RegistryHandle<MassStorageBlock> MASS_STORAGE_DESH =
            massStorage("mass_storage_desh", 100_000, "_desh");
    public static final RegistryHandle<MassStorageBlock> MASS_STORAGE_TCALLOY =
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
    public static final RegistryHandle<OilSpillBlock> OIL_SPILL =
            Reg.block("oil_spill", OilSpillBlock::new, OilSpillBlock::defaultProperties)
                    .andThen(h -> DECO_BLOCKS.add(decoItem("oil_spill", h)));
    public static final RegistryHandle<OilSpillBlock> LEAVES_LAYER =
            Reg.block(
                            "leaves_layer",
                            OilSpillBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.PLANT)
                                            .strength(0.1F)
                                            .sound(SoundType.GRASS)
                                            .noOcclusion()
                                            .noLootTable()
                                            .ignitedByLava()
                                            .pushReaction(((PushReaction) (PushReaction.DESTROY)))
                                            .replaceable())
                    .andThen(h -> DECO_BLOCKS.add(decoItem("leaves_layer", h)));
    public static final RegistryHandle<Block> PLANT_DEAD = deadPlant("plant_dead");
    public static final RegistryHandle<Block> PLANT_DEAD_GRASS = deadPlant("plant_dead_grass");
    public static final RegistryHandle<Block> PLANT_DEAD_FLOWER = deadPlant("plant_dead_flower");
    public static final RegistryHandle<Block> PLANT_DEAD_BIGFLOWER =
            deadPlant("plant_dead_bigflower");
    public static final RegistryHandle<Block> PLANT_DEAD_FERN = deadPlant("plant_dead_fern");
    public static final RegistryHandle<NtmFlowerBlock> PLANT_FLOWER_FOXGLOVE =
            flower("plant_flower_foxglove", NtmFlowerBlock.Variant.FOXGLOVE);
    public static final RegistryHandle<NtmFlowerBlock> PLANT_FLOWER_TOBACCO =
            tintedFlower("plant_flower_tobacco", NtmFlowerBlock.Variant.TOBACCO);
    public static final RegistryHandle<NtmFlowerBlock> PLANT_FLOWER_NIGHTSHADE =
            flower("plant_flower_nightshade", NtmFlowerBlock.Variant.NIGHTSHADE);
    public static final RegistryHandle<NtmFlowerBlock> PLANT_FLOWER_WEED =
            tintedFlower("plant_flower_weed", NtmFlowerBlock.Variant.WEED);
    public static final RegistryHandle<NtmFlowerBlock> PLANT_FLOWER_CD0 =
            flower("plant_flower_cd0", NtmFlowerBlock.Variant.CD0);
    public static final RegistryHandle<NtmFlowerBlock> PLANT_FLOWER_CD1 =
            flower("plant_flower_cd1", NtmFlowerBlock.Variant.CD1);
    public static final RegistryHandle<CustomMachineBlock> CUSTOM_MACHINE = customMachine();
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
    public static final RegistryHandle<CMTankBlock> CM_TANK_STEEL = cmTank("cm_tank_steel");
    public static final RegistryHandle<CMTankBlock> CM_TANK_ALLOY = cmTank("cm_tank_alloy");
    public static final RegistryHandle<CMTankBlock> CM_TANK_DESH = cmTank("cm_tank_desh");
    public static final RegistryHandle<CMTankBlock> CM_TANK_TCALLOY = cmTank("cm_tank_tcalloy");
    public static final RegistryHandle<Block> CM_CIRCUIT_ALUMINIUM = cmCube("cm_circuit_aluminium");
    public static final RegistryHandle<Block> CM_CIRCUIT_COPPER = cmCube("cm_circuit_copper");
    public static final RegistryHandle<Block> CM_CIRCUIT_RED_COPPER =
            cmCube("cm_circuit_red_copper");
    public static final RegistryHandle<Block> CM_CIRCUIT_GOLD = cmCube("cm_circuit_gold");
    public static final RegistryHandle<Block> CM_CIRCUIT_SCHRABIDIUM =
            cmCube("cm_circuit_schrabidium");
    public static final RegistryHandle<CMPortBlock> CM_PORT_STEEL = cmPort("cm_port_steel");
    public static final RegistryHandle<CMPortBlock> CM_PORT_ALLOY = cmPort("cm_port_alloy");
    public static final RegistryHandle<CMPortBlock> CM_PORT_DESH = cmPort("cm_port_desh");
    public static final RegistryHandle<CMPortBlock> CM_PORT_TCALLOY = cmPort("cm_port_tcalloy");
    public static final RegistryHandle<Block> CM_FLUX = cmPillar("cm_flux");
    public static final RegistryHandle<Block> CM_HEAT = cmPillar("cm_heat");
    public static final RegistryHandle<Block> CUSTOM_MACHINE_ANCHOR = cmAnchor();
    public static final RegistryHandle<ReedsBlock> PLANT_REEDS = reeds("plant_reeds");
    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_WEED =
            tintedTallPlant(
                    "plant_tall_weed", BlockTallPlant.Variant.WEED, "hbm:plant_flower_weed");
    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_CD2 =
            tallPlant("plant_tall_cd2", BlockTallPlant.Variant.CD2, "hbm:plant_flower_cd0");
    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_CD3 =
            tallPlant("plant_tall_cd3", BlockTallPlant.Variant.CD3, "hbm:plant_flower_cd0");
    public static final RegistryHandle<BlockTallPlant> PLANT_TALL_CD4 =
            tallPlant("plant_tall_cd4", BlockTallPlant.Variant.CD4, "hbm:plant_flower_cd0");
    public static final RegistryHandle<GraphiteDrilledBlock> BLOCK_GRAPHITE_DRILLED =
            pile("block_graphite_drilled", GraphiteDrilledBlock::new);
    public static final RegistryHandle<GraphiteFuelBlock> BLOCK_GRAPHITE_FUEL =
            pile("block_graphite_fuel", GraphiteFuelBlock::new);
    public static final RegistryHandle<GraphiteSourceBlock> BLOCK_GRAPHITE_PLUTONIUM =
            pile(
                    "block_graphite_plutonium",
                    props ->
                            new GraphiteSourceBlock(
                                    props, () -> ModItems.PILE_ROD_PLUTONIUM.get()));
    public static final RegistryHandle<GraphiteSourceBlock> BLOCK_GRAPHITE_SOURCE =
            pile(
                    "block_graphite_source",
                    props -> new GraphiteSourceBlock(props, () -> ModItems.PILE_ROD_SOURCE.get()));
    public static final RegistryHandle<GraphiteRodBlock> BLOCK_GRAPHITE_ROD =
            pile("block_graphite_rod", GraphiteRodBlock::new);
    public static final RegistryHandle<GraphiteBreedingFuelBlock> BLOCK_GRAPHITE_LITHIUM =
            pile("block_graphite_lithium", GraphiteBreedingFuelBlock::new);
    public static final RegistryHandle<GraphiteBreedingProductBlock> BLOCK_GRAPHITE_TRITIUM =
            pile("block_graphite_tritium", GraphiteBreedingProductBlock::new);
    public static final RegistryHandle<GraphiteNeutronDetectorBlock> BLOCK_GRAPHITE_DETECTOR =
            pile("block_graphite_detector", GraphiteNeutronDetectorBlock::new);
    public static final RegistryHandle<PileBrickBlock> PILE_BRICK =
            Reg.blockItem(
                    "pile_brick",
                    PileBrickBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PileBlock> PILE_BLOCK =
            Reg.blockItem(
                    "pile_block",
                    PileBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(15.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noLootTable()
                                    .requiresCorrectToolForDrops()
                                    .pushReaction(((PushReaction) (PushReaction.BLOCK))));

    public static final RegistryHandle<CargoElevatorBlock> CARGO_ELEVATOR =
            Reg.blockItem(
                    "cargo_elevator",
                    CargoElevatorBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .dynamicShape()
                                    .forceSolidOn()
                                    .requiresCorrectToolForDrops());

    public static final RegistryHandle<VendingMachineBlock> VENDING_MACHINE =
            Reg.blockItem(
                    "vending_machine",
                    props -> new VendingMachineBlock(props, ItemPoolIds.POOL_SODA),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<VendingMachineBlock> VENDING_MACHINE_SNACKS =
            Reg.blockItem(
                    "vending_machine_snacks",
                    props -> new VendingMachineBlock(props, ItemPoolIds.POOL_SNACKS),
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops());
    public static final RegistryHandle<PileDeviceBlock> PILE_LOADER =
            pileDevice(
                    "pile_loader",
                    props -> new PileDeviceBlock(props, PileDeviceBlock.Kind.LOADER));
    public static final RegistryHandle<PileVentBlock> PILE_VENT =
            pileDevice("pile_vent", PileVentBlock::new);
    public static final RegistryHandle<PileDeviceBlock> PILE_CONTROL =
            pileDevice(
                    "pile_control",
                    props -> new PileDeviceBlock(props, PileDeviceBlock.Kind.CONTROL));
    public static final RegistryHandle<FanBlock> FAN =
            Reg.blockItem(
                            "fan",
                            FanBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new FanModel());
    public static final RegistryHandle<PistonInserterBlock> PISTON_INSERTER =
            Reg.blockItem(
                            "piston_inserter",
                            PistonInserterBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .noOcclusion()
                                            .requiresCorrectToolForDrops(),
                            (block, props) -> new DescBlockItem(block, props, 4))
                    .bakedBy(() -> new PistonInserterModel());
    public static final RegistryHandle<FusionTorusBlock> FUSION_TORUS =
            fusionMachine("fusion_torus", FusionTorusBlock::new, 4);

    public static final RegistryHandle<FusionKlystronBlock> FUSION_KLYSTRON =
            fusionMachine("fusion_klystron", FusionKlystronBlock::new, 2);

    public static final RegistryHandle<CreativeFusionKlystronBlock> FUSION_KLYSTRON_CREATIVE =
            fusionMachine("fusion_klystron_creative", CreativeFusionKlystronBlock::new, 1);

    public static final RegistryHandle<FusionBreederBlock> FUSION_BREEDER =
            fusionMachine("fusion_breeder", FusionBreederBlock::new, 3);

    public static final RegistryHandle<FusionCollectorBlock> FUSION_COLLECTOR =
            fusionMachine("fusion_collector", FusionCollectorBlock::new, 2);

    public static final RegistryHandle<FusionBoilerBlock> FUSION_BOILER =
            fusionMachine("fusion_boiler", FusionBoilerBlock::new, 2);

    public static final RegistryHandle<FusionMHDTBlock> FUSION_MHDT =
            fusionMachine("fusion_mhdt", FusionMHDTBlock::new, 4);

    public static final RegistryHandle<FusionCouplerBlock> FUSION_COUPLER =
            fusionMachine("fusion_coupler", FusionCouplerBlock::new, 3);

    public static final RegistryHandle<FusionPlasmaForgeBlock> FUSION_PLASMA_FORGE =
            fusionMachine("fusion_plasma_forge", FusionPlasmaForgeBlock::new, 0);

    public static final RegistryHandle<Block> FUSION_COMPONENT_BSCCO_WELDED =
            fusionComponent("fusion_component_bscco_welded", Block::new);
    public static final RegistryHandle<Block> FUSION_COMPONENT_BLANKET =
            fusionComponent("fusion_component_blanket", Block::new);
    public static final RegistryHandle<Block> FUSION_COMPONENT_MOTOR =
            fusionComponent("fusion_component_motor", Block::new);
    public static final RegistryHandle<ToolConversionBlock> FUSION_COMPONENT =
            fusionComponent(
                    "fusion_component",
                    props ->
                            new ToolConversionBlock(
                                    props,
                                    ToolType.TORCH,
                                    () -> FUSION_COMPONENT_BSCCO_WELDED.get(),
                                    CountIngredient.of(MaterialTagRegistry.STEEL.plateCast(), 1)));
    public static final RegistryHandle<FusionTorusStructBlock> STRUCT_TORUS_CORE =
            Reg.blockItem(
                    "struct_torus_core",
                    FusionTorusStructBlock::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5F, 6F)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(_ -> 15)
                                    .noOcclusion());

    private static final String[] SELLAFIELD_STAGES = {
        "block/sellafield_slaked", "block/sellafield_slaked_1",
        "block/sellafield_slaked_2", "block/sellafield_slaked_3"
    };
    public static RegistryHandle<Block> CMB_BRICK_REINFORCED;
    public static RegistryHandle<Block> BLOCK_GRAPHITE;
    public static RegistryHandle<Block> STONE_POROUS;
    public static RegistryHandle<Block> STONE_GNEISS;
    public static RegistryHandle<Block> ORE_URANIUM;
    public static RegistryHandle<Block> ORE_URANIUM_DEEPSLATE;
    public static RegistryHandle<Block> CONCRETE;
    public static RegistryHandle<Block> CONCRETE_SMOOTH;
    public static RegistryHandle<Block> CONCRETE_ASBESTOS;
    public static RegistryHandle<Block> CONCRETE_REBAR;
    public static final List<RegistryHandle<Block>> CONCRETE_COLORED = new ArrayList<>(16);
    public static RegistryHandle<RebarBlock> REBAR;
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
    public static RegistryHandle<PlatemetalBlock> PLATEMETAL;
    public static RegistryHandle<Block> WASTE_PLANKS;
    public static RegistryHandle<FrozenEarthBlock> FROZEN_DIRT;
    public static RegistryHandle<FrozenEarthBlock> FROZEN_GRASS;
    public static RegistryHandle<Block> FROZEN_LOG;
    public static RegistryHandle<Block> FROZEN_PLANKS;
    public static RegistryHandle<Block> BLOCK_LEAD;
    public static RegistryHandle<Block> BLOCK_BERYLLIUM;
    public static RegistryHandle<Block> BLOCK_DESH;
    public static RegistryHandle<Block> ORE_GNEISS_GAS;
    public static RegistryHandle<OreBasaltBlock> ORE_BASALT_ASBESTOS;
    public static RegistryHandle<Block> ORE_BASALT_GEM;
    public static RegistryHandle<HazardFallingBlock> BLOCK_FALLOUT;
    public static RegistryHandle<Block> BLOCK_COKE_COAL;
    public static RegistryHandle<Block> BLOCK_COKE_LIGNITE;
    public static RegistryHandle<Block> BLOCK_COKE_PETROLEUM;
    public static RegistryHandle<Block> BLOCK_SEMTEX;
    public static RegistryHandle<Block> BLOCK_C4;
    public static RegistryHandle<Block> STONE_RESOURCE_HEMATITE;
    public static RegistryHandle<Block> STONE_RESOURCE_BAUXITE;
    public static RegistryHandle<Block> STONE_RESOURCE_MALACHITE;
    public static RegistryHandle<Block> STONE_RESOURCE_LIMESTONE;
    public static RegistryHandle<DepthBlock> STONE_DEPTH;
    public static RegistryHandle<DepthBlock> STONE_DEPTH_NETHER;
    public static RegistryHandle<DepthBlock> CLUSTER_DEPTH_IRON;
    public static RegistryHandle<DepthBlock> CLUSTER_DEPTH_TITANIUM;
    public static RegistryHandle<DepthBlock> CLUSTER_DEPTH_TUNGSTEN;
    public static RegistryHandle<DepthBlock> ORE_DEPTH_CINNABAR;
    public static RegistryHandle<DepthBlock> ORE_DEPTH_ZIRCONIUM;
    public static RegistryHandle<DepthBlock> ORE_DEPTH_BORAX;
    public static RegistryHandle<DepthBlock> ORE_DEPTH_NETHER_NEODYMIUM;
    public static RegistryHandle<Block> CLUSTER_IRON;
    public static RegistryHandle<Block> CLUSTER_TITANIUM;
    public static RegistryHandle<Block> CLUSTER_ALUMINIUM;
    public static RegistryHandle<Block> CLUSTER_COPPER;
    public static RegistryHandle<DepthBlock> ORE_ALEXANDRITE;
    public static RegistryHandle<Block> BLOCK_METEOR;
    public static RegistryHandle<Block> BLOCK_METEOR_COBBLE;
    public static RegistryHandle<Block> BLOCK_METEOR_BROKEN;
    public static RegistryHandle<Block> BLOCK_METEOR_TREASURE;
    public static RegistryHandle<Block> ORE_OIL;
    public static RegistryHandle<Block> ORE_OIL_EMPTY;
    public static RegistryHandle<Block> ORE_BEDROCK_OIL;
    public static RegistryHandle<OreNetherFireBlock> ORE_NETHER_FIRE;
    public static RegistryHandle<Block> ORE_NETHER_COAL;
    public static RegistryHandle<Block> ORE_TIKITE;
    public static RegistryHandle<Block> ORE_AUSTRALIUM;
    public static RegistryHandle<RotatedPillarBlock> METEOR_PILLAR;
    public static RegistryHandle<Block> NTM_DIRT;
    public static RegistryHandle<NukeCustomBlock> NUKE_CUSTOM;
    public static RegistryHandle<NukeBalefireBlock> NUKE_FSTBMB;
    public static RegistryHandle<BombMultiBlock> BOMB_MULTI;
    public static final RegistryHandle<FireworksBlock> FIREWORKS;
    public static final RegistryHandle<DynamiteBlock> DYNAMITE;
    public static final RegistryHandle<TNTBlock> TNT_NTM;
    public static final RegistryHandle<SemtexBlock> SEMTEX;
    public static final RegistryHandle<C4Block> C4;
    public static final RegistryHandle<FissureBombBlock> FISSURE_BOMB;
    public static final RegistryHandle<DetChargeBlock> DET_CHARGE;
    public static final RegistryHandle<DetCordBlock> DET_CORD;
    public static final RegistryHandle<DetNukeBlock> DET_NUKE;
    public static final RegistryHandle<DetMinerBlock> DET_MINER;
    public static final RegistryHandle<BombFlameWarBlock> FLAME_WAR;
    public static final RegistryHandle<EMPBombBlock> EMP_BOMB;
    public static final RegistryHandle<FloatBombBlock> FLOAT_BOMB;
    public static final RegistryHandle<ThermoBombBlock.Endothermic> THERM_ENDO;
    public static final RegistryHandle<ThermoBombBlock.Exothermic> THERM_EXO;
    public static final RegistryHandle<TaintBlock> TAINT;
    public static final RegistryHandle<BalefireBlock> BALEFIRE;
    public static RegistryHandle<Block> CRYSTAL_HARDENED;
    public static RegistryHandle<CrystalVirusBlock> CRYSTAL_VIRUS;
    public static RegistryHandle<CrystalPulsarBlock> CRYSTAL_PULSAR;
    public static RegistryHandle<Block> BLOCK_SCHRABIDIUM_CLUSTER;
    public static RegistryHandle<Block> BLOCK_EUPHEMIUM_CLUSTER;
    public static RegistryHandle<Block> ORE_TEKTITE_OSMIRIDIUM;
    public static RegistryHandle<Block> TEKTITE;
    public static RegistryHandle<CybercrabBlock> METEOR_SPAWNER;
    public static RegistryHandle<TeslaBlock> TESLA;
    public static RegistryHandle<LootCrateBlock> CRATE;
    public static RegistryHandle<LootCrateBlock> CRATE_RED;
    public static RegistryHandle<Block> METEOR_BATTERY;
    public static RegistryHandle<Block> STONE_RESOURCE_SULFUR;
    public static RegistryHandle<Block> STONE_RESOURCE_ASBESTOS;
    public static RegistryHandle<Block> REINFORCED_STONE;
    public static RegistryHandle<SlabBlock> REINFORCED_STONE_SLAB;
    public static RegistryHandle<StairBlock> REINFORCED_STONE_STAIRS;
    public static RegistryHandle<Block> BRICK_JUNGLE;
    public static RegistryHandle<Block> BRICK_JUNGLE_CRACKED;
    public static RegistryHandle<Block> BRICK_JUNGLE_LAVA;
    public static RegistryHandle<JungleOozeBlock> BRICK_JUNGLE_OOZE;
    public static RegistryHandle<JungleMysticBlock> BRICK_JUNGLE_MYSTIC;
    public static RegistryHandle<FragileBrickBlock> BRICK_JUNGLE_FRAGILE;
    public static RegistryHandle<Block> CRATE_JUNGLE;
    public static RegistryHandle<LogicBlock> LOGIC_BLOCK;
    public static RegistryHandle<InvisibleLogicBlock> LOGIC_BLOCK_INVIS;
    public static RegistryHandle<WasteEarthBlock> WASTE_EARTH;
    public static RegistryHandle<BurningEarthBlock> BURNING_EARTH;
    public static RegistryHandle<ImpactDirtBlock> IMPACT_DIRT;
    public static RegistryHandle<MushBlock> MUSH;
    public static RegistryHandle<WasteMyceliumBlock> WASTE_MYCELIUM;
    public static RegistryHandle<Block> WASTE_LEAVES;
    public static RegistryHandle<FalloutBlock> FALLOUT;
    public static RegistryHandle<RotatedPillarBlock> BLOCK_FIBERGLASS;
    public static RegistryHandle<Block> BLOCK_INSULATOR;
    public static RegistryHandle<Block> BLOCK_ASBESTOS;
    public static RegistryHandle<NuclearWasteBlock> BLOCK_WASTE;
    public static RegistryHandle<NuclearWasteBlock> BLOCK_WASTE_PAINTED;
    public static RegistryHandle<NuclearWasteBlock> BLOCK_WASTE_VITRIFIED;
    public static RegistryHandle<MeltdownGasBlock> GAS_MELTDOWN;
    public static RegistryHandle<PinkCloudBroadcasterBlock> BROADCASTER_PC;
    public static RegistryHandle<SealBlock> SEAL_CONTROLLER;
    public static RegistryHandle<SealHatchBlock> SEAL_HATCH;
    public static RegistryHandle<VentBlock> VENT_CHLORINE;
    public static RegistryHandle<VentBlock> VENT_CLOUD;
    public static RegistryHandle<VentBlock> VENT_PINK_CLOUD;
    public static RegistryHandle<ChlorineSealBlock> VENT_CHLORINE_SEAL;
    public static RegistryHandle<FieldDisturberBlock> FIELD_DISTURBER;
    public static RegistryHandle<ChlorineGasBlock> CHLORINE_GAS;
    public static RegistryHandle<MonoxideGasBlock> GAS_MONOXIDE;
    public static RegistryHandle<CoalGasBlock> GAS_COAL;
    public static RegistryHandle<AsbestosGasBlock> GAS_ASBESTOS;
    public static RegistryHandle<FlammableGasBlock> GAS_FLAMMABLE;
    public static RegistryHandle<ExplosiveGasBlock> GAS_EXPLOSIVE;
    public static RegistryHandle<SellafieldSlakedBlock> SELLAFIELD_SLAKED;
    public static RegistryHandle<SellafieldSlakedBlock> SELLAFIELD_BEDROCK;
    public static RegistryHandle<SellafieldBlock> SELLAFIELD;
    public static RegistryHandle<WasteLogBlock> WASTE_LOG;
    public static RegistryHandle<Block> PINK_LOG;
    public static RegistryHandle<WasteTrinititeBlock> WASTE_TRINITITE;
    public static RegistryHandle<WasteTrinititeBlock> WASTE_TRINITITE_RED;
    public static RegistryHandle<VolcanoBlock> VOLCANO_CORE;
    public static RegistryHandle<VolcanoBlock> VOLCANO_RAD_CORE;
    public static RegistryHandle<FissureBlock> ORE_VOLCANO;
    public static ClassicFluid.Pair VOLCANIC_LAVA_FLUID;
    public static RegistryHandle<VolcanicLavaBlock> VOLCANIC_LAVA_BLOCK;
    public static ClassicFluid.Pair RAD_LAVA_FLUID;
    public static RegistryHandle<RadLavaBlock> RAD_LAVA_BLOCK;
    public static RegistryHandle<LootCrateBlock> CRATE_WEAPON;
    public static RegistryHandle<LootCrateBlock> CRATE_LEAD;
    public static RegistryHandle<LootCrateBlock> CRATE_METAL;
    public static RegistryHandle<AmmoCrateBlock> CRATE_AMMO;
    public static RegistryHandle<CanCrateBlock> CRATE_CAN;
    public static RegistryHandle<SupplyCrateBlock> CRATE_SUPPLY;
    public static RegistryHandle<Block> BLOCK_SCRAP;
    public static RegistryHandle<Block> BLOCK_ELECTRICAL_SCRAP;
    public static RegistryHandle<SlagBlock> BLOCK_SLAG;
    public static RegistryHandle<Block> ASPHALT;
    public static RegistryHandle<Block> ASPHALT_LIGHT;
    public static RegistryHandle<Block> BLOCK_FOAM;
    public static RegistryHandle<BoxcarBlock> BOXCAR;
    public static RegistryHandle<BoatBlock> BOAT;
    public static RegistryHandle<LayeringBlock> FOAM_LAYER;
    public static RegistryHandle<ColoredFallingBlock> SAND_BORON;
    public static RegistryHandle<ColoredFallingBlock> SAND_LEAD;
    public static RegistryHandle<ColoredFallingBlock> SAND_URANIUM;
    public static RegistryHandle<ColoredFallingBlock> SAND_POLONIUM;
    public static RegistryHandle<ColoredFallingBlock> SAND_QUARTZ;
    public static RegistryHandle<LayeringBlock> SAND_BORON_LAYER;
    public static RegistryHandle<FileCabinetBlock> FILING_CABINET_GREEN;
    public static RegistryHandle<FileCabinetBlock> FILING_CABINET_STEEL;
    public static RegistryHandle<Block> GRAVEL_OBSIDIAN;
    public static RegistryHandle<BaseFallingBlock> GRAVEL_DIAMOND;
    public static RegistryHandle<Block> REINFORCED_DUCRETE;
    public static RegistryHandle<RotatedPillarBlock> CONCRETE_PILLAR;
    public static RegistryHandle<UberConcreteBlock> CONCRETE_SUPER;
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
    public static RegistryHandle<SandbagsBlock> SANDBAGS;
    public static RegistryHandle<Block> BRICK_CONCRETE_MARKED;
    public static RegistryHandle<ForgottenBrickBlock> BRICK_FORGOTTEN;
    public static RegistryHandle<ForgottenLockBlock> BRICK_FORGOTTEN_LOCK;
    public static RegistryHandle<Block> TILE_LAB;
    public static RegistryHandle<Block> TILE_LAB_CRACKED;
    public static RegistryHandle<Block> TILE_LAB_BROKEN;

    static {
        var materials = new String[] {"silver", "copper", "white"};
        for (var materialIndex = 0; materialIndex < materials.length; materialIndex++) {
            var material = materials[materialIndex];
            var sprite = "block/boxduct_" + material;
            var tinted = materialIndex == 2;
            for (var size = 0; size < 5; size++) {
                var ductSize = size;
                var name = "fluid_duct_box_" + material + "_" + size;
                FLUID_DUCT_BOX[materialIndex][size] =
                        Reg.blockItem(
                                        name,
                                        properties ->
                                                new FluidDuctBoxBlock(properties, ductSize, tinted),
                                        () ->
                                                BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.METAL)
                                                        .strength(5.0F, 6.0F)
                                                        .sound(ModSoundTypes.PIPE)
                                                        .noOcclusion()
                                                        .forceSolidOn()
                                                        .requiresCorrectToolForDrops(),
                                        BlockItem::new)
                                .bakedBy(() -> new BoxDuctModel());
            }
        }

        for (var size = 0; size < 5; size++) {
            var cableSize = size;
            var name = "red_cable_box_" + size;
            RED_CABLE_BOX[size] =
                    Reg.blockItem(
                                    name,
                                    properties -> new CableBoxBlock(properties, cableSize),
                                    () ->
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.METAL)
                                                    .strength(5.0F, 6.0F)
                                                    .noOcclusion()
                                                    .forceSolidOn()
                                                    .requiresCorrectToolForDrops(),
                                    BlockItem::new)
                            .bakedBy(() -> new BoxDuctModel());
        }

        for (var size = 0; size < 5; size++) {
            var ductSize = size;
            var name = "fluid_duct_exhaust_" + size;
            FLUID_DUCT_EXHAUST[size] =
                    Reg.blockItem(
                                    name,
                                    properties ->
                                            new FluidDuctBoxExhaustBlock(properties, ductSize),
                                    () ->
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(MapColor.METAL)
                                                    .strength(5.0F, 6.0F)
                                                    .sound(ModSoundTypes.PIPE)
                                                    .noOcclusion()
                                                    .forceSolidOn()
                                                    .requiresCorrectToolForDrops(),
                                    BlockItem::new)
                            .bakedBy(() -> new BoxDuctModel());
        }

        ORE_URANIUM =
                oreOutgas("ore_uranium", Materials.URANIUM, 5.0F, 6.0F, () -> GAS_RADON.get());
        ore("ore_thorium", Materials.THORIUM, 5.0F, 6.0F);
        ore("ore_titanium", Materials.TITANIUM, 5.0F, 6.0F);
        ore("ore_sulfur", Materials.SULFUR, 5.0F, 6.0F);
        ore("ore_aluminium", Materials.ALUMINUM, 5.0F, 6.0F);
        ore("ore_fluorite", Materials.FLUORITE, 5.0F, 6.0F);
        ore("ore_niter", Materials.SALTPETER, 5.0F, 6.0F);
        ore("ore_tungsten", Materials.TUNGSTEN, 5.0F, 6.0F);
        ore("ore_lead", Materials.LEAD, 5.0F, 6.0F);
        ORE_BERYLLIUM = ore("ore_beryllium", Materials.BERYLLIUM, 5.0F, 9.0F);
        ore("ore_rare", Materials.RARE_EARTH, 5.0F, 6.0F);
        ORE_LIGNITE = ore("ore_lignite", Materials.LIGNITE, 5.0F, 9.0F);
        oreOutgas("ore_asbestos", Materials.ASBESTOS, 5.0F, 9.0F, () -> GAS_ASBESTOS.get());
        ore("ore_cinnabar", Materials.CINNABAR, 5.0F, 6.0F);
        ore("ore_cobalt", Materials.COBALT, 5.0F, 6.0F);
        ORE_COLTAN = ore("ore_coltan", MaterialTagRegistry.COLTAN, 15.0F, 6.0F);
        ORE_URANIUM_SCORCHED =
                oreOutgas(
                        "ore_uranium_scorched",
                        Materials.URANIUM,
                        5.0F,
                        6.0F,
                        () -> GAS_RADON.get());
        ORE_SCHRABIDIUM = ore("ore_schrabidium", Materials.SCHRABIDIUM, 15.0F, 360.0F);

        ORE_NETHER_URANIUM =
                oreOutgas(
                        "ore_nether_uranium", Materials.URANIUM, 0.4F, 6.0F, () -> GAS_RADON.get());
        ORE_NETHER_URANIUM_SCORCHED =
                oreOutgas(
                        "ore_nether_uranium_scorched",
                        Materials.URANIUM,
                        0.4F,
                        6.0F,
                        () -> GAS_RADON.get());
        ore("ore_nether_tungsten", Materials.TUNGSTEN, 0.4F, 6.0F);
        ore("ore_nether_sulfur", Materials.SULFUR, 0.4F, 6.0F);
        ore("ore_nether_cobalt", Materials.COBALT, 0.4F, 6.0F);
        ore("ore_nether_plutonium", Materials.PLUTONIUM, 0.4F, 6.0F);
        ORE_NETHER_SCHRABIDIUM =
                ore("ore_nether_schrabidium", Materials.SCHRABIDIUM, 15.0F, 360.0F);

        ore("ore_gneiss_iron", Materials.IRON, 1.5F, 6.0F);
        ore("ore_gneiss_gold", Materials.GOLD, 1.5F, 6.0F);
        ORE_GNEISS_URANIUM =
                oreOutgas(
                        "ore_gneiss_uranium", Materials.URANIUM, 1.5F, 6.0F, () -> GAS_RADON.get());
        ORE_GNEISS_URANIUM_SCORCHED =
                oreOutgas(
                        "ore_gneiss_uranium_scorched",
                        Materials.URANIUM,
                        1.5F,
                        6.0F,
                        () -> GAS_RADON.get());
        ore("ore_gneiss_copper", Materials.COPPER, 1.5F, 6.0F);
        oreOutgas("ore_gneiss_asbestos", Materials.ASBESTOS, 1.5F, 6.0F, () -> GAS_ASBESTOS.get());
        ore("ore_gneiss_lithium", Materials.LITHIUM, 1.5F, 6.0F);
        ORE_GNEISS_SCHRABIDIUM = ore("ore_gneiss_schrabidium", Materials.SCHRABIDIUM, 1.5F, 6.0F);
        ore("ore_gneiss_rare", Materials.RARE_EARTH, 1.5F, 6.0F);

        basaltOre("ore_basalt_sulfur", Materials.SULFUR);
        basaltOre("ore_basalt_fluorite", Materials.FLUORITE);

        ORE_BASALT_ASBESTOS =
                Reg.block(
                        "ore_basalt_asbestos",
                        properties -> new OreBasaltBlock(properties, () -> GAS_ASBESTOS.get()),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        var asbestosItem =
                Reg.<Item>item(
                        "ore_basalt_asbestos",
                        properties -> new BlockItem(ORE_BASALT_ASBESTOS.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        ORE_BASALT_ASBESTOS,
                        asbestosItem,
                        Materials.ASBESTOS.frame(),
                        MaterialShape.ORE));

        basaltOre("ore_basalt_molysite", Materials.MOLYSITE);

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

        ORE_URANIUM_DEEPSLATE =
                oreOutgasDeepslate(
                        "ore_uranium_deepslate",
                        Materials.URANIUM,
                        5.0F,
                        6.0F,
                        () -> GAS_RADON.get());
        oreDeepslate("ore_thorium_deepslate", Materials.THORIUM, 5.0F, 6.0F);
        oreDeepslate("ore_titanium_deepslate", Materials.TITANIUM, 5.0F, 6.0F);
        oreDeepslate("ore_sulfur_deepslate", Materials.SULFUR, 5.0F, 6.0F);
        oreDeepslate("ore_aluminium_deepslate", Materials.ALUMINUM, 5.0F, 6.0F);
        oreDeepslate("ore_fluorite_deepslate", Materials.FLUORITE, 5.0F, 6.0F);
        oreDeepslate("ore_niter_deepslate", Materials.SALTPETER, 5.0F, 6.0F);
        oreDeepslate("ore_tungsten_deepslate", Materials.TUNGSTEN, 5.0F, 6.0F);
        oreDeepslate("ore_lead_deepslate", Materials.LEAD, 5.0F, 6.0F);
        oreDeepslate("ore_beryllium_deepslate", Materials.BERYLLIUM, 5.0F, 9.0F);
        oreDeepslate("ore_rare_deepslate", Materials.RARE_EARTH, 5.0F, 6.0F);
        oreDeepslate("ore_cinnabar_deepslate", Materials.CINNABAR, 5.0F, 6.0F);
        oreDeepslate("ore_cobalt_deepslate", Materials.COBALT, 5.0F, 6.0F);
        oreDeepslate("ore_lignite_deepslate", Materials.LIGNITE, 5.0F, 9.0F);
        oreOutgasDeepslate(
                "ore_asbestos_deepslate", Materials.ASBESTOS, 5.0F, 9.0F, () -> GAS_ASBESTOS.get());
        oreDeepslate("ore_coltan_deepslate", MaterialTagRegistry.COLTAN, 15.0F, 6.0F);

        ORE_GNEISS_GAS = extraOre("ore_gneiss_gas", 1.5F, 6.0F);
        ORE_AUSTRALIUM =
                extraOre(
                        "ore_australium",
                        Block::new,
                        5.0F,
                        6.0F,
                        UnaryOperator.identity(),
                        (block, properties) ->
                                new DescBlockItem(block, properties, 3, DescBlockItem.Desc.PLAIN),
                        properties -> properties.rarity(Rarity.UNCOMMON));
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
                ModBlocks.<OreNetherFireBlock>extraOre(
                        "ore_nether_fire",
                        OreNetherFireBlock::new,
                        0.4F,
                        6.0F,
                        UnaryOperator.identity());
        ORE_OIL =
                ModBlocks.<Block>extraOre(
                        "ore_oil", OreOilBlock::new, 5.0F, 6.0F, UnaryOperator.identity());
        ORE_OIL_EMPTY = extraOre("ore_oil_empty", 5.0F, 6.0F);
        ORE_BEDROCK_OIL =
                extraOre(
                        "ore_bedrock_oil",
                        -1.0F,
                        600_000.0F,
                        properties ->
                                properties.isValidSpawn(
                                        (_,
                                                _,
                                                _,
                                                _) -> false));

        STONE_RESOURCE_HEMATITE = resourceStone("stone_resource_hematite");
        STONE_RESOURCE_BAUXITE = resourceStone("stone_resource_bauxite");
        STONE_RESOURCE_MALACHITE =
                resourceStone("stone_resource_malachite", MalachiteResourceStoneBlock::new, true);
        STONE_RESOURCE_LIMESTONE = resourceStone("stone_resource_limestone");

        STONE_DEPTH = depthBlock("stone_depth");
        depthBlock("depth_brick");
        depthBlock("depth_tiles");
        STONE_DEPTH_NETHER = depthBlock("stone_depth_nether");
        depthBlock("depth_nether_brick");
        depthBlock("depth_nether_tiles");
        depthBlock("depth_dnt", 36_000.0F);

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
                        NetherCoalBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(0.4F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(_ -> 10)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("ore_nether_coal", ORE_NETHER_COAL));
        EXTRA_ORE_BLOCKS.add(decoItem("ore_sellafield_diamond", ORE_SELLAFIELD_DIAMOND));
        EXTRA_ORE_BLOCKS.add(decoItem("ore_sellafield_emerald", ORE_SELLAFIELD_EMERALD));
        EXTRA_ORE_BLOCKS.add(decoItem("ore_sellafield_radgem", ORE_SELLAFIELD_RADGEM));

        var schrabItem = decoItem("ore_sellafield_schrabidium", ORE_SELLAFIELD_SCHRABIDIUM);
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        ORE_SELLAFIELD_SCHRABIDIUM,
                        schrabItem,
                        Materials.SCHRABIDIUM.frame(),
                        MaterialShape.ORE));
        var scorchedItem =
                decoItem("ore_sellafield_uranium_scorched", ORE_SELLAFIELD_URANIUM_SCORCHED);
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        ORE_SELLAFIELD_URANIUM_SCORCHED,
                        scorchedItem,
                        Materials.URANIUM.frame(),
                        MaterialShape.ORE));

        STONE_RESOURCE_SULFUR = resourceStone("stone_resource_sulfur");
        STONE_RESOURCE_ASBESTOS =
                resourceStone("stone_resource_asbestos", AsbestosResourceStoneBlock::new, false);

        hazardStorage("block_uranium", Materials.URANIUM, 5F, 30F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_u233",
                Materials.URANIUM_233,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_u235",
                Materials.URANIUM_235,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_u238", Materials.URANIUM_238, 5F, 30F, SoundType.METAL, MapColor.METAL);
        hazardStorage("block_thorium", Materials.THORIUM, 5F, 30F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_plutonium",
                Materials.PLUTONIUM,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_pu_mix",
                Materials.PLUTONIUM_RG,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_pu238",
                Materials.PLUTONIUM_238,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG,
                5);
        hazardStorage(
                "block_pu239",
                Materials.PLUTONIUM_239,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_pu240",
                Materials.PLUTONIUM_240,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_neptunium",
                Materials.NEPTUNIUM_237,
                5F,
                36F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage(
                "block_polonium",
                Materials.POLONIUM_210,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.RADFOG);
        hazardStorage("block_ra226", Materials.RADIUM_226, 5F, 6F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_actinium", Materials.ACTINIUM_227, 5F, 6F, SoundType.METAL, MapColor.METAL);
        hazardStorage(
                "block_schrabidium",
                Materials.SCHRABIDIUM,
                5F,
                360F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.SCHRAB);
        hazardStorage(
                "block_solinium",
                Materials.SOLINIUM,
                5F,
                360F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.SCHRAB);
        hazardStorage(
                "block_schrabidate",
                Materials.SCHRABIDATE,
                5F,
                360F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.SCHRAB);
        hazardStorage(
                "block_schraranium",
                Materials.SCHRARANIUM,
                5F,
                150F,
                SoundType.METAL,
                MapColor.METAL,
                HazardBlock.ExtDisplayEffect.SCHRAB);

        storage("block_titanium", Materials.TITANIUM, 5F, 30F, SoundType.METAL, MapColor.METAL);
        BLOCK_LEAD =
                storage("block_lead", Materials.LEAD, 5F, 30F, SoundType.METAL, MapColor.METAL)
                        .afterRegistration(RadiationSystemNT::markRadResistant);

        BLOCK_GRAPHITE =
                Reg.block(
                        "block_graphite",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(5F, 6F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        BLOCK_GRAPHITE,
                        Reg.<Item>item(
                                "block_graphite",
                                properties -> new BlockItem(BLOCK_GRAPHITE.get(), properties),
                                () -> new Item.Properties().useBlockDescriptionPrefix()),
                        Materials.GRAPHITE.frame(),
                        MaterialShape.BLOCK));

        storage("block_red_copper", Materials.MINGRADE, 5F, 15F, SoundType.METAL, MapColor.METAL);
        storage("block_tungsten", Materials.TUNGSTEN, 5F, 12F, SoundType.METAL, MapColor.METAL);
        storage("block_aluminium", Materials.ALUMINUM, 5F, 12F, SoundType.METAL, MapColor.METAL);
        storage("block_steel", Materials.STEEL, 5F, 30F, SoundType.METAL, MapColor.METAL);
        storage("block_tcalloy", Materials.TC_ALLOY, 5F, 42F, SoundType.METAL, MapColor.METAL);
        storage("block_cdalloy", Materials.CD_ALLOY, 5F, 42F, SoundType.METAL, MapColor.METAL);
        storage("block_cadmium", Materials.CADMIUM, 5F, 54F, SoundType.METAL, MapColor.METAL);
        storage("block_bismuth", Materials.BISMUTH, 5F, 54F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_coltan",
                MaterialTagRegistry.COLTAN,
                5F,
                30F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_niobium", Materials.NIOBIUM, 5F, 30F, SoundType.METAL, MapColor.METAL);
        BLOCK_BERYLLIUM =
                storage(
                        "block_beryllium",
                        Materials.BERYLLIUM,
                        5F,
                        12F,
                        SoundType.METAL,
                        MapColor.METAL);
        storage("block_boron", Materials.BORON, 5F, 6F, SoundType.METAL, MapColor.METAL)
                .afterRegistration(RadiationSystemNT::markRadResistant);
        storage("block_cobalt", Materials.COBALT, 5F, 30F, SoundType.STONE, MapColor.METAL);
        storage("block_zirconium", Materials.ZIRCONIUM, 5F, 18F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_lithium",
                Materials.LITHIUM.frame(),
                LithiumBlock::new,
                5F,
                6F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_lanthanium", Materials.LANTHANUM, 5F, 6F, SoundType.METAL, MapColor.METAL);
        storage(
                "block_dura_steel",
                Materials.DURA_STEEL,
                5F,
                120F,
                SoundType.METAL,
                MapColor.METAL);
        storage(
                "block_combine_steel",
                Materials.CMBSTEEL,
                5F,
                360F,
                SoundType.METAL,
                MapColor.METAL);
        storage(
                "block_magnetized_tungsten",
                Materials.MAGNETIZED_TUNGSTEN,
                5F,
                45F,
                SoundType.METAL,
                MapColor.METAL);
        storage("block_starmetal", Materials.STARMETAL, 5F, 240F, SoundType.METAL, MapColor.METAL);
        BLOCK_DESH =
                storage(
                        "block_desh",
                        Materials.WORKERS_ALLOY,
                        5F,
                        180F,
                        SoundType.METAL,
                        MapColor.METAL);
        storage(
                "block_dineutronium",
                Materials.DINEUTRONIUM,
                5F,
                36000F,
                SoundType.METAL,
                MapColor.METAL);

        fuelBlock("block_uranium_fuel", 5F, 30F, null);
        fuelBlock("block_thorium_fuel", 5F, 30F, null);
        fuelBlock("block_mox_fuel", 5F, 30F, HazardBlock.ExtDisplayEffect.RADFOG);
        fuelBlock("block_plutonium_fuel", 5F, 30F, HazardBlock.ExtDisplayEffect.RADFOG);
        fuelBlock(
                "block_schrabidium_fuel",
                5F,
                360F,
                HazardBlock.ExtDisplayEffect.SCHRAB,
                Rarity.RARE);
        beaconStorage("block_tantalium", 5F, 30F, SoundType.METAL, MapColor.METAL);
        fuelBlock("block_trinitite", 5F, 6F, null, null, SoundType.STONE);
        BLOCK_SCHRABIDIUM_CLUSTER =
                pillarBlock(
                        "block_schrabidium_cluster",
                        5F,
                        36000F,
                        SoundType.STONE,
                        MapColor.STONE,
                        Rarity.RARE);
        BLOCK_EUPHEMIUM_CLUSTER =
                pillarBlock(
                        "block_euphemium_cluster",
                        5F,
                        36000F,
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
                                        .strength(5F, 6F)
                                        .sound(SoundType.WOOL)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("block_insulator", BLOCK_INSULATOR));

        fuelBlock("block_white_phosphorus", 5F, 6F, null, null, SoundType.STONE, MapColor.STONE);
        BLOCK_FALLOUT =
                Reg.block(
                        "block_fallout",
                        HazardFallingBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .strength(0.2F)
                                        .sound(SoundType.GRAVEL));
        DECO_BLOCKS.add(decoItem("block_fallout", BLOCK_FALLOUT));

        BLOCK_COKE_COAL = cokeBlock("block_coke_coal", MaterialTagRegistry.COALCOKE);
        BLOCK_COKE_LIGNITE = cokeBlock("block_coke_lignite", MaterialTagRegistry.LIGCOKE);
        BLOCK_COKE_PETROLEUM = cokeBlock("block_coke_petroleum", MaterialTagRegistry.PETCOKE);
        pillarBlockNoTool(
                "block_tritium", 3F, 1.2F, SoundType.GLASS, MapColor.NONE, NoteBlockInstrument.HAT);
        BLOCK_SEMTEX = plasticExplosive("block_semtex");
        BLOCK_C4 = plasticExplosive("block_c4");
        beaconStorage("block_australium", 5F, 6F, SoundType.STONE, MapColor.METAL)
                .afterRegistration(RadiationSystemNT::markRadResistant);
        capBlock("block_cap_nuka");
        capBlock("block_cap_quantum");
        capBlock("block_cap_sparkle");
        capBlock("block_cap_rad");
        capBlock("block_cap_korl");
        capBlock("block_cap_fritz");

        storage("block_polymer", Materials.POLYMER, 3.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        storage("block_bakelite", Materials.BAKELITE, 3.0F, 3.0F, SoundType.STONE, MapColor.METAL);
        storage("block_rubber", Materials.RUBBER, 3.0F, 9.0F, SoundType.STONE, MapColor.METAL);

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
                        Materials.ASBESTOS,
                        5.0F,
                        9.0F,
                        SoundType.WOOL,
                        MapColor.WOOL,
                        () -> GAS_ASBESTOS.get());
        storage("block_sulfur", Materials.SULFUR, 5.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        storage("block_niter", Materials.SALTPETER, 5.0F, 6.0F, SoundType.STONE, MapColor.METAL);
        storage("block_fluorite", Materials.FLUORITE, 5.0F, 6.0F, SoundType.STONE, MapColor.METAL);

        RegistryHandle<HazardFallingBlock> redPhosphorus =
                Reg.block(
                        "block_red_phosphorus",
                        HazardFallingBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.SAND));
        MATERIAL_BLOCKS.add(
                new MaterialBlockEntry(
                        redPhosphorus,
                        Reg.<Item>item(
                                "block_red_phosphorus",
                                properties -> new BlockItem(redPhosphorus.get(), properties),
                                () -> new Item.Properties().useBlockDescriptionPrefix()),
                        Materials.RED_PHOSPHORUS.frame(),
                        MaterialShape.BLOCK));

        REBAR =
                Reg.block(
                                "rebar",
                                RebarBlock::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.METAL)
                                                .strength(15.0F, 12.0F)
                                                .noOcclusion()
                                                .isSuffocating(
                                                        (_,
                                                                _,
                                                                _) ->
                                                                false)
                                                .isViewBlocking(
                                                        (_,
                                                                _,
                                                                _) ->
                                                                false)
                                                .isValidSpawn(
                                                        (_,
                                                                _,
                                                                _,
                                                                _) ->
                                                                false)
                                                .requiresCorrectToolForDrops())
                        .bakedBy(() -> new RebarModel.Family());
        DECO_BLOCKS.add(decoItem("rebar", REBAR));

        REINFORCED_STONE = decoBlast("reinforced_stone", 15.0F, 60.0F);
        REINFORCED_STONE_SLAB = decoSlab("reinforced_stone_slab", 15.0F, 60.0F);
        REINFORCED_STONE_STAIRS =
                decoStairs("reinforced_stone_stairs", REINFORCED_STONE, 15.0F, 60.0F);

        CRYSTAL_HARDENED =
                decoUnlisted(
                        "crystal_hardened",
                        15.0F,
                        Float.POSITIVE_INFINITY,
                        SoundType.STONE,
                        MapColor.METAL);
        CRYSTAL_VIRUS =
                Reg.block(
                        "crystal_virus",
                        CrystalVirusBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(15.0F, Float.POSITIVE_INFINITY)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        decoItem("crystal_virus", CRYSTAL_VIRUS);

        CRYSTAL_PULSAR =
                Reg.block(
                        "crystal_pulsar",
                        CrystalPulsarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
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
                        WritingBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(15.0F, 96.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem("brick_concrete_marked", BRICK_CONCRETE_MARKED));

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
                        properties -> new OutgasBlock(properties, () -> GAS_ASBESTOS.get(), true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 600.0F)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_asbestos", brickAsbestos));

        RegistryHandle<Block> brickFire = decoBlast("brick_fire", 5.0F, 21.0F);
        decoBlast("cmb_brick", 25.0F, 3_000.0F);

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

        concreteColored("concrete_white", DyeColor.WHITE);
        concreteColored("concrete_orange", DyeColor.ORANGE);
        concreteColored("concrete_magenta", DyeColor.MAGENTA);
        concreteColored("concrete_light_blue", DyeColor.LIGHT_BLUE);
        concreteColored("concrete_yellow", DyeColor.YELLOW);
        CONCRETE_LIME = concreteColored("concrete_lime", DyeColor.LIME);
        concreteColored("concrete_pink", DyeColor.PINK);
        concreteColored("concrete_gray", DyeColor.GRAY);
        concreteColored("concrete_silver", DyeColor.LIGHT_GRAY);
        concreteColored("concrete_cyan", DyeColor.CYAN);
        concreteColored("concrete_purple", DyeColor.PURPLE);
        concreteColored("concrete_blue", DyeColor.BLUE);
        concreteColored("concrete_brown", DyeColor.BROWN);
        concreteColored("concrete_green", DyeColor.GREEN);
        CONCRETE_RED = concreteColored("concrete_red", DyeColor.RED);
        concreteColored("concrete_black", DyeColor.BLACK);

        CONCRETE_PILLAR =
                Reg.block(
                                "concrete_pillar",
                                RotatedPillarBlock::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.STONE)
                                                .strength(15.0F, 108.0F)
                                                .sound(SoundType.STONE)
                                                .requiresCorrectToolForDrops())
                        .afterRegistration(RadiationSystemNT::markRadResistant);
        DECO_BLOCKS.add(blastItem("concrete_pillar", CONCRETE_PILLAR));

        CONCRETE_SUPER =
                Reg.block(
                        "concrete_super",
                        UberConcreteBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
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
                        ForgottenBrickBlock::new,
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
                        ForgottenLockBlock::new,
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
                        BaseFallingBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.GRAVEL)
                                        .strength(0.6F));
        DECO_BLOCKS.add(
                Reg.<Item>item(
                        "gravel_diamond",
                        properties -> new DiamondGravelBlockItem(GRAVEL_DIAMOND.get(), properties),
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
                        CybercrabBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(15.0F, 216.0F)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "meteor_spawner",
                properties -> new BlockItem(METEOR_SPAWNER.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        TESLA =
                Reg.block(
                        "tesla",
                        TeslaBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "tesla",
                properties -> new BlockItem(TESLA.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE =
                Reg.block(
                        "crate",
                        LootCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        Reg.item(
                "crate",
                properties -> new BlockItem(CRATE.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_RED =
                Reg.block(
                        "crate_red",
                        LootCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_red",
                properties -> new BlockItem(CRATE_RED.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_WEAPON =
                Reg.block(
                        "crate_weapon",
                        LootCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        Reg.item(
                "crate_weapon",
                properties -> new BlockItem(CRATE_WEAPON.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_LEAD =
                Reg.block(
                        "crate_lead",
                        LootCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_lead",
                properties -> new BlockItem(CRATE_LEAD.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_METAL =
                Reg.block(
                        "crate_metal",
                        LootCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_metal",
                properties -> new BlockItem(CRATE_METAL.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_AMMO =
                Reg.block(
                        "crate_ammo",
                        AmmoCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(1.0F, 1.5F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "crate_ammo",
                properties -> new BlockItem(CRATE_AMMO.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_CAN =
                Reg.block(
                        "crate_can",
                        CanCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
                                        .strength(1.0F, 1.5F)
                                        .sound(SoundType.WOOD)
                                        .noOcclusion()
                                        .ignitedByLava());
        Reg.item(
                "crate_can",
                properties -> new BlockItem(CRATE_CAN.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        CRATE_SUPPLY =
                Reg.block(
                        "crate_supply",
                        SupplyCrateBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
                                        .strength(1.0F, 1.5F)
                                        .sound(SoundType.WOOD)
                                        .noOcclusion()
                                        .ignitedByLava());
        Reg.item(
                "crate_supply",
                properties -> new ContainerBlockItem(CRATE_SUPPLY.get(), properties),
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
                        SlagBlock::new,
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
                        SandbagsBlock::new,
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
                        properties -> new SpeedyBlock(1.5D, properties),
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
                        properties -> new SpeedyBlock(1.5D, properties),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .sound(SoundType.STONE)
                                        .strength(15.0F, 72.0F)
                                        .lightLevel(_ -> 15)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem("asphalt_light", ASPHALT_LIGHT));

        BOXCAR =
                Reg.block(
                        "boxcar",
                        BoxcarBlock::new,
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
                        BoatBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .sound(SoundType.METAL)
                                        .strength(10.0F, 6.0F)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());
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
                        properties -> new LayeringBlock(properties, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SNOW)
                                        .sound(SoundType.SNOW)
                                        .strength(0.1F)
                                        .forceSolidOff()
                                        .noLootTable()
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY)))
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
                        properties -> new LayeringBlock(properties, false),
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
                        NukeCustomBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());

        NUKE_FSTBMB =
                Reg.blockItem(
                        "nuke_fstbmb",
                        NukeBalefireBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());

        BOMB_MULTI =
                Reg.blockItem(
                                "bomb_multi",
                                BombMultiBlock::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.METAL)
                                                .strength(0.0F, 120.0F)
                                                .noOcclusion()
                                                .requiresCorrectToolForDrops())
                        .bakedBy(
                                () ->
                                        new BombMultiModel(
                                                RegistryUtil.id("models/blocks/bomb_generic.obj")));

        FIREWORKS =
                Reg.blockItem(
                        "fireworks",
                        FireworksBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(0.0F, 3.0F)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        DYNAMITE =
                Reg.blockItem(
                        "dynamite",
                        DynamiteBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava(),
                        BlockItem::new);

        TNT_NTM =
                Reg.blockItem(
                        "tnt",
                        TNTBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava(),
                        BlockItem::new);

        SEMTEX =
                Reg.blockItem(
                        "semtex",
                        SemtexBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava(),
                        BlockItem::new);

        C4 =
                Reg.blockItem(
                        "c4",
                        C4Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava(),
                        BlockItem::new);

        FISSURE_BOMB =
                Reg.blockItem(
                        "fissure_bomb",
                        FissureBombBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(0.0F)
                                        .sound(SoundType.GRASS)
                                        .ignitedByLava(),
                        BlockItem::new);

        DET_CHARGE =
                Reg.blockItem(
                        "det_charge",
                        DetChargeBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(0.1F, 0.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        DET_CORD =
                Reg.blockItem(
                                "det_cord",
                                DetCordBlock::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.METAL)
                                                .strength(0.1F, 0.0F)
                                                .sound(SoundType.STONE)
                                                .noOcclusion()
                                                .requiresCorrectToolForDrops(),
                                BlockItem::new)
                        .bakedBy(() -> new DetCordModel.Family());

        DET_NUKE =
                Reg.blockItem(
                        "det_nuke",
                        DetNukeBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(0.1F, 0.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        DET_MINER =
                Reg.blockItem(
                        "det_miner",
                        DetMinerBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(0.1F, 0.0F)
                                        .sound(SoundType.STONE)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        FLAME_WAR =
                Reg.blockItem(
                        "flame_war",
                        BombFlameWarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        EMP_BOMB =
                Reg.blockItem(
                        "emp_bomb",
                        EMPBombBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        FLOAT_BOMB =
                Reg.blockItem(
                        "float_bomb",
                        FloatBombBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        THERM_ENDO =
                Reg.blockItem(
                        "therm_endo",
                        ThermoBombBlock.Endothermic::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        THERM_EXO =
                Reg.blockItem(
                        "therm_exo",
                        ThermoBombBlock.Exothermic::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops(),
                        BlockItem::new);

        TAINT =
                Reg.block(
                        "taint",
                        TaintBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_GRAY)
                                        .strength(15.0F, 6.0F)
                                        .randomTicks()
                                        .sound(SoundType.STONE)
                                        .noLootTable()
                                        .requiresCorrectToolForDrops());

        BALEFIRE =
                Reg.blockItem(
                        "balefire",
                        BalefireBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.NONE)
                                        .replaceable()
                                        .noCollision()
                                        .instabreak()
                                        .lightLevel(_ -> 15)
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY)))
                                        .noLootTable(),
                        BlockItem::new);

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
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.5F)
                                        .sound(SoundType.GRAVEL));
        decoItem("dirt", NTM_DIRT);

        METEOR_BATTERY =
                meteorDeco(
                        "meteor_battery",
                        (block, properties) ->
                                new DescBlockItem(block, properties, 1, DescBlockItem.Desc.PLAIN));

        Reg.Handle<ColoredFallingBlock> moonTurf =
                Reg.block(
                        "moon_turf",
                        properties -> new ColoredFallingBlock(new ColorRGBA(0xA1A1A1), properties),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.SAND)
                                        .strength(0.5F));
        DECO_BLOCKS.add(decoItem("moon_turf", moonTurf));

        BRICK_JUNGLE = deco("brick_jungle", 15.0F, 216.0F);
        BRICK_JUNGLE_CRACKED = deco("brick_jungle_cracked", 15.0F, 216.0F);

        RegistryHandle<BallsSpawnerBlock> jungleCircle =
                Reg.block(
                        "brick_jungle_circle",
                        BallsSpawnerBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
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
                                        .mapColor(MapColor.STONE)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(_ -> 5)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_lava", BRICK_JUNGLE_LAVA));

        BRICK_JUNGLE_OOZE =
                Reg.block(
                        "brick_jungle_ooze",
                        JungleOozeBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(_ -> 5)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_ooze", BRICK_JUNGLE_OOZE));

        BRICK_JUNGLE_MYSTIC =
                Reg.block(
                        "brick_jungle_mystic",
                        JungleMysticBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(_ -> 5)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("brick_jungle_mystic", BRICK_JUNGLE_MYSTIC));

        BRICK_JUNGLE_FRAGILE =
                Reg.block(
                        "brick_jungle_fragile",
                        FragileBrickBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                "brick_jungle_fragile",
                properties -> new BlockItem(BRICK_JUNGLE_FRAGILE.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());

        for (var glyph = 0; glyph < 16; glyph++) {
            var name = "brick_jungle_glyph_" + glyph;
            deco(
                    name,
                    15.0F,
                    216.0F,
                    (block, properties) ->
                            new DescBlockItem(block, properties, 1, DescBlockItem.Desc.PLAIN));
        }

        CRATE_JUNGLE = decoUnlisted("crate_jungle", 1.0F, 1.5F, SoundType.STONE);

        for (var trap : TrappedBrickBlock.Trap.values()) {
            var name = "brick_jungle_trap_" + trap.name().toLowerCase(Locale.ROOT);
            var suffix =
                    switch (trap) {
                        case FALLING_ROCKS -> "Falling Rocks";
                        case FIRE -> "Fire";
                        case ARROW -> "Arrow";
                        case SPIKES -> "Spikes";
                        case MINE -> "Mine";
                        case WEB -> "Web";
                        case FLAMING_ARROW -> "Flaming Arrow";
                        case PILLAR -> "Pillar";
                        case RAD_CONVERSION -> "Rad Conversion";
                        case MAGIC_CONVERSTION -> "Magic Conversion";
                        case SLOWNESS -> "Slowness";
                        case WEAKNESS -> "Weakness";
                        case POISON_DART -> "Poison Dart";
                        case ZOMBIE -> "Zombie";
                        default -> "Spiders";
                    };
            RegistryHandle<TrappedBrickBlock> block =
                    Reg.<TrappedBrickBlock>block(
                            name,
                            properties -> {
                                if (trap.type == TrappedBrickBlock.TrapType.DETECTOR) {
                                    return new TrappedBrickBlock.Detector(properties, trap);
                                }
                                return new TrappedBrickBlock(properties, trap);
                            },
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(15.0F, 216.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops());
            DECO_BLOCKS.add(
                    decoItem(
                            name,
                            block,
                            (actualBlock, properties) ->
                                    new DescBlockItem(
                                            actualBlock, properties, 1, DescBlockItem.Desc.PLAIN)));
            JUNGLE_TRAPS.add(block);
        }

        for (var dudType : CrashedBombBlock.EnumDudType.values()) {
            var skin = dudType.name().toLowerCase(Locale.ROOT);
            var name = "crashed_" + skin;
            var title =
                    switch (dudType) {
                        case BALEFIRE -> "Crashed Balefire Bomb";
                        case CONVENTIONAL -> "Crashed Conventional Bomb";
                        case NUKE -> "Crashed Nuclear Bomb";
                        case SALTED -> "Crashed Salted Bomb";
                    };
            RegistryHandle<CrashedBombBlock> block =
                    Reg.block(
                            name,
                            properties -> new CrashedBombBlock(properties, dudType),
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
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
                                        .mapColor(MapColor.STONE)
                                        .strength(0.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        decoItem("logic_block", LOGIC_BLOCK);

        LOGIC_BLOCK_INVIS =
                Reg.block(
                        "logic_block_invis",
                        InvisibleLogicBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
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
                        properties ->
                                new OutgasBlock(
                                        properties, () -> ModBlocks.GAS_ASBESTOS.get(), true),
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
                        PlatemetalBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(ModSoundTypes.PLATEMETAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                        "platemetal",
                        properties ->
                                new PlatemetalBlock.PlatemetalItem(PLATEMETAL.get(), properties),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .component(
                                                DataComponents.BLOCK_STATE,
                                                PlatemetalBlock.stateFor(
                                                        PlatemetalBlock.Variant.BASE)))
                .state(() -> DataComponents.BLOCK_STATE);

        deco("block_smore", 15.0F, 360.0F);
        deco("basalt", 5.0F, 6.0F);
        deco("basalt_smooth", 5.0F, 6.0F);
        deco("deco_rbmk", 5.0F, 60.0F, MapColor.METAL);
        deco("deco_rbmk_smooth", 5.0F, 60.0F, MapColor.METAL);
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
                                        .mapColor(MapColor.WOOD)
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
                                        .mapColor(MapColor.WOOD)
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("waste_planks", WASTE_PLANKS));

        FROZEN_DIRT =
                Reg.block(
                        "frozen_dirt",
                        FrozenEarthBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS));
        DECO_BLOCKS.add(decoItem("frozen_dirt", FROZEN_DIRT));

        FROZEN_GRASS =
                Reg.block(
                        "frozen_grass",
                        FrozenEarthBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS));
        DECO_BLOCKS.add(decoItem("frozen_grass", FROZEN_GRASS));

        FROZEN_LOG =
                Reg.block(
                        "frozen_log",
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
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
                                        .mapColor(MapColor.WOOD)
                                        .strength(0.5F, 1.5F)
                                        .sound(SoundType.GLASS)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem("frozen_planks", FROZEN_PLANKS));

        WASTE_EARTH =
                Reg.block(
                        "waste_earth",
                        WasteEarthBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.6F, 0.6F)
                                        .sound(SoundType.GRASS)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("waste_earth", WASTE_EARTH));

        WASTE_MYCELIUM =
                Reg.block(
                        "waste_mycelium",
                        WasteMyceliumBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.6F)
                                        .sound(SoundType.GRASS)
                                        .lightLevel(_ -> 15)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("waste_mycelium", WASTE_MYCELIUM));

        BURNING_EARTH =
                Reg.block(
                        "burning_earth",
                        BurningEarthBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.6F)
                                        .sound(SoundType.GRASS)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("burning_earth", BURNING_EARTH));

        IMPACT_DIRT =
                Reg.block(
                        "impact_dirt",
                        ImpactDirtBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .strength(0.5F)
                                        .sound(SoundType.GRAVEL)
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("impact_dirt", IMPACT_DIRT));

        MUSH =
                Reg.block(
                        "mush",
                        MushBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .instabreak()
                                        .noCollision()
                                        .sound(SoundType.GRASS)
                                        .lightLevel(_ -> 7)
                                        .randomTicks()
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY))));
        DECO_BLOCKS.add(
                Reg.<Item>item(
                        "mush",
                        properties -> new BlockItem(MUSH.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix()));

        WASTE_LEAVES =
                Reg.<Block>block(
                        "waste_leaves",
                        WasteLeavesBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .strength(0.1F, 0.1F)
                                        .sound(SoundType.GRASS)
                                        .noOcclusion()
                                        .isRedstoneConductor(
                                                (_,
                                                        _,
                                                        _) -> false)
                                        .isSuffocating(
                                                (_,
                                                        _,
                                                        _) -> false)
                                        .isViewBlocking(
                                                (_,
                                                        _,
                                                        _) -> false)
                                        .isValidSpawn(
                                                (_,
                                                        _,
                                                        _,
                                                        type) ->
                                                        type == EntityTypes.OCELOT
                                                                || type == EntityTypes.PARROT)
                                        .ignitedByLava()
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY)))
                                        .randomTicks());
        DECO_BLOCKS.add(decoItem("waste_leaves", WASTE_LEAVES));

        FALLOUT = Reg.block("fallout", FalloutBlock::new, FalloutBlock::defaultProperties);
        DECO_BLOCKS.add(decoItem("fallout", FALLOUT));

        BLOCK_WASTE =
                Reg.block("block_waste", NuclearWasteBlock::new, ModBlocks::wasteBlockProperties);
        DECO_BLOCKS.add(decoItem("block_waste", BLOCK_WASTE));
        BLOCK_WASTE_PAINTED =
                Reg.block(
                        "block_waste_painted",
                        NuclearWasteBlock::new,
                        ModBlocks::wasteBlockProperties);
        DECO_BLOCKS.add(decoItem("block_waste_painted", BLOCK_WASTE_PAINTED));
        BLOCK_WASTE_VITRIFIED =
                Reg.block(
                        "block_waste_vitrified",
                        NuclearWasteBlock::new,
                        ModBlocks::wasteBlockProperties);
        DECO_BLOCKS.add(decoItem("block_waste_vitrified", BLOCK_WASTE_VITRIFIED));

        GAS_MELTDOWN =
                Reg.block("gas_meltdown", MeltdownGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_meltdown", GAS_MELTDOWN);

        BROADCASTER_PC =
                Reg.blockItem(
                        "broadcaster_pc",
                        PinkCloudBroadcasterBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 9.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());

        SEAL_CONTROLLER =
                Reg.blockItem(
                        "seal_controller",
                        SealBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(10.0F, 60.0F)
                                        .requiresCorrectToolForDrops());

        SEAL_HATCH =
                Reg.blockItem(
                        "seal_hatch",
                        SealHatchBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        .requiresCorrectToolForDrops()
                                        .pushReaction(((PushReaction) (PushReaction.BLOCK))));

        VENT_CHLORINE = vent("vent_chlorine", VentBlock.CHLORINE, 1.5D);
        VENT_CLOUD = vent("vent_cloud", VentBlock.CLOUD, 1.75D);
        VENT_PINK_CLOUD = vent("vent_pink_cloud", VentBlock.PINK_CLOUD, 2.0D);

        VENT_CHLORINE_SEAL =
                Reg.blockItem(
                        "vent_chlorine_seal",
                        ChlorineSealBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .requiresCorrectToolForDrops());

        CHLORINE_GAS =
                Reg.block("chlorine_gas", ChlorineGasBlock::new, GasBlock::defaultProperties);
        decoItem("chlorine_gas", CHLORINE_GAS);

        FIELD_DISTURBER =
                Reg.block(
                        "field_disturber",
                        FieldDisturberBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 120.0F)
                                        .requiresCorrectToolForDrops());
        decoItem("field_disturber", FIELD_DISTURBER);

        GAS_RADON = Reg.block("gas_radon", RadonGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_radon", GAS_RADON);
        GAS_RADON_DENSE =
                Reg.block("gas_radon_dense", DenseRadonGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_radon_dense", GAS_RADON_DENSE);
        GAS_RADON_TOMB =
                Reg.block("gas_radon_tomb", TombRadonGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_radon_tomb", GAS_RADON_TOMB);
        GAS_ASBESTOS =
                Reg.block("gas_asbestos", AsbestosGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_asbestos", GAS_ASBESTOS);
        GAS_FLAMMABLE =
                Reg.block("gas_flammable", FlammableGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_flammable", GAS_FLAMMABLE);
        GAS_EXPLOSIVE =
                Reg.block("gas_explosive", ExplosiveGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_explosive", GAS_EXPLOSIVE);
        GAS_MONOXIDE =
                Reg.block("gas_monoxide", MonoxideGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_monoxide", GAS_MONOXIDE);
        GAS_COAL = Reg.block("gas_coal", CoalGasBlock::new, GasBlock::defaultProperties);
        decoItem("gas_coal", GAS_COAL);

        SELLAFIELD_SLAKED =
                Reg.block(
                        "sellafield_slaked",
                        SellafieldSlakedBlock::new,
                        SellafieldSlakedBlock::defaultProperties);
        DECO_BLOCKS.add(decoItem("sellafield_slaked", SELLAFIELD_SLAKED));

        SELLAFIELD_BEDROCK =
                Reg.block(
                        "sellafield_bedrock",
                        SellafieldSlakedBlock::new,
                        SellafieldSlakedBlock::sellafieldBedrockProperties);
        DECO_BLOCKS.add(decoItem("sellafield_bedrock", SELLAFIELD_BEDROCK));

        SELLAFIELD =
                Reg.block("sellafield", SellafieldBlock::new, SellafieldBlock::defaultProperties);
        Reg.item(
                        "sellafield",
                        properties ->
                                new SellafieldBlock.SellafieldBlockItem(
                                        SELLAFIELD.get(), properties),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .component(DataComponents.BLOCK_STATE, levelState(0)))
                .state(() -> DataComponents.BLOCK_STATE);

        WASTE_LOG =
                Reg.block(
                        "waste_log",
                        properties ->
                                new WasteLogBlock(properties, () -> ModItems.BURNT_BARK.get()),
                        WasteLogBlock::defaultProperties);
        DECO_BLOCKS.add(decoItem("waste_log", WASTE_LOG));

        WASTE_TRINITITE =
                Reg.block(
                        "waste_trinitite",
                        WasteTrinititeBlock::new,
                        WasteTrinititeBlock::defaultProperties);
        DECO_BLOCKS.add(decoItem("waste_trinitite", WASTE_TRINITITE));
        WASTE_TRINITITE_RED =
                Reg.block(
                        "waste_trinitite_red",
                        WasteTrinititeBlock::new,
                        WasteTrinititeBlock::defaultProperties);
        DECO_BLOCKS.add(decoItem("waste_trinitite_red", WASTE_TRINITITE_RED));

        VOLCANO_CORE = volcano("volcano_core");
        VOLCANO_RAD_CORE = volcano("volcano_rad_core");

        ORE_VOLCANO =
                Reg.block(
                        "ore_volcano",
                        FissureBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(-1.0F, 600_000.0F)
                                        .sound(SoundType.STONE)
                                        .lightLevel(_ -> 15)
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
                                RegistryUtil.id("block/fluid/volcanic_lava"),
                                () -> ModBlocks.VOLCANIC_LAVA_BLOCK.get(),
                                ModBlocks::lavaBucket));
        VOLCANIC_LAVA_BLOCK =
                Reg.block(
                        "volcanic_lava_block",
                        properties ->
                                new VolcanicLavaBlock(
                                        VOLCANIC_LAVA_FLUID.source().get(), properties),
                        () ->
                                classicFluidProps(MapColor.FIRE)
                                        .lightLevel(
                                                state -> ClassicFluidBlock.light(state, 4, 15)));

        RAD_LAVA_FLUID =
                Reg.classicFluid(
                        "rad_lava_fluid",
                        new ClassicFluid.Spec(
                                ClassicFluid.Physics.LAVA,
                                4,
                                15,
                                3000,
                                true,
                                RegistryUtil.id("block/fluid/rad_lava"),
                                () -> ModBlocks.RAD_LAVA_BLOCK.get(),
                                ModBlocks::lavaBucket));
        RAD_LAVA_BLOCK =
                Reg.block(
                        "rad_lava_block",
                        properties -> new RadLavaBlock(RAD_LAVA_FLUID.source().get(), properties),
                        () ->
                                classicFluidProps(MapColor.FIRE)
                                        .lightLevel(
                                                state -> ClassicFluidBlock.light(state, 4, 15)));

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
                        SpeedySlabBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
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
                                        .mapColor(MapColor.WOOD)
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
                        properties ->
                                new SpeedyStairsBlock(
                                        ASPHALT.get().defaultBlockState(), 1.5D, properties),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(15.0F, 72.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem("asphalt_stairs", asphaltStairs));

        RegistryHandle<StairBlock> pinkStairs =
                Reg.<StairBlock>block(
                        "pink_stairs",
                        properties ->
                                new StairBlock(pinkPlanks.get().defaultBlockState(), properties),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOD)
                                        .strength(0.0F, 0.0F)
                                        .sound(SoundType.WOOD)
                                        .ignitedByLava());
        decoItem("pink_stairs", pinkStairs);
    }

    public static final RegistryHandle<Block> SEAL_FRAME =
            structural("seal_frame", SealFrameBlock::new, 10.0F, 60.0F);
    public static final RegistryHandle<Block> MACHINE_TRANSFORMER =
            structural("machine_transformer", 5.0F, 6.0F);
    public static final RegistryHandle<Block> STRUCT_LAUNCHER =
            structural("struct_launcher", 5.0F, 6.0F);
    public static final RegistryHandle<Block> STRUCT_SCAFFOLD =
            structural("struct_scaffold", 5.0F, 6.0F);
    public static final RegistryHandle<Block> FUSION_HEATER =
            structuralUnlisted("fusion_heater", 5.0F, 6.0F);
    public static final RegistryHandle<Block> FUSION_HATCH =
            structuralUnlisted("fusion_hatch", FusionHatchBlock::new, 5.0F, 6.0F);
    public static final RegistryHandle<Block> WATZ_ELEMENT = structural("watz_element", 5.0F, 6.0F);
    public static final RegistryHandle<Block> WATZ_COOLER = structural("watz_cooler", 5.0F, 6.0F);

    public static RegistryHandle<Block> CONCRETE_RED;
    private static final String[] TURNED_FACINGS = {"north", "south", "west", "east"};
    public static final RegistryHandle<StraightRailBlock> RAIL_LARGE_STRAIGHT =
            Reg.blockItem(
                    "rail_large_straight",
                    properties ->
                            new StraightRailBlock(
                                    properties,
                                    RailGauge.STANDARD,
                                    2,
                                    StraightRailBlock.Profile.FLAT),
                    ModBlocks::railProps);

    public static final RegistryHandle<StraightRailBlock> RAIL_LARGE_STRAIGHT_SHORT =
            Reg.blockItem(
                    "rail_large_straight_short",
                    properties ->
                            new StraightRailBlock(
                                    properties,
                                    RailGauge.STANDARD,
                                    0,
                                    StraightRailBlock.Profile.FLAT),
                    ModBlocks::railProps);

    public static final RegistryHandle<StandardCurveRailBlock> RAIL_LARGE_CURVE =
            Reg.blockItem("rail_large_curve", StandardCurveRailBlock::new, ModBlocks::railProps);

    public static final RegistryHandle<RailStandardCurveWide7Block> RAIL_LARGE_CURVE_7 =
            Reg.blockItem(
                    "rail_large_curve_7", RailStandardCurveWide7Block::new, ModBlocks::railProps);

    public static final RegistryHandle<RailStandardCurveWide9Block> RAIL_LARGE_CURVE_9 =
            Reg.blockItem(
                    "rail_large_curve_9", RailStandardCurveWide9Block::new, ModBlocks::railProps);

    public static final RegistryHandle<StraightRailBlock> RAIL_LARGE_RAMP =
            Reg.blockItem(
                    "rail_large_ramp",
                    properties ->
                            new StraightRailBlock(
                                    properties,
                                    RailGauge.STANDARD,
                                    2,
                                    StraightRailBlock.Profile.RAMP),
                    ModBlocks::railProps);

    public static final RegistryHandle<StraightRailBlock> RAIL_LARGE_BUFFER =
            Reg.blockItem(
                    "rail_large_buffer",
                    properties ->
                            new StraightRailBlock(
                                    properties,
                                    RailGauge.STANDARD,
                                    2,
                                    StraightRailBlock.Profile.BUFFER),
                    ModBlocks::railProps);

    public static final RegistryHandle<RailSwitchBlock> RAIL_LARGE_SWITCH =
            Reg.blockItem("rail_large_switch", RailSwitchBlock::clockwise, ModBlocks::railProps);

    public static final RegistryHandle<RailSwitchBlock> RAIL_LARGE_SWITCH_FLIPPED =
            Reg.blockItem(
                    "rail_large_switch_flipped",
                    RailSwitchBlock::counterClockwise,
                    ModBlocks::railProps);

    public static final RegistryHandle<StraightRailBlock> RAIL_NARROW_STRAIGHT =
            Reg.blockItem(
                    "rail_narrow_straight",
                    properties ->
                            new StraightRailBlock(
                                    properties,
                                    RailGauge.NARROW,
                                    2,
                                    StraightRailBlock.Profile.FLAT),
                    ModBlocks::railProps);

    public static final RegistryHandle<RailNarrowCurveBlock> RAIL_NARROW_CURVE =
            Reg.blockItem("rail_narrow_curve", RailNarrowCurveBlock::new, ModBlocks::railProps);

    private ModBlocks() {}

    private static RegistryHandle<PylonMediumBlock> mediumPylon(
            String name,
            boolean transformer,
            String sheet,
            TagKey<Block> mineable,
            UnaryOperator<BlockBehaviour.Properties> material) {
        return Reg.blockItem(
                name,
                properties -> new PylonMediumBlock(transformer, properties),
                () -> {
                    var properties =
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .noOcclusion();
                    return material.apply(properties);
                },
                (block, properties) ->
                        new DescBlockItem(block, properties, 2, DescBlockItem.Desc.PLAIN));
    }

    private static Reg.Handle<FluidPipeBlock> fluidPipe(String name) {
        return Reg.blockItem(
                        name,
                        FluidPipeBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(ModSoundTypes.PIPE)
                                        .forceSolidOn()
                                        .requiresCorrectToolForDrops(),
                        FluidPipeBlockItem::new)
                .subtypes(ItemSubtype.FLUID_CONTENT)
                .bakedBy(() -> new FluidPipeModel());
    }

    private static BlockBehaviour.Properties craneProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties conveyorProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F, 1.2F)
                .noOcclusion()
                .requiresCorrectToolForDrops();
    }

    private static <B extends Block> Reg.Handle<B> droneStation(
            String name, Function<BlockBehaviour.Properties, B> factory, int descLines) {
        return Reg.blockItem(
                name,
                factory,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(0.1F, 6.0F)
                                .requiresCorrectToolForDrops(),
                (block, props) -> new DescBlockItem(block, props, descLines));
    }

    private static Reg.Handle<DroneWaypointBlock> droneWaypoint(
            String name, DroneWaypointBlock.Kind kind, int descLines) {
        var block =
                Reg.block(
                                name,
                                properties -> new DroneWaypointBlock(properties, kind),
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.NONE)
                                                .strength(0.1F, 6.0F)
                                                .noCollision()
                                                .noOcclusion()
                                                .pushReaction(
                                                        ((PushReaction) (PushReaction.DESTROY))))
                        .bakedBy(
                                () -> {
                                    var base = RegistryUtil.id("block/" + name + "_base");
                                    return new DroneWaypointModel(base);
                                });

        assert descLines == (kind == DroneWaypointBlock.Kind.TRANSPORT ? 5 : 0);
        Reg.item(
                name,
                properties -> {
                    if (kind == DroneWaypointBlock.Kind.TRANSPORT) {
                        return new DroneWaypointItem(block.get(), properties);
                    }
                    return new BlockItem(block.get(), properties);
                },
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static RegistryHandle<RadioTorchBlock> radioTorch(
            String id, RadioTorchBlock.Kind kind) {
        var skin =
                switch (kind) {
                    case SENDER -> "sender_off";
                    case RECEIVER -> "rec_off";
                    case COUNTER -> "counter";
                    case LOGIC -> "logic_off";
                    case READER -> "reader";
                    case CONTROLLER -> "controller";
                };
        var litSkin =
                switch (kind) {
                    case SENDER -> "sender_on";
                    case RECEIVER -> "rec_on";
                    case LOGIC -> "logic_on";
                    case COUNTER, READER, CONTROLLER -> null;
                };

        var block =
                Reg.block(
                                id,
                                properties -> new RadioTorchBlock(properties, kind),
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.NONE)
                                                .strength(0.1F, 6.0F)
                                                .noCollision()
                                                .noOcclusion()
                                                .pushReaction(
                                                        ((PushReaction) (PushReaction.DESTROY))))
                        .bakedBy(() -> new RadioTorchModel());

        int descriptionLines =
                switch (kind) {
                    case RECEIVER -> 1;
                    case LOGIC -> 3;
                    case SENDER, COUNTER, READER, CONTROLLER -> 2;
                };
        Reg.item(
                id,
                properties -> new DescBlockItem(block.get(), properties, descriptionLines),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static Reg.Handle<AnvilBlock> anvil(String id, int tier) {
        var block =
                Reg.block(
                        id,
                        properties -> new AnvilBlock(properties, tier),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 60.0F)
                                        .sound(SoundType.ANVIL)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops());

        Reg.item(
                id,
                properties -> new AnvilBlockItem(block.get(), properties, tier),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        ANVILS.add(block);
        return block;
    }

    private static RegistryHandle<StirlingBlock> stirling(
            String name, StirlingBlock.Tier tier, String sheet, int descLines) {
        return Reg.blockItem(
                        name,
                        props -> new StirlingBlock(tier, props),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
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

    private static BlockBehaviour.Properties paProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    private static Map<CellBuckets.Plain, RegistryHandle<? extends MultiblockCellBlock>>
            plainCells() {
        Map<CellBuckets.Plain, RegistryHandle<? extends MultiblockCellBlock>> out =
                new LinkedHashMap<>();
        for (CellBuckets.Plain key : CellBuckets.plain()) {
            out.put(
                    key,
                    declarePlainCell(
                            Reg.block(
                                    key.name(),
                                    props -> MultiblockCellBlock.create(key, props),
                                    () -> cellProperties(key)),
                            key.mineable(),
                            key.sealed()));
        }
        return out;
    }

    private static Reg.Handle<MultiblockCellBlock> declarePlainCell(
            Reg.Handle<MultiblockCellBlock> cell, TagKey<Block> mineable, boolean sealed) {
        return cell.afterRegistration(
                block ->
                        RadiationSystemNT.markRadResistant(
                                block, _ -> sealed));
    }

    private static Map<CellBuckets.Geometry, RegistryHandle<MultiblockGeometryCellBlock>>
            geometryCells() {
        var cells =
                new LinkedHashMap<
                        CellBuckets.Geometry, RegistryHandle<MultiblockGeometryCellBlock>>();
        for (var bucket : CellBuckets.geometry()) {
            var mineable = bucket.key().mineable();
            var cell =
                    Reg.block(
                                    bucket.name(),
                                    properties ->
                                            MultiblockGeometryCellBlock.create(bucket, properties),
                                    () -> cellProperties(bucket.key()).forceSolidOn())
                            .afterRegistration(
                                    block -> {
                                        if (bucket.part().seals()) {
                                            RadiationSystemNT.markRadResistant(
                                                    block,
                                                    state ->
                                                            !state.getValue(
                                                                    MultiblockGeometryCellBlock
                                                                            .OPEN));
                                        }
                                    });
            cells.put(bucket, cell);
        }
        return cells;
    }

    private static BlockBehaviour.Properties cellProperties(CellBuckets.Plain key) {
        var properties =
                BlockBehaviour.Properties.of()
                        .mapColor(key.mapColor())
                        .strength(key.hardness(), key.resistance());
        if (key.sound() == MultiblockCellBlock.SOUND_METAL) {
            properties.sound(SoundType.METAL);
        } else {
            properties.sound(SoundType.STONE);
        }
        properties.lightLevel(_ -> key.light());
        properties.noOcclusion();
        if (key.requiresTool()) {
            properties.requiresCorrectToolForDrops();
        }
        return properties;
    }

    public static Reg.Handle<MultiblockCellBlock> addonPlainCell(
            Reg.Namespace namespace, String name, CellBuckets.Plain key) {
        CellBuckets.addAddonPlain(key);
        var cell =
                namespace.block(
                        name,
                        properties -> MultiblockCellBlock.create(key, properties),
                        () -> cellProperties(key));
        declarePlainCell(cell, key.mineable(), key.sealed());
        PLAIN_CELLS.put(key, cell);
        return cell;
    }

    public static Reg.Handle<MultiblockCellBlock> sharedPlainCell(
            Reg.Namespace namespace, int priority, CellBuckets.Plain key) {
        var cell =
                namespace.sharedBlock(
                        key.name(),
                        priority,
                        properties -> MultiblockCellBlock.create(key, properties),
                        () -> cellProperties(key));
        declarePlainCell(cell, key.mineable(), key.sealed());
        var previous = PLAIN_CELLS.putIfAbsent(key, cell);
        if (previous == null) {
            CellBuckets.addAddonPlain(key);
        }
        return cell;
    }

    public static RegistryHandle<MultiblockGeometryCellBlock> addonGeometryCell(
            Reg.Namespace namespace, CellBuckets.Geometry bucket) {
        var mineable = bucket.key().mineable();
        var cell =
                namespace
                        .block(
                                bucket.name(),
                                properties ->
                                        MultiblockGeometryCellBlock.create(bucket, properties),
                                () -> cellProperties(bucket.key()).forceSolidOn())
                        .afterRegistration(
                                block -> {
                                    if (bucket.part().seals()) {
                                        RadiationSystemNT.markRadResistant(
                                                block,
                                                state ->
                                                        !state.getValue(
                                                                MultiblockGeometryCellBlock.OPEN));
                                    }
                                });
        GEOMETRY_CELLS.put(bucket, cell);
        return cell;
    }

    public static RegistryHandle<? extends MultiblockCellBlock> plainCell(CellBuckets.Plain key) {
        var cell = PLAIN_CELLS.get(key);
        if (cell == null) {
            throw new IllegalStateException("No plain cell is registered for " + key.name());
        }
        return cell;
    }

    public static RegistryHandle<MultiblockGeometryCellBlock> geometryCell(
            CellBuckets.Geometry bucket) {
        return GEOMETRY_CELLS.get(bucket);
    }

    public static List<RegistryHandle<? extends Block>> cellHandles() {
        var cells = new ArrayList<RegistryHandle<? extends Block>>(PLAIN_CELLS.values());
        cells.addAll(GEOMETRY_CELLS.values());
        cells.add(RADAR_MAST_CELL);
        return cells;
    }

    private static RegistryHandle<Item> decoItem(
            String name, RegistryHandle<? extends Block> block) {
        return decoItem(name, block, BlockItem::new);
    }

    private static RegistryHandle<Item> decoItem(
            String name,
            RegistryHandle<? extends Block> block,
            BiFunction<Block, Item.Properties, ? extends Item> itemFactory) {
        return Reg.item(
                name,
                properties -> itemFactory.apply(block.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<Item> blastItem(
            String name, RegistryHandle<? extends Block> block) {
        return Reg.item(
                name,
                props -> new BlastInfoBlockItem(block.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static Reg.Handle<Block> absorber(String name, String texture) {
        return Reg.blockItem(
                name,
                Block::new,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5.0F, 6.0F)
                                .requiresCorrectToolForDrops());
    }

    private static BlockBehaviour.Properties rbmkProps() {
        return rbmkColumnProps().noOcclusion();
    }

    private static BlockBehaviour.Properties rbmkColumnProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 18.0F)
                .requiresCorrectToolForDrops();
    }

    private static <T extends RBMKMiniPanelBlock> Reg.Handle<T> miniPanel(
            String name, Function<BlockBehaviour.Properties, T> factory, int descLines) {
        return Reg.blockItem(
                name,
                factory,
                ModBlocks::rbmkProps,
                (block, properties) -> new DescBlockItem(block, properties, descLines));
    }

    private static RegistryHandle<Block> debris(String name, String mesh) {
        return debris(name, mesh, Block::new);
    }

    private static <B extends Block> RegistryHandle<B> debris(
            String name, String mesh, Function<BlockBehaviour.Properties, B> factory) {
        return Reg.blockItem(
                name,
                factory,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(50.0F, 360.0F)
                                .requiresCorrectToolForDrops()
                                .noOcclusion(),
                BlockItem::new);
    }

    private static BlockBehaviour.Properties pwrProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops();
    }

    private static Reg.Handle<Block> pwrPart(String name, int descLines) {
        return pwrPart(name, descLines, Block::new);
    }

    private static Reg.Handle<Block> pwrPart(
            String name, int descLines, Function<BlockBehaviour.Properties, Block> factory) {
        return Reg.blockItem(
                name,
                factory,
                ModBlocks::pwrProps,
                (block, properties) -> new DescBlockItem(block, properties, descLines));
    }

    private static BlockBehaviour.Properties icfProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 36.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static Reg.Handle<Block> icfPart(String name) {
        return icfPart(name, Block::new);
    }

    private static Reg.Handle<Block> icfPart(
            String name, Function<BlockBehaviour.Properties, Block> factory) {
        return Reg.blockItem(name, factory, ModBlocks::icfProps, BlockItem::new);
    }

    private static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(10.0F, 60.0F)
                .noOcclusion()
                .dynamicShape()
                .forceSolidOn()
                .requiresCorrectToolForDrops();
    }

    private static void sealsWhileClosed(Block door) {
        RadiationSystemNT.markRadResistant(door, state -> !state.getValue(GenericDoorBlock.OPEN));
    }

    private static Reg.Handle<Block> oreCube(String name, float hardness, float resistance) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        var item = decoItem(name, block);
        DECO_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<TritiumLampBlock> tritiumLamp(String name) {
        return Reg.<TritiumLampBlock>block(
                        name,
                        TritiumLampBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.NONE)
                                        .strength(3.0F, 3.0F)
                                        .sound(SoundType.GLASS)
                                        .lightLevel(
                                                state -> {
                                                    if (state.getValue(TritiumLampBlock.LIT)) {
                                                        return 15;
                                                    }
                                                    return 0;
                                                }))
                .andThen(
                        block -> {
                            var item = decoItem(name, block);
                            DECO_BLOCKS.add(item);
                        });
    }

    private static Reg.Handle<SellafieldOreBlock> sellafieldOre(
            String name, String overlay, @Nullable Supplier<Item> drop, int xpMin, int xpMax) {
        return Reg.block(
                name,
                properties -> new SellafieldOreBlock(properties, drop, xpMin, xpMax),
                SellafieldOreBlock::defaultProperties);
    }

    private static BlockBehaviour.Properties classicFluidProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .replaceable()
                .noCollision()
                .strength(0.0F, 300.0F)
                .pushReaction(((PushReaction) (PushReaction.DESTROY)))
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
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

    private static RegistryHandle<StalagmiteBlock> stalagmiteVariant(
            String variant, int meta, @Nullable String drop) {
        var name = "stalagmite_" + variant;
        var texture = "block/stalagmite." + variant;
        return Reg.block(name, StalagmiteBlock::new, ModBlocks::spikeProps)
                .andThen(handle -> DECO_BLOCKS.add(decoItem(name, handle)));
    }

    private static RegistryHandle<StalactiteBlock> stalactiteVariant(
            String variant, int meta, @Nullable String drop) {
        var name = "stalactite_" + variant;
        var texture = "block/stalactite." + variant;
        return Reg.block(name, StalactiteBlock::new, ModBlocks::spikeProps)
                .andThen(handle -> DECO_BLOCKS.add(decoItem(name, handle)));
    }

    private static Reg.Handle<Block> decoBlast(String name, float hardness, float resistance) {
        return decoBlast(name, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<Block> decoBlast(
            String name, float hardness, float resistance, SoundType sound) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> deco(String name, float hardness, float resistance) {
        return deco(name, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<Block> deco(
            String name, float hardness, float resistance, MapColor color) {
        return deco(name, hardness, resistance, SoundType.STONE, color);
    }

    private static Reg.Handle<Block> deco(
            String name, float hardness, float resistance, SoundType sound) {
        return deco(name, hardness, resistance, sound, MapColor.STONE);
    }

    private static Reg.Handle<Block> deco(
            String name, float hardness, float resistance, SoundType sound, MapColor color) {
        return deco(name, hardness, resistance, sound, color, BlockItem::new);
    }

    private static Reg.Handle<Block> deco(
            String name,
            float hardness,
            float resistance,
            BiFunction<Block, Item.Properties, ? extends Item> itemFactory) {
        return deco(name, hardness, resistance, SoundType.STONE, MapColor.STONE, itemFactory);
    }

    private static Reg.Handle<Block> deco(
            String name,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            BiFunction<Block, Item.Properties, ? extends Item> itemFactory) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block, itemFactory));
        return block;
    }

    private static BlockBehaviour.Properties nonBlockingGlass(BlockBehaviour.Properties props) {
        return props.isRedstoneConductor(
                        (_,
                                _,
                                _) -> false)
                .isSuffocating(
                        (_,
                                _,
                                _) -> false)
                .isViewBlocking(
                        (_,
                                _,
                                _) -> false)
                .isValidSpawn(
                        (_,
                                _,
                                _,
                                _) -> false);
    }

    private static RegistryHandle<TransparentBlock> litGlass(String name, DyeColor color) {
        var block =
                Reg.block(
                        name,
                        TransparentBlock::new,
                        () ->
                                nonBlockingGlass(
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.NONE)
                                                .strength(0.3F)
                                                .sound(SoundType.GLASS)
                                                .noOcclusion()
                                                .instrument(NoteBlockInstrument.HAT)
                                                .lightLevel(_ -> 5)));
        STRUCTURAL_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<LadderBlock> ladder(String name) {
        return Reg.block(
                        name,
                        LadderBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.NONE)
                                        .strength(0.25F, 1.2F)
                                        .sound(SoundType.STONE)
                                        .noOcclusion()
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY))))
                .andThen(
                        block -> {
                            var item = decoItem(name, block);
                            DECO_BLOCKS.add(item);
                        });
    }

    private static RegistryHandle<SteelScaffoldBlock> scaffold(
            String name, String sheet, int meta) {
        return Reg.block(
                        name,
                        SteelScaffoldBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 9.0F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops())
                .bakedBy(() -> new ScaffoldModel())
                .andThen(
                        block -> {
                            var item = decoItem(name, block);
                            DECO_BLOCKS.add(item);
                        });
    }

    private static RegistryHandle<Item> grateItem(
            String name, RegistryHandle<? extends Block> block, boolean wide) {
        return Reg.item(
                name,
                props -> new GrateBlockItem(block.get(), props, wide),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<Item> railItem(
            String name, RegistryHandle<? extends Block> block, int lines) {
        return Reg.item(
                name,
                props -> new DescBlockItem(block.get(), props, lines, DescBlockItem.Desc.PLAIN),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static RegistryHandle<DecoToasterBlock> toaster(String name, String sheet, int meta) {
        return Reg.block(
                        name,
                        DecoToasterBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5F, 6F)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops())
                .andThen(handle -> DECO_BLOCKS.add(decoItem(name, handle)));
    }

    private static RegistryHandle<DecoPipeBlock> decoPipe(
            String name, DecoPipeBlock.Kind kind, String skinSuffix) {
        return Reg.block(
                        name,
                        kind::create,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(2F, 3F)
                                        .sound(SteelGrateBlock.SOUND)
                                        .noOcclusion()
                                        .requiresCorrectToolForDrops())
                .andThen(handle -> DECO_BLOCKS.add(pipeItem(name, handle)))
                .bakedBy(() -> new PipeModel(kind));
    }

    private static RegistryHandle<Item> pipeItem(
            String name, RegistryHandle<? extends Block> block) {
        return Reg.<Item>item(
                name,
                props -> new DescBlockItem(block.get(), props, 1, DescBlockItem.Desc.PLAIN),
                () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static Reg.Handle<CrateBlock> crate(
            String name, CrateType type, float hardness, float resistance) {
        return crate(name, props -> new CrateBlock(props, type), hardness, resistance);
    }

    private static Reg.Handle<CrateBlock> crate(
            String name,
            Function<BlockBehaviour.Properties, CrateBlock> factory,
            float hardness,
            float resistance) {
        var block =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new StorageCrateBlockItem(block.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        CRATES.add(item);
        return block;
    }

    private static Reg.Handle<MassStorageBlock> massStorage(String name, int capacity, String art) {
        var block =
                Reg.block(
                        name,
                        properties -> new MassStorageBlock(properties, capacity),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        Reg.item(
                name,
                properties -> new ContainerBlockItem.WithInfo(block.get(), properties),
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
        var block =
                Reg.<Block>block(
                        name,
                        BaseFallingBlock::new,
                        () -> {
                            var properties =
                                    BlockBehaviour.Properties.of()
                                            .mapColor(color)
                                            .sound(sound)
                                            .strength(hardness, resistance);
                            return material.apply(properties);
                        });
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> deadPlant(String name) {
        var block =
                Reg.<Block>block(
                        name,
                        DeadPlantBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIRT)
                                        .noCollision()
                                        .instabreak()
                                        .sound(SoundType.GRASS)
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY))));
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<NtmFlowerBlock> flower(String name, NtmFlowerBlock.Variant variant) {
        var block =
                Reg.block(
                        name,
                        properties -> new NtmFlowerBlock(properties, variant),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .noCollision()
                                        .noOcclusion()
                                        .instabreak()
                                        .sound(SoundType.GRASS)
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY))));
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<NtmFlowerBlock> tintedFlower(
            String name, NtmFlowerBlock.Variant variant) {
        var block = flower(name, variant);
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
        var block = Reg.block(name, Block::new, ModBlocks::cmProps);
        var item = decoItem(name, block);
        MACHINE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<CMTankBlock> cmTank(String name) {
        var block = Reg.<CMTankBlock>block(name, CMTankBlock::new, () -> cmProps().noOcclusion());
        var item = decoItem(name, block);
        MACHINE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<CMPortBlock> cmPort(String name) {
        var block = Reg.<CMPortBlock>block(name, CMPortBlock::new, ModBlocks::cmProps);
        var item = decoItem(name, block);
        MACHINE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<Block> cmPillar(String name) {
        var block = Reg.block(name, Block::new, ModBlocks::cmProps);
        var item = decoItem(name, block);
        MACHINE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<Block> cmAnchor() {
        var block =
                Reg.<Block>block("custom_machine_anchor", CMAnchorBlock::new, ModBlocks::cmProps);
        var item = decoItem("custom_machine_anchor", block);
        MACHINE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<CustomMachineBlock> customMachine() {
        var block =
                Reg.<CustomMachineBlock>block(
                        "custom_machine",
                        CustomMachineBlock::new,
                        () -> cmProps().lightLevel(_ -> 15));
        var item =
                Reg.<Item>item(
                                "custom_machine",
                                properties -> new CustomMachineItem(block.get(), properties),
                                () -> new Item.Properties().useBlockDescriptionPrefix())
                        .subtypes(ItemSubtype.CUSTOM_MACHINE);
        MACHINE_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<ReedsBlock> reeds(String name) {
        var block =
                Reg.<ReedsBlock>block(
                                name,
                                ReedsBlock::new,
                                () ->
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.PLANT)
                                                .noCollision()
                                                .noOcclusion()
                                                .instabreak()
                                                .sound(SoundType.GRASS)
                                                .pushReaction(
                                                        ((PushReaction) (PushReaction.DESTROY))))
                        .bakedBy(() -> new ReedsModel.Family());
        var item = decoItem(name, block);
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<BlockTallPlant> tallPlant(
            String name, BlockTallPlant.Variant variant, String flower) {
        var block =
                Reg.block(
                        name,
                        properties -> new BlockTallPlant(properties, variant),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.PLANT)
                                        .noCollision()
                                        .noOcclusion()
                                        .instabreak()
                                        .sound(SoundType.GRASS)
                                        .pushReaction(((PushReaction) (PushReaction.DESTROY)))
                                        .randomTicks());
        var item = decoItem(name, block);
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<BlockTallPlant> tintedTallPlant(
            String name, BlockTallPlant.Variant variant, String flower) {
        var block = tallPlant(name, variant, flower);
        return block;
    }

    private static <B extends DrilledGraphiteBlock> Reg.Handle<B> pile(
            String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
        var block = Reg.<B>block(name, factory, DrilledGraphiteBlock::pileProperties);

        Reg.item(
                name,
                properties -> new BlockItem(block.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static <B extends PileDeviceBlock> Reg.Handle<B> pileDevice(
            String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
        var block =
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
                properties -> new BlockItem(block.get(), properties),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    private static <T extends Block> Reg.Handle<T> fusionMachine(
            String name, Function<BlockBehaviour.Properties, T> factory, int descLines) {
        return Reg.blockItem(
                name,
                factory,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5F, 36F)
                                .requiresCorrectToolForDrops()
                                .noOcclusion(),
                (block, properties) -> new DescBlockItem(block, properties, descLines));
    }

    private static <T extends Block> Reg.Handle<T> fusionComponent(
            String name, Function<BlockBehaviour.Properties, T> factory) {
        return Reg.blockItem(
                name,
                factory,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5F, 18F)
                                .requiresCorrectToolForDrops(),
                BlockItem::new);
    }

    private static Reg.Handle<Block> ore(
            String name, Material material, float hardness, float resistance) {
        return ore(name, material.frame(), hardness, resistance);
    }

    private static Reg.Handle<Block> ore(
            String name, MaterialTags dictionary, float hardness, float resistance) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, dictionary, MaterialShape.ORE);
    }

    private static Reg.Handle<Block> oreOutgas(
            String name, Material material, float hardness, float resistance, Supplier<Block> gas) {
        var dictionary = material.frame();
        var block =
                Reg.<Block>block(
                        name,
                        properties -> new OutgasBlock(properties, gas, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, dictionary, MaterialShape.ORE);
    }

    private static Reg.Handle<Block> basaltOre(String name, Material material) {
        var dictionary = material.frame();
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(5.0F, 6.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, dictionary, MaterialShape.ORE);
    }

    public record MaterialBlockEntry(
            RegistryHandle<? extends Block> block,
            RegistryHandle<Item> item,
            MaterialTags dict,
            MaterialShape shape) {}

    private static <B extends Block> Reg.Handle<B> materialBlock(
            String name, Reg.Handle<B> block, MaterialTags material, MaterialShape shape) {
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new BlockItem(block.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        MATERIAL_BLOCKS.add(new MaterialBlockEntry(block, item, material, shape));
        return block;
    }

    private static Reg.Handle<Block> oreDeepslate(
            String name, Material mat, float hardness, float resistance) {
        return oreDeepslate(name, mat.frame(), hardness, resistance);
    }

    private static Reg.Handle<Block> oreDeepslate(
            String name, MaterialTags dict, float hardness, float resistance) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DEEPSLATE)
                                        .strength(hardness * 1.5F, resistance)
                                        .sound(SoundType.DEEPSLATE)
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, dict, MaterialShape.ORE);
    }

    private static Reg.Handle<Block> oreOutgasDeepslate(
            String name, Material mat, float hardness, float resistance, Supplier<Block> gas) {
        var block =
                Reg.<Block>block(
                        name,
                        properties -> new OutgasBlock(properties, gas, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DEEPSLATE)
                                        .strength(hardness * 1.5F, resistance)
                                        .sound(SoundType.DEEPSLATE)
                                        .randomTicks()
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, mat.frame(), MaterialShape.ORE);
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
        return extraOre(
                name,
                factory,
                hardness,
                resistance,
                extra,
                BlockItem::new,
                UnaryOperator.identity());
    }

    private static <B extends Block> Reg.Handle<B> extraOre(
            String name,
            Function<BlockBehaviour.Properties, B> factory,
            float hardness,
            float resistance,
            UnaryOperator<BlockBehaviour.Properties> extra,
            BiFunction<? super B, Item.Properties, ? extends Item> itemFactory,
            UnaryOperator<Item.Properties> itemProperties) {
        var block =
                Reg.block(
                        name,
                        factory,
                        () -> {
                            var properties =
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(hardness, resistance)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops();
                            return extra.apply(properties);
                        });
        var item =
                Reg.<Item>item(
                        name,
                        properties -> itemFactory.apply(block.get(), properties),
                        () -> {
                            var properties = new Item.Properties().useBlockDescriptionPrefix();
                            return itemProperties.apply(properties);
                        });
        EXTRA_ORE_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<Block> resourceStone(String name) {
        return resourceStone(name, Block::new, false);
    }

    private static Reg.Handle<Block> resourceStone(
            String name,
            Function<BlockBehaviour.Properties, ? extends Block> factory,
            boolean ownDrops) {
        var block =
                Reg.<Block>block(
                        name,
                        factory,
                        () -> {
                            var properties =
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(5.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops();
                            if (ownDrops) {
                                properties.noLootTable();
                            }
                            return properties;
                        });

        var item = decoItem(name, block);
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<DepthBlock> depthBlock(String name) {
        return depthBlock(name, 6.0F);
    }

    private static Reg.Handle<DepthBlock> depthBlock(String name, float resistance) {
        var block =
                Reg.block(
                        name,
                        DepthBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(-1.0F, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new DepthBlockItem(block.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<Block> hazardStorage(
            String name,
            Material mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        return hazardStorage(name, mat, hardness, resistance, sound, color, null);
    }

    private static Reg.Handle<Block> hazardStorage(
            String name,
            Material mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            HazardBlock.@Nullable ExtDisplayEffect display) {
        return hazardStorage(name, mat, hardness, resistance, sound, color, display, 0);
    }

    private static Reg.Handle<Block> hazardStorage(
            String name,
            Material mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            HazardBlock.@Nullable ExtDisplayEffect display,
            int light) {
        var block =
                Reg.<Block>block(
                        name,
                        properties -> new HazardBlock(properties).setDisplayEffect(display),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .lightLevel(_ -> light)
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, mat.frame(), MaterialShape.BLOCK);
    }

    private static Reg.Handle<Block> storage(
            String name,
            Material mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        return storage(name, mat.frame(), hardness, resistance, sound, color);
    }

    private static Reg.Handle<Block> storage(
            String name,
            MaterialTags dict,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        return storage(name, dict, Block::new, hardness, resistance, sound, color);
    }

    private static Reg.Handle<Block> storage(
            String name,
            MaterialTags dict,
            Function<BlockBehaviour.Properties, ? extends Block> factory,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        var block =
                Reg.<Block>block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        return materialBlock(name, block, dict, MaterialShape.BLOCK);
    }

    private static Reg.Handle<Block> storageOutgas(
            String name,
            Material mat,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color,
            Supplier<Block> gas) {
        var block =
                Reg.<Block>block(
                        name,
                        properties -> new OutgasBlock(properties, gas, true),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .randomTicks()
                                        .ignitedByLava());
        return materialBlock(name, block, mat.frame(), MaterialShape.BLOCK);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            HazardBlock.@Nullable ExtDisplayEffect display) {
        return fuelBlock(name, hardness, resistance, display, null, SoundType.METAL);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            HazardBlock.@Nullable ExtDisplayEffect display,
            @Nullable Rarity rarity) {
        return fuelBlock(
                name, hardness, resistance, display, rarity, SoundType.METAL, MapColor.METAL);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            HazardBlock.@Nullable ExtDisplayEffect display,
            @Nullable Rarity rarity,
            SoundType sound) {
        return fuelBlock(name, hardness, resistance, display, rarity, sound, MapColor.METAL);
    }

    private static Reg.Handle<Block> fuelBlock(
            String name,
            float hardness,
            float resistance,
            HazardBlock.@Nullable ExtDisplayEffect display,
            @Nullable Rarity rarity,
            SoundType sound,
            MapColor color) {
        var block =
                Reg.<Block>block(
                        name,
                        properties -> new HazardBlock(properties).setDisplayEffect(display),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new BlockItem(block.get(), properties),
                        () -> {
                            var properties = new Item.Properties().useBlockDescriptionPrefix();
                            if (rarity != null) {
                                properties.rarity(rarity);
                            }
                            return properties;
                        });
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<Block> beaconStorage(
            String name, float hardness, float resistance, SoundType sound, MapColor color) {
        var block =
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
        var block =
                Reg.<Block>block(
                        name,
                        RotatedPillarBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new BlockItem(block.get(), properties),
                        () -> {
                            var properties = new Item.Properties().useBlockDescriptionPrefix();
                            if (rarity != null) {
                                properties.rarity(rarity);
                            }
                            return properties;
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

    private static Reg.Handle<Block> cokeBlock(String name, MaterialTags coke) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5F, 6F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static RegistryHandle<Block> plasticExplosive(String name) {
        var block =
                Reg.<Block>block(
                        name,
                        PlasticExplosiveBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.FIRE)
                                        .strength(2F, 1.2F)
                                        .sound(SoundType.METAL)
                                        .ignitedByLava());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> capBlock(String name) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5F, 6F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<SlabBlock> decoSlab(String name, float hardness, float resistance) {
        return decoSlab(name, hardness, resistance, SoundType.STONE);
    }

    private static Reg.Handle<SlabBlock> decoSlab(
            String name, float hardness, float resistance, SoundType sound) {
        var block = registerSlab(name, hardness, resistance, sound);
        var item = decoItem(name, block);
        DECO_BLOCKS.add(item);
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
        var block = registerStair(name, base, hardness, resistance, sound);
        var item = decoItem(name, block);
        DECO_BLOCKS.add(item);
        return block;
    }

    private static Reg.Handle<SlabBlock> registerSlab(
            String name, float hardness, float resistance, SoundType sound) {
        return Reg.block(
                name,
                SlabBlock::new,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.STONE)
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
                                .mapColor(base.get().defaultMapColor())
                                .strength(hardness, resistance)
                                .sound(sound)
                                .requiresCorrectToolForDrops());
    }

    private static Reg.Handle<Block> decoUnlisted(
            String name, float hardness, float resistance, SoundType sound, MapColor color) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        decoItem(name, block);
        return block;
    }

    private static Reg.Handle<Block> decoNoSpawnBlast(
            String name, float hardness, float resistance) {
        return decoNoSpawnBlast(name, hardness, resistance, MapColor.STONE);
    }

    private static Reg.Handle<Block> decoNoSpawnBlast(
            String name, float hardness, float resistance, MapColor color) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .isValidSpawn(
                                                (_,
                                                        _,
                                                        _,
                                                        _) -> false)
                                        .requiresCorrectToolForDrops());
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new NoSpawnBlastInfoBlockItem(block.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        DECO_BLOCKS.add(item);
        return block;
    }

    private static RegistryHandle<Block> concreteColored(String name, DyeColor color) {
        var block = decoNoSpawnBlast(name, 15.0F, 84.0F, color.getMapColor());
        CONCRETE_COLORED.add(block);
        return block;
    }

    private static RegistryHandle<Block> fallingCubeBlast(
            String name, MapColor color, SoundType sound, float hardness, float resistance) {
        var block =
                Reg.<Block>block(
                        name,
                        BaseFallingBlock::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .sound(sound)
                                        .strength(hardness, resistance)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(blastItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> concreteExt(String name) {
        return decoNoSpawnBlast(name, 15.0F, 84.0F);
    }

    private static Reg.Handle<Block> lightstone(String name) {
        return deco(name, 2.0F, 9.0F);
    }

    private static Reg.Handle<Block> tileLab(String name, boolean randomTick) {
        var block =
                Reg.<Block>block(
                        name,
                        properties ->
                                new OutgasBlock(
                                        properties, () -> ModBlocks.GAS_ASBESTOS.get(), true),
                        () -> {
                            var properties =
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(1.0F, 12.0F)
                                            .sound(SoundType.GLASS)
                                            .requiresCorrectToolForDrops();
                            if (randomTick) {
                                properties.randomTicks();
                            }
                            return properties;
                        });
        DECO_BLOCKS.add(decoItem(name, block));
        return block;
    }

    private static Reg.Handle<Block> meteorDeco(String name) {
        return meteorDeco(name, BlockItem::new);
    }

    private static Reg.Handle<Block> meteorDeco(
            String name, BiFunction<Block, Item.Properties, ? extends Item> itemFactory) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_BLACK)
                                        .strength(15.0F, 216.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        DECO_BLOCKS.add(decoItem(name, block, itemFactory));
        return block;
    }

    private static String sandbagsSuffix(int mask) {
        var suffix = new StringBuilder();
        for (var direction : Direction.Plane.HORIZONTAL) {
            if (SandbagsBlock.set(mask, direction)) {
                suffix.append('_');
                suffix.append(direction.getName());
            }
        }
        return suffix.toString();
    }

    private static String sandbagsState(int mask) {
        var state = new StringBuilder();
        for (var direction : Direction.Plane.HORIZONTAL) {
            if (!state.isEmpty()) {
                state.append(',');
            }
            state.append(direction.getName());
            state.append('=');
            state.append(SandbagsBlock.set(mask, direction));
        }
        return state.toString();
    }

    private static Reg.Handle<ColoredFallingBlock> sandMix(String name, int dust) {
        var block =
                Reg.block(
                        name,
                        properties -> new ColoredFallingBlock(new ColorRGBA(dust), properties),
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .sound(SoundType.SAND)
                                        .strength(0.5F));
        decoItem(name, block);
        return block;
    }

    private static Reg.Handle<FileCabinetBlock> filingCabinet(String name, String texture) {
        return Reg.blockItem(
                name,
                FileCabinetBlock::new,
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(10.0F, 9.0F)
                                .requiresCorrectToolForDrops());
    }

    private static Reg.Handle<Block> decoUnlisted(
            String name, float hardness, float resistance, SoundType sound) {
        return decoUnlisted(name, hardness, resistance, sound, MapColor.STONE);
    }

    private static Reg.Handle<Block> decoCt(String name, float hardness, float resistance) {
        var block =
                Reg.block(
                        name,
                        Block::new,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(hardness, resistance)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops());
        var item =
                Reg.<Item>item(
                        name,
                        properties -> new BlockItem(block.get(), properties),
                        () -> new Item.Properties().useBlockDescriptionPrefix());
        DECO_BLOCKS.add(item);
        return block;
    }

    private static BlockBehaviour.Properties wasteBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static RegistryHandle<VentBlock> vent(
            String name, VentBlock.Plume plume, double spread) {
        return Reg.blockItem(
                name,
                properties -> new VentBlock(properties, plume, spread),
                () ->
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5.0F, 6.0F)
                                .noLootTable()
                                .requiresCorrectToolForDrops());
    }

    private static BlockItemStateProperties levelState(int level) {
        return BlockItemStateProperties.EMPTY.with(SellafieldBlock.LEVEL, level);
    }

    private static BlockBehaviour.Properties volcanoProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 6_000.0F)
                .sound(SoundType.STONE)
                .isValidSpawn(
                        (_,
                                _,
                                _,
                                _) -> false)
                .noLootTable();
    }

    private static RegistryHandle<VolcanoBlock> volcano(String name) {
        Reg.Handle<VolcanoBlock> block =
                Reg.block(name, VolcanoBlock::new, ModBlocks::volcanoProperties);
        Reg.item(
                        name,
                        properties -> new VolcanoBlock.VolcanoBlockItem(block.get(), properties),
                        () ->
                                new Item.Properties()
                                        .useBlockDescriptionPrefix()
                                        .component(
                                                DataComponents.BLOCK_STATE,
                                                BlockItemStateProperties.EMPTY.with(
                                                        VolcanoBlock.MODE,
                                                        VolcanoBlock.Mode.STATIC_ACTIVE)))
                .state(() -> DataComponents.BLOCK_STATE);
        return block;
    }

    private static ItemStack lavaBucket() {
        return new ItemStack(Items.LAVA_BUCKET);
    }

    private static Reg.Handle<Block> structural(String name, float hardness, float resistance) {
        return structural(name, Block::new, hardness, resistance);
    }

    private static Reg.Handle<Block> structural(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            float hardness,
            float resistance) {
        var block =
                Reg.block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
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
        var block =
                Reg.<Block>block(
                        name,
                        factory,
                        () ->
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(hardness, resistance)
                                        .requiresCorrectToolForDrops());
        decoItem(name, block);
        return block;
    }

    private static BlockBehaviour.Properties railProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .noOcclusion()
                .requiresCorrectToolForDrops();
    }
}
