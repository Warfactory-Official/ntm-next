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

package com.hbm;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blocks.FlammableBlocks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.blocks.network.ChunkDuctNodes;
import com.hbm.blocks.network.GeneratedConductors;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.config.*;
import com.hbm.data.*;
import com.hbm.entity.LoadedEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.train.RailCarBaseEntity;
import com.hbm.explosion.FalloutJournal;
import com.hbm.extprop.ModEntityData;
import com.hbm.handler.*;
import com.hbm.handler.neutron.NeutronHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.radiation.*;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.handler.resistance.DamageResistanceHandler;
import com.hbm.hazard.HazardAssignment;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.TravelersEquipmentHazards;
import com.hbm.hazard.TrinketsHazards;
import com.hbm.inventory.ItemContents;
import com.hbm.inventory.TrinketsAccessories;
import com.hbm.inventory.WornAccessories;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.ingredient.ConsumedComponentPredicate;
import com.hbm.inventory.recipes.ingredient.FluidContentPredicate;
import com.hbm.inventory.recipes.ingredient.UnmodifiedGunPredicate;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModCreativeTabs;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.food.ConsumableTreatment;
import com.hbm.items.food.ConsumableTreatments;
import com.hbm.items.machine.ICFPelletItem;
import com.hbm.items.special.Autogen;
import com.hbm.items.special.BetaFeaturesItem;
import com.hbm.items.tool.RTTYPagerItem;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.logistics.courier.CourierEndpointRegistry;
import com.hbm.main.Polaroid;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncWire;
import com.hbm.particle.HbmParticles;
import com.hbm.platform.Platform;
import com.hbm.platform.PlayerLevelChange;
import com.hbm.platform.ServerEvents;
import com.hbm.potion.HbmPotion;
import com.hbm.registration.Reg;
import com.hbm.registration.Registrar;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.TomSaveData;
import com.hbm.saveddata.satellites.RadiationBursts;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.ModSounds;
import com.hbm.stats.ModStats;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.ChunkUtil;
import com.hbm.world.HbmFeatures;
import com.hbm.world.HbmWorldgen;
import com.hbm.world.gen.nbt.HbmStructureProcessors;
import com.hbm.world.loot.ConfigScaledNumberProvider;
import com.hbm.world.loot.EntityEnchantmentLevel;
import com.hbm.world.ore.OreGeneration;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NuclearTech {
    public static final String MOD_ID = "hbm";
    public static final Logger LOGGER = LoggerFactory.getLogger("NTM");
    private static volatile boolean clientFacades;

    private NuclearTech() {}

    public static void init() {
        LOGGER.info("NTM common init on {}", "NeoForge");
        Polaroid.roll();
        JoinGreeting.register();

        ModDataComponents.registerBlockItemStates();
        load(ModBlocks.class);
        ModDataComponents.register();
        ModStats.register();
        HbmCriteria.register();
        FluidContentPredicate.register();
        ConsumedComponentPredicate.register();
        UnmodifiedGunPredicate.register();
        ModEntityData.register();
        GeneratedConductors.init();
        load(ModItems.class);
        Autogen.register();
        load(ModBlockEntities.class);
        ModEntities.register();
        ModMenus.register();
        ModCreativeTabs.register();
        ModRecipes.register();
        ModSounds.register();
        HbmParticles.register();
        ChunkUtil.register();
        HbmPotion.register();
        HbmStructureTypes.register();
        HbmFeatures.register();
        HbmStructureProcessors.register();
        Registrar.registerLootNumberProviderType(
                "config_scaled", ConfigScaledNumberProvider.MAP_CODEC);
        Registrar.registerLootNumberProviderType(
                "entity_enchantment_level", EntityEnchantmentLevel.MAP_CODEC);
        Registrar.registerSyncedDataPackRegistry(HazardAssignment.REGISTRY, HazardAssignment.CODEC);
        Registrar.registerSyncedDataPackRegistry(HazmatProtection.REGISTRY, HazmatProtection.CODEC);
        Registrar.registerDataPackRegistry(BiomeRadiation.REGISTRY, BiomeRadiation.CODEC);
        Registrar.registerSyncedDataPackRegistry(
                EquipmentProtection.REGISTRY, EquipmentProtection.CODEC);
        Registrar.registerSyncedDataPackRegistry(SuitProtection.REGISTRY, SuitProtection.CODEC);
        Registrar.registerSyncedDataPackRegistry(
                ConsumableTreatment.REGISTRY, ConsumableTreatment.CODEC);
        Registrar.registerDataPackRegistry(
                IrradiationTransformation.REGISTRY, IrradiationTransformation.CODEC);
        Registrar.registerSyncedDataPackRegistry(
                CustomMachineDefinition.REGISTRY, CustomMachineDefinition.CODEC);
        Registrar.registerSyncedDataPackRegistry(MachineData.REGISTRY, MachineConfig.CODEC);
        Registrar.registerSyncedDataPackRegistry(ExplosionData.REGISTRY, DataGroup.CODEC);
        Registrar.registerSyncedDataPackRegistry(MobData.REGISTRY, DataGroup.CODEC);
        Registrar.registerSyncedDataPackRegistry(WorldData.REGISTRY, DataGroup.CODEC);
        Registrar.registerSyncedDataPackRegistry(ItemData.REGISTRY, DataGroup.CODEC);
        Registrar.registerSyncedDataPackRegistry(EnergyData.REGISTRY, DataGroup.CODEC);
        Registrar.registerSyncedDataPackRegistry(RadiationData.REGISTRY, DataGroup.CODEC);
        Registrar.registerDataPackRegistry(RadiationSettings.REGISTRY, RadiationSettings.CODEC);
        Registrar.registerDataPackRegistry(
                RadiationDiffusivity.REGISTRY, RadiationDiffusivity.CODEC);
        Registrar.registerDataPackRegistry(RadSourceRule.REGISTRY, RadSourceRule.CODEC);
        Registrar.registerSyncedDataPackRegistry(
                RadResistanceRule.REGISTRY, RadResistanceRule.CODEC);
        Registrar.registerDataPackRegistry(FalloutConfigJSON.REGISTRY, FalloutConfigJSON.CODEC);
        NtmCapabilities.registerTokens();
        NTMFluids.init();
        LevelNodeGraph.init();
        PowerNetwork.init();
        FluidNetwork.init();
        HazardSystem.init();
        RadiationSystemNT.init();
        RadVisServer.init();
        PollutionHandler.install();
        UnstableFuses.init();
        LoadedEntities.init();
        DamageResistanceHandler.init();
        BossSpawnHandler.init();
        ImpactWorldHandler.init();
        PedestalBlockEntity.init();
        ArmorSuitEffects.init();
        ChunkUtil.init();
        ChunkPublication.init();
        FalloutJournal.init();
        StructureAssemblyBlockEntity.init();
        AssembledMembers.init();
        ResolvedCapCache.init();
        EndpointRegistry.init();
        ChunkDuctNodes.init();
        ComputerCraft.init();
        HbmWorldgen.register();

        final Consumer<MinecraftServer> callback =
                server -> {
                    if (server.getTickCount() % 20 == 0 && HbmConfig.pollForExternalEdit()) {
                        deriveLiveConfigFacades();
                        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT)
                            deriveClientConfigFacades();
                    }
                };
        ServerEvents.TICK_POST.add(callback);

        NeutronHandler.install();

        final Consumer<MinecraftServer> callback2 =
                server -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        AkimboGhost.tick(player);
                    }
                };
        ServerEvents.TICK_POST.add(callback2);

        final Consumer<ServerLevel> callback3 = TomSaveData::publish;
        NeoForge.EVENT_BUS.addListener(
                (LevelEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level2) callback3.accept(level2);
                });
        final Consumer<MinecraftServer> callback4 = server -> TomSaveData.onServerStopping();
        ServerEvents.STOPPING.add(callback4);
        final Consumer<MinecraftServer> callback5 = BetaFeaturesItem::tick;
        ServerEvents.TICK_PRE.add(callback5);
        final Consumer<MinecraftServer> callback6 = RadioSystem::publish;
        ServerEvents.TICK_PRE.add(callback6);
        final Consumer<MinecraftServer> callback7 = CourierEndpointRegistry::preTick;
        ServerEvents.TICK_PRE.add(callback7);
        RadarBlockEntity.registerScanSystem();
        final Consumer<MinecraftServer> callback8 = RadiationBursts::expire;
        ServerEvents.TICK_PRE.add(callback8);
        final Consumer<MinecraftServer> callback9 =
                server -> {
                    for (ServerLevel level : server.getAllLevels()) {
                        SatelliteSavedData data = SatelliteSavedData.get(level);
                        for (var satellite : data.sats.values()) satellite.tick(level);
                    }
                };
        ServerEvents.TICK_PRE.add(callback9);
        final Consumer<MinecraftServer> callback10 = SatelliteRayEvents::tick;
        ServerEvents.TICK_PRE.add(callback10);
        final Consumer<MinecraftServer> callback11 = server -> SatelliteRayEvents.clear();
        ServerEvents.STOPPING.add(callback11);
        final Consumer<MinecraftServer> callback12 = server -> RadiationBursts.clear();
        ServerEvents.STOPPING.add(callback12);
        final Consumer<MinecraftServer> callback13 = server -> CourierEndpointRegistry.clear();
        ServerEvents.STOPPING.add(callback13);
        final Consumer<MinecraftServer> callback14 = RTTYPagerItem::tickInventory;
        ServerEvents.TICK_POST.add(callback14);
        final Consumer<MinecraftServer> callback15 = server -> BeSyncTable.pump();
        ServerEvents.TICK_POST.add(callback15);
        final Consumer<MinecraftServer> callback16 = server -> SyncWire.beforeTick();
        ServerEvents.TICK_PRE.add(callback16);
        final Consumer<MinecraftServer> callback17 = RailCarBaseEntity::updateTrains;
        ServerEvents.TICK_POST.add(callback17);
        final Consumer<ServerPlayer> callback18 =
                player -> {
                    ChunkTrackerIndex.forget(player, player.level());
                    BeSyncTable.forget(player.getUUID());
                };
        ServerEvents.DISCONNECT.add(callback18);
        final Consumer<ServerPlayer> callback19 = ChunkTrackerIndex::respawn;
        ServerEvents.RESPAWN.add(callback19);
        final PlayerLevelChange callback20 =
                (player, origin, destination) -> {
                    ChunkTrackerIndex.forget(player, origin);
                    BeSyncTable.forgetIn(origin, player.getUUID());
                };
        ServerEvents.LEVEL_CHANGE.add(callback20);
        final Consumer<MinecraftServer> callback21 = server -> PacketWire.flushPending();
        ServerEvents.TICK_POST.add(callback21);
    }

    private static void load(Class<?>... holders) {
        for (Class<?> holder : holders) {
            try {
                Class.forName(holder.getName(), true, holder.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "content holder is missing: " + holder.getName(), e);
            }
        }
    }

    public static void deriveConfigFacades() {
        BalanceConfig.loadFrom(((ContentConfig) HbmConfig.CONTENT).store());
        deriveLiveConfigFacades();
        if (clientFacades) deriveClientConfigFacades();
    }

    public static void deriveLiveConfigFacades() {
        ConfigStore runtime = ((RuntimeConfig) HbmConfig.RUNTIME).store();
        RadiationConfig.loadFrom(runtime);
        BombConfig.loadFrom(runtime);
    }

    public static void deriveClientConfigFacades() {
        ConfigStore client = HbmConfig.STORE;
        RenderConfig.loadFrom(client);
        HudConfig.loadFrom(client);
        GunVisualConfig.loadFrom(client);
        InteractionConfig.loadFrom(client);
        AudioConfig.loadFrom(client);
        ConfigSchema.loadAddonClientFacades(client);
        clientFacades = true;
    }

    public static void commonSetup() {
        deriveConfigFacades();
        RadiationSystemNT.onLoadComplete();
        Reg.runAfterRegistration();
        NTMFluids.bake();
        ICFPelletItem.init();
        ItemContents.bootstrap();
        XWeaponModManager.init();
        HazardSystem.registerContent();
        if (Platform.isModLoaded("travelersbackpack")) {
            HazardSystem.addExternalEquipmentApplicator(TravelersEquipmentHazards::apply);
        }
        if (Platform.isModLoaded("trinkets_updated")) {
            HazardSystem.addExternalEquipmentApplicator(TrinketsHazards::apply);
            WornAccessories.register(TrinketsAccessories::collect);
        }
        ModItems.initHazards();
        DispenserBehaviorHandler.init();
    }

    public static void registerFlammability() {
        FlammableBlocks.register();
    }

    public static void serverAboutToStart(MinecraftServer server) {
        assert server.levelKeys().isEmpty();
        DataGroups.applyAll(server.registryAccess());
        PacketWire.onServerStarting(server);
        ModRecipes.bootstrap();
        ItemPool.load(server.registryAccess());
        HazardSystem.applyDataPack(server.registryAccess());
        HazmatRegistry.applyDataPack(server.registryAccess());
        BiomeRadiation.applyDataPack(server.registryAccess());
        EquipmentProtections.applyDataPack(server.registryAccess());
        SuitProtections.applyDataPack(server.registryAccess());
        ConsumableTreatments.applyDataPack(server.registryAccess());
        IrradiationTransformations.applyDataPack(server.registryAccess());
        CustomMachineDefinitions.applyDataPack(server.registryAccess());
        RadiationSystemNT.registerConstantSources(server.registryAccess());
        RadiationSystemNT.registerResistantStates(server.registryAccess());
    }

    public static void serverStopped() {
        PedestalBlockEntity.clearLeases();
        HazmatRegistry.clear();
        BiomeRadiation.clear();
        EquipmentProtections.clear();
        SuitProtections.clear();
        ConsumableTreatments.clear();
        IrradiationTransformations.clear();
        OreGeneration.serverStopped();
        PacketWire.onServerStopped();
        BobmazonOffers.invalidate();
    }
}
