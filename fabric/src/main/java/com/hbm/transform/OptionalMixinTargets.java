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

package com.hbm.transform;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

final class OptionalMixinTargets {
    private OptionalMixinTargets() {}

    static void absent(Predicate<String> installed, Set<String> out) {
        if (!installed.test("ae2"))
            Collections.addAll(
                    out,
                    "appeng.helpers.patternprovider.PatternProviderReturnInventory",
                    "appeng.parts.CableBusContainer",
                    "appeng.util.inv.CombinedInternalInventory");
        if (!installed.test("alexscaves"))
            Collections.addAll(
                    out,
                    "com.github.alexmodguy.alexscaves.server.entity.living.BoundroidEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.CaniacEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.DinosaurEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.ForsakenEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.MagnetronEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity",
                    "com.github.alexmodguy.alexscaves.server.entity.living.UnderzealotEntity");
        if (!installed.test("axiom"))
            Collections.addAll(
                    out,
                    "com.moulberry.axiom.clipboard.ClipboardObject",
                    "com.moulberry.axiom.packets.AxiomServerboundRequestChunkData",
                    "com.moulberry.axiom.packets.AxiomServerboundSetBlock",
                    "com.moulberry.axiom.packets.AxiomServerboundSetBuffer",
                    "com.moulberry.axiom.tools.stamp.TransformedBlockRegions",
                    "com.moulberry.axiom.world_modification.DispatchTask$Block");
        if (!installed.test("bandwidthoptimizer"))
            Collections.addAll(
                    out,
                    "com.PinkCats.bandwidthoptimizer.channel.algorithm.zstd.KineticStreamingLayer",
                    "com.PinkCats.bandwidthoptimizer.integration.minecraft.CustomPayloadPacketCompat",
                    "com.PinkCats.bandwidthoptimizer.report.ChannelTransportPacketRankSourceResolver");
        if (!installed.test("c2me-rewrites-chunk-serializer")
                && !installed.test("c2me_rewrites_chunk_serializer"))
            Collections.addAll(
                    out, "com.ishland.c2me.rewrites.chunk_serializer.common.ChunkDataSerializer");
        if (!installed.test("compact_storage"))
            Collections.addAll(
                    out,
                    "com.witchica.compactstorage.block.entity.base.BaseCompactStorageBlockEntity",
                    "com.witchica.compactstorage.block.entity.base.BaseItemDrumBlockEntity");
        if (!installed.test("create"))
            Collections.addAll(
                    out,
                    "com.zurrtum.create.content.fluids.FlowSource",
                    "com.zurrtum.create.content.fluids.FlowSource$FluidHandler",
                    "com.zurrtum.create.content.fluids.FluidPropagator",
                    "com.zurrtum.create.content.fluids.pump.PumpBlockEntity");
        if (!installed.test("effortlessbuilding"))
            Collections.addAll(
                    out,
                    "nl.requios.effortlessbuilding.network.PacketHandler",
                    "nl.requios.effortlessbuilding.utilities.UndoManager",
                    "nl.requios.effortlessbuilding.utilities.UndoManager$UndoEntry");
        if (!installed.test("entity_model_features"))
            Collections.addAll(
                    out,
                    "traben.entity_model_features.models.animation.math.variables.VariableRegistry");
        if (!installed.test("flashback"))
            Collections.addAll(
                    out,
                    "com.moulberry.flashback.playback.ReplayServer",
                    "com.moulberry.flashback.record.Recorder");
        if (!installed.test("fusion"))
            Collections.addAll(out, "com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl");
        if (!installed.test("iris"))
            Collections.addAll(
                    out,
                    "net.irisshaders.iris.pipeline.IrisRenderingPipeline",
                    "net.irisshaders.iris.pipeline.fallback.ShaderSynthesizer",
                    "net.irisshaders.iris.pipeline.programs.ExtendedShader",
                    "net.irisshaders.iris.pipeline.programs.ShaderCreator",
                    "net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver",
                    "net.irisshaders.iris.vertices.sodium.terrain.XHFPTerrainVertex");
        if (!installed.test("litematica"))
            Collections.addAll(
                    out,
                    "fi.dy.masa.litematica.scheduler.tasks.TaskDeleteBlocksByPlacement",
                    "fi.dy.masa.litematica.scheduler.tasks.TaskFillArea",
                    "fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand",
                    "fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect",
                    "fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic",
                    "fi.dy.masa.litematica.schematic.LitematicaSchematic",
                    "fi.dy.masa.litematica.util.SchematicPlacingUtils",
                    "fi.dy.masa.litematica.util.WorldPlacingUtils",
                    "fi.dy.masa.litematica.world.ChunkSchematic",
                    "fi.dy.masa.litematica.world.WorldSchematic");
        if (!installed.test("lithium"))
            Collections.addAll(out, "net.caffeinemc.mods.lithium.common.world.ChunkLoadTricks");
        if (!installed.test("neb") && !installed.test("neb-fabric"))
            Collections.addAll(
                    out,
                    "cn.ussshenzhou.notenoughbandwidth.aggregation.AggregatedEncodePacket",
                    "cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket",
                    "cn.ussshenzhou.notenoughbandwidth.indextype.NamespaceIndexManager",
                    "cn.ussshenzhou.notenoughbandwidth.zstd.Context",
                    "cn.ussshenzhou.notenoughbandwidth.zstd.ZstdHelper");
        if (!installed.test("neoforge"))
            Collections.addAll(out, "net.neoforged.neoforge.transfer.StacksResourceHandler");
        if (!installed.test("punchy")) Collections.addAll(out, "punchy.config.PunchyConfig");
        if (!installed.test("reborncore"))
            Collections.addAll(
                    out,
                    "reborncore.api.items.InventoryBase",
                    "reborncore.common.blockentity.MachineBaseBlockEntity");
        if (!installed.test("replaymod"))
            Collections.addAll(
                    out,
                    "com.replaymod.replay.FullReplaySender",
                    "com.replaymod.replay.FullReplaySender$2");
        if (!installed.test("roughlyenoughitems"))
            Collections.addAll(
                    out, "me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay");
        if (!installed.test("scalablelux"))
            Collections.addAll(
                    out,
                    "ca.spottedleaf.starlight.common.light.StarLightInterface",
                    "ca.spottedleaf.starlight.common.light.StarLightInterface$ConcurrentLightQueue",
                    "ca.spottedleaf.starlight.common.light.StarLightInterface$SimpleLightQueue",
                    "ca.spottedleaf.starlight.common.light.vanillainterface.ThreadedLevelLightEngineVanillaInterface");
        if (!installed.test("sodium"))
            Collections.addAll(
                    out,
                    "net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline",
                    "net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline",
                    "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer",
                    "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager",
                    "net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer",
                    "net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder$Vertex",
                    "net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext",
                    "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext",
                    "net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl",
                    "net.caffeinemc.mods.sodium.client.world.LevelSlice",
                    "net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks");
        if (!installed.test("sophisticatedcore"))
            Collections.addAll(
                    out,
                    "net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents$InventoryData",
                    "net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents$UpgradeData");
        if (!installed.test("sound_physics_remastered"))
            Collections.addAll(
                    out,
                    "com.sonicether.soundphysics.SoundPhysics",
                    "com.sonicether.soundphysics.utils.RaycastUtils");
        if (!installed.test("storagedrawers"))
            Collections.addAll(
                    out,
                    "com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers",
                    "com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersStandard",
                    "com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.StandardDrawerGroup$DrawerData",
                    "com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData");
        if (!installed.test("techreborn"))
            Collections.addAll(
                    out,
                    "techreborn.blockentity.cable.CableBlockEntity",
                    "techreborn.blockentity.cable.CableTickManager",
                    "techreborn.blockentity.cable.CableTickManager$SortableStorage",
                    "techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity",
                    "techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity$1");
        if (!installed.test("tghindsight"))
            Collections.addAll(out, "com.taylorgibbs.hindsight.client.replay.ReplayViewer");
        if (!installed.test("toms_storage"))
            Collections.addAll(
                    out,
                    "com.tom.storagemod.block.entity.CraftingTerminalBlockEntity",
                    "com.tom.storagemod.block.entity.FilingCabinetBlockEntity",
                    "com.tom.storagemod.util.CraftingMatrix",
                    "com.tom.storagemod.util.FilingCabinetContainer");
        if (!installed.test("worldedit"))
            Collections.addAll(
                    out,
                    "com.sk89q.worldedit.coremc.internal.CoreMcWorld",
                    "com.sk89q.worldedit.coremc.internal.CoreMcWorldNativeAccess",
                    "com.sk89q.worldedit.extent.transform.BlockTransformExtent",
                    "com.sk89q.worldedit.function.operation.Operations");
    }
}
